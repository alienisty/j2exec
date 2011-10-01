package com.j2speed.exec;

import static com.j2speed.exec.Compiler.using;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.File;

import org.junit.Test;

import com.j2speed.exec.impl.AbstractResultBuilderFactory;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressWarnings;

public class InvocationTest {
   private static final String CMD_PREFIX = "java -cp bin com.j2speed.exec.";
   private static final String CONCATENATE = CMD_PREFIX + "Concatenate";
   private static final String CONCATENATE_WITH_PWD = CMD_PREFIX + "ConcatenateWithPWD";
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
      String actual = concatenate.concat(PREFIX, POSTFIX);

      assertEquals(PREFIX + POSTFIX, actual);
   }

   @Test
   public void testCommandWithVarargsAndResult() {
      Concat concatenate = using(Concat.class).compile();

      String separator = "-";
      String one = "one";
      String two = "two";
      String three = "three";
      String actual = concatenate.concat(PREFIX, POSTFIX, separator, one, two, three);

      assertEquals(PREFIX + POSTFIX + separator + one + two + three, actual);

      actual = concatenate.concat(PREFIX, POSTFIX, separator);

      assertEquals(PREFIX + POSTFIX + separator, actual);
   }

   @Test
   @SuppressWarnings("DMI_HARDCODED_ABSOLUTE_FILENAME")
   public void testCommandWithParametersAndResultAndPWD() {
      String testCP = (new File("").getAbsolutePath() + File.separator + "bin").replace("\\", "/");
      String command = CONCATENATE_WITH_PWD.replace("-cp bin", "-cp " + testCP) + " {?} {?}";

      Concat concatenate = using(Concat.class)
               .on("working", String.class, File.class, String.class).run(command).compile();

      File pwd = new File("/");
      String actual = concatenate.working(PREFIX, pwd, POSTFIX);

      assertEquals(PREFIX + pwd.getAbsolutePath() + POSTFIX, actual);
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
      String prefix = "prefix with spaces";
      String actual = concatenate.concat(prefix, POSTFIX);

      assertEquals(" " + prefix + " " + POSTFIX, actual);
   }

   @Test(expected = TimeoutException.class)
   public void testCommandTimeout() {
      ForEver forEver = using(ForEver.class).timeout(500).compile();
      forEver.doNothing();
   }

   @Test(expected = TimeoutException.class)
   public void testCommandTimeoutAnnotation() {
      ForEver forEver = using(ForEver.class).compile();
      forEver.doNothing(500);
   }

   @RedirectError
   @ResultFactory(Result.class)
   interface Nothing {
      @Run(CONCATENATE)
      String toDo();
   }

   @ResultFactory(Result.class)
   interface Concat {
      @Run(CONCATENATE + " {?} {?}")
      String concat(@NonNull String prefix, @NonNull String postfix);

      @Run(CONCATENATE + " {?} {?} {?} {?}")
      String concat(@NonNull String prefix, @NonNull String postfix, @NonNull String separator,
               @NonNull String... more);

      @Run("cmd")
      String working(String prefix, @WorkingDir File dir, String postfix);
   }

   interface Concat2 {
      @Run(CONCATENATE + " \\{?} {?} {?}")
      @ResultFactory(Result.class)
      String concat(@NonNull String prefix, @NonNull String postfix);
   }

   interface ForEver {
      @Run(FOREVER)
      void doNothing();

      @Run(FOREVER)
      void doNothing(@Timeout long timeout);
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
