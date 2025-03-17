package com.linkedin.d2.balancer.util;

import com.linkedin.r2.util.NamedThreadFactory;
import com.linkedin.r2.util.UncaughtExceptionHandler;


/**
 * A {@link java.util.concurrent.ThreadFactory} that tracks whether a thread belongs to the D2 single-threaded
 * service-discovery related executors.
 */
public final class D2ExecutorThreadFactory extends NamedThreadFactory {
  private static final ThreadLocal<Boolean> BELONGS_TO_EXECUTOR = ThreadLocal.withInitial(() -> false);

  public D2ExecutorThreadFactory(String name) {
    super(name);
  }

  public D2ExecutorThreadFactory(String name, UncaughtExceptionHandler uncaughtExceptionHandler) {
    super(name, uncaughtExceptionHandler);
  }

  @Override
  public Thread newThread(Runnable runnable) {
    return super.newThread(() -> {
      BELONGS_TO_EXECUTOR.set(true);
      runnable.run();
    });
  }

  /**
   * Indicates whether the thread belongs to D2 executors or not.
   */
  public static boolean isFromExecutor() {
    return BELONGS_TO_EXECUTOR.get();
  }
}
