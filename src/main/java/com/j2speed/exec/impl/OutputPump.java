package com.j2speed.exec.impl;

import static com.j2speed.exec.impl.Controller.kill;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import com.j2speed.exec.Processor;

import edu.umd.cs.findbugs.annotations.NonNull;

class OutputPump implements Runnable {

   private final InputStream input;
   private final Processor processor;
   private final Process process;

   OutputPump(Process process, InputStream input, Processor processor) {
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

   static void pump(@NonNull InputStream input, @NonNull Processor processor)
            throws IOException {
      int read;
      ByteBuffer buffer = ByteBuffer.allocate(4096);
      byte[] buf = buffer.array();
      while ((read = input.read(buf, buffer.position(), buffer.remaining())) != -1) {
         buffer.position(buffer.position() + read);
         buffer.flip();
         try {
            processor.process(buffer);
         } catch (Exception e) {
            // exception are swallowed, but errors should not be caught
         } finally {
            buffer.compact();
         }
      }
   }
}
