package com.j2speed.exec.impl;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import edu.umd.cs.findbugs.annotations.NonNull;

class StringResultBuilderSupport {
   @NonNull
   private final Charset charset;
   @NonNull
   private final StringBuilder builder = new StringBuilder(64);

   StringResultBuilderSupport() {
      this(Charset.forName("US-ASCII"));
   }

   StringResultBuilderSupport(@NonNull Charset charset) {
      this.charset = charset;
   }

   public final void process(ByteBuffer buffer) {
      builder.append(charset.decode(buffer).array());
   }
   
   public void done() {
   }
   
   int size() {
      return builder.length();
   }
   
   String buildString() {
      return builder.toString();
   }
}
