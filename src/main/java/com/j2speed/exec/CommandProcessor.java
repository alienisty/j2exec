package com.j2speed.exec;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

final class CommandProcessor {
   private static final Executor EXECUTOR;
   private static final ScheduledExecutorService WATCH;
   static {
      Threads threads = new Threads();
      EXECUTOR = Executors.newCachedThreadPool(threads);
      WATCH = Executors.newSingleThreadScheduledExecutor(threads);
   }

   private static final class Threads implements ThreadFactory {
      private static final ThreadFactory BASE_FACTORY = Executors
            .defaultThreadFactory();

      @Override
      public Thread newThread(Runnable r) {
         Thread thread = BASE_FACTORY.newThread(r);
         thread.setDaemon(true);
         thread.setName(thread.getName() + "-command-processor");
         return thread;
      }
   }
}
