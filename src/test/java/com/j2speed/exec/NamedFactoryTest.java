package com.j2speed.exec;

import static org.junit.Assert.*;

import java.util.concurrent.Executors;

import org.junit.Test;

public class NamedFactoryTest {

   @Test
   public void testNewThread() {
      final Runnable runnable = new Runnable() {
         public void run() {
         };
      };

      NamedFactory factory = new NamedFactory("test-thread");
      Thread thread = factory.newThread(runnable);

      assertTrue(thread.isDaemon());
      assertEquals("test-thread-0", thread.getName());

      final Thread baseThread = Executors.defaultThreadFactory().newThread(runnable);
      assertEquals(baseThread.getThreadGroup(), thread.getThreadGroup());
   }
}
