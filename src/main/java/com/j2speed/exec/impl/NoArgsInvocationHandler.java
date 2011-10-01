/**
 * 
 */
package com.j2speed.exec.impl;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;

import com.j2speed.exec.ErrorBuilderFactory;
import com.j2speed.exec.ResultBuilderFactory;

/**
 * This encapsulates the invocation mapping for a specific command
 * 
 * @author Alessandro Nistico
 */
public final class NoArgsInvocationHandler extends SingleInvocationHandler {

   public NoArgsInvocationHandler(Method method, long timeout, int normalTermination,
            ResultBuilderFactory<?> resultBuilderFactory,
            ErrorBuilderFactory<?> errorBuilderFactory, ProcessBuilder builder,
            List<Argument> arguments) {
      super(method, timeout, normalTermination, resultBuilderFactory, errorBuilderFactory, builder,
               arguments);
   }

   @Override
   Process start(ProcessBuilder builder, Argument[] arguments, Object[] args) throws IOException {
      return builder.start();
   }
}
