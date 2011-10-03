package com.j2speed.exec.impl;

import static org.junit.Assert.*;

import java.nio.charset.Charset;

import org.junit.Test;

import com.j2speed.exec.impl.LineProcessor;

public class LineProcessorTest {
   private static final String platformSeparator = System.getProperty("line.separator");

   @Test
   public void testProcessByteArrayInt() {
      final String[] lines = { "Hello", "World!" };

      LineProcessor processor = new LineProcessor() {
         int count=0;
         @Override
         protected void process(String line) {
            assertEquals(lines[count++], line);
         }
      };
      
      byte [] output = (lines[0] + platformSeparator + lines[1]).getBytes(Charset.forName("US-ASCII"));
      
      processor.process(output, output.length);
   }

}
