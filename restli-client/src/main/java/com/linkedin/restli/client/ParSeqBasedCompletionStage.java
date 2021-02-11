package com.linkedin.restli.client;

import com.linkedin.parseq.Engine;
import com.linkedin.parseq.Task;
import com.linkedin.parseq.function.Success;
import com.linkedin.parseq.promise.Promises;
import com.linkedin.parseq.promise.SettablePromise;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;


public class ParSeqBasedCompletionStage<T> extends CompletableFuture<T>
{

  static class Builder<T>
  {
    private Engine _engine = null;
    private Executor _asyncExecutor = null;

    public Builder(Engine engine, Executor executor)
    {
      _engine = engine;
      _asyncExecutor = executor != null? executor: ForkJoinPool.commonPool();
    }

    public Builder(Engine engine)
    {
      this(engine, null);
    }

    public static Builder with(Engine engine)
    {
      return new Builder(engine);
    }

    public static Builder with(Engine engine, Executor executor)
    {
      return new Builder(engine, executor);
    }

    private void checkEngine()
    {
      if (_engine == null)
      {
        throw new IllegalArgumentException("Engine need to be set in order to create ParSeqBasedCompletionStage");
      }
    }

    public ParSeqBasedCompletionStage<T> createStageFromTask(Task<T> task)
    {
      checkEngine();
      return new ParSeqBasedCompletionStage<T>(_engine, _asyncExecutor).from(task);
    }

    public ParSeqBasedCompletionStage<T> createStageFromValue(T resultValue)
    {
      Task<T> valueTask = Task.value(resultValue);
      _engine.run(valueTask);
      return createStageFromTask(valueTask);
    }

    public ParSeqBasedCompletionStage<T> createStageFromFuture(Future<T> future, Executor executor)
    {
      checkEngine();
      return new ParSeqBasedCompletionStage<T>(_engine, _asyncExecutor).from(future, executor);
    }

    public ParSeqBasedCompletionStage<T> createStageFromCompletionStage(CompletionStage<T> stage)
    {
      checkEngine();
      return new ParSeqBasedCompletionStage<T>(_engine, _asyncExecutor).from(stage);
    }
  }

  protected Engine _engine = null;
  protected Task<T> _task = null; // The underlying ParSeq task to acquire the value this completionStage needs
  protected Executor _asyncExecutor = null;

  /**
   * Not allowing to use the class without an engine and executor
   * TODO:
   * (1) Without engine, will use {@link CompletableFuture} default implementation
   * (2) or use default engine
   */
  private ParSeqBasedCompletionStage() {
  }

  private ParSeqBasedCompletionStage(Engine engine, Executor executor)
  {
    _engine = engine;
    _asyncExecutor = executor != null? executor: ForkJoinPool.commonPool();
  }

  private ParSeqBasedCompletionStage(Engine engine)
  {
    this(engine, null);
  }

  public static ParSeqBasedCompletionStage with(Engine engine)
  {
    return new ParSeqBasedCompletionStage(engine, null);
  }

  public static ParSeqBasedCompletionStage with(Engine engine, Executor executor)
  {
    return new ParSeqBasedCompletionStage(engine, executor);
  }

  private ParSeqBasedCompletionStage<T> withTask(Task<T> t)
  {
    _task = t;
    return this;
  }

  public ParSeqBasedCompletionStage<T> from(CompletionStage<T> completionStage)
  {
    return nextStageByComposingTask(Task.fromCompletionStage("Create from CompletionStage:", () -> completionStage));
  }

  /**
   * Create the stage from a ParSeq task that is executed asynchronously
   * @param task the ParSeq task that is executed asynchronously.
   * @return the new stage created from ParSeq task
   */
  public ParSeqBasedCompletionStage<T> from(Task<T> task)
  {
    ensureFuture(task.transform(
        prevTaskResult -> {
          if (prevTaskResult.isFailed()) {
            this.completeExceptionally(prevTaskResult.getError());
          } else {
            this.complete(prevTaskResult.get());
          }
          return Success.of((Void) null);
        }
    ));
    return withTask(task);
  }

