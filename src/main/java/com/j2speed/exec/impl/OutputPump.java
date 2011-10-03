package com.j2speed.exec.impl;

import static com.j2speed.exec.impl.Controller.kill;

import java.io.IOException;
import java.io.InputStream;

import com.j2speed.exec.OutputProcessor;

import edu.umd.cs.findbugs.annotations.NonNull;

class OutputPump implements Runnable {

   private final InputStream input;
   private final OutputProcessor processor;
   private final Process process;

   OutputPump(Process process, InputStream input, OutputProcessor processor) {
      this.input = input;
      this.processor = processor;
      this.process = process;
   }

   @Override
   public void run() {
      try {
         pump(input, processor);
      } catch (IOException e) {
         kill(process);
      }
   }

   static void pump(@NonNull InputStream input, @NonNull OutputProcessor processor)
            throws IOException {
      int read;
      byte[] buffer = new byte[4096];
      while ((read = input.read(buffer)) != -1) {
         try {
            processor.process(buffer, read);
         } catch (Exception e) {
            // exception are swallowed, but errors should not be caought
         }
      }
   }
}
