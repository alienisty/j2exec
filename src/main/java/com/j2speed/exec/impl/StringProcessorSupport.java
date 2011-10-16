package com.j2speed.exec.impl;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Accumulates a {@link String} from the output.
 * 
 * @author alex
 */
public class StringProcessorSupport {
   @NonNull
   private final Charset charset;
   @NonNull
   private final StringBuilder builder = new StringBuilder(64);

   /**
    * A processor that uses the US-ASCII encoding to decode characters from the output byte stream.
    */
   public StringProcessorSupport() {
      this(Charset.forName("US-ASCII"));
   }

   public StringProcessorSupport(@NonNull Charset charset) {
      this.charset = charset;
   }

   public final void process(ByteBuffer buffer) {
      builder.append(charset.decode(buffer).array());
   }

   /**
    * Clears and reset the content of the processor.
    */
   public void reset() {
      builder.delete(0, builder.length());
   }

   /**
    * @return the current length of the building string.
    */
   public int length() {
      return builder.length();
   }

   public String buildString() {
      return builder.toString();
   }
}
