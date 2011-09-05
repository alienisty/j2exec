package com.j2speed.exec;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.NonNull;

public final class CommandBuilder {
   static final int OUTPUT_PROCESSOR = -1;
   static final int ERROR_PROCESSOR = -2;

   public static <T> T build(String call, Class<? extends T> type) {
      return null;
   }

   /**
    * parse srtings like:
    * 
    * <pre>
    * 	cmd -s {?} -d "a \"quoted\" srting" -g {?}
    * </pre>
    */
   public static ParsedCommand parse(@NonNull String str) {
      List<String> tokens = new ArrayList<String>();
      List<Argument> arguments = new LinkedList<Argument>();
      ProcessBuilder builder = new ProcessBuilder(tokens);

      boolean quoting = false;
      int tokenStart = 0;
      String prefix = null;
      int index = -1;
      parsing: for (int cIdx = 0, strLength = str.length(); cIdx < strLength;) {
         final char ch = str.charAt(cIdx++);
         switch (ch) {
         case ' ':
            if (!quoting) {
               if (prefix != null) {
                  // argument found
                  arguments.add(new Argument(prefix, str.substring(tokenStart,
                        cIdx - 1), index));
               } else if ((cIdx - tokenStart) > 1) {
                  tokens.add(str.substring(tokenStart, cIdx - 1));
               }
               prefix = null;
               tokenStart = cIdx;
            }
            continue parsing;
         case '\\': // escaping
            cIdx++;
            continue parsing;
         case '\"':
            quoting = quoting ^ true;
            continue parsing;
         case '{':
            prefix = str.substring(tokenStart, cIdx - 1);
            checkCharacterIs('?', cIdx++, str);
            checkCharacterIs('}', cIdx++, str);
            tokens.add("");
            tokenStart = cIdx;
            continue parsing;
         }
      }
      if (prefix != null) {
         arguments.add(new Argument(prefix, str.substring(tokenStart), index));
      } else if (tokenStart < str.length()) {
         tokens.add(str.substring(tokenStart));
      }

      return new ParsedCommand(builder, arguments);
   }

   private static void checkCharacterIs(char expected, int cIdx, String str) {
      if (expected != str.charAt(cIdx)) {
         throw new RuntimeException("Syntax error at " + (cIdx + 1) + " in \""
               + str + "\"");
      }
   }

   public static void main(String[] args) {
      ParsedCommand command = parse("cmd -s {?} -d \"a \\\"quoted\\\" srting\"   -g={?} -f h");
   }

   private static final class ParsedCommand {
      final ProcessBuilder builder;
      final Argument[] arguments;

      public ParsedCommand(ProcessBuilder builder, List<Argument> arguments) {
         this.builder = builder;
         this.arguments = arguments.toArray(new Argument[arguments.size()]);
      }

   }
}
