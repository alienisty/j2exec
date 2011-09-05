package com.j2speed.exec;

public interface OutputProcessor {
	OutputProcessor SINK = new OutputProcessor() {
		@Override
		public void process(byte[] buffer, int legth) {
		}
	};

	void process(byte[] buffer, int legth);
}
