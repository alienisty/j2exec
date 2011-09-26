package com.j2speed.exec.impl;

import static java.util.concurrent.Executors.defaultThreadFactory;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

final class NamedFactory implements ThreadFactory {
   private static final ThreadFactory base = defaultThreadFactory();
   private final AtomicInteger count = new AtomicInteger(0);
   private final String prefix;

   NamedFactory(String prefix) {
      this.prefix = prefix;
   }

   @Override
   public Thread newThread(Runnable r) {
      Thread thread = base.newThread(r);
      thread.setName(prefix + "-" + count.getAndIncrement());
      thread.setDaemon(true);
      return thread;
   }
}