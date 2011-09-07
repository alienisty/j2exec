/**
 * 
 */
package com.j2speed.exec;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * This encapsulates the invocation mapping for a specific command
 * 
 * @author Alessandro Nistico
 */
public final class ProcessInvocationHandler implements InvocationHandler {
   private static final int OUTPUT_PROCESSOR = 1;
   private static final int ERROR_BUILDER = 2;
   @NonNull
   private final ProcessBuilder builder;
   @NonNull
   private final Argument[] arguments;
   @NonNull
   private final int[] paramsTypes;

   public ProcessInvocationHandler(@NonNull Method method, @NonNull ProcessBuilder builder,
            @NonNull List<Argument> arguments) {

      final int argsCount = arguments.size();
      final Class<?>[] params = method.getParameterTypes();
      final int paramsCount;
      if ((paramsCount = params.length) < argsCount) {
         throw new IllegalArgumentException("Not enough parameters in the method");
      }

      this.builder = builder;

      this.arguments = new Argument[argsCount];
      this.paramsTypes = new int[paramsCount];

      mapArguments(arguments, argsCount, params);

      mapProcessors(argsCount, params, paramsCount);
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

   private void mapProcessors(final int argsCount, final Class<?>[] params, final int paramsCount) {
      boolean errorBuilderSelected = false;
      boolean outputProcessorSelected = false;
      for (int i = argsCount; i < paramsCount; i++) {
         final Class<?> parameter = params[i];
         if (ErrorBuilder.class.isAssignableFrom(parameter)) {
            if (errorBuilderSelected) {
               throw new IllegalArgumentException("Only one error builder is allowed");
            }
            errorBuilderSelected = true;
            paramsTypes[i] = ERROR_BUILDER;
         } else if (OutputProcessor.class.isAssignableFrom(parameter)) {
            if (outputProcessorSelected) {
               throw new IllegalArgumentException("Only one output processor is allowed");
            }
            outputProcessorSelected = true;
            paramsTypes[i] = OUTPUT_PROCESSOR;
         } else {
            throw new IllegalArgumentException("Unsupported parameter type " + parameter);
         }
      }
   }

   @Override
   public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      Process process;
      ErrorBuilder<?> error = null;
      OutputProcessor processor = OutputProcessor.SINK;
      synchronized (builder) {
         final List<String> command = builder.command();
         for (int i = 0; i < arguments.length; i++) {
            final Argument argument;
            // We use toString() on the argument value to force an NPE if the value is not provided
            command.set((argument = arguments[i]).getIndex(), argument.apply(args[i].toString()));
         }
         for (int i = arguments.length; i < args.length; i++) {
            switch (paramsTypes[i]) {
            case OUTPUT_PROCESSOR:
               processor = (OutputProcessor) args[i];
               break;
            case ERROR_BUILDER:
               error = (ErrorBuilder<?>) args[i];
               break;
            default:
               throw new IllegalArgumentException("Unexpected value " + args[i]);
            }
         }
         process = builder.start();
      }

      Controller.register(process);

      if (!builder.redirectErrorStream()) {
         if (error == null) {
            error = new DefaultErrorBuilder();
         }
         Controller.start(new OutputPump(process.getErrorStream(), error));
      }
      // TODO set up watchdog?
      try {
         processOutput(process, processor);
      } finally {
         cleanUp(process);
      }
      if (processor instanceof ResultBuilder<?>) {
         return ((ResultBuilder<?>) processor).build();
      }
      return null;
   }

   private static void processOutput(Process process, OutputProcessor processor) throws IOException {
      InputStream is = process.getInputStream();
      byte[] buffer = new byte[4096];
      int read;
      while ((read = is.read(buffer)) != -1) {
         processor.process(buffer, read);
      }
   }

   private static void cleanUp(Process process) throws InterruptedException {
      process.waitFor();
   }
}
