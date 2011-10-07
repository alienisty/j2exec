package com.j2speed.exec.impl;

import java.nio.charset.Charset;

import com.j2speed.exec.ErrorBuilder;
import com.j2speed.exec.ExecutionException;

public final class DefaultErrorBuilder extends StringResultBuilderSupport implements ErrorBuilder<ExecutionException> {
   public DefaultErrorBuilder() {
   }

   public DefaultErrorBuilder(Charset charset) {
      super(charset);
   }

   @Override
   public ExecutionException build() {
      if (size() > 0) {
         return new ExecutionException(buildString());
      }
      return null;
   }
}
