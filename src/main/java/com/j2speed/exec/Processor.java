package com.j2speed.exec;

import java.nio.ByteBuffer;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Base contract for all processors.
 * 
 * @author alex
 */
public interface Processor {
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
