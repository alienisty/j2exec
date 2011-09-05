package com.j2speed.exec;

public abstract class LineProcessor implements OutputProcessor {

	public final void process(byte[] buffer, int offeset, int legth) {
		// TODO implement the line buffering

	}

	protected abstract void process(String line);
}
