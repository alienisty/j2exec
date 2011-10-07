/**
 * 
 */
package com.j2speed.exec.impl;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.j2speed.exec.Env;
import com.j2speed.exec.ErrorBuilderFactory;
import com.j2speed.exec.ResultBuilderFactory;
import com.j2speed.exec.Run;
import com.j2speed.exec.Timeout;
import com.j2speed.exec.WorkingDir;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * This encapsulates the invocation mapping for a specific command
 * 
 * @author Alessandro Nistico
 */
public final class VarargsInvocationHandler extends SingleInvocationHandler {
   @NonNull
   private final List<String> command;

   public VarargsInvocationHandler(Method method, long timeout, int normalTermination,
            ResultBuilderFactory<?> resultBuilderFactory,
            ErrorBuilderFactory<?> errorBuilderFactory, ProcessBuilder builder,
            List<Argument> arguments) {
      super(method, timeout, normalTermination, resultBuilderFactory, errorBuilderFactory, builder,
               arguments);
      Annotation[] varargAnnotations = method.getParameterAnnotations()[method.getParameterTypes().length - 1];
      if (varargAnnotations != null) {
         checkVarArgsAnnotations(varargAnnotations);
      }
      command = builder.command();
   }

   private void checkVarArgsAnnotations(@NonNull Annotation[] annotations) {
      final Class<?>[] exclusions = { Run.class, Env.class, Timeout.class, WorkingDir.class };
      for (Annotation a : annotations) {
         for (Class<?> exclusion : exclusions) {
            if (exclusion == a.annotationType()) {
               StringBuilder message = new StringBuilder(64).append("None any of");
               for (Class<?> exc : exclusions) {
                  message.append(" @").append(exc.getSimpleName());
               }
               throw new IllegalArgumentException(message.toString());
            }
         }
      }
   }

   @Override
   Process start(ProcessBuilder builder, Argument[] arguments, Object[] args) throws IOException {
      final int last = arguments.length - 1;
      final int varargsCount;
      final int nonVarargsCount;
      final Object[] varargs = (Object[]) args[nonVarargsCount = args.length - 1];
      final List<String> command = new ArrayList<String>(this.command.size()
               + (varargsCount = varargs.length));
      command.addAll(this.command);
      Argument argument;
      for (int i = 0, a = 0; i < nonVarargsCount; i++) {
         if (notExecutionParameter(args, i)) {
            // We use toString() on the argument value to force an NPE if the value is not provided
            command.set((argument = arguments[a++]).getIndex(), argument.apply(args[i].toString()));
         }
      }
      if (varargsCount > 0) {
         int i;
         // We use toString() on the argument value to force an NPE if the value is not provided
         command.set((argument = arguments[last]).getIndex(),
                  argument.apply(varargs[i = varargsCount - 1].toString()));
         while (--i >= 0) {
            command.add(argument.getIndex(), argument.apply(varargs[i].toString()));
         }
      }

      return builder.command(command).start();
   }
}
