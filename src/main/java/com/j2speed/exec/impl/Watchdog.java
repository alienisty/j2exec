package com.j2speed.exec.impl;

import static com.j2speed.exec.impl.Controller.kill;

import java.util.concurrent.Future;

import javax.annotation.concurrent.ThreadSafe;

import com.j2speed.exec.TimeoutException;


import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * A watchdog that, when executed, will kill a process, if not already terminated.
 * 
 * @author Alessandro Nistico
 */
@ThreadSafe
final class Watchdog implements Runnable {
   
   @NonNull
   private final Process process;
   @CheckForNull
   private volatile Future<?> future;

   private volatile boolean timedout;

   Watchdog(@NonNull Process process) {
      this.process = process;
   }

   void setFuture(@NonNull Future<?> future) {
      this.future = future;
   }

   void cancel() throws TimeoutException {
      Future<?> future;
      if ((future = this.future) != null) {
         future.cancel(true);
      }
      if (timedout) {
         throw new TimeoutException();
      }
   }

   @Override
   public void run() {
      try {
         process.exitValue();
      } catch (IllegalThreadStateException e) {
         // process not done yet, must be destroyed
         timedout = true;
         kill(process);
      }
   }
}
