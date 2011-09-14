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

public final class CommandCompiler {

   @NonNull
   private final ProcessBuilder builder;
   @NonNull
   private final List<Argument> arguments;
   @CheckForNull
   private ResultBuilderFactory<?> resultBuilderFactory;
   @CheckForNull
   private ErrorBuilderFactory<?> errorBuilderFactory;

   private int normalTermination = 0;

   private long timeout;

   /**
    * parse strings like:
    * 
    * 
    * <pre>
    *    <cmd> ([<option>]['{?}'])*
    * </pre>
    * 
    * Example:
    * 
    * <pre>
    *    cmd -s {?} -d "a \"quoted\" srting" -g {?}
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
    * <li>must be a Single Abstract Method
    * <li>parameters mapping to arguments must be at the beginning (in order of mapping)
    * <li>An optional OutputProcessor must be the last parameter and is valid only if a
    * {@link ResultBuilder} hasn't been set.
    * </ul>
    */
   public static CommandCompiler parse(@NonNull String str) {
      List<String> tokens = new ArrayList<String>();
      ProcessBuilder builder = new ProcessBuilder(tokens);

      List<Argument> arguments = new LinkedList<Argument>();

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
                  arguments.add(new Argument(prefix, str.substring(tokenStart, cIdx - 1), index));
               } else if ((cIdx - tokenStart) > 1) {
                  tokens.add(str.substring(tokenStart, cIdx - 1));
               }
               prefix = null;
               tokenStart = cIdx;
            }
            continue parsing;

         case '\\': // escaping
            if (tokenStart == (cIdx - 1)) {
               tokenStart = cIdx;
            }
            cIdx++; // do not parse next char
            continue parsing;

         case '"':
            quoting = quoting ^ true; // toggle quoting
            continue parsing;

         case '{':
            prefix = str.substring(tokenStart, cIdx - 1);
            checkCharacterIs('?', cIdx++, str);
            checkCharacterIs('}', cIdx++, str);
            index = tokens.size();
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

      return new CommandCompiler(builder, arguments);
   }

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
      invocationHandler(command).setRedirectErrorStream(redirect);
   }

   private static ProcessInvocationHandler invocationHandler(@NonNull Object command) {
      final InvocationHandler invocationHandler = Proxy.getInvocationHandler(command);
      if (invocationHandler instanceof ProcessInvocationHandler) {
         return (ProcessInvocationHandler) invocationHandler;
      }
      throw new IllegalArgumentException("Not a managed command");
   }

   private static void checkCharacterIs(char expected, int cIdx, String str) {
      if (expected != str.charAt(cIdx)) {
         throw new RuntimeException("Syntax error at " + (cIdx + 1) + " in \"" + str + "\"");
      }
   }

   private CommandCompiler(@NonNull ProcessBuilder builder, @NonNull List<Argument> arguments) {
      this.builder = builder;
      this.arguments = arguments;
   }

   @NonNull
   public CommandCompiler timeout(long timeout) {
      this.timeout = timeout;
      return this;
   }

   @NonNull
   public CommandCompiler normalTermination(int normalTermination) {
      this.normalTermination = normalTermination;
      return this;
   }

   @NonNull
   public CommandCompiler workingDirectory(@CheckForNull File workingDirectory) {
      builder.directory(workingDirectory);
      return this;
   }

   @NonNull
   public CommandCompiler redirectErrorStream(boolean redirect) {
      builder.redirectErrorStream(redirect);
      return this;
   }

   @NonNull
   public CommandCompiler environment(@NonNull Map<String, String> environment) {
      builder.environment().putAll(environment);
      return this;
   }

   @NonNull
   public CommandCompiler resultBuilderFactory(@NonNull ResultBuilderFactory<?> resultBuilderFactory) {
      this.resultBuilderFactory = resultBuilderFactory;
      return this;
   }

   @NonNull
   public CommandCompiler errorBuilderFactory(@NonNull ErrorBuilderFactory<?> errorBuilderFactory) {
      this.errorBuilderFactory = errorBuilderFactory;
      return this;
   }

   @NonNull
   public <T> T compile(Class<? extends T> type) {
      if (!type.isInterface()) {
         throw new IllegalArgumentException("Type must be an interface");
      }
      if (type.getMethods().length != 1) {
         throw new IllegalArgumentException("The interface must define only one method");
      }

      InvocationHandler handler = new ProcessInvocationHandler(type.getMethods()[0], timeout,
               normalTermination, resultBuilderFactory, errorBuilderFactory, builder, arguments);

      return type.cast(newProxyInstance(type.getClassLoader(), new Class[] { type }, handler));

   }
}
