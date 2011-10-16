package com.j2speed.exec.impl;

import java.io.OutputStream;

import com.j2speed.exec.OutputProcessor;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;

public abstract class BaseOutputProcessor implements OutputProcessor {

   @CheckForNull
   private OutputStream processInput;

   @Override
   public final void setProcessInput(OutputStream input) {
      this.processInput = input;
   }

   @NonNull
   public final OutputStream processInput() {
      if (processInput == null) {
         throw new IllegalStateException("Process not running");
      }
      return processInput;
   }

   @Override
   public void done() {
   }
}
