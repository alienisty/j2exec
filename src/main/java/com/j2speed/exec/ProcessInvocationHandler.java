/**
 * 
 */
package com.j2speed.exec;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * This encapsulates the invocation mapping for a specific command
 * 
 * @author Alessandro Nistico
 */
public final class ProcessInvocationHandler implements InvocationHandler {
	@NonNull
	private final ProcessBuilder builder;
	@NonNull
	private final int[] paremetersMapping;

	public ProcessInvocationHandler(@NonNull ProcessBuilder builder,
			@NonNull int[] paremetersMapping) {
		this.builder = builder;
		this.paremetersMapping = paremetersMapping;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {
		Process process;
		OutputProcessor processor = OutputProcessor.SINK;
		ErrorBuilder<?> error = null;
		synchronized (builder) {
			final List<String> command = builder.command();
			for (int i = args.length; --i >= 0;) {
				final int paramIndex;
				switch (paramIndex = paremetersMapping[i]) {
				case CommandBuilder.OUTPUT_PROCESSOR:
					processor = (OutputProcessor) args[i];
					break;
				case CommandBuilder.ERROR_PROCESSOR:
					error = (ErrorBuilder<?>) args[i];
					break;
				default:
					command.set(paramIndex, String.valueOf(args[i]));
				}
			}
			process = builder.start();
		}
		
		Controller.register(process);
		
		if (!builder.redirectErrorStream()) {
			if (error == null) {
				error = new DefaultErrorBuilder();
			}
			Controller.start(new OutputPump(process.getErrorStream(), error));
		}
		// TODO set up watchdog?
		try {
			processOutput(process, processor);
		} finally {
			cleanUp(process);
		}
		if (processor instanceof ResultBuilder<?>) {
			return ((ResultBuilder<?>) processor).build();
		}
		return null;
	}

	private static void processOutput(Process process, OutputProcessor processor)
			throws IOException {
		InputStream is = process.getInputStream();
		byte[] buffer = new byte[4096];
		int read;
		while ((read = is.read(buffer)) != -1) {
			processor.process(buffer, read);
		}
	}

	private static void cleanUp(Process process) throws InterruptedException {
		process.waitFor();
	}
}
