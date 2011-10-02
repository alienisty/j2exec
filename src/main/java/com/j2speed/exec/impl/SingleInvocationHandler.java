/**
 * 
 */
package com.j2speed.exec.impl;

import static com.j2speed.exec.impl.Controller.pump;
import static com.j2speed.exec.impl.Controller.register;
import static com.j2speed.exec.impl.InvocationUtils.buildError;
import static com.j2speed.exec.impl.InvocationUtils.processOutput;
import static java.util.Collections.emptyMap;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import com.j2speed.exec.Env;
import com.j2speed.exec.ErrorBuilder;
import com.j2speed.exec.ErrorBuilderFactory;
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
public class SingleInvocationHandler implements InvocationHandler {

   private final int normalTermination;
   @NonNull
   private final ProcessBuilder builder;
   @NonNull
   private Map<String, String> environment;
   @NonNull
   private final Argument[] arguments;
   @CheckForNull
   private ResultBuilderFactory<?> resultBuilderFactory;
   @CheckForNull
   private final ErrorBuilderFactory<?> errorBuilderFactory;

   long timeout;

   private int outputProcessorIndex = -1;

   int workingDirIndex = -1;

   int environmentIndex = -1;

   int timeoutIndex = -1;

   public SingleInvocationHandler(@NonNull Method method, long timeout, int normalTermination,
            @CheckForNull ResultBuilderFactory<?> resultBuilderFactory,
            @CheckForNull ErrorBuilderFactory<?> errorBuilderFactory,
            @NonNull ProcessBuilder builder, @NonNull List<Argument> arguments) {

      final int argsCount = arguments.size();
      final Class<?>[] params = method.getParameterTypes();

      if (params.length < argsCount) {
         throw new IllegalArgumentException("Not enough parameters in the method");
      }

      if (resultBuilderFactory != null) {
         if (method.getReturnType() != resultBuilderFactory.getResultType()) {
            throw new IllegalArgumentException("Incompatible result type "
                     + resultBuilderFactory.getResultType() + ", expected "
                     + method.getReturnType());
         }
      }

      this.normalTermination = normalTermination;
      this.timeout = timeout;
      this.builder = builder;
      this.environment = emptyMap();
      this.arguments = arguments.toArray(new Argument[argsCount]);
      this.resultBuilderFactory = resultBuilderFactory;
      this.errorBuilderFactory = errorBuilderFactory;

      parseParameters(method);
   }

   @Override
   public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      Process process;
      boolean redirectError;
      long timeout;
      synchronized (builder) {
         process = start(builder, arguments, args);
         redirectError = builder.redirectErrorStream();
         timeout = this.timeout;
      }
      final Watchdog watchdog = register(process, timeout);

      final ErrorBuilder<?> error = processError(process, redirectError);
      final ResultBuilder<?> result = processOutput(args, process, resultBuilderFactory);

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
         pump(new OutputPump(process.getErrorStream(), error));
      }
      return error;
   }

   private void parseParameters(final Method method) {
      final Class<?>[] params = method.getParameterTypes();
      final Annotation[][] annotations = method.getParameterAnnotations();
      for (int i = 0, argsCount = params.length; i < argsCount; i++) {
         if (OutputProcessor.class.isAssignableFrom(params[i])) {
            if (outputProcessorIndex != -1) {
               throw new IllegalArgumentException("Only one OuputProcessor is allowed");
            }
            outputProcessorIndex = i;
            // If an output processor parameter is used, the return value of the method should
            // be void and the factory should be set to null
            resultBuilderFactory = null;
            if (method.getReturnType() != Void.class) {
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

   final boolean notExecutionParameter(@NonNull Object[] args, int index) {
      if (index == workingDirIndex) {
         builder.directory((File) args[index]);
      } else if (index == timeoutIndex) {
         timeout = ((Long) args[index]).longValue();
      } else if (index == environmentIndex) {
         setEnvironment(args[index]);
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
}
