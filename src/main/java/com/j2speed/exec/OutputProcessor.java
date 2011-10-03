package com.j2speed.exec;

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
      public void process(byte[] buffer, int legth) {
      }

      @Override
      public void done() {
      }
   };

   /**
    * Process the provided data.
    * 
    * @param buffer
    *           the buffer containing the data.
    * @param legth
    *           the length of the data available in the buffer.
    */
   void process(byte[] buffer, int legth);

   /**
    * Invoked when no more output is available.
    */
   void done();
}
