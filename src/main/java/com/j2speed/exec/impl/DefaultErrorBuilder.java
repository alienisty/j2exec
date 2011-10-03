package com.j2speed.exec.impl;

import java.io.ByteArrayOutputStream;

import com.j2speed.exec.ErrorBuilder;
import com.j2speed.exec.ExecutionException;

public final class DefaultErrorBuilder implements ErrorBuilder<ExecutionException> {

   private final ByteArrayOutputStream builder = new ByteArrayOutputStream();

   @Override
   public ExecutionException build() {
      if (builder.size() > 0) {
         return new ExecutionException(builder.toString());
      }
      return null;
   }

   @Override
   public void process(byte[] buffer, int legth) {
      builder.write(buffer, 0, legth);
   }

   @Override
   public void done() {
   }
}
