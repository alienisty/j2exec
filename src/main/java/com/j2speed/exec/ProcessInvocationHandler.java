/**
 * 
 */
package com.j2speed.exec;

import static com.j2speed.exec.Controller.done;
import static com.j2speed.exec.Controller.pump;
import static com.j2speed.exec.Controller.register;

import java.io.File;
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
   @NonNull
   private final ProcessBuilder builder;
   @NonNull
   private final Argument[] arguments;
   @CheckForNull
   private final ResultBuilderFactory<?> resultBuilderFactory;
   @CheckForNull
   private final ErrorBuilderFactory<?> errorBuilderFactory;

   private long timeout;

   public ProcessInvocationHandler(@NonNull Method method, long timeout, int normalTermination,
            @CheckForNull ResultBuilderFactory<?> resultBuilderFactory,
            @CheckForNull ErrorBuilderFactory<?> errorBuilderFactory,
            @NonNull ProcessBuilder builder, @NonNull List<Argument> arguments) {

      final int argsCount = arguments.size();
      final Class<?>[] params = method.getParameterTypes();

      if (params.length < argsCount) {
         throw new IllegalArgumentException("Not enough parameters in the method");
      }

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

      this.normalTermination = normalTermination;
      this.timeout = timeout;
      this.builder = builder;
      this.arguments = new Argument[argsCount];
      this.resultBuilderFactory = resultBuilderFactory;
      this.errorBuilderFactory = errorBuilderFactory;

      mapArguments(arguments, argsCount, params);
   }

   private void mapArguments(List<Argument> arguments, final int argsCount, final Class<?>[] params) {
      final Iterator<Argument> args = arguments.iterator();
      for (int i = 0; i < argsCount; i++) {
         this.arguments[i] = args.next();
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
      synchronized (builder) {
         final List<String> command = builder.command();
         for (int i = 0; i < arguments.length; i++) {
            final Argument argument;
            // We use toString() on the argument value to force an NPE if the value is not provided
            command.set((argument = arguments[i]).getIndex(), argument.apply(args[i].toString()));
         }
         process = builder.start();
      }

      final Watchdog watchdog = register(process, timeout);

      final ErrorBuilder<? extends Throwable> error;
      if (!builder.redirectErrorStream()) {
         if (errorBuilderFactory != null) {
            error = errorBuilderFactory.create();
         } else {
            error = new DefaultErrorBuilder();
         }
         pump(process.getErrorStream(), error);
      } else {
         error = null;
      }

      final ResultBuilder<?> result = processOutput(args, process);
      
      watchdog.cancel();
      
      if (process.exitValue() != normalTermination) {
         throw processError(error);
      }

      return result.build();
   }

   private ResultBuilder<?> processOutput(Object[] args, Process process)
            throws InterruptedException {

      final OutputProcessor processor;
      final ResultBuilder<?> result;
      if (resultBuilderFactory != null) {
         processor = result = resultBuilderFactory.create();
      } else if (args != null && args.length > arguments.length) {
         processor = (OutputProcessor) args[arguments.length];
         result = ResultBuilder.VOID;
      } else {
         processor = OutputProcessor.SINK;
         result = ResultBuilder.VOID;
      }

      try {
         OutputPump.pump(process.getInputStream(), processor);
      } catch (Throwable th) {
         process.destroy();
      } finally {
         done(process);
      }

      process.waitFor();

      return result;
   }

   @NonNull
   private static Throwable processError(@CheckForNull ErrorBuilder<? extends Throwable> error) {
      if (error != null) {
         final Throwable exception = error.build();
         if (exception != null) {
            return exception;
         }
      }
      return new ExecutionException("Unknown error");
   }
}
