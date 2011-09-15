package com.j2speed.exec;

import static com.j2speed.exec.CommandCompiler.parse;

import org.junit.Test;

public class CommandCompilerTest {

   @Test(expected = IllegalArgumentException.class)
   public void testCompileNotAnInterface() {
      class NotAnInterface {
      }
      parse("anything").compile(NotAnInterface.class);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testCompileTooManyMethods() {
      parse("anything").compile(TooManyMethods.class);
   }


   interface TooManyMethods {
      void method1();

      void method2();
   }
}
