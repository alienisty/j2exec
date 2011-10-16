package com.j2speed.exec.impl;

import java.nio.charset.Charset;

import com.j2speed.exec.ResultBuilder;

import edu.umd.cs.findbugs.annotations.NonNull;

public class StringResultBuilder extends StringOuputProcessor implements
         ResultBuilder<String> {

   public StringResultBuilder() {
      super();
   }

   public StringResultBuilder(@NonNull Charset charset) {
      super(charset);
   }
   
   @Override
   public String build() {
      return buildString();
   }
}
