package com.j2speed.exec.impl;

import static com.j2speed.exec.impl.Compiler.using;

import java.io.File;
import java.util.Map;

import org.junit.Test;

import com.j2speed.exec.Env;
import com.j2speed.exec.ErrorBuilderFactory;
import com.j2speed.exec.ErrorFactory;
import com.j2speed.exec.ResultFactory;
import com.j2speed.exec.Run;
import com.j2speed.exec.Timeout;
import com.j2speed.exec.WorkingDir;
import com.j2speed.exec.impl.AbstractResultBuilderFactory;

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

   @Test(expected = InstantiationException.class)
   public void testAbstractResultFactory() throws Throwable {
      try {
         using(TestInterface2.class);
      } catch (RuntimeException e) {
         throw e.getCause();
      }
   }

   @Test(expected = InstantiationException.class)
   public void testAbstractErrorFactory() throws Throwable {
      try {
         using(TestInterface3.class);
      } catch (RuntimeException e) {
         throw e.getCause();
      }
   }

   @Test(expected = IllegalArgumentException.class)
   public void testThatWorkingDirExcludesEnv() {
      using(WorkingDirExcludesEnv.class).compile();
   }

   interface WorkingDirExcludesEnv {
      @Run("cmd")
      void m(@WorkingDir @Env File dir);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testThatWorkingDirExcludesTimeout() {
      using(WorkingDirExcludesTimeout.class).compile();
   }

   interface WorkingDirExcludesTimeout {
      @Run("cmd")
      void m(@WorkingDir @Timeout File dir);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testThatWorkingDirRequiresFile() {
      using(WorkingDirRequiresFile.class).compile();
   }

   interface WorkingDirRequiresFile {
      @Run("cmd")
      void m(@WorkingDir String dir);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testThatEnvExcludesWorkingDir() {
      using(EnvExcludesWorkingDir.class).compile();
   }

   interface EnvExcludesWorkingDir {
      @Run("cmd")
      void m(@Env @WorkingDir Map<String, String> env);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testThatEnvExcludesTimeout() {
      using(EnvExcludesTimeout.class).compile();
   }

   interface EnvExcludesTimeout {
      @Run("cmd")
      void m(@Env @Timeout Map<String, String> env);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testThatEnvRequiresMap() {
      using(EnvRequiresMap.class).compile();
   }

   interface EnvRequiresMap {
      @Run("cmd")
      void m(@Env String env);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testThatTimeoutExcludesWorkingDir() {
      using(TimeoutExcludesWorkingDir.class).compile();
   }

   interface TimeoutExcludesWorkingDir {
      @Run("cmd")
      void m(@Timeout @WorkingDir long timeout);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testThatTimeoutExcludesEnv() {
      using(TimeoutExcludesEnv.class).compile();
   }

   interface TimeoutExcludesEnv {
      @Run("cmd")
      void m(@Timeout @Env long timeout);
   }

   @Test(expected = IllegalArgumentException.class)
   public void testThatTimeoutRequiresPrimitiveLong() {
      using(TimeoutRequiresPrimitiveLong.class).compile();
   }

   interface TimeoutRequiresPrimitiveLong {
      @Run("cmd")
      void m(@Timeout Long timeout);
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
