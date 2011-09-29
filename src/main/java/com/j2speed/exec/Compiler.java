package com.j2speed.exec;

import static java.lang.reflect.Proxy.newProxyInstance;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.j2speed.exec.impl.Argument;
import com.j2speed.exec.impl.MultiCommandInvocationHanlder;
import com.j2speed.exec.impl.SingleCommandInvocationHandler;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;

public abstract class Compiler<T> {

   private boolean redirectError;
   @CheckForNull
   private long timeout;
   @CheckForNull
   private int normalTermination;
   @CheckForNull
   private ResultBuilderFactory<?> resultFactory;
   @CheckForNull
   private ErrorBuilderFactory<?> errorFactory;
   @CheckForNull
   private File workingDirectory;
   @CheckForNull
   private Map<String, String> environment;

   @NonNull
   public static <T> CompilingType<T> using(@NonNull Class<? extends T> type) {
      return new CompilingType<T>(type);
   }

   /**
    * @return a new instance for the invocation proxy, whether the compilation starts from an
    *         existing one or not.
    */
   @NonNull
   public abstract T compile();

   public Compiler<T> timeout(long timeout) {
      this.timeout = timeout;
      return this;
   }

   protected long timeout() {
      return timeout;
   }

   public Compiler<T> redirectError(boolean redirectError) {
      this.redirectError = redirectError;
      return this;
   }

   protected boolean redirectError() {
      return redirectError;
   }

   public Compiler<T> normalTermination(int normalTermination) {
      this.normalTermination = normalTermination;
      return this;
   }

   protected int normalTermination() {
      return normalTermination;
   }

   /**
    * Sets the working directory for the managed command.
    * 
    * @param workingDir
    */
   @NonNull
   public Compiler<T> workIn(@CheckForNull File workingDir) {
      this.workingDirectory = workingDir;
      return this;
   }

   protected File workingDirectory() {
      return workingDirectory;
   }

   /**
    * Sets the environment for the managed command.
    * <p>
    * Note that this values override the current system's environment.
    * 
    * @param environment
    * @return
    */
   @NonNull
   public Compiler<T> environment(@CheckForNull Map<String, String> environment) {
      this.environment = environment;
      return this;
   }

   protected Map<String, String> environment() {
      return environment;
   }

   public Compiler<T> use(@CheckForNull ResultBuilderFactory<?> resultFactory) {
      this.resultFactory = resultFactory;
      return this;
   }

   protected ResultBuilderFactory<?> resultFactory() {
      return resultFactory;
   }

   public Compiler<T> use(@CheckForNull ErrorBuilderFactory<?> errorFactory) {
      this.errorFactory = errorFactory;
      return this;
   }

   protected ErrorBuilderFactory<?> errorFactory() {
      return errorFactory;
   }

   void parseAnnotations(@NonNull AnnotatedElement element) {
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
            normalTermination = ((NormalTermination) annotation).value();
         }
      }
   }

   @NonNull
   private static <T> T createFactory(@NonNull Class<? extends T> type) {
      try {
         return type.newInstance();
      } catch (InstantiationException e) {
         throw new RuntimeException(e);
      } catch (IllegalAccessException e) {
         throw new RuntimeException(e);
      }
   }

   public static final class CompilingType<T> extends Compiler<T> {
      @NonNull
      private final Class<? extends T> type;
      @NonNull
      private Map<Method, CompilingMethod<T>> compilingMethods = new HashMap<Method, CompilingMethod<T>>();

      CompilingType(@NonNull Class<? extends T> type) {
         this.type = parseType(type);
      }

      @NonNull
      private Class<? extends T> parseType(@NonNull Class<? extends T> type) {
         if (!type.isInterface()) {
            throw new IllegalArgumentException("Type must be an interface");
         }
         super.parseAnnotations(type);
         for (Method method : type.getMethods()) {
            compilingMethods.put(method, new CompilingMethod<T>(this, method));
         }
         return type;
      }

      @NonNull
      public CompilingMethod<T> on(String methodName, Class<?>... parameterTypes) {
         CompilingMethod<T> compiler;
         try {
            compiler = compilingMethods.get(type.getMethod(methodName, parameterTypes));
         } catch (NoSuchMethodException e) {
            compiler = null;
         }
         if (compiler == null) {
            throw new IllegalArgumentException("Unknown method");
         }
         return compiler;
      }

      @Override
      public CompilingType<T> timeout(long timeout) {
         super.timeout(timeout);
         return this;
      }

      @Override
      public CompilingType<T> redirectError(boolean redirectError) {
         super.redirectError(redirectError);
         return this;
      }

      @Override
      public CompilingType<T> normalTermination(int normalTermination) {
         super.normalTermination(normalTermination);
         return this;
      }

      @Override
      public CompilingType<T> workIn(File workingDir) {
         super.workIn(workingDir);
         return this;
      }

      @Override
      public CompilingType<T> environment(Map<String, String> environment) {
         super.environment(environment);
         return this;
      }

      @Override
      public CompilingType<T> use(ResultBuilderFactory<?> resultFactory) {
         super.use(resultFactory);
         return this;
      }

      @Override
      public CompilingType<T> use(ErrorBuilderFactory<?> errorFactory) {
         super.use(errorFactory);
         return this;
      }

      @Override
      public T compile() {
         InvocationHandler handler = null;
         if (compilingMethods.size() > 1) {
            MultiCommandInvocationHanlder multiHandler = new MultiCommandInvocationHanlder();
            for (CompilingMethod<T> compilingMethod : compilingMethods.values()) {
               multiHandler.add(compilingMethod.method, compilingMethod.newHandler());
            }
            handler = multiHandler;
         } else {
            for (CompilingMethod<?> methodCompiler : compilingMethods.values()) {
               handler = methodCompiler.newHandler();
            }
         }
         return type.cast(newProxyInstance(type.getClassLoader(), new Class[] { type }, handler));
      }
   }

   public static final class CompilingMethod<T> extends Compiler<T> {
      private CompilingType<T> global;

      private Method method;
      private String command;

      CompilingMethod(@NonNull CompilingType<T> compiler, @NonNull Method method) {
         this.global = compiler;
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
      public CompilingMethod<T> run(@NonNull String command) {
         this.command = command.trim();
         return this;
      }

      @NonNull
      public CompilingType<T> then() {
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
      public CompilingMethod<T> workIn(File workingDir) {
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
      public CompilingMethod<T> use(ResultBuilderFactory<?> resultFactory) {
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
      public CompilingMethod<T> use(ErrorBuilderFactory<?> errorFactory) {
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

      private SingleCommandInvocationHandler newHandler() {
         if (command == null || command.isEmpty()) {
            throw new IllegalStateException("No command specified for method " + method);
         }
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

         builder.directory(workingDirectory());

         builder.redirectErrorStream(redirectError());

         if (global.environment() != null && !global.environment().isEmpty()) {
            builder.environment().putAll(global.environment());
         }
         if (environment() != null && !environment().isEmpty()) {
            builder.environment().putAll(environment());
         }

         return new SingleCommandInvocationHandler(method, timeout(), normalTermination(),
                  resultFactory(), errorFactory(), builder, arguments);
      }

      private static void checkCharacterIs(char expected, int cIdx, String str) {
         if (expected != str.charAt(cIdx)) {
            throw new RuntimeException("Syntax error at " + (cIdx + 1) + " in \"" + str + "\"");
         }
      }
   }
}