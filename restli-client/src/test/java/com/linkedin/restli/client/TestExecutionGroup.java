package com.linkedin.restli.client;

import com.linkedin.parseq.Engine;
import com.linkedin.parseq.ParSeqUnitTestHelper;
import com.linkedin.parseq.Task;
import com.linkedin.parseq.batching.Batch;
import com.linkedin.parseq.batching.BatchingStrategy;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.*;


@SuppressWarnings({"rawtypes", "unchecked"})
public class TestExecutionGroup
{
  private Engine _engine = null;
  private ParSeqUnitTestHelper _parSeqUnitTestHelper = null;
  private MockBatchableResource _resourceClient;
  private Task<Void> task1;
  private ExecutionGroup eg;
  private MockBatchableResource client1;

  @BeforeClass
  public void setup() throws Exception
  {
    _parSeqUnitTestHelper = new ParSeqUnitTestHelper();
    _parSeqUnitTestHelper.setUp();
    _engine = _parSeqUnitTestHelper.getEngine();
  }

  @BeforeMethod
  public void doBeforeMethod()
  {
    eg = new ExecutionGroup(_engine);
    client1 = mock(MockBatchableResource.class);
    when(client1.get(any())).thenReturn("1");
    task1 = Task.callable(() -> {
      client1.get(1L);
      return null;
    });
  }

  @Test
  public void testAddToGroupAndExecute_SingleGroup() throws Exception
  {
    eg.addTaskByFluentClient(client1, task1);
    eg.execute();
    Assert.assertEquals(eg.getClientToTaskListMap().size(), 0);
    awaitAllTasks(task1);
    verify(client1, times(1)).get(any());
  }

  @Test
  public void testAddToGroupAndExecute_MultipleGroup() throws Exception
  {
    // Task will be called twice in two groups
    Task mockTask = mock(Task.class);
    ExecutionGroup eg2 = new ExecutionGroup(_engine);
    eg.addTaskByFluentClient(client1, mockTask);
    eg2.addTaskByFluentClient(client1, mockTask);
    eg.execute();
    eg2.execute();
    Thread.sleep(200); // wait for engine to execute
    verify(mockTask, times(2)).contextRun(any(), any(), any());
  }

  @Test
  public void testBatching() throws Exception
  {
    // Test at least tasks from single client should be batched
    String value1 = "TASK1";
    String value2 = "TASK2";
    _resourceClient = mock(MockBatchableResource.class);
    // Create a strategy that will run the batch on every 2 requests;
    MockBatchingStrategy _mockBatchingStrategy = new MockBatchingStrategy(_resourceClient, 2);
    Task<String> t1 = _mockBatchingStrategy.batchable(1L);
    Task<String> t2 = _mockBatchingStrategy.batchable(2L);
    eg.addTaskByFluentClient(client1, t1, t2);
    eg.execute();
    when(_resourceClient.batchGet(any())).thenReturn(new HashMap<Long, String>()
    {{
      put(1L, value1);
      put(2L, value2);
    }});
    awaitAllTasks(t1, t2);
    verify(_resourceClient, never()).get(any());
    verify(_resourceClient, times(1)).batchGet(any());
  }

  @Test
  public void testCallableExecution() throws Exception
  {
    MockBatchableResource client = new MockBatchableResource();
    eg.batchOn(() -> {
      Assert.assertTrue(client.validateExecutionGroupFromContext(eg));
    }, client);
  }

  @Test
  public void testCallableExecution_Nested() throws Exception
  {
    MockBatchableResource client = new MockBatchableResource();
    eg.batchOn(() -> {
      Assert.assertTrue(client.validateExecutionGroupFromContext(eg)); //outer - eg
      ExecutionGroup eg2 = new ExecutionGroup(_engine);
      try {
        eg2.batchOn(() -> {
          Assert.assertTrue(client.validateExecutionGroupFromContext(eg2)); // inner - eg2
        }, client);
      } catch (Exception ignored) {
      }
      Assert.assertTrue(client.validateExecutionGroupFromContext(eg)); // outer -eg
    }, client);
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

  void awaitAllTasks(Task... tasks) throws Exception
  {
    for (Task t : tasks) {
      t.await();
    }
  }
}

class MockBatchableResource implements FluentClient
{
  public String get(Long key)
  {
    return String.valueOf(key);
  }

  public Map<Long, String> batchGet(Collection<Long> keys)
  {
    return keys.stream().collect(Collectors.toMap(key -> key, key -> get(key)));
  }

  public boolean validateExecutionGroupFromContext(ExecutionGroup eg)
  {
    return eg == this.getExecutionGroupFromContext();
  }
}

class MockBatchingStrategy extends BatchingStrategy<Integer, Long, String>
{
  private final MockBatchableResource _client;
  private int _batchSize = 1024;

  public MockBatchingStrategy(MockBatchableResource client)
  {
    _client = client;
  }

  public MockBatchingStrategy(MockBatchableResource client, int batchSize)
  {
    this(client);
    _batchSize = batchSize;
  }

  @Override
  public void executeBatch(Integer group, Batch<Long, String> batch)
  {
    Map<Long, String> batchResult = _client.batchGet(batch.keys());
    batch.foreach((key, promise) -> promise.done(batchResult.get(key)));
  }

  @Override
  public Integer classify(Long entry)
  {
    return 0;
  }

  @Override
  public int maxBatchSizeForGroup(Integer group)
  {
    return _batchSize;
  }
}