  public ParSeqBasedCompletionStage<T> from(Future<T> future, Executor executor)
  {
    return withTask(Task.async("Create from Future", () -> {
      final SettablePromise<T> promise = Promises.settable();
      executor.execute(() -> {
        try {
          promise.done(future.get());
        } catch (Throwable t) {
          promise.fail(t);
        }
      });
      return promise;
    }));
  }

  /**
   * Schedule or run the task so that it will be executed asynchronously
   */
  private <U> Task<U> ensureFuture(Task<U> task)
  {
    // TODO: optimize:
    // all ParSeq Task created in lambda should using the same context, this can be achieved by scheduleToRun(_engine)
    _engine.run(task);
    return task;
  }

  private <U> ParSeqBasedCompletionStage<U> nextStageByComposingTask(Task<U> composedTask)
  {
    return new ParSeqBasedCompletionStage<U>(_engine, _asyncExecutor).from(ensureFuture(composedTask));
  }

  @Override
  public <U> ParSeqBasedCompletionStage<U> thenApply(Function<? super T, ? extends U> fn) {
    return nextStageByComposingTask(_task.map("thenApply", fn::apply));
  }

  @Override
  public <U> ParSeqBasedCompletionStage<U> thenApplyAsync(Function<? super T, ? extends U> fn, Executor executor) {
    return nextStageByComposingTask(_task.flatMap("thenApplyAsync", (t) -> Task.blocking(() -> fn.apply(t), executor)));
  }

  @Override
  public <U> ParSeqBasedCompletionStage<U> thenApplyAsync(Function<? super T, ? extends U> fn) {
    return thenApplyAsync(fn, _asyncExecutor);
  }

  @Override
  public ParSeqBasedCompletionStage<Void> thenAccept(Consumer<? super T> action) {
    return nextStageByComposingTask(_task.andThen("thenAccept",
        Task.action(() -> action.accept(_task.get()))));
  }

  @Override
  public ParSeqBasedCompletionStage<Void> thenAcceptAsync(Consumer<? super T> action, Executor executor) {
    return nextStageByComposingTask(_task.andThen("thenAcceptAsync", Task.blocking(() -> {
      action.accept(_task.get());
      return null;
    }, executor)));
  }

  @Override
  public ParSeqBasedCompletionStage<Void> thenAcceptAsync(Consumer<? super T> action) {
    return thenAcceptAsync(action, _asyncExecutor);
  }

  @Override
  public ParSeqBasedCompletionStage<Void> thenRun(Runnable action) {
    return nextStageByComposingTask(_task.andThen("thenRun",
        Task.action(action::run)));
  }

  @Override
  public ParSeqBasedCompletionStage<Void> thenRunAsync(Runnable action, Executor executor) {
    return nextStageByComposingTask(_task.andThen("thenRunAsync", Task.blocking(() -> {
      action.run();
      return null;
    }, executor)));
  }

  @Override
  public ParSeqBasedCompletionStage<Void> thenRunAsync(Runnable action) {
    return thenRunAsync(action, _asyncExecutor);
  }

  @Override
  public <U> ParSeqBasedCompletionStage<U> thenCompose(Function<? super T, ? extends CompletionStage<U>> fn) {
    return nextStageByComposingTask(_task.flatMap("thenCompose", t -> Task.fromCompletionStage(() -> fn.apply(t))));
  }

