package com.j2speed.exec;

import java.io.IOException;
import java.io.InputStream;

public class OutputPump implements Runnable {

   private final InputStream input;
   private final OutputProcessor processor;

   public OutputPump(InputStream input, OutputProcessor processor) {
      this.input = input;
      this.processor = processor;
   }

   @Override
   public void run() {
      try {
         pump(input, processor);
      } catch (IOException e) {
      }
   }

   public static void pump(InputStream input, OutputProcessor processor) throws IOException {
      int read;
      byte[] buffer = new byte[4096];
      while ((read = input.read(buffer)) != -1) {
         processor.process(buffer, read);
      }
   }

}
