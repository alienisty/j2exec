package com.j2speed.exec;

import java.io.File;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

public class ConcatenateWithPWD {
   @SuppressWarnings("NP_ALWAYS_NULL")
   public static void main(String[] args) {
      File pwd = new File("");
      System.out.print(args[0] + pwd.getAbsolutePath() + args[1]);
   }
}
