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
      int read;
      byte[] buffer = new byte[4096];
      try {
         while ((read = input.read(buffer)) != -1) {
            processor.process(buffer, read);
         }
      } catch (IOException e) {
         return;
      }
   }

}
