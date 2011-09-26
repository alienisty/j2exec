package com.j2speed.exec.impl;

import static java.util.concurrent.Executors.newCachedThreadPool;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import javax.annotation.concurrent.GuardedBy;

import com.j2speed.exec.ExecutionException;
import com.j2speed.exec.OutputProcessor;


import edu.umd.cs.findbugs.annotations.NonNull;

final class Controller {

   private static final ExecutorService PROCESSOR;

   private static final ScheduledExecutorService SCHEDULER;

   @GuardedBy("running")
   private static final Collection<Process> running = new HashSet<Process>();

   @GuardedBy("running")
   private static boolean active = true;

   static {
      PROCESSOR = newCachedThreadPool(new NamedFactory("output-processor"));
      SCHEDULER = newSingleThreadScheduledExecutor(new NamedFactory("watchdog"));
      Runtime.getRuntime().addShutdownHook(new ShutdownHook());
   }

   private Controller() {
   }

   @NonNull
   static Watchdog register(@NonNull Process process, long timeout) {
      synchronized (running) {
         if (active) {
            final Watchdog watchdog = new Watchdog(process);
            if (timeout > 0) {
               watchdog.setFuture(SCHEDULER.schedule(watchdog, timeout, MILLISECONDS));
            }
            running.add(process);
            return watchdog;
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

   static void pump(@NonNull InputStream input, @NonNull OutputProcessor processor) {
      PROCESSOR.execute(new OutputPump(input, processor));
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
