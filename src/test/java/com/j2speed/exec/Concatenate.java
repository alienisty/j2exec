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
            System.out.print(arg);
         }
   }
}
