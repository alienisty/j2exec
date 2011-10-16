package com.j2speed.exec.impl;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import com.j2speed.exec.OutputProcessor;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * An {@link OutputProcessor} that accumulates a {@link String} from the output.
 * 
 * @author alex
 */
public class StringOuputProcessor extends BaseOutputProcessor {
   private final StringProcessorSupport processor;

   /**
    * A processor that uses the US-ASCII encoding to decode characters from the output byte stream.
    */
   public StringOuputProcessor() {
      this(Charset.forName("US-ASCII"));
   }

   public StringOuputProcessor(@NonNull Charset charset) {
      this.processor = new StringProcessorSupport(charset);
   }

   public final void process(ByteBuffer buffer) {
      processor.process(buffer);
   }

   /**
    * Clears and reset the content of the processor.
    */
   public final void reset() {
      processor.reset();
   }

   /**
    * @return the current length of the building string.
    */
   public final int length() {
      return processor.length();
   }

   public final String buildString() {
      return processor.buildString();
   }
}
