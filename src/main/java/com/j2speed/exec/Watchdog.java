package com.j2speed.exec;

import static com.j2speed.exec.Controller.kill;
import edu.umd.cs.findbugs.annotations.NonNull;

final class Watchdog implements Runnable {
   @NonNull
   private final Process process;

   Watchdog(@NonNull Process process) {
      this.process = process;
   }

   @Override
   public void run() {
      try {
         process.exitValue();
      } catch (IllegalThreadStateException e) {
         // process not done yet, must be destroyed
         kill(process);
      }
   }
}
