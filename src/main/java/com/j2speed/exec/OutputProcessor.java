package com.j2speed.exec;

import java.io.OutputStream;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Allows to process output from a process.
 * 
 * @author Alessandro Nistico
 */
public interface OutputProcessor extends Processor {
   /**
    * Set the output stream connected to the normal input of the process.
    * <p>
    * This method is invoked by the framework before starting processing the output from the running
    * process.
    * <p>
    * {@link OutputProcessor} implementations can use the {@link OutputStream} provided to write
    * data to the process if necessary.
    * 
    * @param input
    */
   void setProcessInput(@NonNull OutputStream input);
}
