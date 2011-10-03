package com.j2speed.exec.impl;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import com.j2speed.exec.OutputProcessor;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * A line based {@link OutputProcessor}.
 * 
 * @author Alessandro Nistico
 */
public abstract class LineProcessor implements OutputProcessor {
   private static final String platformSeparator = System.getProperty("line.separator");

   @NonNull
   private final String separator;
   private final int separatorLength;
   private int separatorPosition;

   private Charset charset;
   private final StringBuilder builder = new StringBuilder(64);

   protected LineProcessor() {
      this(Charset.forName("US-ASCII"), platformSeparator);
   }

   protected LineProcessor(@NonNull Charset charset, @NonNull String separator) {
      this.charset = charset;
      this.separator = separator;
      separatorLength = separator.length();
   }

   public final void process(byte[] buffer, int length) {
      char[] chars = charset.decode(ByteBuffer.wrap(buffer, 0, length)).array();
      char s = separator.charAt(separatorPosition);
      for (int i = 0, charsCount = chars.length; i < charsCount; i++) {
         char c = chars[i];
         builder.append(c);
         if (c == s) {
            separatorPosition++;
            if (separatorPosition == separatorLength) {
               separatorPosition = 0;
               builder.setLength(builder.length() - separatorLength);
               try {
                  process(builder.toString());
               } finally {
                  builder.setLength(0);
               }
            }
         }
      }
   }
   
   @Override
   public final void done() {
      process(builder.toString());
   }

   protected abstract void process(String line);
}
