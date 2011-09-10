package com.j2speed.exec;

public interface ErrorBuilderFactory<T extends Throwable> {
   ErrorBuilder<T> create();
}
