package com.j2speed.exec;

import static com.j2speed.exec.CommandBuilder.parse;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.File;

import org.junit.Test;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

public class CommandBuilderTest {
   @Test
   @SuppressWarnings("NP")
   public void testBuildNoParametersAlternative() {
      JavaVersion java = parse("java -version").redirectErrorStream(true).build(JavaVersion.class,
               new Result(), null);

      System.out.println(java.version());
   }

   @Test
   public void testCommandWithParameterAndResult() {
      Concat concatenate = parse("java -cp bin com.j2speed.exec.Concatenate {?} {?}")
               .workingDirectory(new File(".")).build(Concat.class, new Result(), null);
      String prefix = "prefix";
      String postfix = "postfix";
      String actual = concatenate.concat(prefix, postfix);

      assertEquals(prefix + postfix, actual);
   }

   @Test
   public void testCommandWithEscaping() {
      Concat concatenate = parse("java -cp bin com.j2speed.exec.Concatenate \\{?} {?} {?}")
               .workingDirectory(new File(".")).build(Concat.class, new Result(), null);
      String prefix = "prefix";
      String postfix = "postfix";
      String actual = concatenate.concat(prefix, postfix);

      assertEquals("{?}" + prefix + postfix, actual);
   }

   @Test
   public void testCommandWithQouting() {
      Concat concatenate = parse("java -cp bin com.j2speed.exec.Concatenate \" {?} \" {?}")
               .workingDirectory(new File(".")).build(Concat.class, new Result(), null);
      String prefix = "prefix";
      String postfix = "postfix";
      String actual = concatenate.concat(prefix, postfix);

      assertEquals("\" " + prefix + " \"" + postfix, actual);
   }

   interface JavaVersion {
      String version();
   }

   interface Concat {
      String concat(String prefix, String postfix);
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
