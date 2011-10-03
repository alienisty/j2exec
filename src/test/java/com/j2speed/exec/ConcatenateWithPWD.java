package com.j2speed.exec;

import static com.j2speed.exec.Concatenate.trimQuotes;

import java.io.File;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

public class ConcatenateWithPWD {
   @SuppressWarnings("NP_ALWAYS_NULL")
   public static void main(String[] args) {
      File pwd = new File("");
      System.out.print(trimQuotes(args[0]) + pwd.getAbsolutePath() + trimQuotes(args[1]));
   }
}
