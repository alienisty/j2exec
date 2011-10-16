package com.j2speed.exec;

/**
 * Allows to build specific exception from the execution of a process.
 * 
 * @author Alessandro Nistico
 * 
 * @param <T>
 */
public interface ErrorBuilder<T extends Throwable> extends Processor {
   T build();
}
