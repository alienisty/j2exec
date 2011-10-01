package com.j2speed.exec.impl;

import static com.j2speed.exec.impl.Controller.done;

import java.io.IOException;
import java.io.InputStream;

import com.j2speed.exec.ErrorBuilder;
import com.j2speed.exec.ExecutionException;
import com.j2speed.exec.OutputProcessor;
import com.j2speed.exec.ResultBuilder;
import com.j2speed.exec.ResultBuilderFactory;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;

abstract class InvocationUtils {

   private InvocationUtils() {
   }

   @NonNull
   static ResultBuilder<?> processOutput(Object[] args, Process process, @CheckForNull ResultBuilderFactory<?> resultBuilderFactory)
            throws InterruptedException {

      final ResultBuilder<?> result;
      if (resultBuilderFactory != null) {
         result = resultBuilderFactory.create();
      } else {
         result = ResultBuilder.VOID;
      }

      try {
         pump(process.getInputStream(), result);
      } catch (Throwable th) {
         process.destroy();
      } finally {
         done(process);
      }

      process.waitFor();
      
      return result;
   }

   static void pump(@NonNull InputStream input, @NonNull OutputProcessor processor) throws IOException {
      int read;
      byte[] buffer = new byte[4096];
      while ((read = input.read(buffer)) != -1) {
         processor.process(buffer, read);
      }
   }

   @NonNull
   static Throwable buildError(@CheckForNull ErrorBuilder<? extends Throwable> error) {
      if (error != null) {
         final Throwable exception = error.build();
         if (exception != null) {
            return exception;
         }
      }
      return new ExecutionException("Unknown error");
   }

}
