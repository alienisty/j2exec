package com.j2speed.exec;

public class ExecutionException extends RuntimeException {

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
