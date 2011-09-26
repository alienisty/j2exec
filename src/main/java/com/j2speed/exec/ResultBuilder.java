package com.j2speed.exec;

import edu.umd.cs.findbugs.annotations.CheckForNull;

public interface ResultBuilder<T> extends OutputProcessor {
   
   ResultBuilder<Void> VOID = new ResultBuilder<Void>() {
      @Override
      public Void build() {
         return null;
      }

      @Override
      public void process(byte[] buffer, int legth) {
      }
   };

   @CheckForNull
   T build();
}
