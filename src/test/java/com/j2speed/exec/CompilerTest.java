package com.j2speed.exec;

import static com.j2speed.exec.Compiler.*;

import org.junit.Test;

public class CompilerTest {

   @Test(expected = IllegalArgumentException.class)
   public void testCompileNotAnInterface() {
      class NotAnInterface {
      }
      using(NotAnInterface.class).compile();
   }
}
