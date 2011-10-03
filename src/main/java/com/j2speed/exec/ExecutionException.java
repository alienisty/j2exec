package com.j2speed.exec;

/**
 * Generic execution exception.
 * 
 * TODO should we add support to include the failing command line? 
 * 
 * @author Alessandro Nistico
 */
public class ExecutionException extends RuntimeException {

   private static final long serialVersionUID = 1L;

   public ExecutionException() {
      super();
   }

   public ExecutionException(String message, Throwable cause) {
      super(message, cause);
   }

   public ExecutionException(String message) {
      super(message);
   }

   public ExecutionException(Throwable cause) {
      super(cause);
   }
}
