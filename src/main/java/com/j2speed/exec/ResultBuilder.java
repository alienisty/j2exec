package com.j2speed.exec;

import edu.umd.cs.findbugs.annotations.CheckForNull;

/**
 * Allows to build a result by processing of the process output.
 * 
 * @author Alessandro Nistico
 * 
 * @param <T>
 */
public interface ResultBuilder<T> extends OutputProcessor {
   /**
    * Builds the result. This is invoked by the framework after the process has ended.
    * 
    * @return the created result or {@code null}
    */
   @CheckForNull
   T build();
}
