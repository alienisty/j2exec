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
import java.util.List;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * This encapsulates the invocation mapping for a specific command
 * 
 * @author Alessandro Nistico
 */
public final class Command {
   private final int normalTermination;
   @NonNull
   private final ProcessBuilder builder;
   @NonNull
   private final Argument[] arguments;

   public Command(@NonNull ProcessBuilder builder, @NonNull List<Argument> arguments) {
      this.normalTermination = 0;
      this.builder = builder;
      this.arguments = arguments.toArray(new Argument[arguments.size()]);
   }

   /**
    * Sets the working directory for the invocation handler
    * 
    * @param workingDirectory
    */
   public void setWorkingDirectory(@CheckForNull File workingDirectory) {
      builder.directory(workingDirectory);
   }

   public void setRedirectError(boolean redirect) {
      builder.redirectErrorStream(redirect);
   }

   public <T> T run(@NonNull ResultBuilder<T> resultBuilder, Object... args) throws Throwable {
      Process process;
      ErrorBuilder<? extends Throwable> error = new DefaultErrorBuilder();
      OutputProcessor processor = resultBuilder;
      synchronized (builder) {
         final List<String> command = builder.command();
         for (int i = 0; i < arguments.length; i++) {
            final Argument argument;
            // We use toString() on the argument value to force an NPE if the value is not provided
            command.set((argument = arguments[i]).getIndex(), argument.apply(args[i].toString()));
         }
         process = builder.start();
      }

      register(process);

      if (!builder.redirectErrorStream()) {
         start(new OutputPump(process.getErrorStream(), error));
      }
      // TODO set up watchdog?
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
      return resultBuilder.build();
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

   private static void cleanUp(Process process) throws InterruptedException {
      process.waitFor();
   }
}
