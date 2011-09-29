package com.j2speed.exec;

import static com.j2speed.exec.Compiler.using;

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

   @Test(expected=InstantiationException.class)
   public void testUninstantiableResultFactory() throws Throwable {
      try {
         using(TestInterface2.class);
      } catch (RuntimeException e) {
         throw e.getCause();
      }
   }

   @Test(expected=InstantiationException.class)
   public void testUninstantiableErrorFactory() throws Throwable {
      try {
         using(TestInterface3.class);
      } catch (RuntimeException e) {
         throw e.getCause();
      }
   }

   interface TestInterface {
      void testMethod();
   }

   interface TestInterface2 {
      @ResultFactory(BadResult.class)
      String testMethod();
   }

   interface TestInterface3 {
      @ErrorFactory(BadError.class)
      void testMethod();
   }

   public static abstract class BadResult extends AbstractResultBuilderFactory<String> {
   }
   public static abstract class BadError implements ErrorBuilderFactory<Throwable> {
   }
}
