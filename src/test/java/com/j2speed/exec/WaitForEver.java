package com.j2speed.exec;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

public class WaitForEver {
   @SuppressWarnings(value = { "UW_UNCOND_WAIT", "WA_NOT_IN_LOOP" })
   public static void main(String[] args) throws InterruptedException {
      synchronized (WaitForEver.class) {
         WaitForEver.class.wait();
      }
   }
}
