/*
   Copyright (c) 2021 LinkedIn Corp.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package com.linkedin.restli.client;

import com.linkedin.parseq.Engine;
import com.linkedin.parseq.ParSeqUnitTestHelper;
import com.linkedin.parseq.Task;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;


/**
 * Unit test for {@link ParSeqBasedCompletionStage}
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class TestParSeqBasedCompletionStage
{
  ParSeqUnitTestHelper _parSeqUnitTestHelper;
  Engine _engine;
  ParSeqBasedCompletionStageFactory<String> _parSeqBasedCompletionStageFactory;
  ExecutorService _executor = ForkJoinPool.commonPool();

  private static final String TESTVALUE1 = "testValue1";
  private static final String TESTVALUE2 = "testValue2";
  private static final String THREAD_NAME_VALUE = "thread_name_value";
  private final Executor _mockExecutor = new RenamingThreadExecutor(THREAD_NAME_VALUE);
  private static final RuntimeException EXCEPTION = new RuntimeException("Test");
  private static final ExecutorService service = Executors.newCachedThreadPool();

  private CompletionException verifyCompletionException()
  {
    return argThat(new ArgumentMatcher<CompletionException>()
    {
      @Override
      public boolean matches(Object argument)
      {
        return argument instanceof CompletionException && ((CompletionException) argument).getCause() == EXCEPTION;
      }
    });
  }

  private CompletionException verifyException()
  {
    return argThat(new ArgumentMatcher<CompletionException>()
    {
      @Override
      public boolean matches(Object argument)
      {
        return argument == EXCEPTION;
      }
    });
  }

  @BeforeClass
  public void setup() throws Exception
  {
    _parSeqUnitTestHelper = new ParSeqUnitTestHelper();
    _parSeqUnitTestHelper.setUp();
    _engine = _parSeqUnitTestHelper.getEngine();
    _parSeqBasedCompletionStageFactory = new ParSeqBasedCompletionStageFactory<String>(_engine);
  }

  @BeforeMethod
  public void prepareMethod()
  {
  }

  /* ------------- Facilities for testing -------------- */

  /**
   * Simulate an Executor
   */
  protected static class RenamingThreadExecutor implements Executor
  {
    private final String threadName;

    protected RenamingThreadExecutor(String threadName)
    {
      this.threadName = threadName;
    }

    @Override
    public void execute(Runnable command)
    {
      String originalName = Thread.currentThread().getName();
      Thread.currentThread().setName(threadName);
      try {
        command.run();
      } finally {
        Thread.currentThread().setName(originalName);
      }
    }
  }

  private CompletionStage<String> createTestStage(String val)
  {
    return createTestStage(val, 100); // Default value: 100 ms
  }

  private CompletionStage<String> createCompletableFuture(String val)
  {
    return createCompletableFuture(val, 100); // Default value: 100 ms
  }

  private CompletionStage<String> createTestStage(String val, long milliSeconds)
  {
    return milliSeconds > 0? createStageFromTask(delayedCompletingTask(val, milliSeconds)): createStageFromValue(val);
//     Uncomment below to Test with CompletableFuture impl
//     return milliSeconds > 0? createCompletableFuture(val, milliSeconds): CompletableFuture.completedFuture(val);
  }

  private <U> CompletableFuture<U> createCompletableFuture(String val, long milliSeconds)
  {
    CompletableFuture stage = new CompletableFuture<>();
    _executor.execute(() -> {
      try {
        Thread.sleep(milliSeconds);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      stage.complete(val);
    });
    return stage;
  }

  private CompletionStage<String> createTestFailedStage(Throwable t)
  {
    return createTestFailedStage(t, 100);
  }

  private CompletionStage<String> createTestFailedStage(Throwable t, long milliSeconds)
  {
    return milliSeconds > 0 ? createStageFromTask(delayedFailingTask(t, milliSeconds)): createStageFromThrowable(t);
    // Uncomment below to Test with CompletableFuture impl
//    CompletableFuture<String> returnStage = new CompletableFuture<>();
//    returnStage.completeExceptionally(t);
//    return milliSeconds > 0 ? createCompletableFuture(milliSeconds, t): returnStage;
  }

  private <U> CompletableFuture<U> createCompletableFuture(long milliSeconds, Throwable t)
  {
    CompletableFuture stage = new CompletableFuture<>();
    _executor.execute(() -> {
      try {
        Thread.sleep(milliSeconds);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      stage.completeExceptionally(t);
    });
    return stage;
  }

  private ParSeqBasedCompletionStage<String> createStageFromValue(String value)
  {
    return _parSeqBasedCompletionStageFactory.buildStageFromValue(value);
  }

  private ParSeqBasedCompletionStage<String> createStageFromTask(Task<String> task)
  {
    return _parSeqBasedCompletionStageFactory.buildStageFromTask(task);
  }

  private ParSeqBasedCompletionStage<String> createStageFromThrowable(Throwable t)
  {
    return _parSeqBasedCompletionStageFactory.buildStageFromThrowable(t);
  }

  private <T> Task<T> delayedCompletingTask(T value, long milliseconds)
  {
    Task<T> task = Task.blocking(() -> {
      Thread.sleep(milliseconds);
      return value;
    }, _executor);
    _engine.run(task);
    return task;
  }

  private <T> Task<T> delayedFailingTask(Throwable t, long milliseconds)
  {
    Task<T> task = Task.blocking(() -> {
      Thread.sleep(milliseconds);
      return null;
    }, _executor).flatMap((v) -> Task.failure(t));
    _engine.run(task);
    return task;
  }

  private <T> T finish(CompletionStage<T> completionStage, long milliseconds) throws Exception
  {
    try {
      return completionStage.toCompletableFuture().get(milliseconds, TimeUnit.MILLISECONDS);
    } catch (InterruptedException | ExecutionException e) { // Only throws TimeoutException
      return null;
    }
  }

  private <T> T finish(CompletionStage<T> completionStage) throws Exception
  {
    return finish(completionStage, 5000);
  }

  /* ------------- testing builder -------------- */

  @Test
  public void testCreateStageFromValue() throws Exception
  {
    String testResult = "testCreateStageFromValue";
    ParSeqBasedCompletionStage<String> stageFromValue =
        _parSeqBasedCompletionStageFactory.buildStageFromValue(testResult);
    Assert.assertEquals(testResult, stageFromValue.toCompletableFuture().get());
  }

  @Test
  public void testCreateStageFromThrowable() throws Exception
  {
    ParSeqBasedCompletionStage<String> stageFromValue =
        _parSeqBasedCompletionStageFactory.buildStageFromThrowable(EXCEPTION);
    try {
      stageFromValue.toCompletableFuture().get();
      fail("Should fail");
    } catch (Exception e) {
      Assert.assertEquals(EXCEPTION, e.getCause());
    }
  }

  @Test
  public void testCreateStageFromTask() throws Exception
  {
    String testResult = "testCreateStageFromTask";
    Task<String> valueTask = Task.value(testResult);
    _engine.run(valueTask);
    ParSeqBasedCompletionStage<String> stageFromTask = _parSeqBasedCompletionStageFactory.buildStageFromTask(valueTask);
    Assert.assertEquals(testResult, stageFromTask.toCompletableFuture().get());
  }

  @Test
  public void testCreateStageFromCompletionStage_ParSeqBasedCompletionStage() throws Exception
  {
    String testResult = "testCreateStageFromCompletionStage";
    ParSeqBasedCompletionStage<String> stageFromValue =
        _parSeqBasedCompletionStageFactory.buildStageFromValue(testResult);
    ParSeqBasedCompletionStage<String> stageFromCompletionStage =
        _parSeqBasedCompletionStageFactory.buildStageFromCompletionStage(stageFromValue);
    Assert.assertEquals(testResult, stageFromCompletionStage.toCompletableFuture().get());
  }

  @Test
  public void testCreateStageFromCompletionStage_CompletableFuture() throws Exception
  {
    String testResult = "testCreateStageFromCompletableFuture";
    CompletableFuture<String> completableFuture = new CompletableFuture<>();
    completableFuture.complete(testResult);
    ParSeqBasedCompletionStage<String> stageFromCompletionStage =
        _parSeqBasedCompletionStageFactory.buildStageFromCompletionStage(completableFuture);
    Assert.assertEquals(testResult, stageFromCompletionStage.toCompletableFuture().get());
  }

  @Test
  public void testCreateStageFromFuture_CompletableFuture() throws Exception
  {
    String testResult = "testCreateStageFromCompletableFuture";
    CompletableFuture<String> completableFuture = new CompletableFuture<>();
    completableFuture.complete(testResult);
    ParSeqBasedCompletionStage<String> stageFromCompletionStage =
        _parSeqBasedCompletionStageFactory.buildStageFromFuture(completableFuture, _executor);
    Assert.assertEquals(testResult, stageFromCompletionStage.toCompletableFuture().get());
  }

  @Test
  public void testCreateStageFromSupplierAsync() throws Exception
  {
    String testResult = "testCreateStageFromCompletableFuture";
    ParSeqBasedCompletionStage<String> stageFromCompletionStage =
        _parSeqBasedCompletionStageFactory.buildStageFromSupplierAsync(() -> testResult);
    Assert.assertEquals(testResult, stageFromCompletionStage.toCompletableFuture().get());
  }

  @Test
  public void testCreateStageFromSupplierAsync_withExecutor() throws Exception
  {
    String testResult = "testCreateStageFromCompletableFuture";
    ParSeqBasedCompletionStage<String> stageFromCompletionStage =
        _parSeqBasedCompletionStageFactory.buildStageFromSupplierAsync(() -> testResult, _executor);
    Assert.assertEquals(testResult, stageFromCompletionStage.toCompletableFuture().get());
  }

  @Test
  public void testCreateStageFromRunnable() throws Exception
  {
    final String[] stringArr = new String[1];
    String testResult = "testCreateStageFromCompletableFuture";
    ParSeqBasedCompletionStage<Void> stageFromCompletionStage =
        _parSeqBasedCompletionStageFactory.buildStageFromRunnableAsync(() -> {
          stringArr[0] = testResult;
        });
    stageFromCompletionStage.toCompletableFuture().get(); //ensure completion
    Assert.assertEquals(stringArr[0], testResult);
  }

  @Test
  public void testCreateStageFromRunnable_withExecutor() throws Exception
  {
    final String[] stringArr = new String[1];
    String testResult = "testCreateStageFromCompletableFuture";
    ParSeqBasedCompletionStage<Void> stageFromCompletionStage =
        _parSeqBasedCompletionStageFactory.buildStageFromRunnableAsync(() -> {
          try {
            Thread.sleep(500);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          stringArr[0] = testResult;
        }, _executor);
    Assert.assertEquals(stringArr[0], null);
    stageFromCompletionStage.toCompletableFuture().get(); //ensure completion
    Assert.assertEquals(stringArr[0], testResult);
  }

  /* ------------- testing toCompletableFuture -------------- */

  @Test
  public void testToCompletableFuture_success() throws Exception
  {
    CompletionStage completableFuture = createTestStage(TESTVALUE1).toCompletableFuture();
    assertEquals(completableFuture.toCompletableFuture().get(), TESTVALUE1);
  }

  @Test
  public void testToCompletableFuture_fail() throws Exception
  {
    CompletableFuture completableFuture = createTestFailedStage(EXCEPTION).toCompletableFuture();
    try {
      completableFuture.get();
    } catch (Exception e) {
      assertEquals(e.getCause(), EXCEPTION);
    }
  }

  /* ------------- testing thenApply, thenAccept, thenRun -------------- */

  @Test
  public void testThenApply() throws Exception
  {
    CompletionStage<String> stage2 = createTestStage(TESTVALUE1).thenApply(v -> TESTVALUE2);
    Assert.assertEquals(TESTVALUE2, stage2.toCompletableFuture().get());
  }

  @Test
  public void testThenApplyAsync() throws Exception
  {
    CompletionStage<String> completionStage = createTestStage(TESTVALUE1);

    CountDownLatch waitLatch = new CountDownLatch(1);

    completionStage.thenApplyAsync(r -> {
      assertEquals(THREAD_NAME_VALUE, Thread.currentThread().getName());
      waitLatch.countDown();
      return "";
    }, _mockExecutor);

    finish(completionStage);
    waitLatch.await(1000, TimeUnit.MILLISECONDS);
  }

  @Test
  public void testThenApply_FailFirst() throws Exception
  {
    BiFunction<Object, Throwable, ?> handler = mock(BiFunction.class);
    finish(createTestFailedStage(EXCEPTION).thenApply(v -> TESTVALUE2).handle(handler));
    verify(handler).apply(isNull(String.class), verifyCompletionException());
  }

  @Test
  public void testThenApply_FailSecond() throws Exception
  {
    BiFunction<Object, Throwable, ?> handler = mock(BiFunction.class);
    finish(createTestStage(TESTVALUE1).thenApply(v -> {
      throw EXCEPTION;
    }).handle(handler));
    verify(handler).apply(isNull(String.class), verifyCompletionException());
  }

  @Test
  public void testThenApply_unFinish() throws Exception
  {
    CountDownLatch waitLatch = new CountDownLatch(1);
    CompletionStage<String> stage2 = createTestStage(TESTVALUE1, 200).thenApply(v -> {
      waitLatch.countDown();
      return TESTVALUE2;
    });
    assertFalse(waitLatch.await(100, TimeUnit.MILLISECONDS));
    finish(stage2);
    assertTrue(waitLatch.await(100, TimeUnit.MILLISECONDS));
  }

  @Test
  public void testThenAccept() throws Exception
  {
    Consumer<String> consumer = mock(Consumer.class);
    finish(createTestStage(TESTVALUE1).thenAccept(consumer));
    verify(consumer, times(1)).accept(TESTVALUE1);
  }

  @Test
  public void testThenAccept_FailFirst() throws Exception
  {
    BiFunction<Object, Throwable, ?> handler = mock(BiFunction.class);
    finish(createTestFailedStage(EXCEPTION).thenAccept(v -> {
    }).handle(handler));
    verify(handler).apply(isNull(String.class), verifyCompletionException());
  }

  @Test
  public void testThenAccept_FailSecond() throws Exception
  {
    BiFunction<Object, Throwable, ?> handler = mock(BiFunction.class);
    finish(createTestStage(TESTVALUE1).thenAccept(v -> {
      throw EXCEPTION;
    }).handle(handler));
    verify(handler).apply(isNull(String.class), verifyCompletionException());
  }

  @Test
  public void testThenAcceptAsync() throws Exception
  {
    CompletionStage<String> completionStage = createTestStage(TESTVALUE1);

    CountDownLatch waitLatch = new CountDownLatch(1);

    completionStage.thenAcceptAsync(r -> {
      assertEquals(THREAD_NAME_VALUE, Thread.currentThread().getName());
      waitLatch.countDown();
    }, _mockExecutor);

    finish(completionStage);
    waitLatch.await(1000, TimeUnit.MILLISECONDS);
  }

  @Test
  public void testThenRun() throws Exception
  {
    CompletionStage<String> completionStage = createTestStage(TESTVALUE1);
    Runnable runnable = mock(Runnable.class);
    finish(completionStage.thenRun(runnable));
    verify(runnable, times(1)).run();
  }

  @Test
  public void testThenRun_FailFirst() throws Exception
  {
    BiFunction<Object, Throwable, ?> handler = mock(BiFunction.class);
    finish(createTestFailedStage(EXCEPTION).thenRun(() -> {
    }).handle(handler));
    verify(handler).apply(isNull(String.class), verifyCompletionException());
  }

  @Test
  public void testThenRun_FailSecond() throws Exception
  {
    BiFunction<Object, Throwable, ?> handler = mock(BiFunction.class);
    finish(createTestStage(TESTVALUE1).thenRun(() -> {
      throw EXCEPTION;
    }).handle(handler));
    verify(handler).apply(isNull(String.class), verifyCompletionException());
  }

  @Test
  public void testThenRunAsync() throws Exception
  {
    CompletionStage<String> completionStage = createTestStage(TESTVALUE1);

    CountDownLatch waitLatch = new CountDownLatch(1);

    completionStage.thenRunAsync(() -> {
      assertEquals(THREAD_NAME_VALUE, Thread.currentThread().getName());
      waitLatch.countDown();
    }, _mockExecutor);

    finish(completionStage);
    waitLatch.await(1000, TimeUnit.MILLISECONDS);
  }

  /* ------------- testing thenCompose, thenCombine -------------- */

  @Test
  public void testThenCompose_success() throws Exception
  {
    Consumer<String> consumer = mock(Consumer.class);
    CompletionStage<String> completionStage = createTestStage(TESTVALUE1);
    CompletionStage<String> completionStage2 = createTestStage(TESTVALUE2);
    finish(completionStage.thenCompose(r -> completionStage2).thenAccept(consumer));
    verify(consumer, times(1)).accept(TESTVALUE2);
  }

  @Test
  public void testThenCompose_failureFromFirst() throws Exception
  {
    CompletionStage<String> completionStage = createTestFailedStage(EXCEPTION);
    CompletionStage<String> completionStage2 = createTestStage(TESTVALUE2);

    BiFunction<String, Throwable, ?> handler = mock(BiFunction.class);
    CompletionStage completionStage3 = completionStage.thenCompose(r -> completionStage2).handle(handler);

    finish(completionStage);
    finish(completionStage2);
    finish(completionStage3);

    verify(handler).apply(isNull(String.class), verifyCompletionException());
  }

  @Test
  public void testThenCompose_failureFromSecond() throws Exception
  {
    CompletionStage<String> completionStage = createTestStage(TESTVALUE1);
    CompletionStage<String> completionStage2 = createTestFailedStage(EXCEPTION);

    BiFunction<String, Throwable, ?> handler = mock(BiFunction.class);
    CompletionStage completionStage3 = completionStage.thenCompose(r -> completionStage2).handle(handler);

    finish(completionStage);
    finish(completionStage2);
    finish(completionStage3);

    verify(handler).apply(isNull(String.class), verifyCompletionException());
  }

  @Test
  public void testThenComposeAsync() throws Exception
  {
    Consumer<String> consumer = mock(Consumer.class);
    CountDownLatch waitLatch = new CountDownLatch(1);

    CompletionStage<String> completionStage = createTestStage(TESTVALUE1);
    CompletionStage<String> completionStage2 = createTestStage(TESTVALUE2);
    finish(completionStage.thenComposeAsync(r -> {
      assertEquals(THREAD_NAME_VALUE, Thread.currentThread().getName());
      waitLatch.countDown();
      return completionStage2;
    }, _mockExecutor).thenAccept(consumer));

    waitLatch.await(1000, TimeUnit.MILLISECONDS);
    verify(consumer, times(1)).accept(TESTVALUE2);
  }

  @Test
  public void testThenCombine() throws Exception
  {
    CompletionStage<String> completionStage1 = createTestStage(TESTVALUE1);
    CompletionStage<String> completionStage2 = createTestStage(TESTVALUE2);

    BiFunction<String, String, Integer> combiner = mock(BiFunction.class);
    when(combiner.apply(TESTVALUE1, TESTVALUE2)).thenReturn(0);

    Consumer<Integer> intConsumer = mock(Consumer.class);
    finish(completionStage1.thenCombine(completionStage2, combiner).thenAccept(intConsumer));

    verify(combiner).apply(TESTVALUE1, TESTVALUE2);
    verify(intConsumer).accept(0);
  }

  @Test
  public void testThenCombine_testSymmetry() throws Exception
  {
    CompletionStage<String> completionStage1 = createTestStage(TESTVALUE1);
    CompletionStage<String> completionStage2 = createTestStage(TESTVALUE2);

    BiFunction<String, String, Integer> combiner = mock(BiFunction.class);
    when(combiner.apply(TESTVALUE2, TESTVALUE1)).thenReturn(0);

    Consumer<Integer> intConsumer = mock(Consumer.class);
    finish(completionStage2.thenCombine(completionStage1, combiner).thenAccept(intConsumer));

    verify(combiner).apply(TESTVALUE2, TESTVALUE1);
    verify(intConsumer).accept(0);
  }

  @Test
  public void testThenCombineAsync() throws Exception
  {
    CountDownLatch waitLatch = new CountDownLatch(1);
    CompletionStage<String> completionStage1 = createTestStage(TESTVALUE1);
    CompletionStage<String> completionStage2 = createTestStage(TESTVALUE2);
    finish(completionStage1.thenCombineAsync(completionStage2, (a, b) ->  {
        assertEquals(THREAD_NAME_VALUE, Thread.currentThread().getName());
      waitLatch.countDown();
        return 0;
    }, _mockExecutor));
    assertTrue(waitLatch.await(1000, TimeUnit.MILLISECONDS));
  }

  @Test public void testThenCombine_combinerException() throws Exception {
    CompletionStage<String> completionStage1 = createTestStage(TESTVALUE1);
    CompletionStage<String> completionStage2 = createTestStage(TESTVALUE2);
    BiFunction<Object, Throwable, ?> handler = mock(BiFunction.class);

    finish(completionStage1.thenCombine(completionStage2, (a, b) -> {
      throw EXCEPTION;
    }).handle(handler));

    verify(handler).apply(isNull(String.class),verifyCompletionException());
  }

  @Test public void testThenCombine_FirstStageException() throws Exception {

    CompletionStage<String> completionStage1 = createTestFailedStage(EXCEPTION);
    CompletionStage<String> completionStage2 = createTestStage(TESTVALUE2);

    BiFunction<Object, Throwable, ?> handler = mock(BiFunction.class);
    BiFunction<String, String, Integer> combiner = mock(BiFunction.class);

    finish(completionStage1.thenCombine(completionStage2, combiner).handle(handler));

    verify(handler).apply(isNull(String.class), verifyCompletionException());
    verifyZeroInteractions(combiner);
  }

  @Test public void testThenCombine_SecondStageException() throws Exception {
    CompletionStage<String> completionStage1 =  createTestStage(TESTVALUE1);
    CompletionStage<String> completionStage2 = createTestFailedStage(EXCEPTION);

    BiFunction<Object, Throwable, ?> handler = mock(BiFunction.class);
    BiFunction<String, String, Integer> combiner = mock(BiFunction.class);

    finish(completionStage1.thenCombine(completionStage2, combiner).handle(handler));

    verify(handler).apply(isNull(String.class), verifyCompletionException());
    verifyZeroInteractions(combiner);

  }

  /* ------------- testing acceptEither, applyToEither, runAfterEither -------------- */

  @Test
  public void testAcceptEither_Success_Success() throws Exception
  {
    Consumer<Object> consumer = mock(Consumer.class);
    CompletionStage<String> completionStage = createTestStage(TESTVALUE1);
    CompletionStage<String> completionStage2 = createTestStage(TESTVALUE2);
    finish(completionStage.acceptEither(completionStage2, consumer));
    verify(consumer, times(1)).accept(any(String.class));
  }

  @Test
  public void testAcceptEither_Success_UnFinish() throws Exception
  {
    Consumer<Object> consumer = mock(Consumer.class);
    CountDownLatch waitLatch = new CountDownLatch(1);
    CompletionStage<String> completionStage = createTestStage(TESTVALUE1);
    CompletionStage<String> completionStage2 = createTestStage(TESTVALUE2, 1000).thenApply((v) -> {
      waitLatch.countDown();
      return v;
    });
    finish(completionStage.acceptEither(completionStage2, consumer));
    assertFalse(waitLatch.await(100, TimeUnit.MILLISECONDS));
    verify(consumer, times(1)).accept(any(String.class));
  }

  @Test
  public void testAcceptEither_Success_FAIL() throws Exception
  {
    Consumer<Object> consumer = mock(Consumer.class);
    CompletionStage<String> completionStage = createTestStage(TESTVALUE1, 1000);
    CompletionStage<String> completionStage2 = createTestFailedStage(EXCEPTION, 0); // Failure come first

    CompletionStage eitherStage = completionStage.acceptEither(completionStage2, consumer);
    finish(eitherStage);
    finish(completionStage);
    finish(completionStage2);
    try {
      eitherStage.toCompletableFuture().get();
      fail("should fail");
    } catch (Exception ignore) { }
    verify(consumer, never()).accept(any(String.class));
  }

  @Test
  public void testAcceptEither_Fail_UnFinish() throws Exception
  {
    Consumer<Object> consumer = mock(Consumer.class);
    CountDownLatch waitLatch = new CountDownLatch(1);
    CompletionStage<String> completionStage = createTestFailedStage(EXCEPTION, 0);
    CompletionStage<String> completionStage2 = createTestStage(TESTVALUE1, 2000).thenApply((v) -> {
      waitLatch.countDown();
      return TESTVALUE2;
    });
    assertFalse(waitLatch.await(100, TimeUnit.MILLISECONDS));
    CompletionStage eitherStage = completionStage.acceptEither(completionStage2, consumer);
    finish(eitherStage);
    verify(consumer, never()).accept(any(String.class));
  }

  @Test
  public void testAcceptEither_Fail_FAIL() throws Exception
  {
    Consumer<Object> consumer = mock(Consumer.class);
    BiFunction<Object, Throwable, ?> handler = mock(BiFunction.class);
    CompletionStage<String> completionStage = createTestFailedStage(EXCEPTION);
    CompletionStage<String> completionStage2 = createTestFailedStage(EXCEPTION);
    finish(completionStage.acceptEither(completionStage2, consumer).handle(handler));
    verify(consumer, never()).accept(any());
    verify(handler).apply(isNull(String.class), verifyCompletionException());
  }

  @Test
  public void testAcceptEither_UnFinish_UnFinish() throws Exception
  {
    Consumer<Object> consumer = mock(Consumer.class);
    CountDownLatch waitLatch = new CountDownLatch(1);
    CompletionStage<String> completionStage1 = createTestStage(TESTVALUE1, 100);
    CompletionStage<String> completionStage2 = createTestStage(TESTVALUE2, 100);
    CompletionStage stage3 = completionStage1.acceptEither(completionStage2, consumer).thenApply((v) -> {
      waitLatch.countDown();
      return v;
    });
    assertFalse(waitLatch.await(10, TimeUnit.MILLISECONDS));
    finish(stage3);
    assertTrue(waitLatch.await(1000, TimeUnit.MILLISECONDS));
  }

  @Test
  public void testAcceptEitherAsync() throws Exception
  {
    Consumer<String> consumer = mock(Consumer.class);
    CountDownLatch waitLatch = new CountDownLatch(1);

    CompletionStage<String> completionStage = createTestStage(TESTVALUE1);
    CompletionStage<String> completionStage2 = createTestStage(TESTVALUE2);
    finish(completionStage.acceptEitherAsync(completionStage2, r -> {
      assertEquals(THREAD_NAME_VALUE, Thread.currentThread().getName());
      waitLatch.countDown();
    }, _mockExecutor));

    assertTrue(waitLatch.await(1000, TimeUnit.MILLISECONDS));
  }

  @Test
  public void testApplyToEither_Success_Success() throws Exception
  {
    Function<Object, ?> function = mock(Function.class);
    CompletionStage<String> completionStage = createTestStage(TESTVALUE1);
    CompletionStage<String> completionStage2 = createTestStage(TESTVALUE2);
    finish(completionStage.applyToEither(completionStage2, function));
    verify(function, times(1)).apply(any(String.class));
  }

  @Test
  public void testApplyToEither_Success_UnFinish() throws Exception
  {
    Function<Object, ?> function = mock(Function.class);
    CountDownLatch waitLatch = new CountDownLatch(1);
    CompletionStage<String> completionStage = createTestStage(TESTVALUE1);
    CompletionStage<String> completionStage2 = createTestStage(TESTVALUE2, 1000).thenApply((v) -> {
      waitLatch.countDown();
      return v;
    });
    finish(completionStage.applyToEither(completionStage2, function));
    assertFalse(waitLatch.await(100, TimeUnit.MILLISECONDS));
    verify(function, times(1)).apply(any(String.class));
  }

  @Test
  public void testApplyToEither_Success_FAIL() throws Exception
  {
    Function<Object, ?> function = mock(Function.class);
    CompletionStage<String> completionStage = createTestStage(TESTVALUE1, 1000);
    CompletionStage<String> completionStage2 = createTestFailedStage(EXCEPTION, 0); // Failure come first

    CompletionStage eitherStage = completionStage.applyToEither(completionStage2, function);
    finish(eitherStage);
    finish(completionStage);
    finish(completionStage2);
    try {
      eitherStage.toCompletableFuture().get();
      fail("should fail");
    } catch (Exception ignore) { }
    verify(function, never()).apply(any(String.class));
  }

  @Test
  public void testApplyToEither_Fail_UnFinish() throws Exception
  {
    Function<Object, ?> function = mock(Function.class);
    CountDownLatch waitLatch = new CountDownLatch(1);
    CompletionStage<String> completionStage = createTestFailedStage(EXCEPTION, 0);
    CompletionStage<String> completionStage2 = createTestStage(TESTVALUE1, 200).thenApply((v) -> {
      waitLatch.countDown();
      return TESTVALUE2;
    });
    assertFalse(waitLatch.await(100, TimeUnit.MILLISECONDS));
    CompletionStage eitherStage = completionStage.applyToEither(completionStage2, function);
    finish(eitherStage);
    verify(function, never()).apply(any(String.class));
  }

  @Test
  public void testApplyToEither_Fail_FAIL() throws Exception
  {
    BiFunction<Object, Throwable, ?> handler = mock(BiFunction.class);
    Function<Object, ?> function = mock(Function.class);
    CompletionStage<String> completionStage = createTestFailedStage(EXCEPTION);
    CompletionStage<String> completionStage2 = createTestFailedStage(EXCEPTION);
    finish(completionStage.applyToEither(completionStage2, function).handle(handler));
    verify(function, never()).apply(any());
    verify(handler).apply(isNull(String.class), verifyCompletionException());
  }

  @Test
  public void testApplyToEither_UnFinish_UnFinish() throws Exception
  {
    Function<Object, ?> function = mock(Function.class);
    CountDownLatch waitLatch = new CountDownLatch(1);
    CompletionStage<String> completionStage1 = createTestStage(TESTVALUE1, 100);
    CompletionStage<String> completionStage2 = createTestStage(TESTVALUE2, 100);
    CompletionStage stage3 = completionStage1.applyToEither(completionStage2, function).thenApply((v) -> {
      waitLatch.countDown();
      return v;
    });
    assertFalse(waitLatch.await(10, TimeUnit.MILLISECONDS));
    finish(stage3);
    assertTrue(waitLatch.await(1000, TimeUnit.MILLISECONDS));
  }

  @Test
  public void testApplyToEitherAsync() throws Exception
  {
    Consumer<String> consumer = mock(Consumer.class);
    CountDownLatch waitLatch = new CountDownLatch(1);

    CompletionStage<String> completionStage = createTestStage(TESTVALUE1);
    CompletionStage<String> completionStage2 = createTestStage(TESTVALUE2);
    finish(completionStage.applyToEitherAsync(completionStage2, r -> {
      assertEquals(THREAD_NAME_VALUE, Thread.currentThread().getName());
      waitLatch.countDown();
      return r;
    }, _mockExecutor).thenAccept(consumer));

    assertTrue(waitLatch.await(1000, TimeUnit.MILLISECONDS));
  }

  @Test
  public void testRunAfterEither_Success_Success() throws Exception
  {
    Runnable runnable = mock(Runnable.class);
    CompletionStage<String> completionStage = createTestStage(TESTVALUE1);
    CompletionStage<String> completionStage2 = createTestStage(TESTVALUE2);
    finish(completionStage.runAfterEither(completionStage2, runnable));
    verify(runnable, times(1)).run();
  }

  @Test
  public void testRunAfterEither_Success_UnFinish() throws Exception
  {
    Runnable runnable = mock(Runnable.class);
    CountDownLatch waitLatch = new CountDownLatch(1);
    CompletionStage<String> completionStage = createTestStage(TESTVALUE1);
    CompletionStage<String> completionStage2 = createTestStage(TESTVALUE2, 1000).thenApply((v) -> {
      waitLatch.countDown();
      return v;
    });
    finish(completionStage.runAfterEither(completionStage2, runnable));
    assertFalse(waitLatch.await(100, TimeUnit.MILLISECONDS));
    verify(runnable, times(1)).run();
  }

  @Test
  public void testRunAfterEither_Success_FAIL() throws Exception
  {
    Runnable runnable = mock(Runnable.class);
    CompletionStage<String> completionStage = createTestStage(TESTVALUE1, 1000);
    CompletionStage<String> completionStage2 = createTestFailedStage(EXCEPTION, 0); // Failure come first

    CompletionStage eitherStage = completionStage.runAfterEither(completionStage2, runnable);
    finish(eitherStage);
    finish(completionStage);
    finish(completionStage2);
    try {
      eitherStage.toCompletableFuture().get();
      fail("should fail");
    } catch (Exception ignore) { }
    verify(runnable, never()).run();
  }

  @Test
  public void testRunAfterEither_Fail_UnFinish() throws Exception
  {
    Runnable runnable = mock(Runnable.class);
    CountDownLatch waitLatch = new CountDownLatch(1);
    CompletionStage<String> completionStage = createTestFailedStage(EXCEPTION, 0);
    CompletionStage<String> completionStage2 = createTestStage(TESTVALUE1, 500).thenApply((v) -> {
      waitLatch.countDown();
      return TESTVALUE2;
    });
    assertFalse(waitLatch.await(100, TimeUnit.MILLISECONDS));
    CompletionStage eitherStage = completionStage.runAfterEither(completionStage2, runnable);
    finish(eitherStage);
    finish(completionStage2);
    verify(runnable, never()).run();
  }

  @Test
  public void testRunAfterEither_Fail_FAIL() throws Exception
  {
    BiFunction<Object, Throwable, ?> handler = mock(BiFunction.class);
    Runnable runnable = mock(Runnable.class);
    CompletionStage<String> completionStage = createTestFailedStage(EXCEPTION);
    CompletionStage<String> completionStage2 = createTestFailedStage(EXCEPTION);
    finish(completionStage.runAfterEither(completionStage2, runnable).handle(handler));
    verify(runnable, never()).run();
    verify(handler).apply(isNull(String.class), verifyCompletionException());
  }

  @Test
  public void testRunAfterEither_UnFinish_UnFinish() throws Exception
  {
    Runnable runnable = mock(Runnable.class);
    CountDownLatch waitLatch = new CountDownLatch(1);
    CompletionStage<String> completionStage1 = createTestStage(TESTVALUE1, 100);
    CompletionStage<String> completionStage2 = createTestStage(TESTVALUE2, 100);
    CompletionStage stage3 = completionStage1.runAfterEither(completionStage2, runnable).thenApply((v) -> {
      waitLatch.countDown();
      return v;
    });
    assertFalse(waitLatch.await(10, TimeUnit.MILLISECONDS));
    finish(stage3);
    assertTrue(waitLatch.await(1000, TimeUnit.MILLISECONDS));
  }

  @Test
  public void testRunAfterEitherAsync() throws Exception
  {
    Consumer<String> consumer = mock(Consumer.class);
    CountDownLatch waitLatch = new CountDownLatch(1);

    CompletionStage<String> completionStage = createTestStage(TESTVALUE1);
    CompletionStage<String> completionStage2 = createTestStage(TESTVALUE2);
    finish(completionStage.runAfterEitherAsync(completionStage2, () -> {
      assertEquals(THREAD_NAME_VALUE, Thread.currentThread().getName());
      waitLatch.countDown();
    }, _mockExecutor));

    assertTrue(waitLatch.await(1000, TimeUnit.MILLISECONDS));
  }


  /* ------------- testing thenAcceptBoth, runAfterBoth -------------- */

  @Test public void testThenAcceptBoth() throws Exception {
    CompletionStage<String> completionStage1 = createTestStage(TESTVALUE1);
    CompletionStage<String> completionStage2 = createTestStage(TESTVALUE2);

    BiConsumer<String, String> consumer = mock(BiConsumer.class);
    finish(completionStage1.thenAcceptBoth(completionStage2, consumer));
    verify(consumer).accept(TESTVALUE1, TESTVALUE2);
  }

  @Test public void testThenAcceptBoth_exceptionInConsumer() throws Exception {
    CompletionStage<String> completionStage1 = createTestStage(TESTVALUE1);
    CompletionStage<String> completionStage2 = createTestStage(TESTVALUE2);
    Function<Throwable, Void> exceptionallyFunction = mock(Function.class);
    BiConsumer<String, String> consumer = (v,t) -> {
      throw EXCEPTION;
    };
    finish(completionStage1.thenAcceptBoth(completionStage2, consumer).exceptionally(exceptionallyFunction));
    verify(exceptionallyFunction).apply(verifyCompletionException());

  }

  @Test public void testThenAcceptBoth_firstStageException() throws Exception {
    CompletionStage<String> completionStage1 = createTestFailedStage(EXCEPTION);
    CompletionStage<String> completionStage2 = createTestStage(TESTVALUE2);
    Function<Throwable, Void> exceptionallyFunction = mock(Function.class);
    BiConsumer<String, String> consumer = mock(BiConsumer.class);
    finish(completionStage1.thenAcceptBoth(completionStage2, consumer).exceptionally(exceptionallyFunction));
    verifyZeroInteractions(consumer);
    verify(exceptionallyFunction).apply(verifyCompletionException());
  }

  @Test public void testThenAcceptBoth_secondStageException() throws Exception {
    CompletionStage<String> completionStage1 = createTestStage(TESTVALUE1);
    CompletionStage<String> completionStage2 = createTestFailedStage(EXCEPTION);
    Function<Throwable, Void> exceptionallyFunction = mock(Function.class);
    BiConsumer<String, String> consumer = mock(BiConsumer.class);
    finish(completionStage1.thenAcceptBoth(completionStage2, consumer).exceptionally(exceptionallyFunction));
    verifyZeroInteractions(consumer);
    verify(exceptionallyFunction).apply(verifyCompletionException());

  }

  @Test public void testThenAcceptBothAsync() throws Exception {
    CountDownLatch waitLatch = new CountDownLatch(1);
    CompletionStage<String> completionStage1 = createTestStage(TESTVALUE1);
    CompletionStage<String> completionStage2 = createTestStage(TESTVALUE2);

    finish(completionStage1.thenAcceptBothAsync(completionStage2, (a, b) -> {
      assertEquals(THREAD_NAME_VALUE, Thread.currentThread().getName());
      waitLatch.countDown();
    }, _mockExecutor));
    assertTrue(waitLatch.await(1000, TimeUnit.MILLISECONDS));
  }

  @Test public void testRunAfterBoth() throws Exception {
    CompletionStage<String> completionStage1 = createTestStage(TESTVALUE1);
    CompletionStage<String> completionStage2 = createTestStage(TESTVALUE2);

    Runnable runnable = mock(Runnable.class);
    finish(completionStage1.runAfterBoth(completionStage2, runnable));
    verify(runnable, times(1)).run();
  }

  @Test public void testRunAfterBoth_exceptionInRunnable() throws Exception {
    CompletionStage<String> completionStage1 = createTestStage(TESTVALUE1);
    CompletionStage<String> completionStage2 = createTestStage(TESTVALUE2);
    Function<Throwable, Void> exceptionallyFunction = mock(Function.class);
    Runnable runnable = () -> {
      throw EXCEPTION;
    };
    finish(completionStage1.runAfterBoth(completionStage2,runnable).exceptionally(exceptionallyFunction));
    verify(exceptionallyFunction).apply(verifyCompletionException());
  }

  @Test public void testRunAfterBoth_firstStageException() throws Exception {
    CompletionStage<String> completionStage1 = createTestFailedStage(EXCEPTION);
    CompletionStage<String> completionStage2 = createTestStage(TESTVALUE2);
    Function<Throwable, Void> exceptionallyFunction = mock(Function.class);
    Runnable runnable = mock(Runnable.class);
    finish(completionStage1.runAfterBoth(completionStage2, runnable).exceptionally(exceptionallyFunction));
    verifyZeroInteractions(runnable);
    verify(exceptionallyFunction).apply(verifyCompletionException());
  }

  @Test public void testRunAfterBoth_secondStageException() throws Exception {
    CompletionStage<String> completionStage1 = createTestStage(TESTVALUE1);
    CompletionStage<String> completionStage2 = createTestFailedStage(EXCEPTION);
    Function<Throwable, Void> exceptionallyFunction = mock(Function.class);
    Runnable runnable = mock(Runnable.class);
    finish(completionStage1.runAfterBoth(completionStage2, runnable).exceptionally(exceptionallyFunction));
    verifyZeroInteractions(runnable);
    verify(exceptionallyFunction).apply(verifyCompletionException());

  }

  @Test public void testRunAfterBothAsync() throws Exception {
    CountDownLatch waitLatch = new CountDownLatch(1);
    CompletionStage<String> completionStage1 = createTestStage(TESTVALUE1);
    CompletionStage<String> completionStage2 = createTestStage(TESTVALUE2);

    finish(completionStage1.runAfterBothAsync(completionStage2, () -> {
      assertEquals(THREAD_NAME_VALUE, Thread.currentThread().getName());
      waitLatch.countDown();
    }, _mockExecutor));

    assertTrue(waitLatch.await(1000, TimeUnit.MILLISECONDS));
  }

  /* ------------- testing exceptionally, handle, whenComplete -------------- */
  @Test
  public void testExceptionally() throws Exception
  {
    AtomicReference<Throwable> exception = new AtomicReference<>();
    CompletionStage<String> stage = createTestFailedStage(EXCEPTION).exceptionally((t) -> {
      exception.set(t);
      return null;
    });
    finish(stage);
    Assert.assertEquals(exception.get(), EXCEPTION);
  }

  @Test
  public void testExceptionally_noError() throws Exception
  {
    Function<Throwable, String> exceptionallyFunction = mock(Function.class);
    CompletionStage stage = createTestStage(TESTVALUE1).thenApply(v -> v).exceptionally(exceptionallyFunction);
    finish(stage);
    verify(exceptionallyFunction, never()).apply(any());
    assertEquals(stage.toCompletableFuture().get(), TESTVALUE1);
    assertEquals(stage.thenApply(v -> TESTVALUE2).toCompletableFuture().get(), TESTVALUE2);
  }

  @Test
  public void testExceptionally_OnError() throws Exception
  {
    Function<Throwable, String> exceptionallyFunction = mock(Function.class);
    finish(createTestStage(TESTVALUE1).thenApply(v -> {
      throw EXCEPTION;
    }).exceptionally(exceptionallyFunction));
    verify(exceptionallyFunction, times(1)).apply(any(Throwable.class));
  }

  @Test
  public void testExceptionally_noError_notPassingException() throws Exception
  {
    Function<Throwable, String> exceptionallyFunction = mock(Function.class);
    finish(createTestStage(TESTVALUE1).thenApply(v -> v)
        .exceptionally((t) -> TESTVALUE2)
        .exceptionally(exceptionallyFunction));
    verify(exceptionallyFunction, never()).apply(any());
  }

  @Test
  public void testExceptionally_onError_notPassingException() throws Exception
  {
    Function<Throwable, String> exceptionallyFunction = mock(Function.class);
    finish(createTestStage(TESTVALUE1).thenApply(v -> {
      throw EXCEPTION;
    }).exceptionally((t) -> TESTVALUE2).exceptionally(exceptionallyFunction));
  }


  @Test
  public void testExceptionNotPassedToPreviousStage() throws Exception
  {
    Function<Throwable, String> exceptionallyFunction = mock(Function.class);
    CompletionStage<String> completionStage = createTestStage(TESTVALUE1);
    BiFunction<Object, Throwable, ?> handler = mock(BiFunction.class);

    CompletionStage stage2 = completionStage.thenApply(v -> v).exceptionally(exceptionallyFunction).thenApply(i -> {
      throw EXCEPTION;
    }).handle(handler);
    finish(stage2);

    verify(exceptionallyFunction, never()).apply(any());
    verify(handler, times(1)).apply(isNull(), verifyCompletionException());
  }

  @Test
  public void testHandle() throws Exception
  {
    CompletionStage<String> completionStage = createTestStage(TESTVALUE1);

    BiFunction<String, Throwable, Integer> consumer = mock(BiFunction.class);
    finish(completionStage.handle(consumer));

    verify(consumer).apply(TESTVALUE1, null);
  }

  @Test
  public void testHandle_unwrapException() throws Exception
  {
    // CompletionStage explicitly failed should fail with unwrapped exception
    CompletionStage<String> completionStage = createTestFailedStage(EXCEPTION);

    BiFunction<String, Throwable, ?> handler = mock(BiFunction.class);
    finish(completionStage.handle(handler));

    verify(handler, times(1)).apply(isNull(String.class), verifyException());
  }

  @Test
  public void testHandle_notPassingException() throws Exception
  {
    CompletionStage<String> completionStage = createTestFailedStage(EXCEPTION);

    BiFunction<String, Throwable, Integer> consumer = mock(BiFunction.class);
    Function<Throwable, Integer> errorHandler = mock(Function.class);
    finish(completionStage.handle(consumer).exceptionally(errorHandler));

    verify(consumer).apply(null, EXCEPTION);
    verify(errorHandler, never()).apply(any());
  }

  @Test
  public void testHandleAsync() throws Exception
  {
    CountDownLatch waitLatch = new CountDownLatch(1);
    CompletionStage<String> completionStage = createTestStage(TESTVALUE1);
    finish(completionStage);

    completionStage.handleAsync((v, t) -> {
      assertEquals(THREAD_NAME_VALUE, Thread.currentThread().getName());
      waitLatch.countDown();
      return TESTVALUE2;
    }, _mockExecutor);

    assertTrue(waitLatch.await(1000, TimeUnit.MILLISECONDS));
  }

  @Test
  public void testHandle_exceptionFromHandle_success() throws Exception
  {
    Function<Throwable, String> exceptionallyFunction = mock(Function.class);
    CompletionStage<String> completionStage = createTestStage(TESTVALUE1);

    BiFunction<String, Throwable, String> consumer = (v,t) -> {
      throw EXCEPTION;
    };
    finish(completionStage.handle(consumer).exceptionally(exceptionallyFunction));
    verify(exceptionallyFunction).apply(verifyCompletionException());
  }

  @Test
  public void testHandle_exceptionFromHandle_error() throws Exception
  {
    CompletionStage<String> completionStage = createTestFailedStage(EXCEPTION);

    BiFunction<String, Throwable, Integer> consumer = (s, throwable) -> {
      throw EXCEPTION;
    };

    Function<Throwable, Integer> errorHandler = mock(Function.class);
    finish(completionStage.handle(consumer).exceptionally(errorHandler));

    verify(errorHandler).apply(verifyCompletionException());
  }

  @Test public void testExceptionPropagation_shouldNotFailOnThrowable() throws Exception{
    Consumer<Integer> intConsumer = mock(Consumer.class);
    CompletionStage<String> completionStage = createTestFailedStage(EXCEPTION);

    Function<Throwable, Void> errorFunction = mock(Function.class);
    finish(completionStage.thenApply(String::length).thenApply(i -> i * 2).thenAccept(intConsumer).exceptionally(errorFunction));


    verifyZeroInteractions(intConsumer);
    verify(errorFunction, times(1)).apply(verifyCompletionException());


  }

  @Test
  public void testWhenComplete() throws Exception
  {
    BiConsumer<String, Throwable> biConsumer = mock(BiConsumer.class);
    CompletionStage<String> stage = createTestStage(TESTVALUE1).whenComplete(biConsumer);
    finish(stage);
    verify(biConsumer).accept(TESTVALUE1, null);
  }

  @Test
  public void testWhenComplete_withException() throws Exception
  {
    BiConsumer<String, Throwable> biConsumer = mock(BiConsumer.class);
    CompletionStage<String> stage = createTestFailedStage(EXCEPTION).whenComplete(biConsumer);
    finish(stage);
    verify(biConsumer, times(1)).accept(null, EXCEPTION);
  }

  @Test
  public void testWhenComplete_useUnwrappedException() throws Exception
  {
    BiConsumer<String, Throwable> biConsumer = mock(BiConsumer.class);
    CompletionStage<String> completionStage = createTestFailedStage(EXCEPTION);
    finish(completionStage.whenComplete(biConsumer));
    verify(biConsumer, times(1)).accept(null, EXCEPTION);
  }

  @Test
  public void testWhenComplete_completeWithException() throws Exception
  {
    BiConsumer<String, Throwable> consumer = (v, t) -> {
      throw EXCEPTION;
    };

    Function<Throwable, String> exceptionallyFunction = mock(Function.class);

    CompletionStage<String> completionStage = createTestStage(TESTVALUE1);
    finish(completionStage.whenComplete(consumer).exceptionally(exceptionallyFunction));
    verify(exceptionallyFunction, times(1)).apply(verifyCompletionException());
  }

  @Test
  public void testWhenComplete_handleExceptionWithSuccess() throws Exception
  {
    // Also test propagating

    Function<Throwable, String> exceptionallyFunction = mock(Function.class);

    CompletionStage<String> completionStage = createTestFailedStage(EXCEPTION);
    finish(completionStage.whenComplete((v, t) -> {}).exceptionally(exceptionallyFunction));
    verify(exceptionallyFunction, times(1)).apply(verifyCompletionException());
  }

  @Test
  public void testWhenCompleteAsync() throws Exception
  {
    CountDownLatch waitLatch = new CountDownLatch(1);
    CompletionStage<String> stage = createTestStage(TESTVALUE1).whenCompleteAsync((v, t) -> {
      assertEquals(THREAD_NAME_VALUE, Thread.currentThread().getName());
      waitLatch.countDown();
    }, _mockExecutor);
    finish(stage);
    assertTrue(waitLatch.await(1000, TimeUnit.MILLISECONDS));
  }

  private void testWithComposableApi(CompletionStage stage, List<CountDownLatch> latches) throws Exception
  {
    Consumer<String> consumer = mock(Consumer.class);
    CompletionStage<String> completionStage = createTestStage(TESTVALUE1);
    if (latches == null)
    {
      // synchronization not needed, then wait blockingly
      finish(completionStage.thenCompose(r -> stage).thenAccept(consumer));
      verify(consumer, times(1)).accept(TESTVALUE2);
    }
    else
    {
      new Thread(() -> {
        try
        {
          latches.get(0).countDown();
          finish(completionStage.thenCompose(r -> stage).thenAccept(consumer));
          verify(consumer, times(1)).accept(TESTVALUE2);
          latches.get(1).countDown();
        }
        catch (Exception e)
        {
          throw new RuntimeException("Not expected");
        }
      }).start();
    }
  }

  private void testWithUnFinishedStage(CompletionStage stage) throws Exception
  {
    testWithComposableApi(stage, null);
  }

  private void testWithUnStartedStage(CompletionStage stage, Task task) throws Exception
  {
    List<CountDownLatch> latches = new ArrayList<>(Arrays.asList(new CountDownLatch(1), new CountDownLatch(1)));
    testWithComposableApi(stage, latches);
    latches.get(0).await(5000, TimeUnit.MILLISECONDS);
    _engine.run(task);
    latches.get(1).await(5000, TimeUnit.MILLISECONDS);
    verify((ParSeqBasedCompletionStage) stage, times(1)).getTask();
  }

  // To test the correctness of ParSeqBasedCompletionStage##getOrGenerateTaskFromStage
  @Test
  public void testGetTaskOfParSeqBasedCompletionStage() throws Exception {
    // Control: CompletableFuture with completed value
    CompletionStage<String> completionStageCompletableFuture = CompletableFuture.completedFuture(TESTVALUE2);
    testWithUnFinishedStage(completionStageCompletableFuture);

    // treatment: Use a ParSeqBasedCompletionStage with A Task already resolved
    CompletionStage<String> completionStageParSeq = createTestStage(TESTVALUE2, 0);
    assert(completionStageParSeq instanceof ParSeqBasedCompletionStage);
    CompletionStage<String> spyStage = Mockito.spy(completionStageParSeq);
    testWithUnFinishedStage(spyStage);
    verify((ParSeqBasedCompletionStage) spyStage, times(1)).getTask();


    // treatment: Use a ParSeqBasedCompletionStage with a task has not started
    Task<String> testTask = Task.value(TESTVALUE2);
    CompletionStage<String> completionStageParSeq2 = createStageFromTask(testTask);
    assert(completionStageParSeq2 instanceof ParSeqBasedCompletionStage);
    CompletionStage<String> spyStage2 = Mockito.spy(completionStageParSeq2);
    testWithUnStartedStage(spyStage2, testTask);

    // treatment: Use a ParSeqBasedCompletionStage started but will finish later
    CompletionStage<String> completionStageParSeq3 = createTestStage(TESTVALUE2, 100);
    assert(completionStageParSeq3 instanceof ParSeqBasedCompletionStage);
    CompletionStage<String> spyStage3 = Mockito.spy(completionStageParSeq3);
    testWithUnFinishedStage(spyStage3);
    verify((ParSeqBasedCompletionStage) spyStage3, times(1)).getTask();
  }

  /* ------------- testing multi-stages or Comprehensive tests -------------- */

  @Test
  public void testSeveralStageCombinations() throws Exception
  {
    Function<String, CompletionStage<String>> upperCaseFunction =
        s -> _parSeqBasedCompletionStageFactory.buildStageFromValue(s.toUpperCase());

    CompletionStage<String> stage1 = _parSeqBasedCompletionStageFactory.buildStageFromValue("the quick ");

    CompletionStage<String> stage2 = _parSeqBasedCompletionStageFactory.buildStageFromValue("brown fox ");

    CompletionStage<String> stage3 = stage1.thenCombine(stage2, (s1, s2) -> s1 + s2);

    CompletionStage<String> stage4 = stage3.thenCompose(upperCaseFunction);

    CompletionStage<String> stage5 =
        _parSeqBasedCompletionStageFactory.buildStageFromSupplierAsync(simulatedTask(2, "jumped over"));

    CompletionStage<String> stage6 = stage4.thenCombineAsync(stage5, (s1, s2) -> s1 + s2, service);

    CompletionStage<String> stage6_sub_1_slow =
        _parSeqBasedCompletionStageFactory.buildStageFromSupplierAsync(simulatedTask(4, "fell into"));

    CompletionStage<String> stage7 =
        stage6.applyToEitherAsync(stage6_sub_1_slow, String::toUpperCase, service);

    CompletionStage<String> stage8 =
        _parSeqBasedCompletionStageFactory.buildStageFromSupplierAsync(simulatedTask(3, " the lazy dog"), service);

    CompletionStage<String> finalStage = stage7.thenCombineAsync(stage8, (s1, s2) -> s1 + s2, service);

    assertEquals(finalStage.toCompletableFuture().get(), "THE QUICK BROWN FOX JUMPED OVER the lazy dog");
  }

  private Supplier<String> simulatedTask(int numSeconds, String taskResult) throws Exception
  {
    return () -> {
      try {
        Thread.sleep(numSeconds * 100);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      return taskResult;
    };
  }

  @AfterClass
  void tearDown() throws Exception
  {
    if (_parSeqUnitTestHelper != null) {
      _parSeqUnitTestHelper.tearDown();
    } else {
      throw new RuntimeException(
          "Tried to shut down Engine but it either has not even been created or has " + "already been shut down");
    }
  }

}
