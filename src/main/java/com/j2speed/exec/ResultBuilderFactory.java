package com.j2speed.exec;

import edu.umd.cs.findbugs.annotations.NonNull;

public interface ResultBuilderFactory<T> {
   
   @NonNull
   ResultBuilder<T> create();

   @NonNull
   Class<? extends T> getResultType();
}
