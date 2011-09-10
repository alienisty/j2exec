package com.j2speed.exec;

import java.io.ByteArrayOutputStream;

import org.junit.Test;

public class CommandBuilderTest {

   @Test
   public void testBuildNoParameters() {
      JavaVersion java = CommandBuilder.build("java -version", JavaVersion.class, new Result(),
               null, null, null);
      CommandBuilder.setRedirectError(java, true);

      System.out.println(java.version());
   }

   interface JavaVersion {
      String version();
   }

   private static class Result implements ResultBuilderFactory<String> {
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
