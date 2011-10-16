package com.j2speed.exec.impl;

import static java.lang.reflect.Proxy.newProxyInstance;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import com.j2speed.exec.ErrorBuilderFactory;
import com.j2speed.exec.ResultBuilderFactory;

import edu.umd.cs.findbugs.annotations.NonNull;

public final class TypeCompiler<T> extends Compiler<T> {
   @NonNull
   private final Class<? extends T> type;
   @NonNull
   private Map<Method, MethodCompiler<T>> compilingMethods = new HashMap<Method, MethodCompiler<T>>();

   TypeCompiler(@NonNull Class<? extends T> type) {
      this.type = parseType(type);
   }

   @NonNull
   private Class<? extends T> parseType(@NonNull Class<? extends T> type) {
      if (!type.isInterface()) {
         throw new IllegalArgumentException("Type must be an interface");
      }
      super.parseAnnotations(type);
      for (Method method : type.getMethods()) {
         compilingMethods.put(method, new MethodCompiler<T>(this, method));
      }
      return type;
   }

   @NonNull
   public MethodCompiler<T> on(String methodName, Class<?>... parameterTypes) {
      MethodCompiler<T> compiler;
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
   public TypeCompiler<T> timeout(long timeout) {
      super.timeout(timeout);
      return this;
   }

   @Override
   public TypeCompiler<T> redirectError(boolean redirectError) {
      super.redirectError(redirectError);
      return this;
   }

   @Override
   public TypeCompiler<T> normalTermination(int normalTermination) {
      super.normalTermination(normalTermination);
      return this;
   }

   @Override
   public TypeCompiler<T> workIn(File workingDir) {
      super.workIn(workingDir);
      return this;
   }

   @Override
   public TypeCompiler<T> environment(Map<String, String> environment) {
      super.environment(environment);
      return this;
   }

   @Override
   public TypeCompiler<T> use(ResultBuilderFactory<?> resultFactory) {
      super.use(resultFactory);
      return this;
   }

   @Override
   public TypeCompiler<T> use(ErrorBuilderFactory<?> errorFactory) {
      super.use(errorFactory);
      return this;
   }

   @Override
   public T compile() {
      InvocationHandler handler = null;
      if (compilingMethods.size() > 1) {
         MultiMethodInvocationHanlder multiHandler = new MultiMethodInvocationHanlder();
         for (Map.Entry<Method, MethodCompiler<T>> compilers : compilingMethods.entrySet()) {
            multiHandler.add(compilers.getKey(), compilers.getValue().newHandler());
         }
         handler = multiHandler;
      } else {
         for (MethodCompiler<?> methodCompiler : compilingMethods.values()) {
            handler = methodCompiler.newHandler();
         }
      }
      return type.cast(newProxyInstance(type.getClassLoader(), new Class[] { type }, handler));
   }
}