package com.j2speed.exec;
import static org.junit.Assert.*;
import static com.j2speed.exec.Compiler.*;

import org.junit.Test;

public class CompilerTest {

   @Test(expected = IllegalArgumentException.class)
   public void testCompileNotAnInterface() {
      class NotAnInterface {
      }
      using(NotAnInterface.class).compile();
   }

   @Test(expected = IllegalArgumentException.class)
   public void testUnknownMethod() {
      using(TestInterface.class).on("wrongMethod");
   }

   @Test(expected = IllegalStateException.class)
   public void testUnspecifiedCommandForMethod() {
      using(TestInterface.class).compile();
   }

   @Test(expected = RuntimeException.class)
   public void testSyntaxError1() {
      using(TestInterface.class).on("testMethod").run("cmd {}").compile();
   }

   @Test(expected = RuntimeException.class)
   public void testSyntaxError2() {
      using(TestInterface.class).on("testMethod").run("cmd {!}").compile();
   }

   @Test(expected = RuntimeException.class)
   public void testSyntaxError3() {
      using(TestInterface.class).on("testMethod").run("cmd {? -").compile();
   }

   @Test(expected = RuntimeException.class)
   public void testSyntaxError4() {
      using(TestInterface.class).on("testMethod").run("cmd {? }").compile();
   }

   @Test(expected = RuntimeException.class)
   public void testSyntaxError5() {
      using(TestInterface.class).on("testMethod").run("cmd { ?}").compile();
   }
   
   public void testUninstantiableResultFactory() {
      fail();
   }

   interface TestInterface {
      void testMethod();
   }
}
