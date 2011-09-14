package com.j2speed.exec;

import static com.j2speed.exec.CommandCompiler.parse;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.File;

import org.junit.Test;

import edu.umd.cs.findbugs.annotations.NonNull;

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

   @Test
   public void testCompileNoParameters() {
      Nothing nothing = parse("java -cp bin com.j2speed.exec.Concatenate")
               .redirectErrorStream(true).resultBuilderFactory(new Result()).compile(Nothing.class);

      String actual = nothing.run();
      assertEquals(Concatenate.NOTHING, actual);
   }

   @Test
   public void testCommandWithParameterAndResult() {
      Concat concatenate = parse("java -cp bin com.j2speed.exec.Concatenate {?} {?}")
               .workingDirectory(new File(".")).resultBuilderFactory(new Result())
               .compile(Concat.class);
      String prefix = "prefix";
      String postfix = "postfix";
      String actual = concatenate.run(prefix, postfix);

      assertEquals(prefix + postfix, actual);
   }

   @Test
   public void testCommandWithEscaping() {
      Concat concatenate = parse("java -cp bin com.j2speed.exec.Concatenate \\{?} {?} {?}")
               .workingDirectory(new File(".")).resultBuilderFactory(new Result())
               .compile(Concat.class);
      String prefix = "prefix";
      String postfix = "postfix";
      String actual = concatenate.run(prefix, postfix);

      assertEquals("{?}" + prefix + postfix, actual);
   }

   @Test
   public void testCommandWithQouting() {
      Concat concatenate = parse("java -cp bin com.j2speed.exec.Concatenate \" {?} \" {?}")
               .workingDirectory(new File(".")).resultBuilderFactory(new Result())
               .compile(Concat.class);
      String prefix = "prefix";
      String postfix = "postfix";
      String actual = concatenate.run(prefix, postfix);

      assertEquals("\" " + prefix + " \"" + postfix, actual);
   }

   interface Nothing {
      String run();
   }

   interface Concat {
      String run(@NonNull String prefix, @NonNull String postfix);
   }

   interface TooManyMethods {
      void method1();

      void method2();
   }

   private static class Result extends AbstractResultBuilderFactory<String> {
      @Override
      public ResultBuilder<String> create() {
         return new ResultBuilder<String>() {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();

            @Override
            public void process(byte[] buffer, int legth) {
               this.buffer.write(buffer, 0, legth);
            }

            @Override
            public String build() {
               return buffer.toString();
            }
         };
      }
   }
}
