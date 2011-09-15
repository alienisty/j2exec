package com.j2speed.exec;

import static com.j2speed.exec.CommandCompiler.parse;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.File;

import org.junit.Test;

import edu.umd.cs.findbugs.annotations.NonNull;

public class InvocationTest {
   private static final String CMD_PREFIX = "java -cp bin com.j2speed.exec.";
   private static final String CONCATENATE = CMD_PREFIX + "Concatenate";
   private static final String FOREVER = CMD_PREFIX + "WaitForEver";
   private static final String POSTFIX = "postfix";
   private static final String PREFIX = "prefix";

   @Test
   public void testCommandWithNoParameters() {
      Nothing nothing = parse(CONCATENATE).redirectErrorStream(true)
               .resultBuilderFactory(new Result()).compile(Nothing.class);

      String actual = nothing.run();
      assertEquals(Concatenate.NOTHING, actual);
   }

   @Test
   public void testCommandWithParameterAndResult() {
      Concat concatenate = parse(CONCATENATE + " {?} {?}").workingDirectory(new File("."))
               .resultBuilderFactory(new Result()).compile(Concat.class);
      String actual = concatenate.run(PREFIX, POSTFIX);

      assertEquals(PREFIX + POSTFIX, actual);
   }

   @Test
   public void testCommandWithEscaping() {
      Concat concatenate = parse(CONCATENATE + " \\{?} {?} {?}").workingDirectory(new File("."))
               .resultBuilderFactory(new Result()).compile(Concat.class);
      String actual = concatenate.run(PREFIX, POSTFIX);

      assertEquals("{?}" + PREFIX + POSTFIX, actual);
   }

   @Test
   public void testCommandWithQouting() {
      Concat concatenate = parse(CONCATENATE + " \" {?} \" {?}").workingDirectory(new File("."))
               .resultBuilderFactory(new Result()).compile(Concat.class);
      String actual = concatenate.run(PREFIX, POSTFIX);

      assertEquals("\" " + PREFIX + " \"" + POSTFIX, actual);
   }

   @Test(expected = ExecutionException.class)
   public void testCommandTimeout() {
      ForEver forEver = parse(FOREVER).timeout(500).compile(ForEver.class);
      forEver.doNothing();
   }

   interface Nothing {
      String run();
   }

   interface Concat {
      String run(@NonNull String PREFIX, @NonNull String POSTFIX);
   }

   interface ForEver {
      void doNothing();
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
