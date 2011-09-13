package com.j2speed.exec;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import edu.umd.cs.findbugs.annotations.NonNull;

public abstract class AbstractResultBuilderFactory<T> implements ResultBuilderFactory<T> {
   @NonNull
   private final Class<? extends T> resultType;

   protected AbstractResultBuilderFactory() {
      final ParameterizedType genericSuperclass = (ParameterizedType) getClass()
               .getGenericSuperclass();
      final Type type = genericSuperclass.getActualTypeArguments()[0];
      if (type instanceof Class<?>) {
         @SuppressWarnings("unchecked")
         Class<? extends T> resultType = (Class<? extends T>) type;
         this.resultType = resultType;
      } else if (type instanceof ParameterizedType) {
         @SuppressWarnings("unchecked")
         Class<? extends T> resultType = (Class<? extends T>) ((ParameterizedType) type)
                  .getRawType();
         this.resultType = resultType;
      } else {
         throw new IllegalStateException();
      }
   }

   @Override
   public final Class<? extends T> getResultType() {
      return resultType;
   }
}
