package com.j2speed.exec.impl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;



public class MultiCommandInvocationHanlder implements InvocationHandler {

   Map<Method, SingleCommandInvocationHandler> handlers = new HashMap<Method, SingleCommandInvocationHandler>();

   @Override
   public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      return handlers.get(method).invoke(proxy, method, args);
   }

   public void add(Method method, SingleCommandInvocationHandler handler) {
      if (method == null) {
         throw new IllegalArgumentException("Method null");
      }
      if (handler == null) {
         throw new IllegalArgumentException("Handler null");
      }
      handlers.put(method, handler);
   }
}
