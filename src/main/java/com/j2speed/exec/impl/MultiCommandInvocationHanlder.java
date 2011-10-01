package com.j2speed.exec.impl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class MultiCommandInvocationHanlder implements InvocationHandler {

   private final Map<Method, SingleInvocationHandler> handlers = new HashMap<Method, SingleInvocationHandler>();

   @Override
   public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      SingleInvocationHandler handler = handlers.get(method);
      if (handler == null) {
         throw new UnsupportedOperationException("Method " + method + " not mapped");
      }
      return handler.invoke(proxy, method, args);
   }

   public void add(Method method, SingleInvocationHandler handler) {
      if (method == null) {
         throw new IllegalArgumentException("Method null");
      }
      if (handler == null) {
         throw new IllegalArgumentException("Handler null");
      }
      handlers.put(method, handler);
   }
}
