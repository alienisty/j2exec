/**
 * 
 */
package com.j2speed.exec.impl;

import static com.j2speed.exec.impl.Controller.done;
import static com.j2speed.exec.impl.Controller.pump;
import static com.j2speed.exec.impl.Controller.register;
import static com.j2speed.exec.impl.OutputPump.pump;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.j2speed.exec.Env;
import com.j2speed.exec.ErrorBuilder;
import com.j2speed.exec.ErrorBuilderFactory;
import com.j2speed.exec.ExecutionException;
import com.j2speed.exec.OutputProcessor;
import com.j2speed.exec.ResultBuilder;
import com.j2speed.exec.ResultBuilderFactory;
import com.j2speed.exec.Timeout;
import com.j2speed.exec.WorkingDir;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * This encapsulates the invocation mapping for a specific command
 * 
 * @author Alessandro Nistico
 */
public class MethodInvocationHandler implements InvocationHandler {

   /**
    * Builder for void results. Discards all output and return {@code null}
    */
   private static final ResultBuilder<Void> VOID = new ResultBuilder<Void>() {
      @Override
      public void setProcessInput(OutputStream input) {
      };

      @Override
      public void process(ByteBuffer buffer) {
      }

      @Override
      public void done() {
      }

      @Override
      public Void build() {
         return null;
      }

   };

   private final int normalTermination;
   @NonNull
   private final ProcessBuilder builder;
   @NonNull
   private final Map<String, String> environment;
   @NonNull
   private final Argument[] arguments;
   @CheckForNull
   private OutputProcessor outputProcessor;
   @CheckForNull
   private final ResultBuilderFactory<?> resultBuilderFactory;
   @CheckForNull
   private final ErrorBuilderFactory<?> errorBuilderFactory;

   private long timeout;

   private final int outputProcessorIndex;

   private final int workingDirIndex;

   private final int environmentIndex;

   private final int timeoutIndex;

   public MethodInvocationHandler(@NonNull Method method, long timeout, int normalTermination,
            @CheckForNull ResultBuilderFactory<?> resultBuilderFactory,
            @CheckForNull ErrorBuilderFactory<?> errorBuilderFactory,
            @NonNull ProcessBuilder builder, @NonNull List<Argument> arguments) {

      final int argsCount = arguments.size();
      final Class<?>[] params = method.getParameterTypes();
      final int paramsCount;
      if ((paramsCount = params.length) < argsCount) {
         throw new IllegalArgumentException("Not enough parameters in the method");
      }

      if (resultBuilderFactory != null) {
         if (method.getReturnType() != resultBuilderFactory.getResultType()) {
            throw new IllegalArgumentException("Incompatible result type "
                     + resultBuilderFactory.getResultType() + ", expected "
                     + method.getReturnType());
         }
      }

      int outputProcessorIndex = -1;
      int workingDirIndex = -1;
      int environmentIndex = -1;
      int timeoutIndex = -1;
      Map<String, String> environment = Collections.emptyMap();
      final Annotation[][] annotations = method.getParameterAnnotations();
      for (int i = 0; i < paramsCount; i++) {
         if (OutputProcessor.class.isAssignableFrom(params[i])) {
            if (outputProcessorIndex != -1) {
               throw new IllegalArgumentException("Only one OuputProcessor is allowed");
            }
            outputProcessorIndex = i;
            // If an output processor parameter is used, the return value of the method should
            // be void and the factory should be set to null
            resultBuilderFactory = null;
            if (method.getReturnType() != void.class) {
               throw new IllegalStateException("Method " + method
                        + " has an OutputProcessor parameter and should return void");
            }
         }

         Annotation[] paramAnnotations = annotations[i];
         if (paramAnnotations == null || paramAnnotations.length == 0) {
            continue;
         }

         if (contains(paramAnnotations, WorkingDir.class, Env.class, Timeout.class)) {
            if (!File.class.isAssignableFrom(params[i])) {
               throw new IllegalArgumentException("@WorkingDir requires a parameter type of "
                        + File.class);
            }
            workingDirIndex = i;
         } else if (contains(paramAnnotations, Env.class, WorkingDir.class, Timeout.class)) {
            if (!Map.class.isAssignableFrom(params[i])) {
               throw new IllegalArgumentException("@Env requires a parameter type of " + Map.class
                        + "<String,String>");
            }
            environmentIndex = i;
            environment = builder.environment();
         } else if (contains(paramAnnotations, Timeout.class, WorkingDir.class, Env.class)) {
            if (params[i] != long.class) {
               throw new IllegalArgumentException("@Timeout requires a parameter type of "
                        + long.class);
            }
            timeoutIndex = i;
         }
      }

      this.normalTermination = normalTermination;
      this.timeout = timeout;
      this.builder = builder;
      this.arguments = arguments.toArray(new Argument[argsCount]);
      this.resultBuilderFactory = resultBuilderFactory;
      this.errorBuilderFactory = errorBuilderFactory;
      this.outputProcessorIndex = outputProcessorIndex;
      this.workingDirIndex = workingDirIndex;
      this.environmentIndex = environmentIndex;
      this.environment = environment;
      this.timeoutIndex = timeoutIndex;
   }

