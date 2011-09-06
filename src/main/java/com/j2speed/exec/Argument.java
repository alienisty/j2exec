/**
 * 
 */
package com.j2speed.exec;

/**
 * @author alex
 * 
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

   public int getIndex() {
      return index;
   }

   public String apply(Object value) {
      try {
         return buffer.append(value).append(postfix).toString();
      } finally {
         buffer.setLength(prefixLength);
      }
   }
}
