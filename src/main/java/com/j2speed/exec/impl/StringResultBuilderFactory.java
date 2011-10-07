package com.j2speed.exec.impl;

import com.j2speed.exec.ResultBuilder;

public class StringResultBuilderFactory extends AbstractResultBuilderFactory<String> {

   @Override
   public ResultBuilder<String> create() {
      return new StringResultBuilder();
   }
}