   @Override
   public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      final Process process;
      final boolean redirectError;
      final long timeout;
      final OutputProcessor output;
      synchronized (builder) {
         process = start(builder, arguments, args);
         output = outputProcessor;
         redirectError = builder.redirectErrorStream();
         timeout = this.timeout;
      }
      final Watchdog watchdog = register(process, timeout);

      final ErrorBuilder<?> error = processError(process, redirectError);
      final ResultBuilder<?> result = processOutput(process, output);

      watchdog.cancel();
      if (process.exitValue() != normalTermination) {
         throw buildError(error);
      }

      return result.build();
   }

   @NonNull
   Process start(ProcessBuilder builder, Argument[] arguments, @NonNull Object[] args)
            throws IOException {
      final List<String> command = builder.command();
      for (int i = 0, a = 0, count = args.length; i < count; i++) {
         if (notExecutionParameter(args, i)) {
            final Argument argument;
            // We use toString() on the argument value to force an NPE if the value is not provided
            command.set((argument = arguments[a++]).getIndex(), argument.apply(args[i].toString()));
         }
      }

      return builder.start();
   }

   final boolean notExecutionParameter(@NonNull Object[] args, int index) {
      if (index == workingDirIndex) {
         builder.directory((File) args[index]);
      } else if (index == timeoutIndex) {
         timeout = ((Long) args[index]).longValue();
      } else if (index == environmentIndex) {
         setEnvironment(args[index]);
      } else if (index == outputProcessorIndex) {
         outputProcessor = (OutputProcessor) args[index];
      } else {
         return true;
      }
      return false;
   }

   @SuppressWarnings("unchecked")
   final void setEnvironment(Object env) {
      builder.environment().putAll(environment); // reset base
      builder.environment().putAll((Map<String, String>) env); // override with the passed in values
   }

   private ErrorBuilder<? extends Throwable> processError(Process process, boolean redirectError) {
      final ErrorBuilder<? extends Throwable> error;
      if (redirectError) {
         error = null;
      } else {
         if (errorBuilderFactory != null) {
            error = errorBuilderFactory.create();
         } else {
            error = new DefaultErrorBuilder();
         }
         pump(new OutputPump(process, process.getErrorStream(), error));
      }
      return error;
   }

   private ResultBuilder<?> processOutput(Process process, @CheckForNull OutputProcessor output)
            throws InterruptedException {

      ResultBuilder<?> result = VOID;
      if (output == null) {
         if (resultBuilderFactory != null) {
            result = resultBuilderFactory.create();
         }
         output = result;
      }

      try {
         output.setProcessInput(process.getOutputStream());
         pump(process.getInputStream(), output);
      } catch (Throwable th) {
         process.destroy();
      } finally {
         done(process);
      }

      process.waitFor();

      return result;
   }

   private boolean contains(@NonNull Annotation[] annotations, @NonNull Class<?> annotation,
            @NonNull Class<?>... exclusions) {
      boolean found = false;
      boolean foundExclusion = false;
      for (Annotation a : annotations) {
         if (annotation == a.annotationType()) {
            found = true;
         } else {
            for (Class<?> exclusion : exclusions) {
               foundExclusion = foundExclusion || exclusion == a.annotationType();
            }
         }
         if (found && foundExclusion) {
            StringBuilder message = new StringBuilder(64).append('@')
                     .append(annotation.getSimpleName()).append(" cannot be used with any of");
            for (Class<?> exclusion : exclusions) {
               message.append(" @").append(exclusion.getSimpleName());
            }
            throw new IllegalArgumentException(message.toString());
         }
      }
      return found;
   }

   @NonNull
   private static Throwable buildError(@CheckForNull ErrorBuilder<? extends Throwable> error) {
      if (error != null) {
         final Throwable exception = error.build();
         if (exception != null) {
            return exception;
         }
      }
      return new ExecutionException("Unknown error");
   }
}
