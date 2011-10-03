package com.j2speed.exec.impl;


import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Map;

import com.j2speed.exec.ErrorBuilderFactory;
import com.j2speed.exec.ErrorFactory;
import com.j2speed.exec.NormalTermination;
import com.j2speed.exec.RedirectError;
import com.j2speed.exec.ResultBuilderFactory;
import com.j2speed.exec.ResultFactory;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Note that by design only one binding parameter is allowed within a quotation section.
 * 
 * @version
 * @since
 * @author Alessandro Nistico
 * 
 * @param <T>
 */
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
   public static <T> TypeCompiler<T> using(@NonNull Class<? extends T> type) {
      return new TypeCompiler<T>(type);
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
}