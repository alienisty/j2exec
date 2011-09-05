package com.j2speed.exec;

public class DefaultErrorBuilder implements ErrorBuilder<ExecutionException> {
   private final StringBuilder builder = new StringBuilder(128);

   @Override
   public ExecutionException build() {
      return new ExecutionException(builder.toString());
   }

   @Override
   public void process(byte[] buffer, int legth) {
   }

}
