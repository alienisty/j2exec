package com.j2speed.exec;

import static java.util.concurrent.Executors.newCachedThreadPool;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import javax.annotation.concurrent.GuardedBy;

import edu.umd.cs.findbugs.annotations.NonNull;

final class Controller {

   private static final ExecutorService processors;

   private static final ScheduledExecutorService watchdog;

   @GuardedBy("running")
   private static final Collection<Process> running = new HashSet<Process>();

   @GuardedBy("running")
   private static boolean active = true;

   static {
      processors = newCachedThreadPool(new NamedFactory("output-processor"));
      watchdog = newSingleThreadScheduledExecutor(new NamedFactory("watchdog"));
      Runtime.getRuntime().addShutdownHook(new ShutdownHook());
   }

   private Controller() {
   }

   static void register(@NonNull Process process, long timeout) {
      synchronized (running) {
         if (active) {
            if (timeout > 0) {
               watchdog.schedule(new Watchdog(process), timeout, MILLISECONDS);
            }
            running.add(process);
         } else {
            process.destroy();
            throw new ExecutionException("shutting down");
         }
      }
   }

   static void done(@NonNull Process process) {
      synchronized (running) {
         running.remove(process);
      }
   }

   static void kill(@NonNull Process process) {
      process.destroy();
      done(process);
   }

   static void start(@NonNull OutputPump pump) {
      processors.execute(pump);
   }

   private static final class ShutdownHook extends Thread {
      public ShutdownHook() {
         super("shutdown-processor");
      }

      @Override
      public void run() {
         synchronized (running) {
            active = false;
            for (Process process : running) {
               try {
                  process.destroy();
               } catch (Throwable th) {
               }
            }
         }
      }
   }
}
