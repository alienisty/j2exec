package com.j2speed.exec;

import static com.j2speed.exec.Compiler.using;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;

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
      Nothing nothing = using(Nothing.class).compile();

      String actual = nothing.toDo();
      assertEquals(Concatenate.NOTHING, actual);
   }

   @Test
   public void testCommandWithParameterAndResult() {
      Concat concatenate = using(Concat.class).compile();
      String actual = concatenate.run(PREFIX, POSTFIX);

      assertEquals(PREFIX + POSTFIX, actual);
   }

   @Test
   public void testCommandWithEscaping() {
      Concat2 concatenate = using(Concat2.class).compile();
      String actual = concatenate.concat(PREFIX, POSTFIX);

      assertEquals("{?}" + PREFIX + POSTFIX, actual);
   }

   @Test
   public void testCommandWithQouting() {
      Concat2 concatenate = using(Concat2.class).on("concat", String.class, String.class)
               .run(CONCATENATE + " \" {?} \" {?}").compile();
      String actual = concatenate.concat(PREFIX, POSTFIX);

      assertEquals("\" " + PREFIX + " \"" + POSTFIX, actual);
   }

   @Test(expected = TimeoutException.class)
   public void testCommandTimeout() {
      ForEver forEver = using(ForEver.class).timeout(500).compile();
      forEver.doNothing();
   }

   @RedirectError
   @ResultFactory(Result.class)
   interface Nothing {
      @Command(CONCATENATE)
      String toDo();
   }

   interface Concat {
      @Command(CONCATENATE + " {?} {?}")
      @ResultFactory(Result.class)
      String run(@NonNull String prefix, @NonNull String postfix);
   }

   interface Concat2 {
      @Command(CONCATENATE + " \\{?} {?} {?}")
      @ResultFactory(Result.class)
      String concat(@NonNull String prefix, @NonNull String postfix);
   }

   interface ForEver {
      @Command(FOREVER)
      void doNothing();
   }

   public static class Result extends AbstractResultBuilderFactory<String> {
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
