package com.j2speed.exec;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

public class Concatenate {
   static final String NOTHING = "Nothing";

   @SuppressWarnings("NP_ALWAYS_NULL")
   public static void main(String[] args) {
      if (args.length == 0) {
         System.out.print(NOTHING);
      } else
         for (String arg : args) {
            System.out.print(trimQuotes(arg));
         }
   }

   // Some platform and or JDK passes the quote character as well, so we trim them out to have
   // consistent test results.
   static String trimQuotes(String value) {
      if (value.isEmpty()) {
         return value;
      }
      if (value.charAt(0) == '"') {
         value = value.substring(1);
      }
      if (value.charAt(value.length() - 1) == '"') {
         value = value.substring(0, value.length() - 1);
      }
      return value;
   }
}
