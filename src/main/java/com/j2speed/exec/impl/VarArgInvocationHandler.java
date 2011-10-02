/**
 * 
 */
package com.j2speed.exec.impl;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.j2speed.exec.ErrorBuilderFactory;
import com.j2speed.exec.ResultBuilderFactory;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * This encapsulates the invocation mapping for a specific command
 * 
 * @author Alessandro Nistico
 */
public final class VarArgInvocationHandler extends SingleInvocationHandler {
   @NonNull
   private final List<String> command;

   public VarArgInvocationHandler(Method method, long timeout, int normalTermination,
            ResultBuilderFactory<?> resultBuilderFactory,
            ErrorBuilderFactory<?> errorBuilderFactory, ProcessBuilder builder,
            List<Argument> arguments) {
      super(method, timeout, normalTermination, resultBuilderFactory, errorBuilderFactory, builder,
               arguments);
      command = builder.command();
   }

   @Override
   Process start(ProcessBuilder builder, Argument[] arguments, Object[] args) throws IOException {
      final int last;
      final int varargsCount;
      final Object[] varargs = (Object[]) args[last = args.length - 1];
      final List<String> command = new ArrayList<String>(this.command.size()
               + (varargsCount = varargs.length));
      command.addAll(this.command);
      Argument argument;
      for (int i = 0, a = 0; i < last; i++) {
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
