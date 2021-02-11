package com.linkedin.restli.client;
import com.linkedin.parseq.Engine;
import com.linkedin.parseq.ParSeqUnitTestHelper;
import com.linkedin.parseq.Task;
import com.linkedin.restli.common.EntityResponse;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


/**
 * Unit test for {@link ParSeqBasedCompletionStage}
 */
public class TestParSeqBasedCompletionStage
{
  ParSeqUnitTestHelper _parSeqUnitTestHelper;
  Engine _engine;
  ParSeqBasedCompletionStage.Builder _builder;



  @BeforeClass
  public void setup() throws Exception
  {
    _parSeqUnitTestHelper = new ParSeqUnitTestHelper();
    _parSeqUnitTestHelper.setUp();
    _engine = _parSeqUnitTestHelper.getEngine();
    _builder = ParSeqBasedCompletionStage.Builder.with(_engine);
  }

  @Test
  public void testCreateStageFromValue() throws Exception
  {
    String testResult = "testCreateStageFromValue";
    ParSeqBasedCompletionStage<String> stageFromValue
        = _builder.createStageFromValue(testResult);
    Assert.assertEquals(testResult, stageFromValue.toCompletableFuture().get());
  }

  @Test
  public void testCreateStageFromTask() throws Exception
  {
    String testResult = "testCreateStageFromTask";
    Task<String> valueTask = Task.value(testResult);
    _engine.run(valueTask);
    ParSeqBasedCompletionStage<String> stageFromTask
    = _builder.createStageFromTask(valueTask);
    Assert.assertEquals(testResult, stageFromTask.toCompletableFuture().get());
  }


  @Test
  public void testCreateStageFromParSeqBasedCompletionStage() throws Exception
  {
    String testResult = "testCreateStageFromCompletaitonStage";
    ParSeqBasedCompletionStage<String> stageFromValue
        = _builder.createStageFromValue(testResult);
    ParSeqBasedCompletionStage<String> stageFromCompletionStage =
    _builder.createStageFromCompletionStage(stageFromValue);
    Assert.assertEquals(testResult, stageFromCompletionStage.toCompletableFuture().get());
  }

  @Test
  public void testCreateStageFromCompletableFuture() throws Exception
  {
    String testResult = "testCreateStageFromCompletableFuture";
    CompletableFuture<String> completableFuture = new CompletableFuture<>();
    completableFuture.complete(testResult);
    ParSeqBasedCompletionStage<String> stageFromCompletionStage =
        _builder.createStageFromCompletionStage(completableFuture);
    Assert.assertEquals(testResult, stageFromCompletionStage.toCompletableFuture().get());
  }

  @Test
  public void testThenApply() throws Exception
  {
    String stage1Value = "stage1Value";
    String stage2Value = "stage2Value";
    Task<String> valueTask = Task.value(stage1Value);
    _engine.run(valueTask);
    ParSeqBasedCompletionStage<String> stage2
        = _builder.createStageFromTask(valueTask).thenApply(v -> stage2Value);
    Assert.assertEquals(stage2Value, stage2.toCompletableFuture().get());

  }

  //CountDownLatch latch = new CountDownLatch(1);

  @AfterClass
  void tearDown() throws Exception
  {
    if (_parSeqUnitTestHelper != null)
    {
      _parSeqUnitTestHelper.tearDown();
    }
    else
    {
      throw new RuntimeException("Tried to shut down Engine but it either has not even been created or has "
          + "already been shut down");
    }
  }


}
