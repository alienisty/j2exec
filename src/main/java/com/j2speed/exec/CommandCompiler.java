package com.j2speed.exec;

import static java.lang.reflect.Proxy.newProxyInstance;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
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

   private static SingleCommandInvocationHandler invocationHandler(@NonNull Object command) {
      final InvocationHandler invocationHandler = Proxy.getInvocationHandler(command);
      if (invocationHandler instanceof SingleCommandInvocationHandler) {
         return (SingleCommandInvocationHandler) invocationHandler;
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

   /**
    * @param timeout
    *           in milliseconds
    * @return
    */
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

   public static <T> CompilingProxy<T> with(Class<? extends T> type) {
      return null;
   }

   public static <T> CompilingProxy<T> with(T proxy) {
      return null;
   }

   @NonNull
   public <T> T compile(Class<? extends T> type) {
      if (!type.isInterface()) {
         throw new IllegalArgumentException("Type must be an interface");
      }
      if (type.getMethods().length != 1) {
         throw new IllegalArgumentException("The interface must define only one method");
      }

      InvocationHandler handler = new SingleCommandInvocationHandler(type.getMethods()[0], timeout,
               normalTermination, resultBuilderFactory, errorBuilderFactory, builder, arguments);

      return type.cast(newProxyInstance(type.getClassLoader(), new Class[] { type }, handler));

   }

   private static abstract class Compiler<T> {

      boolean redirectError;
      @CheckForNull
      long timeout;
      @CheckForNull
      int normalTermination;
      @CheckForNull
      ResultBuilderFactory<?> resultFactory;
      @CheckForNull
      ErrorBuilderFactory<?> errorFactory;
      @CheckForNull
      File workingDirectory;
      @CheckForNull
      Map<String, String> environment;

      public abstract Compiler<T> on(String methodName);

      /**
       * @return a new instance for the invocation proxy, whether the compilation starts from an
       *         existing one or not.
       */
      @NonNull
      public abstract T compile();

      /**
       * Re-compile and re-use the invocation proxy passed at the beginning of the chain.
       * 
       * @throws IllegalStateException
       *            if the compilation does not start from an existing invocation proxy.
       */
      public abstract void swap();

      /**
       * Sets the working directory for the managed command.
       * 
       * @param workingDir
       */
      @NonNull
      public Compiler<T> use(@CheckForNull File workingDir) {
         this.workingDirectory = workingDir;
         return this;
      }

      /**
       * Sets the environment for the managed command.
       * <p>
       * Note that this values override the current system's environment.
       * 
       * @param environment
       * @return
       */
      public Compiler<T> use(@CheckForNull Map<String, String> environment) {
         this.environment = environment;
         return this;
      }

      private void parseAnnotations(@NonNull AnnotatedElement element) {
         redirectError = false;
         resultFactory = null;
         errorFactory = null;
         for (Annotation annotation : element.getAnnotations()) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            if (annotationType == RedirectError.class) {
               redirectError = true;
            } else if (annotationType == ResultFactory.class) {
               resultFactory = createFactory(((ResultFactory) annotation).value());
            } else if (annotationType == ErrorFactory.class) {
               errorFactory = createFactory(((ErrorFactory) annotation).value());
            } else if (annotationType == NormalTermination.class) {
               normalTermination = parse((NormalTermination) annotation);
            }
         }
      }

      private int parse(NormalTermination normalTermination) {
         Class<?> parser = normalTermination.parser();
         if (parser == Integer.class) {
            return Integer.parseInt(normalTermination.value());
         }
         try {
            try {
               return (Integer) parser.getMethod("parseInt", String.class).invoke(parser,
                        normalTermination.value());
            } catch (InvocationTargetException e) {
               throw e.getTargetException();
            }
         } catch (RuntimeException e) {
            throw e;
         } catch (Error e) {
            throw e;
         } catch (Throwable th) {
            throw new RuntimeException(th);
         }
      }

      private static <T> T createFactory(@NonNull Class<? extends T> type) {
         try {
            return type.newInstance();
         } catch (InstantiationException e) {
            throw new RuntimeException(e);
         } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
         }
      }
   }

   public static class CompilingProxy<T> extends Compiler<T> {
      @CheckForNull
      private T proxy;
      @NonNull
      private Class<? extends T> type;
      @NonNull
      private Map<Method, CompilingMethod<?>> methodCompilers = new HashMap<Method, CompilingMethod<?>>();

      private CompilingProxy(T proxy) {
         if (!Proxy.isProxyClass(proxy.getClass())) {
            throw new IllegalArgumentException("Object of " + proxy.getClass() + " is not a proxy");
         }
         // TODO is proxy a managed commands proxy? (should check using the invocation handler
         // type?)

         Class<?>[] interfaces = proxy.getClass().getInterfaces();
         if (interfaces == null || interfaces.length > 1) {
            throw new IllegalArgumentException("Invalid proxy format, wrong number of interfaces "
                     + (interfaces == null ? 0 : interfaces.length));
         }

         this.type = (Class<? extends T>) interfaces[0];
         super.parseAnnotations(type);
      }

      private CompilingProxy(@NonNull Class<? extends T> type) {
         super.parseAnnotations(type);
         this.type = type;
      }

      @Override
      public CompilingMethod<T> on(String methodName) {
         return new CompilingMethod<T>(this, methodName);
      }

      @Override
      public final CompilingProxy<T> use(File workingDir) {
         super.use(workingDir);
         return this;
      }

      private void setMethodCompiler(CompilingMethod<?> compiler) {
         methodCompilers.put(compiler.method, compiler);
      }

      @Override
      public T compile() {
         InvocationHandler handler = null;
         if (methodCompilers.size() > 1) {
            MultiCommandInvocationHanlder multiHandler = new MultiCommandInvocationHanlder();
            for (CompilingMethod<?> methodCompiler : methodCompilers.values()) {
               multiHandler.add(methodCompiler.method, methodCompiler.newHandler());
            }
            handler = multiHandler;
         } else {
            for (CompilingMethod<?> methodCompiler : methodCompilers.values()) {
               handler = methodCompiler.newHandler();
            }
         }
         return type.cast(newProxyInstance(type.getClassLoader(), new Class[] { type }, handler));
      }

      @Override
      public void swap() {
         if (proxy == null) {

         }
         // TODO Auto-generated method stub

      }
   }

   public static class CompilingMethod<T> extends Compiler<T> {
      private CompilingProxy<T> compiler;

      private String name;
      private Method method;
      private String command;

      private CompilingMethod(@NonNull CompilingProxy<T> compiler, @NonNull String name) {
         this.compiler = compiler;
         this.name = name;
         try {
            parseMethod(name);
         } catch (NoSuchMethodException e) {
            // there is no no-args method
         }
      }

      private void parseMethod(@NonNull String name, Class<?>... parameters)
               throws NoSuchMethodException {
         this.method = compiler.type.getMethod(name);
         parseAnnotations(method);
         compiler.setMethodCompiler(this);
      }

      private void parseAnnotations(Method method) {
         super.parseAnnotations(method);
         command = ((Command) method.getAnnotation(Command.class)).value();
      }

      public CompilingMethod<T> withParameters(Class<?>... parameters) throws NoSuchMethodException {
         // need to override the default one because this call means that the no-args method is not
         // what is wanted here.
         method = null;
         parseMethod(name, parameters);
         return this;
      }

      @Override
      public final CompilingMethod<T> on(String methodName) {
         return new CompilingMethod<T>(compiler, methodName);
      }

      @Override
      public CompilingMethod<T> use(File workingDir) {
         super.use(workingDir);
         return this;
      }

      @Override
      public final T compile() {
         return compiler.compile();
      }

      @Override
      public final void swap() {
         compiler.swap();
      }

      private SingleCommandInvocationHandler newHandler() {
         ProcessBuilder builder = new ProcessBuilder(new ArrayList<String>());
         List<Argument> arguments = new LinkedList<Argument>();

         return newHandler(builder, arguments);
      }

      private SingleCommandInvocationHandler newHandler(ProcessBuilder builder,
               List<Argument> arguments) {
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
                     arguments.add(new Argument(prefix, command.substring(tokenStart, cIdx - 1),
                              index));
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

         long timeout = this.timeout > 0 ? this.timeout : compiler.timeout;
         int normalTermination = this.normalTermination != 0 ? this.normalTermination
                  : compiler.normalTermination;
         ResultBuilderFactory<?> resultFactory = this.resultFactory != null ? this.resultFactory
                  : compiler.resultFactory;
         ErrorBuilderFactory<?> errorFactory = this.errorFactory != null ? this.errorFactory
                  : compiler.errorFactory;

         return new SingleCommandInvocationHandler(method, timeout, normalTermination,
                  resultFactory, errorFactory, builder, arguments);
      }
   }
}
