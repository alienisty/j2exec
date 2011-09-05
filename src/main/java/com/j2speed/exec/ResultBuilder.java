package com.j2speed.exec;

import edu.umd.cs.findbugs.annotations.CheckForNull;

public interface ResultBuilder<T> extends OutputProcessor {
   @CheckForNull
   T build();
}