  @Override
  public <U> ParSeqBasedCompletionStage<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn,
      Executor executor) {
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
  public <U> ParSeqBasedCompletionStage<U> thenComposeAsync(Function<? super T, ? extends CompletionStage<U>> fn) {
    return thenComposeAsync(fn, _asyncExecutor);
  }
//
//  @Override
//  public <U, V> ParSeqBasedCompletionStage<V> thenCombine(CompletionStage<? extends U> other,
//      BiFunction<? super T, ? super U, ? extends V> fn) {
//    return null;
//  }
//
//  @Override
//  public <U, V> ParSeqBasedCompletionStage<V> thenCombineAsync(CompletionStage<? extends U> other,
//      BiFunction<? super T, ? super U, ? extends V> fn) {
//    return null;
//  }
//
//  @Override
//  public <U, V> ParSeqBasedCompletionStage<V> thenCombineAsync(CompletionStage<? extends U> other,
//      BiFunction<? super T, ? super U, ? extends V> fn, Executor executor) {
//    return null;
//  }

//  @Override
//  public <U> CompletionStage<Void> thenAcceptBoth(CompletionStage<? extends U> other,
//      BiConsumer<? super T, ? super U> action) {
//    return null;
//  }
//
//  @Override
//  public <U> CompletionStage<Void> thenAcceptBothAsync(CompletionStage<? extends U> other,
//      BiConsumer<? super T, ? super U> action) {
//    return null;
//  }
//
//  @Override
//  public <U> CompletionStage<Void> thenAcceptBothAsync(CompletionStage<? extends U> other,
//      BiConsumer<? super T, ? super U> action, Executor executor) {
//    return null;
//  }
//
//  @Override
//  public CompletionStage<Void> runAfterBoth(CompletionStage<?> other, Runnable action) {
//    return null;
//  }
//
//  @Override
//  public CompletionStage<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action) {
//    return null;
//  }
//
//  @Override
//  public CompletionStage<Void> runAfterBothAsync(CompletionStage<?> other, Runnable action, Executor executor) {
//    return null;
//  }
//
//  @Override
//  public <U> CompletionStage<U> applyToEither(CompletionStage<? extends T> other, Function<? super T, U> fn) {
//    return null;
//  }
//
//  @Override
//  public <U> CompletionStage<U> applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T, U> fn) {
//    return null;
//  }
//
//  @Override
//  public <U> CompletionStage<U> applyToEitherAsync(CompletionStage<? extends T> other, Function<? super T, U> fn,
//      Executor executor) {
//    return null;
//  }
//
//  @Override
//  public CompletionStage<Void> acceptEither(CompletionStage<? extends T> other, Consumer<? super T> action) {
//    return null;
//  }
//
//  @Override
//  public CompletionStage<Void> acceptEitherAsync(CompletionStage<? extends T> other, Consumer<? super T> action) {
//    return null;
//  }
//
//  @Override
//  public CompletionStage<Void> acceptEitherAsync(CompletionStage<? extends T> other, Consumer<? super T> action,
//      Executor executor) {
//    return null;
//  }
//
//  @Override
//  public CompletionStage<Void> runAfterEither(CompletionStage<?> other, Runnable action) {
//    return null;
//  }
//
//  @Override
//  public CompletionStage<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action) {
//    return null;
//  }
//
//  @Override
//  public CompletionStage<Void> runAfterEitherAsync(CompletionStage<?> other, Runnable action, Executor executor) {
//    return null;
//  }

  /* ------------- Exception handling -------------- */
//
//  @Override
//  public CompletionStage<T> exceptionally(Function<Throwable, ? extends T> fn) {
//    return null;
//  }

//  @Override
//  public <U> CompletionStage<U> handle(BiFunction<? super T, Throwable, ? extends U> fn) {
//    return null;
//  }
//
//  @Override
//  public <U> CompletionStage<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn) {
//    return null;
//  }
//
//  @Override
//  public <U> CompletionStage<U> handleAsync(BiFunction<? super T, Throwable, ? extends U> fn, Executor executor) {
//    return null;
//  }

  @Override
  public CompletableFuture<T> toCompletableFuture() {
    final CompletableFuture<T> future = new CompletableFuture<T>();
    _task.toCompletionStage().whenComplete(
        (value, exception) -> {
          if (exception != null) {
            future.completeExceptionally(exception);
          }
          else {
            future.complete(value);
          }
        }
    );
    return future;
  }


  /* ------------- Should not override -------------- */
//
//  @Override
//  public CompletionStage<T> whenComplete(BiConsumer<? super T, ? super Throwable> action) {
//    return null;
//  }
//
//  @Override
//  public CompletionStage<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action) {
//    return null;
//  }
//
//  @Override
//  public CompletionStage<T> whenCompleteAsync(BiConsumer<? super T, ? super Throwable> action, Executor executor) {
//    return null;
//  }
//
  /* ------------- For testing -------------- */

  protected Task<T> getTask() {
    return _task;
  }



}
