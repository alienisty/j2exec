package com.j2speed.exec;

import java.nio.ByteBuffer;

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
    * Builder for void results. Discards all output and return {@code null}
    */
   ResultBuilder<Void> VOID = new ResultBuilder<Void>() {
      @Override
      public Void build() {
         return null;
      }

      @Override
      public void process(ByteBuffer buffer) {
      }

      public void done() {
      }
   };

   /**
    * Builds the result. This is invoked by the framework after the process has ended.
    * 
    * @return the created result or {@code null}
    */
   @CheckForNull
   T build();
}
