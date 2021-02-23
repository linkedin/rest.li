/*
 * Copyright 2021 LinkedIn, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.linkedin.restli.client;

import com.linkedin.parseq.Engine;
import com.linkedin.parseq.Task;
import com.linkedin.parseq.function.Failure;
import com.linkedin.parseq.function.Success;
import com.linkedin.parseq.promise.Promises;
import com.linkedin.parseq.promise.SettablePromise;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;


/**
 * The JDK default {@link CompletionStage} implementation, i.e. {@link CompletableFuture}, uses Fork-Join model which optimizes
 * some of the CPU intensive workload.
 *
 * We provide an ParSeq based {@link CompletionStage} implementation which will be optimized for IO intensive workload
 * with batching support and suits Rest.Li use cases.
 *
 * {@link ParSeqBasedCompletionStage} can be created from ParSeq {@link Task}, {@link Future}, {@link CompletionStage},
 * {@link Supplier}, {@link Runnable} or value/failures directly.
 *
 * This class cannot be constructed directly.
 * User should initialize the {@link ParSeqBasedCompletionStageFactory} to create the stage. One needs to pass an {@link Engine} to the
 * {@link ParSeqBasedCompletionStageFactory} in order to create the stage. All task will be executed in the engine's executors unless {@link CompletionStage}'s async methods are used.
 *
 * One can configure {@link ParSeqBasedCompletionStageFactory} with a {@link java.util.concurrent.Executor} so async method will be using this
 * executor. If not specified, the common {@link ForkJoinPool} will be used as the async executor in the async methods.
 *
 * Example:
 * <blockquote><pre>
 *   Engine _engine;
 *   Executor _executor;
 *   Task{@code <String>} task;
 *   ParSeqBasedCompletionStage{@code <String>} stage =
 *    new ParSeqBasedCompletionStageFactory(_engine, _executor).buildStageFromTask(task);
 * </pre></blockquote>
 *
 *
 * @param <T> The Type of the value this CompletionStage is holding
 */
public class ParSeqBasedCompletionStage<T> implements CompletionStage<T>
{

  protected Engine _engine = null;
  protected Task<T> _task = null; // The underlying ParSeq task to acquire the value this completionStage needs
  protected Executor _asyncExecutor = null;

  /**
   * Not allowing to use the class without an engine and executor
   * TODO:
   * (1) Without engine, will use {@link CompletableFuture} default implementation
   * (2) or use default engine
   */
  private ParSeqBasedCompletionStage()
  {
  }

  ParSeqBasedCompletionStage(Engine engine, Executor executor)
  {
    _engine = engine;
    _asyncExecutor = executor != null ? executor : ForkJoinPool.commonPool();
  }

  ParSeqBasedCompletionStage(Engine engine)
  {
    this(engine, null);
  }

  private ParSeqBasedCompletionStage<T> withTask(Task<T> t)
  {
    _task = t;
    return this;
  }

  ParSeqBasedCompletionStage<T> from(CompletionStage<T> completionStage)
  {
    return nextStageByComposingTask(Task.fromCompletionStage("Create from CompletionStage:", () -> completionStage));
  }

  /**
   * Create the stage from a ParSeq task that is executed asynchronously
   * @param task the ParSeq task that is executed asynchronously.
   * @return the new stage created from ParSeq task
   */
  ParSeqBasedCompletionStage<T> from(Task<T> task)
  {
    return withTask(task);
  }

  ParSeqBasedCompletionStage<T> from(Future<T> future, Executor executor)
  {
    return from(ensureFuture(Task.async("Create from Future", () -> {
      final SettablePromise<T> promise = Promises.settable();
      executor.execute(() -> {
        try {
          promise.done(future.get());
        } catch (Throwable t) {
          promise.fail(t);
        }
      });
      return promise;
    })));
  }

  /**
   * Ensure execution of a task will produce a value, by running this task with a engine.
   * @param t a task that has not started
   * @param engine the engine used to start the task
   * @return the same task
   */
  static <U> Task<U> ensureFutureByEngine(Task<U> t, Engine engine)
  {
    engine.run(t);
    return t;
  }

