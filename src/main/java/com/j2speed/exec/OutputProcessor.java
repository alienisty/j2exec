package com.j2speed.exec;

import java.nio.ByteBuffer;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Allows to process output from a process.
 * 
 * @author Alessandro Nistico
 */
public interface OutputProcessor {
   /**
    * The sink implementation, discards all generated output.
    */
   OutputProcessor SINK = new OutputProcessor() {
      @Override
      public void process(ByteBuffer buffer) {
      }

      @Override
      public void done() {
      }
   };

   /**
    * Process the provided data.
    * 
    * @param buffer
    *           the buffer containing the data. Note that the buffer is passed already flipped.
    */
   void process(@NonNull ByteBuffer buffer);

   /**
    * Invoked when no more output is available.
    */
   void done();
}
