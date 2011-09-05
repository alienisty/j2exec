package com.j2speed.exec;

import edu.umd.cs.findbugs.annotations.NonNull;

public final class Command {
   @NonNull
   private final ProcessBuilder builder;
   @NonNull
   private final int[] paremetersMapping;

   public Command(ProcessBuilder builder, int[] paremetersMapping) {
      this.builder = builder;
      this.paremetersMapping = paremetersMapping;
   }

   public void run(String... params) {

   }
}