  /**
   * Ensure execution of a task will produce a value
   * i.e schedule or run the task so that it will be executed asynchronously
   */
  private <U> Task<U> ensureFuture(Task<U> task)
  {
    // TODO: to optimize: all ParSeq Task created in lambda should using the same context,
    // this can be achieved by scheduleToRun(_engine) https://github.com/linkedin/parseq/pull/291
    return ensureFutureByEngine(task, _engine);
  }

  /**
   * To wrap the exception from the last stage into a {@link CompletionException} so they can be
   * propagated according to the rules defined in {@link CompletionStage} documentation
   */
  private <U> Task<U> wrapException(Task<U> task)
  {
    return task.transform(prevTaskResult -> {
      if (prevTaskResult.isFailed()) {
        Throwable t = prevTaskResult.getError();
        if (t instanceof CompletionException) {
          return Failure.of(t);
        } else {
          CompletionException ex = new CompletionException(prevTaskResult.getError());
          return Failure.of(ex);
        }
      }
      return Success.of(prevTaskResult.get());
    });
  }

  private <U> ParSeqBasedCompletionStage<U> nextStageByComposingTask(Task<U> composedTask)
  {
    return new ParSeqBasedCompletionStage<U>(_engine, _asyncExecutor).from(ensureFuture(wrapException(composedTask)));
  }

  @Override
  public <U> ParSeqBasedCompletionStage<U> thenApply(Function<? super T, ? extends U> fn)
  {
    return nextStageByComposingTask(_task.map("thenApply", fn::apply));
  }

  @Override
  public <U> ParSeqBasedCompletionStage<U> thenApplyAsync(Function<? super T, ? extends U> fn, Executor executor)
  {
    return nextStageByComposingTask(_task.flatMap("thenApplyAsync", (t) -> Task.blocking(() -> fn.apply(t), executor)));
  }

  @Override
  public <U> ParSeqBasedCompletionStage<U> thenApplyAsync(Function<? super T, ? extends U> fn)
  {
    return thenApplyAsync(fn, _asyncExecutor);
  }

  @Override
  public ParSeqBasedCompletionStage<Void> thenAccept(Consumer<? super T> action)
  {
    return nextStageByComposingTask(_task.flatMap("thenAccept", (t) -> Task.action(() -> action.accept(t))));
  }

  @Override
  public ParSeqBasedCompletionStage<Void> thenAcceptAsync(Consumer<? super T> action, Executor executor)
  {
    return nextStageByComposingTask(_task.flatMap("thenAcceptAsync", t -> Task.blocking(() -> {
      action.accept(_task.get());
      return null;
    }, executor)));
  }

  @Override
  public ParSeqBasedCompletionStage<Void> thenAcceptAsync(Consumer<? super T> action)
  {
    return thenAcceptAsync(action, _asyncExecutor);
  }

  @Override
  public ParSeqBasedCompletionStage<Void> thenRun(Runnable action)
  {
    return nextStageByComposingTask(_task.flatMap("thenRun", (t) -> Task.action(action::run)));
  }

  @Override
  public ParSeqBasedCompletionStage<Void> thenRunAsync(Runnable action, Executor executor)
  {
    return nextStageByComposingTask(_task.flatMap("thenRunAsync", t -> Task.blocking(() -> {
      action.run();
      return null;
    }, executor)));
  }

  @Override
  public ParSeqBasedCompletionStage<Void> thenRunAsync(Runnable action)
  {
    return thenRunAsync(action, _asyncExecutor);
  }

  @Override
  public <U> ParSeqBasedCompletionStage<U> thenCompose(Function<? super T, ? extends CompletionStage<U>> fn)
  {
    return nextStageByComposingTask(_task.flatMap("thenCompose", t ->
        // Note: Need to wrap here since it is dependent of the returned composedTask
        wrapException(Task.fromCompletionStage(() -> fn.apply(t)))));
  }

