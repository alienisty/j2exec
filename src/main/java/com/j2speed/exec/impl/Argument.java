/**
 * 
 */
package com.j2speed.exec.impl;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * An argument for a command.
 * 
 * @author alex
 */
public final class Argument {
   private final StringBuilder buffer;
   private final String postfix;
   private final int prefixLength;
   private final int index;

   public Argument(String prefix, String postfix, int index) {
      this.prefixLength = prefix.length();
      this.buffer = new StringBuilder(prefix.length() + postfix.length() + 16).append(prefix);
      this.postfix = postfix;
      this.index = index;
   }

   /**
    * The position for the argument in the parsed command.
    * 
    * @return
    */
   public int getIndex() {
      return index;
   }

   /**
    * Apply the value for the argument to be used in a command invocation.
    * 
    * @param value
    *           the value use for the invocation.
    * 
    * @return the string to use in the command line.
    */
   public String apply(@NonNull Object value) {
      try {
         return buffer.append(value).append(postfix).toString();
      } finally {
         buffer.setLength(prefixLength);
      }
   }
}
