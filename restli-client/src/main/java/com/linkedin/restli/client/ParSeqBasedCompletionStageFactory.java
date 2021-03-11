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
import com.linkedin.parseq.promise.Promises;
import com.linkedin.parseq.promise.SettablePromise;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.function.Supplier;


/**
 * A factory class to build CompletionStage. Note {@link ParSeqBasedCompletionStage} can not be directly built unless
 * the factory is used.
 *
 */
class ParSeqBasedCompletionStageFactory<T>
{
  private Engine _engine = null;
  private Executor _asyncExecutor = null;

  public ParSeqBasedCompletionStageFactory(Engine engine, Executor executor)
  {
    _engine = engine;
    _asyncExecutor = executor != null ? executor : ForkJoinPool.commonPool();
  }

  public ParSeqBasedCompletionStageFactory(Engine engine)
  {
    this(engine, null);
  }

  private void checkEngine()
  {
    if (_engine == null) {
      throw new IllegalArgumentException("Engine need to be set in order to build ParSeqBasedCompletionStage");
    }
  }

  /**
   * build {@link ParSeqBasedCompletionStage} by using a {@link Task}
   * Note the input Task needs to run by the {@link Engine} so it can produce an output to create this Stage
   *
   * @param task the input {@link Task} to create the stage, which needs to be run by the engine
   * @return @link ParSeqBasedCompletionStage} instance
   */
  public ParSeqBasedCompletionStage<T> buildStageFromTask(Task<T> task)
  {
    checkEngine();
    return new ParSeqBasedCompletionStage<T>(_engine, _asyncExecutor,task);
  }

  /**
   * build {@link ParSeqBasedCompletionStage} by using a value
   *
   * @param resultValue
   * @return @link ParSeqBasedCompletionStage} instance
   */
  public ParSeqBasedCompletionStage<T> buildStageFromValue(T resultValue)
  {
    Task<T> valueTask = Task.value(resultValue);
    _engine.run(valueTask);
    return buildStageFromTask(valueTask);
  }

  /**
   * build {@link ParSeqBasedCompletionStage} by using a {@link Throwable
   *
   * @param t the throwable used to build the stage
   * @return {@link ParSeqBasedCompletionStage} instance
   */
  public ParSeqBasedCompletionStage<T> buildStageFromThrowable(Throwable t)
  {
    Task<T> valueTask = Task.failure(t);
    _engine.run(valueTask);
    return buildStageFromTask(valueTask);
  }

  /**
   * build {@link ParSeqBasedCompletionStage} by using a {@link Future}
   *
   * @param future the future to be used to build the CompletionStage.
   *               For CompletableFuture, please use {@link #buildStageFromCompletionStage(CompletionStage)}
   * @param executor the executor needed to fetch future result asynchronously
   * @return {@link ParSeqBasedCompletionStage} instance
   */
  public ParSeqBasedCompletionStage<T> buildStageFromFuture(Future<T> future, Executor executor)
  {
    checkEngine();
    return new ParSeqBasedCompletionStage<T>(_engine, _asyncExecutor,
    ParSeqBasedCompletionStage.ensureFutureByEngine(Task.async("Create from Future", () -> {
      final SettablePromise<T> promise = Promises.settable();
      executor.execute(() -> {
        try {
          promise.done(future.get());
        } catch (Throwable t) {
          promise.fail(t);
        }
      });
      return promise;
    }), _engine));
  }

  /**
   * build {@link ParSeqBasedCompletionStage} by using another {@link CompletionStage}
   *
   * @param stage the {@link CompletionStage} used to create the this {@link CompletionStage}
   * @return {@link ParSeqBasedCompletionStage} instance
   */
  public ParSeqBasedCompletionStage<T> buildStageFromCompletionStage(CompletionStage<T> stage)
  {
    checkEngine();
    return new ParSeqBasedCompletionStage<T>(_engine, _asyncExecutor,
        ParSeqBasedCompletionStage.ensureFutureByEngine(
            ParSeqBasedCompletionStage.wrapException(
            Task.fromCompletionStage("Create from CompletionStage:", () ->stage)
            ), _engine)
    );
  }

  /**
   * Return a new {@link ParSeqBasedCompletionStage} that is asynchronously completed
   * by a task running the {@link Runnable}
   *
   * also see {@link CompletableFuture#runAsync(Runnable)}
   *
   * @param runnable the {@link Runnable} to be run in order to complete this stage
   * @return {@link ParSeqBasedCompletionStage} instance completes after running the {@link Runnable}
   */
  public ParSeqBasedCompletionStage<Void> buildStageFromRunnableAsync(Runnable runnable)
  {
    checkEngine();
    return new ParSeqBasedCompletionStage<Void>(_engine, _asyncExecutor,
        ParSeqBasedCompletionStage.ensureFutureByEngine(Task.callable(() -> {
          runnable.run();
          return null;
        }), _engine));
  }

  /**
   * Return a new {@link ParSeqBasedCompletionStage} that is asynchronously completed
   * by a task running the {@link Runnable} in the {@link Executor} passed in.
   *
   * also see {@link #buildStageFromRunnableAsync(Runnable)}
   *
   * @param runnable the {@link Runnable} to be run in the executor
   * @param executor the {@link Executor} to run the {@link Runnable}
   * @return {@link ParSeqBasedCompletionStage} instance completes after running the {@link Runnable} in the {@link Executor}
   */
  public ParSeqBasedCompletionStage<Void> buildStageFromRunnableAsync(Runnable runnable, Executor executor)
  {
    checkEngine();
    return new ParSeqBasedCompletionStage<Void>(_engine, _asyncExecutor,
        ParSeqBasedCompletionStage.ensureFutureByEngine(Task.blocking(() -> {
          runnable.run();
          return null;
        }, executor), _engine));
  }

  /**
   * Return a new {@link ParSeqBasedCompletionStage} that is asynchronously completed
   * by a task running the {@link Runnable} with the value obtained by calling the given {@link Supplier}.
   *
   * also see {@link CompletableFuture#supplyAsync(Supplier)}}
   *
   * @param supplier the {@link Supplier} to be run in order to obtain the value to complete this stage.
   * @return {@link ParSeqBasedCompletionStage} instance completes after running the {@link Supplier}
   */
  public ParSeqBasedCompletionStage<T> buildStageFromSupplierAsync(Supplier<T> supplier)
  {
    checkEngine();
    return new ParSeqBasedCompletionStage<T>(_engine, _asyncExecutor,
        ParSeqBasedCompletionStage.ensureFutureByEngine(Task.callable(supplier::get), _engine));
  }

  public ParSeqBasedCompletionStage<T> buildStageFromSupplierAsync(Supplier<T> supplier, Executor executor)
  {
    checkEngine();
    return new ParSeqBasedCompletionStage<T>(_engine, _asyncExecutor,
        ParSeqBasedCompletionStage.ensureFutureByEngine(Task.blocking(supplier::get, executor), _engine));
  }
}