  @Override
  public <U> ParSeqBasedCompletionStage<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn,
      Executor executor)
  {
    return nextStageByComposingTask(_task.flatMap("thenCompose", t -> Task.async(() -> {
      final SettablePromise<U> promise = Promises.settable();
      executor.execute(() -> {
        CompletionStage<? extends U> future = fn.apply(t);
        future.whenComplete((value, exception) -> {
          if (exception != null) {
            promise.fail(exception);
          } else {
            promise.done(value);
          }
        });
      });
      return promise;
    })));
  }

  @Override
  public <U> ParSeqBasedCompletionStage<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn)
  {
    return thenComposeAsync(fn, _asyncExecutor);
  }

  @Override
  public <U, V> ParSeqBasedCompletionStage<V> thenCombine(CompletionStage<? extends U> other,
      BiFunction<? super T, ? super U, ? extends V> fn)
  {
    Task<U> that = Task.fromCompletionStage(() -> other);
    return nextStageByComposingTask(Task.par(_task, that).map("thenCombine", fn::apply));
  }

  @Override
  public <U, V> ParSeqBasedCompletionStage<V> thenCombineAsync(CompletionStage<? extends U> other,
      BiFunction<? super T, ? super U, ? extends V> fn, Executor executor)
  {
    Task<U> that = Task.fromCompletionStage(() -> other);
    return nextStageByComposingTask(
        Task.par(_task, that).flatMap("thenCombineAsync", (t, u) -> Task.blocking(() -> fn.apply(t, u), executor)));
  }

  @Override
  public <U, V> ParSeqBasedCompletionStage<V> thenCombineAsync(CompletionStage<? extends U> other,
      BiFunction<? super T, ? super U, ? extends V> fn)
  {
    return thenCombineAsync(other, fn, _asyncExecutor);
  }

  @Override
  public <U> ParSeqBasedCompletionStage<Void> thenAcceptBoth(CompletionStage<? extends U> other,
      BiConsumer<? super T, ? super U> action)
  {
    Task<U> that = Task.fromCompletionStage(() -> other);
    return nextStageByComposingTask(
        Task.par(_task, that).flatMap("thenAcceptBoth", (t, u) -> Task.action(() -> action.accept(t, u))));
  }

  /**
   * If both stage completes exceptionally, the returned stage will complete exceptionally with {@link CompletionException}
   * wrapping the first encountered exception.
   */
  @Override
  public <U> ParSeqBasedCompletionStage<Void> thenAcceptBothAsync(CompletionStage<? extends U> other,
      BiConsumer<? super T, ? super U> action, Executor executor)
  {
    Task<U> that = Task.fromCompletionStage(() -> other);
    return nextStageByComposingTask(Task.par(_task, that).flatMap("thenAcceptBothAsync", (t, u) -> Task.blocking(() -> {
      action.accept(t, u);
      return null;
    }, executor)));
  }

  @Override
  public <U> ParSeqBasedCompletionStage<Void> thenAcceptBothAsync(CompletionStage<? extends U> other,
      BiConsumer<? super T, ? super U> action)
  {
    return thenAcceptBothAsync(other, action, _asyncExecutor);
  }

  /**
   * If both stage completes exceptionally, the returned stage will complete exceptionally with {@link CompletionException}
   * wrapping the first encountered exception.
   */
  @Override
  public ParSeqBasedCompletionStage<Void> runAfterBoth(CompletionStage<?> other, Runnable action)
  {
    Task<?> that = Task.fromCompletionStage(() -> other);
    return nextStageByComposingTask(Task.par(_task, that).flatMap("runAfterBoth", t -> Task.action(action::run)));
  }

  @Override
  public ParSeqBasedCompletionStage<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action,
      Executor executor)
  {
    Task<?> that = Task.fromCompletionStage(() -> other);
    return nextStageByComposingTask(Task.par(_task, that).flatMap("thenAcceptBothAsync", (t, u) -> Task.blocking(() -> {
      action.run();
      return null;
    }, executor)));
  }

  @Override
  public ParSeqBasedCompletionStage<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action)
  {
    return runAfterBothAsync(other, action, _asyncExecutor);
  }

  /**
   *  According to the {@link CompletionStage} documentation:
   *  <quote>
   *  If a stage is dependent on either of two others, and only one of them completes exceptionally,
   *  no guarantees are made about whether the dependent stage completes normally or exceptionally
   *  </quote>
   *
   *  Therefore we only need to guarantee that if both stage completes exceptionally, the returned stage also completes
   *  exceptionally.
   */
  private <U> ParSeqBasedCompletionStage<U> produceEitherStage(String taskName, CompletionStage<? extends T> other,
      Function<? super T, U> fn)
  {
    Task<T> that = Task.fromCompletionStage(() -> other);
    // TODO: Synchronization is now needed since we cannot enforce a happen-before relation.
    //       This can be optimized once ensureFuture() switch to use ParSeq's scheduleToRun() implementation,
    //       so that both completionStage' tasks will be added to the same plan. In the same plan, tasks are executed
    //       in serial order, therefore one can cancel another to ensure only one executes.
    final AtomicBoolean[] sync = {new AtomicBoolean(false)};
    return nextStageByComposingTask(Task.async(taskName, () -> {
      final SettablePromise<U> result = Promises.settable();
      Stream.of(_task, that).map(task -> task.onFailure(throwable -> {
        // If either fail, will also fail the stage.
        // This is to keep consistent with current {@link CompletableFuture} implementation;
        // Note this behavior, according to the {@link CompletionStage} documentation is undefined.
        if (sync[0].compareAndSet(false, true)) {
          result.fail(throwable); // If any failed, try to fail the promise (failfast)
        }
      }).andThen((t) -> {
        if (sync[0].compareAndSet(false, true)) {
          try {
            result.done(fn.apply(t));
          } catch (Throwable throwable) {
            result.fail(throwable);
          }
        }
      })).forEach(this::ensureFuture);

      return result;
    }));
  }

  private <U> ParSeqBasedCompletionStage<U> produceEitherStageAsync(String taskName, CompletionStage<? extends T> other,
      Function<? super T, U> fn, Executor executor)
  {
    Task<T> that = Task.fromCompletionStage(() -> other);
    final AtomicBoolean[] sync = {new AtomicBoolean(false)};
    return nextStageByComposingTask(Task.async(taskName, () -> {
      final SettablePromise<U> result = Promises.settable();
      Stream.of(_task, that).map(task -> task.onFailure(throwable -> {
        if (sync[0].compareAndSet(false, true)) {
          result.fail(throwable);
        }
      }).flatMap((t) -> Task.blocking(() -> {
        if (sync[0].compareAndSet(false, true)) {
          try {
            result.done(fn.apply(t));
          } catch (Throwable throwable) {
            result.fail(throwable);
          }
        }
        return (U) null;
      }, executor))).forEach(this::ensureFuture);

      return result;
    }));
  }

  @Override
  public <U> ParSeqBasedCompletionStage<U> applyToEither(CompletionStage<? extends T> other, Function<? super T, U> fn)
  {
    return produceEitherStage("applyToEither", other, fn);
  }

  @Override
  public <U> ParSeqBasedCompletionStage<U> applyToEitherAsync(CompletionStage<? extends T> other,
      Function<? super T, U> fn, Executor executor)
  {
    return produceEitherStageAsync("applyToEitherAsync", other, fn, executor);
  }

  @Override
  public <U> ParSeqBasedCompletionStage<U> applyToEitherAsync(CompletionStage<? extends T> other,
      Function<? super T, U> fn)
  {
    return applyToEitherAsync(other, fn, _asyncExecutor);
  }

  @Override
  public ParSeqBasedCompletionStage<Void> acceptEither(CompletionStage<? extends T> other, Consumer<? super T> action)
  {
    return produceEitherStage("acceptEither", other, (t) -> {
      action.accept(t);
      return null;
    });
  }

  @Override
  public ParSeqBasedCompletionStage<Void> acceptEitherAsync(CompletionStage<? extends T> other,
      Consumer<? super T> action, Executor executor)
  {
    return produceEitherStageAsync("applyEitherAsync", other, (t) -> {
      action.accept(t);
      return null;
    }, executor);
  }

  @Override
  public ParSeqBasedCompletionStage<Void> acceptEitherAsync(CompletionStage<? extends T> other,
      Consumer<? super T> action)
  {
    return acceptEitherAsync(other, action, _asyncExecutor);
  }

  /**
   * Cast {@code CompletionStage<U>} to {@code CopmletionStage<T>}
   *
   */
  private <U> CompletionStage<T> cast(CompletionStage<U> other, Function<? super U, ? extends T> fn)
  {
    return ensureFuture(Task.async("cast", () -> {
      final SettablePromise<T> promise = Promises.settable();
      other.whenComplete((value, exception) -> {
        if (exception != null) {
          promise.fail(exception);
        } else {
          promise.done(fn.apply(value));
        }
      });
      return promise;
    })).toCompletionStage();
  }

  @Override
  public ParSeqBasedCompletionStage<Void> runAfterEither(CompletionStage<?> other, Runnable action)
  {
    return produceEitherStage("runAfterEither", cast(other, (v) -> null), (t) -> {
      action.run();
      return null;
    });
  }

  @Override
  public ParSeqBasedCompletionStage<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action,
      Executor executor)
  {
    return produceEitherStageAsync("runAfterEitherAsync", cast(other, (v) -> null), (t) -> {
      action.run();
      return null;
    }, executor);
  }

  @Override
  public ParSeqBasedCompletionStage<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action)
  {
    return runAfterEitherAsync(other, action, _asyncExecutor);
  }

  @Override
  public ParSeqBasedCompletionStage<T> exceptionally(Function<Throwable, ? extends T> fn)
  {
    return nextStageByComposingTask(_task.recover(fn::apply));
  }

  @Override
  public <U> ParSeqBasedCompletionStage<U> handle(BiFunction<? super T, Throwable, ? extends U> fn)
  {
    return nextStageByComposingTask(_task.transform("handle", prevTaskResult -> {
      try {
        return Success.of(fn.apply(prevTaskResult.isFailed() ? null : prevTaskResult.get(), prevTaskResult.getError()));
      } catch (Throwable throwable) {
        return Failure.of(throwable);
      }
    }));
  }

  @Override
  public <U> ParSeqBasedCompletionStage<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn,
      Executor executor)
  {
    return nextStageByComposingTask(_task.transformWith("handleAsync", (prevTaskResult) -> Task.blocking(
        () -> fn.apply(prevTaskResult.isFailed() ? null : prevTaskResult.get(), prevTaskResult.getError()), executor)));
  }

  @Override
  public <U> ParSeqBasedCompletionStage<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn)
  {
    return handleAsync(fn, _asyncExecutor);
  }

  @Override
  public ParSeqBasedCompletionStage<T> whenComplete(BiConsumer<? super T, ? super Throwable> action)
  {
    return nextStageByComposingTask(_task.transform("whenComplete", prevTaskResult -> {
      if (prevTaskResult.isFailed()) {
        try {
          action.accept(null, prevTaskResult.getError());
        } catch (Throwable e) {
          // no ops
        }
        return Failure.of(prevTaskResult.getError());
      } else {
        try {
          action.accept(prevTaskResult.get(), prevTaskResult.getError());
        } catch (Throwable e) {
          return Failure.of(e);
        }
        return Success.of(prevTaskResult.get());
      }
    }));
  }

  @Override
  public ParSeqBasedCompletionStage<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action,
      Executor executor)
  {
    return nextStageByComposingTask(_task.transformWith("whenCompleteAsync", prevTaskResult -> {
      if (prevTaskResult.isFailed()) {
        return Task.blocking(() -> {
          try {
            action.accept(null, prevTaskResult.getError());
          } catch (Exception e) {
            // no ops
          }
          return null;
        }, executor)
            .flatMap((t) -> Task.failure(prevTaskResult.getError())); // always Complete the stage with original failure
      } else {
        return Task.blocking(() -> {
          action.accept(prevTaskResult.get(),
              prevTaskResult.getError()); // Complete the stage with original value or new failure
          return prevTaskResult.get();
        }, executor);
      }
    }));
  }

  @Override
  public ParSeqBasedCompletionStage<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action)
  {
    return whenCompleteAsync(action, _asyncExecutor);
  }

  @Override
  public CompletableFuture<T> toCompletableFuture()
  {
    final CompletableFuture<T> future = new CompletableFuture<T>();
    _task.toCompletionStage().whenComplete((value, exception) -> {
      if (exception != null) {
        future.completeExceptionally(exception);
      } else {
        future.complete(value);
      }
    });
    return future;
  }

  /* ------------- For testing -------------- */

  Task<T> getTask()
  {
    return _task;
  }
}
