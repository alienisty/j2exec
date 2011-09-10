package com.j2speed.exec;

import static java.lang.reflect.Proxy.newProxyInstance;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;

public final class CommandBuilder {

   /**
    * TODO Parse strings with the following format:
    * 
    * <pre>
    *    <cmd> ([<option>]['{?}'])*
    * </pre>
    * 
    * The string "{?}" is a binding parameter, similarly to the one found in JDBC. Like in JDBC they
    * are positional, so the first maps to the first parameter in the interface method, the second
    * to the second and so on.
    * 
    * TODO example
    * 
    * Note allowed format for the interface:
    * <ul>
    * <li>must be a closure (single method)
    * <li>parameters mapping to arguments must be at the beginning (in order of mapping)
    * <li>OutputProcessors must be last parameters
    * <li>You can only have either an OutputProcessor or a ResultBuilder and/or an ErrorBuilder.
    * </ul>
    * 
    * @param call
    * @param type
    * @return
    */
   public static <T> T build(@NonNull String call, @NonNull Class<? extends T> type,
            @CheckForNull ResultBuilderFactory<?> resultFactory,
            @CheckForNull ErrorBuilderFactory<?> errorFactory, @CheckForNull File workingDir,
            @CheckForNull Map<String, String> environment) {

      if (!type.isInterface()) {
         throw new IllegalArgumentException("Type must be an interface");
      }
      if (type.getMethods().length != 1) {
         throw new IllegalArgumentException("The interface must define only one method");
      }

      ParsedCommand parse = parse(call);

      if (workingDir != null) {
         parse.builder.directory(workingDir);
      }

      if (environment != null && !environment.isEmpty()) {
         parse.builder.environment().putAll(environment);
      }

      InvocationHandler handler = new ProcessInvocationHandler(type.getMethods()[0], resultFactory,
               errorFactory, parse.builder, parse.arguments);

      return type.cast(newProxyInstance(type.getClassLoader(), new Class[] { type }, handler));
   }

   /**
    * Sets the working directory for the managed command proxied through the specified object built
    * using the {@link #build(String, Class, File, Map)} method.
    * 
    * @param command
    * @param workingDir
    */
   public static void setWorkingDir(@NonNull Object command, @CheckForNull File workingDir) {
      invocationHandler(command).setWorkingDirectory(workingDir);
   }

   public static void setRedirectError(@NonNull Object command, boolean redirect) {
      invocationHandler(command).setRedirectError(redirect);
   }

   private static ProcessInvocationHandler invocationHandler(@NonNull Object command) {
      final InvocationHandler invocationHandler = Proxy.getInvocationHandler(command);
      if (invocationHandler instanceof ProcessInvocationHandler) {
         return (ProcessInvocationHandler) invocationHandler;
      }
      throw new IllegalArgumentException("Not a managed command");
   }

   /**
    * parse strings like:
    * 
    * <pre>
    * 	cmd -s {?} -d "a \"quoted\" srting" -g {?}
    * </pre>
    */
   private static ParsedCommand parse(@NonNull String str) {
      List<String> tokens = new ArrayList<String>();
      ProcessBuilder builder = new ProcessBuilder(tokens);

      List<Argument> arguments = new LinkedList<Argument>();

      boolean quoting = false;
      int tokenStart = 0;
      String prefix = null;
      int index = -1;

      parsing: for (int cIdx = 0, strLength = str.length(); cIdx < strLength;) {

         switch (str.charAt(cIdx++)) {
         case ' ':
            if (!quoting) {
               if (prefix != null) {
                  // argument found
                  arguments.add(new Argument(prefix, str.substring(tokenStart, cIdx - 1), index));
               } else if ((cIdx - tokenStart) > 1) {
                  tokens.add(str.substring(tokenStart, cIdx - 1));
               }
               prefix = null;
               tokenStart = cIdx;
            }
            continue parsing;

         case '\\': // escaping
            cIdx++; // do not parse next char
            continue parsing;

         case '\"':
            quoting = quoting ^ true; // toggle quoting
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
         throw new RuntimeException("Syntax error at " + (cIdx + 1) + " in \"" + str + "\"");
      }
   }

   private static final class ParsedCommand {
      @NonNull
      final ProcessBuilder builder;
      @NonNull
      final List<Argument> arguments;

      public ParsedCommand(@NonNull ProcessBuilder builder, @NonNull List<Argument> arguments) {
         this.builder = builder;
         this.arguments = arguments;
      }
   }
}
