/**
 * 
 */
package com.j2speed.exec;

import static com.j2speed.exec.Controller.done;
import static com.j2speed.exec.Controller.register;
import static com.j2speed.exec.Controller.start;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * This encapsulates the invocation mapping for a specific command
 * 
 * @author Alessandro Nistico
 */
public final class ProcessInvocationHandler implements InvocationHandler {

   private final int normalTermination;

   private long timeout;

   @NonNull
   private final ProcessBuilder builder;
   @NonNull
   private final Argument[] arguments;
   @NonNull
   private final int[] paramsTypes;
   @CheckForNull
   private final ResultBuilderFactory<?> resultBuilderFactory;
   @CheckForNull
   private final ErrorBuilderFactory<?> errorBuilderFactory;

   public ProcessInvocationHandler(@NonNull Method method, long timeout,
            @CheckForNull ResultBuilderFactory<?> resultBuilderFactory,
            @CheckForNull ErrorBuilderFactory<?> errorBuilderFactory,
            @NonNull ProcessBuilder builder, @NonNull List<Argument> arguments) {

      final int argsCount = arguments.size();
      final Class<?>[] params = method.getParameterTypes();
      final int paramsCount;

      if ((paramsCount = params.length) < argsCount) {
         throw new IllegalArgumentException("Not enough parameters in the method");
      }

      this.normalTermination = 0;
      this.timeout = timeout;
      this.builder = builder;
      this.arguments = new Argument[argsCount];
      this.paramsTypes = new int[paramsCount];
      this.resultBuilderFactory = resultBuilderFactory;
      this.errorBuilderFactory = errorBuilderFactory;

      mapArguments(arguments, argsCount, params);

      if (resultBuilderFactory == null) {
         if (params.length > argsCount) {
            if (!OutputProcessor.class.isAssignableFrom(params[argsCount])) {
               throw new IllegalArgumentException("Unsupported parameter type " + params[argsCount]);
            }
         }
      } else {
         if (method.getReturnType() != resultBuilderFactory.getResultType()) {
            throw new IllegalArgumentException("Incompatible result type "
                     + resultBuilderFactory.getResultType() + ", expected "
                     + method.getReturnType());
         }
      }
   }

   private void mapArguments(List<Argument> arguments, final int argsCount, final Class<?>[] params) {
      final Iterator<Argument> args = arguments.iterator();
      for (int i = 0; i < argsCount; i++) {
         this.arguments[i] = args.next();
         this.paramsTypes[i] = 0;
         if (OutputProcessor.class.isAssignableFrom(params[i])) {
            throw new IllegalArgumentException("OuputProcessor not allowed as argument");
         }
      }
   }

   /**
    * Sets the working directory for the invocation handler
    * 
    * @param workingDirectory
    */
   public void setWorkingDirectory(@CheckForNull File workingDirectory) {
      builder.directory(workingDirectory);
   }

   public void setRedirectErrorStream(boolean redirect) {
      builder.redirectErrorStream(redirect);
   }

   /**
    * @param timeout
    *           in milliseconds
    */
   public void setTimeout(long timeout) {
      this.timeout = timeout;
   }

   @Override
   public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      Process process;
      OutputProcessor processor = OutputProcessor.SINK;
      synchronized (builder) {
         final List<String> command = builder.command();
         for (int i = 0; i < arguments.length; i++) {
            final Argument argument;
            // We use toString() on the argument value to force an NPE if the value is not provided
            command.set((argument = arguments[i]).getIndex(), argument.apply(args[i].toString()));
         }
         if (resultBuilderFactory != null) {
            processor = resultBuilderFactory.create();
         } else {
            processor = (OutputProcessor) args[arguments.length];
         }
         process = builder.start();
      }

      register(process, timeout);

      ErrorBuilder<? extends Throwable> error = null;
      if (!builder.redirectErrorStream()) {
         if (errorBuilderFactory == null) {
            error = new DefaultErrorBuilder();
         } else {
            error = errorBuilderFactory.create();
         }
         start(new OutputPump(process.getErrorStream(), error));
      }
      try {
         processOutput(process, processor);
      } catch (Throwable th) {
         process.destroy();
      } finally {
         done(process);
      }

      process.waitFor();

      if (process.exitValue() != normalTermination) {
         processError(error);
      }

      if (resultBuilderFactory != null) {
         return ((ResultBuilder<?>) processor).build();
      }

      return null;
   }

   private static void processError(@CheckForNull ErrorBuilder<? extends Throwable> error)
            throws Throwable {
      if (error != null) {
         final Throwable exception = error.build();
         if (exception != null) {
            throw exception;
         }
      }
   }

   private static void processOutput(Process process, OutputProcessor processor) throws IOException {
      InputStream is = process.getInputStream();
      byte[] buffer = new byte[4096];
      int read;
      while ((read = is.read(buffer)) != -1) {
         processor.process(buffer, read);
      }
   }
}
