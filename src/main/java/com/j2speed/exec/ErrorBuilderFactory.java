package com.j2speed.exec;

/**
 * Allows to build specific {@link ErrorBuilder}s on request.
 * 
 * @author Alessandro Nistico
 * 
 * @param <T>
 */
public interface ErrorBuilderFactory<T extends Throwable> {
   /**
    * Create a new builder instance.
    * 
    * @return the new instance.
    */
   ErrorBuilder<T> create();
}
