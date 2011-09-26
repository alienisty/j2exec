package com.j2speed.exec;

import static java.lang.reflect.Proxy.newProxyInstance;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
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

   @NonNull
   public static <T> CompilingType<T> using(@NonNull Class<? extends T> type) {
      return new CompilingType<T>(type);
   }

   @NonNull
   public static <T> CompilingType<T> with(@NonNull T proxy) {
      return new CompilingType<T>(proxy);
   }

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

   public Compiler<T> timeout(long timeout) {
      this.timeout = timeout;
      return this;
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

   /**
    * Sets the environment for the managed command.
    * <p>
    * Note that this values override the current system's environment.
    * 
    * @param environment
    * @return
    */
   @NonNull
   public Compiler<T> use(@CheckForNull Map<String, String> environment) {
      this.environment = environment;
      return this;
   }

   protected void parseAnnotations(@NonNull AnnotatedElement element) {
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
      @CheckForNull
      private T proxy;
      @NonNull
      private final Class<? extends T> type;
      @NonNull
      private Map<Method, CompilingMethod<T>> compilingMethods = new HashMap<Method, CompilingMethod<T>>();

      CompilingType(@NonNull T proxy) {
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
         this.proxy = proxy;
         @SuppressWarnings("unchecked")
         Class<? extends T> type = (Class<? extends T>) interfaces[0];
         this.type = parseType(type);
      }

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
      public CompilingType<T> workIn(File workingDir) {
         super.workIn(workingDir);
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

      @Override
      @edu.umd.cs.findbugs.annotations.SuppressWarnings()
      public void swap() {
         if (proxy == null) {
            throw new IllegalStateException();
         }
         throw new UnsupportedOperationException();
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

      protected void parseAnnotations(Method method) {
         super.parseAnnotations(method);
         command = ((Command) method.getAnnotation(Command.class)).value();
      }

      @NonNull
      public CompilingMethod<T> run(String command) {
         this.command = command;
         return this;
      }

      @NonNull
      public CompilingType<T> then() {
         return global;
      }

      @Override
      public CompilingMethod<T> workIn(File workingDir) {
         super.workIn(workingDir);
         return this;
      }

      @Override
      public T compile() {
         return global.compile();
      }

      @Override
      public void swap() {
         global.swap();
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

         long timeout = this.timeout > 0 ? this.timeout : global.timeout;
         int normalTermination = this.normalTermination != 0 ? this.normalTermination
                  : global.normalTermination;
         ResultBuilderFactory<?> resultFactory = this.resultFactory != null ? this.resultFactory
                  : global.resultFactory;
         ErrorBuilderFactory<?> errorFactory = this.errorFactory != null ? this.errorFactory
                  : global.errorFactory;
         File workingDir = workingDirectory != null ? workingDirectory : global.workingDirectory;
         if (workingDir != null) {
            builder.directory(workingDir);
         }
         boolean redirect = redirectError ? redirectError : global.redirectError;
         builder.redirectErrorStream(redirect);

         if (global.environment != null && !global.environment.isEmpty()) {
            builder.environment().putAll(global.environment);
         }
         if (environment != null && !environment.isEmpty()) {
            builder.environment().putAll(environment);
         }

         return new SingleCommandInvocationHandler(method, timeout, normalTermination,
                  resultFactory, errorFactory, builder, arguments);
      }

      private static void checkCharacterIs(char expected, int cIdx, String str) {
         if (expected != str.charAt(cIdx)) {
            throw new RuntimeException("Syntax error at " + (cIdx + 1) + " in \"" + str + "\"");
         }
      }
   }
}