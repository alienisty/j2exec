package com.j2speed.exec.impl;

import static com.j2speed.exec.impl.InvocationUtils.pump;

import java.io.IOException;
import java.io.InputStream;

import com.j2speed.exec.OutputProcessor;

public class OutputPump implements Runnable {

   private final InputStream input;
   private final OutputProcessor processor;

   public OutputPump(InputStream input, OutputProcessor processor) {
      this.input = input;
      this.processor = processor;
   }

   @Override
   public void run() {
      try {
         pump(input, processor);
      } catch (IOException e) {
      }
   }
}
