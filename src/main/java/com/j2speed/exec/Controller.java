package com.j2speed.exec;

import static java.util.concurrent.Executors.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

import javax.annotation.concurrent.GuardedBy;

import edu.umd.cs.findbugs.annotations.NonNull;

final class Controller {
	private static final ThreadFactory base = defaultThreadFactory();

	@GuardedBy("running")
	private static final Collection<Process> running = new HashSet<Process>();
	
	@GuardedBy("running")
	private static boolean active = true;
	
	static {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				synchronized (running) {
					active=false;
					for (Process process : running) {
						try {
							process.destroy();
						} catch (Throwable th) {
						}
					}
				}
			}
		});
	}

	private Controller() {
	}

	private static final ExecutorService EXECUTOR = newCachedThreadPool(new ThreadFactory() {
		@Override
		public Thread newThread(Runnable r) {
			Thread thread = base.newThread(r);
			thread.setDaemon(true);
			return thread;
		}
	});
	
	static void register(@NonNull Process process) {
		synchronized (running) {
			if (active) {
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

	static void start(@NonNull OutputPump pump) {
		EXECUTOR.execute(pump);
	}
}
