package com.j2speed.exec.impl;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.j2speed.exec.ErrorBuilderFactory;
import com.j2speed.exec.ResultBuilderFactory;
import com.j2speed.exec.Run;

import edu.umd.cs.findbugs.annotations.NonNull;

public final class MethodCompiler<T> extends Compiler<T> {
   private TypeCompiler<T> global;

   private Method method;
   private String command;

   MethodCompiler(@NonNull TypeCompiler<T> global, @NonNull Method method) {
      this.global = global;
      this.method = method;
      parseAnnotations(method);
   }

   void parseAnnotations(Method method) {
      super.parseAnnotations(method);
      final Run command = (Run) method.getAnnotation(Run.class);
      if (command != null) {
         this.command = command.value().trim();
      }
   }

   @NonNull
   public MethodCompiler<T> run(@NonNull String command) {
      this.command = command.trim();
      return this;
   }

   @NonNull
   public TypeCompiler<T> then() {
      return global;
   }

   @Override
   protected long timeout() {
      if (super.timeout() > 0)
         return super.timeout();
      else
         return global.timeout();
   }

   @Override
   protected int normalTermination() {
      if (super.normalTermination() != 0)
         return super.normalTermination();
      else
         return global.normalTermination();
   }

   @Override
   protected boolean redirectError() {
      if (super.redirectError())
         return super.redirectError();
      else
         return global.redirectError();
   }

   @Override
   public MethodCompiler<T> workIn(File workingDir) {
      super.workIn(workingDir);
      return this;
   }

   @Override
   protected File workingDirectory() {
      if (super.workingDirectory() != null)
         return super.workingDirectory();
      else
         return global.workingDirectory();
   }

   @Override
   public MethodCompiler<T> use(ResultBuilderFactory<?> resultFactory) {
      super.use(resultFactory);
      return this;
   }

   @Override
   protected ResultBuilderFactory<?> resultFactory() {
      if (super.resultFactory() != null)
         return super.resultFactory();
      else
         return global.resultFactory();
   }

   @Override
   public MethodCompiler<T> use(ErrorBuilderFactory<?> errorFactory) {
      super.use(errorFactory);
      return this;
   }

   @Override
   protected ErrorBuilderFactory<?> errorFactory() {
      if (super.errorFactory() != null)
         return super.errorFactory();
      else
         return global.errorFactory();
   }

   @Override
   public T compile() {
      return global.compile();
   }

   SingleInvocationHandler newHandler() {
      if (command == null || command.isEmpty()) {
         throw new IllegalStateException("No command specified for method " + method);
      }
      ProcessBuilder builder = new ProcessBuilder(new ArrayList<String>());
      List<Argument> arguments = new LinkedList<Argument>();

      return parseCommand(builder, arguments);
   }

   private SingleInvocationHandler parseCommand(ProcessBuilder builder, List<Argument> arguments) {
      List<String> tokens = builder.command();

      boolean quoting = false;
      int tokenStart = 0;
      String prefix = null;
      int index = -1;

      parsing: for (int cIdx = 0, strLength = command.length(); cIdx < strLength;) {

         final char ch = command.charAt(cIdx++);
         switch (ch) {
         case ' ':
            if (!quoting) {
               if (prefix != null) {
                  // argument found
                  arguments.add(new Argument(prefix, command.substring(tokenStart, cIdx - 1), index));
               } else if ((cIdx - tokenStart) > 1) {
                  tokens.add(command.substring(tokenStart, cIdx - 1));
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
            prefix = command.substring(tokenStart, cIdx - 1);
            checkCharacterIs('?', cIdx++, command);
            checkCharacterIs('}', cIdx++, command);
            index = tokens.size();
            tokens.add("");
            tokenStart = cIdx;
            continue parsing;
         }

      }

      if (prefix != null) {
         arguments.add(new Argument(prefix, command.substring(tokenStart), index));
      } else if (tokenStart < command.length()) {
         tokens.add(command.substring(tokenStart));
      }

      builder.directory(workingDirectory());

      builder.redirectErrorStream(redirectError());

      if (global.environment() != null && !global.environment().isEmpty()) {
         builder.environment().putAll(global.environment());
      }
      if (environment() != null && !environment().isEmpty()) {
         builder.environment().putAll(environment());
      }

      final ResultBuilderFactory<?> resultBuilderFactory = method.getReturnType() == Void.class ? null
               : resultFactory();
      final Class<?>[] parameterTypes = method.getParameterTypes();
      if (parameterTypes == null || parameterTypes.length == 0) {
         return new NoArgsInvocationHandler(method, timeout(), normalTermination(),
                  resultBuilderFactory, errorFactory(), builder, arguments);
      }

      if (parameterTypes[parameterTypes.length - 1].isArray()) {
         return new VarArgInvocationHandler(method, timeout(), normalTermination(),
                  resultBuilderFactory, errorFactory(), builder, arguments);
      }

      return new SingleInvocationHandler(method, timeout(), normalTermination(),
               resultBuilderFactory, errorFactory(), builder, arguments);
   }

   private static void checkCharacterIs(char expected, int cIdx, String str) {
      if (expected != str.charAt(cIdx)) {
         throw new RuntimeException("Syntax error at " + (cIdx + 1) + " in \"" + str + "\"");
      }
   }
}