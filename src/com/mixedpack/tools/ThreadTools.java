package com.mixedpack.tools;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ThreadTools {
  public final static ExecutorService internetDaraFillerPool = Executors.newFixedThreadPool(5);

  public static <T> T tryExecution(Callable<T> callable) {
    return tryExecution(callable, 5);
  }

  public static <T> T tryExecution(Callable<T> callable, int tries) {
    for (int i = 0; i < tries; i++) {
      try {
        Future<T> future = internetDaraFillerPool.submit(callable);
        return future.get();
      } catch (Throwable t) {
        t.printStackTrace();
      }
    }
    return null;
  }
}
