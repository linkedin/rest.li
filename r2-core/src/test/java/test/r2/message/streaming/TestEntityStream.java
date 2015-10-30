package test.r2.message.streaming;

import com.linkedin.data.ByteString;
import com.linkedin.r2.message.stream.entitystream.EntityStream;
import com.linkedin.r2.message.stream.entitystream.EntityStreams;
import com.linkedin.r2.message.stream.entitystream.Observer;
import com.linkedin.r2.message.stream.entitystream.ReadHandle;
import com.linkedin.r2.message.stream.entitystream.Reader;
import com.linkedin.r2.message.stream.entitystream.WriteHandle;
import com.linkedin.r2.message.stream.entitystream.Writer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Zhenkai Zhu
 */
public class TestEntityStream
{

  @Test
  public void testEntityStream() throws Exception
  {
    TestWriter writer = new TestWriter();
    ControlReader reader = new ControlReader();
    TestObserver ob1 = new TestObserver();
    TestObserver ob2 = new TestObserver();
    EntityStream stream = EntityStreams.newEntityStream(writer);
    stream.addObserver(ob1);
    stream.addObserver(ob2);

    // write is not possible without a reader
    Assert.assertEquals(writer.getWritePossibleCount(), 0);

    stream.setReader(reader);
    // write is not possible before reader reads
    Assert.assertEquals(writer.getWritePossibleCount(), 0);

    reader.read(1);
    // write become possible
    Assert.assertEquals(writer.getWritePossibleCount(), 1);
    Assert.assertEquals(writer.remaining(), 1);
    writer.write();
    Assert.assertEquals(writer.remaining(), 0);

    reader.read(10);
    // write again become possible
    Assert.assertEquals(writer.getWritePossibleCount(), 2);
    Assert.assertEquals(writer.remaining(), 10);
    while(writer.remaining() > 1)
    {
      writer.write();
    }

    Assert.assertEquals(writer.remaining(), 1);
    reader.read(10);
    // write hasn't become impossible when reader reads again, so onWritePossible should not have been invoked again
    Assert.assertEquals(writer.getWritePossibleCount(), 2);

    while(writer.remaining() > 0)
    {
      writer.write();
    }

    Assert.assertEquals(ob1.getChunkCount(), 21);
    Assert.assertEquals(ob2.getChunkCount(), 21);
    Assert.assertEquals(reader.getChunkCount(), 21);

    try
    {
      writer.write();
      Assert.fail("should fail with IllegalStateException");
    }
    catch (IllegalStateException ex)
    {
      // expected
    }
  }

  @Test
  public void testNoStackOverflow() throws Exception
  {
    Writer dumbWriter = new Writer()
    {
      WriteHandle _wh;
      long _count = 0;
      final int _total = 1024 * 1024 * 1024;
      @Override
      public void onInit(WriteHandle wh)
      {
        _wh = wh;
      }

      @Override
      public void onWritePossible()
      {
        while(_wh.remaining() > 0 && _count < _total )
        {
          byte[] bytes = new byte[(int)Math.min(4096, _total - _count)];
          _wh.write(ByteString.copy(bytes));
          _count += bytes.length;
        }
        if (_count >= _total )
        {
          _wh.done();
        }
      }

      @Override
      public void onAbort(Throwable ex)
      {
        // do nothing
      }
    };

    Reader dumbReader = new Reader()
    {
      ReadHandle _rh;
      @Override
      public void onInit(ReadHandle rh)
      {
        _rh = rh;
        _rh.request(1);
      }

      @Override
      public void onDataAvailable(ByteString data)
      {
        _rh.request(1);
      }

      @Override
      public void onDone()
      {

      }

      @Override
      public void onError(Throwable e)
      {

      }
    };

    EntityStreams.newEntityStream(dumbWriter).setReader(dumbReader);
  }

  @Test
  public void testObserverThrow()
  {
    TestWriter writer = new TestWriter();
    ControlReader reader = new ControlReader();
    Observer observer = new TestObserver(){
      @Override
      public void onDone()
      {
        throw new RuntimeException("broken observer throws");
      }

      @Override
      public void onDataAvailable(ByteString data)
      {
        throw new RuntimeException("broken observer throws");
      }

      @Override
      public void onError(Throwable ex)
      {
        throw new RuntimeException("broken observer throws");
      }
    };

    EntityStream es = EntityStreams.newEntityStream(writer);
    es.addObserver(observer);
    es.setReader(reader);
    reader.read(1);
    writer.write();
    writer.done();
    writer.error(new RuntimeException("writer has problem"));

    Assert.assertEquals(writer.abortedTimes(), 0);
    Assert.assertEquals(reader.getChunkCount(), 1);
    Assert.assertEquals(reader.doneTimes(), 1);
    Assert.assertEquals(reader.errorTimes(), 0);

    writer = new TestWriter();
    reader = new ControlReader();
    es = EntityStreams.newEntityStream(writer);
    es.addObserver(observer);
    es.setReader(reader);
    reader.read(1);
    writer.write();
    Exception ex = new RuntimeException("writer has problem");
    writer.error(ex);

    Assert.assertEquals(writer.abortedTimes(), 0);
    Assert.assertEquals(reader.getChunkCount(), 1);
    Assert.assertEquals(reader.errorTimes(), 1);
  }

  @Test
  public void testReaderThrow()
  {
    ControlReader reader = new ControlReader(){
      @Override
      public void onDataAvailable(ByteString data)
      {
        super.onDataAvailable(data);
        throw new RuntimeException("broken reader throws");
      }
    };

    TestWriter writer = new TestWriter();
    TestObserver observer = new TestObserver();
    EntityStream es = EntityStreams.newEntityStream(writer);
    es.addObserver(observer);
    es.setReader(reader);
    reader.read(1);
    writer.write();
    writer.done();

    Assert.assertEquals(writer.abortedTimes(), 1);
    Assert.assertEquals(observer.getChunkCount(), 1);
    Assert.assertEquals(observer.errorTimes(), 1);
    Assert.assertEquals(observer.doneTimes(), 0);
    Assert.assertEquals(reader.getChunkCount(), 1);
    Assert.assertEquals(reader.errorTimes(), 1);
    Assert.assertEquals(reader.doneTimes(), 0);

    writer = new TestWriter();
    observer = new TestObserver();
    reader = new ControlReader(){
      @Override
      public void onDone()
      {
        super.onDone();
        throw new RuntimeException("broken reader throws");
      }
    };
    es = EntityStreams.newEntityStream(writer);
    es.addObserver(observer);
    es.setReader(reader);
    reader.read(1);
    writer.write();
    writer.done();
    Assert.assertEquals(writer.abortedTimes(), 1);
    Assert.assertEquals(observer.getChunkCount(), 1);
    Assert.assertEquals(observer.doneTimes(), 1);
    Assert.assertEquals(observer.errorTimes(), 0);
    Assert.assertEquals(reader.getChunkCount(), 1);
    Assert.assertEquals(reader.doneTimes(), 1);
    Assert.assertEquals(reader.errorTimes(), 0);


    writer = new TestWriter();
    observer = new TestObserver();
    reader = new ControlReader(){
      @Override
      public void onError(Throwable error)
      {
        super.onError(error);
        throw new RuntimeException("broken reader throws");
      }
    };
    es = EntityStreams.newEntityStream(writer);
    es.addObserver(observer);
    es.setReader(reader);
    reader.read(1);
    writer.write();
    writer.error(new RuntimeException("writer got problem"));
    Assert.assertEquals(writer.abortedTimes(), 1);
    Assert.assertEquals(observer.getChunkCount(), 1);
    Assert.assertEquals(observer.doneTimes(), 0);
    Assert.assertEquals(observer.errorTimes(), 1);
    Assert.assertEquals(reader.getChunkCount(), 1);
    Assert.assertEquals(reader.doneTimes(), 0);
    Assert.assertEquals(reader.errorTimes(), 1);
  }

  @Test
  public void testWriterThrow()
  {
    ControlReader reader = new ControlReader() {
      @Override
      public void onDone()
      {
        super.onDone();
        throw new RuntimeException("broken reader throws");
      }
    };
    TestWriter writer = new TestWriter()
    {
      @Override
      public void onAbort(Throwable ex)
      {
        super.onAbort(ex);
        throw new RuntimeException("broken writer throws");
      }
    };

    TestObserver observer = new TestObserver();
    EntityStream es = EntityStreams.newEntityStream(writer);
    es.addObserver(observer);
    es.setReader(reader);
    reader.read(1);
    writer.write();
    writer.done();

    Assert.assertEquals(writer.abortedTimes(), 1);
    Assert.assertEquals(observer.getChunkCount(), 1);
    Assert.assertEquals(observer.doneTimes(), 1);
    Assert.assertEquals(observer.errorTimes(), 0);
    Assert.assertEquals(reader.getChunkCount(), 1);
    Assert.assertEquals(reader.doneTimes(), 1);
    Assert.assertEquals(reader.errorTimes(), 0);

    reader = new ControlReader();
    writer = new TestWriter()
    {
      @Override
      public void onWritePossible()
      {
        throw new RuntimeException("broken writer throws");
      }
    };
    observer = new TestObserver();
    es = EntityStreams.newEntityStream(writer);
    es.addObserver(observer);
    es.setReader(reader);
    reader.read(1);
    writer.write();
    writer.done();

    Assert.assertEquals(writer.abortedTimes(), 1);
    Assert.assertEquals(observer.getChunkCount(), 0);
    Assert.assertEquals(observer.doneTimes(), 0);
    Assert.assertEquals(observer.errorTimes(), 1);
    Assert.assertEquals(reader.getChunkCount(), 0);
    Assert.assertEquals(reader.doneTimes(), 0);
    Assert.assertEquals(reader.errorTimes(), 1);
  }

  @Test
  public void testCancelSimple() throws Exception
  {
    TestWriter writer = new TestWriter();
    TestObserver observer = new TestObserver();
    final ControlReader reader = new ControlReader();
    EntityStream es = EntityStreams.newEntityStream(writer);
    es.addObserver(observer);
    es.setReader(reader);
    reader.read(1000);
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    scheduler.schedule(new Runnable()
    {
      @Override
      public void run()
      {
        reader.cancel();
      }
    }, 50, TimeUnit.MILLISECONDS);

    while(writer.remaining() > 0)
    {
      writer.write();
      Thread.sleep(1);
    }

    Assert.assertEquals(writer.abortedTimes(), 1);
    Assert.assertTrue(observer.getChunkCount() < 100);
    Assert.assertEquals(observer.doneTimes(), 0);
    Assert.assertEquals(observer.errorTimes(), 1);
    Assert.assertEquals(observer.getLastEvent(), "onError");
    Assert.assertTrue(reader.getChunkCount() < 100);
    Assert.assertEquals(reader.doneTimes(), 0);
    Assert.assertEquals(reader.errorTimes(), 0);
    scheduler.shutdown();
  }

  @Test
  public void testCancelWhenNotWritePossible() throws Exception
  {
    TestWriter writer = new TestWriter();
    TestObserver observer = new TestObserver();
    final ControlReader reader = new ControlReader();
    EntityStream es = EntityStreams.newEntityStream(writer);
    es.addObserver(observer);
    es.setReader(reader);
    reader.read(10);
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    final CountDownLatch latch = new CountDownLatch(1);
    scheduler.schedule(new Runnable()
    {
      @Override
      public void run()
      {
        reader.cancel();
        latch.countDown();
      }
    }, 50, TimeUnit.MILLISECONDS);

    while(writer.remaining() > 0)
    {
      writer.write();
    }

    Assert.assertTrue(latch.await(100, TimeUnit.MILLISECONDS));
    Assert.assertEquals(writer.abortedTimes(), 1);
    Assert.assertEquals(observer.getChunkCount(), 10);
    Assert.assertEquals(observer.doneTimes(), 0);
    Assert.assertEquals(observer.errorTimes(), 1);
    Assert.assertEquals(observer.getLastEvent(), "onError");
    Assert.assertEquals(reader.getChunkCount(), 10);
    Assert.assertEquals(reader.doneTimes(), 0);
    Assert.assertEquals(reader.errorTimes(), 0);
    scheduler.shutdown();
  }

  @Test
  public void testRaceBetweenWriteAndCancel() throws Exception
  {
    ExecutorService executor = Executors.newFixedThreadPool(2);

    for (int i = 0; i < 1000; i ++)
    {
      testRaceBetweenWriteAndCancel(executor);
    }

    executor.shutdown();
  }

  private void testRaceBetweenWriteAndCancel(ExecutorService executor) throws Exception
  {
    final CountDownLatch startLatch = new CountDownLatch(1);
    final CountDownLatch finishLatch = new CountDownLatch(2);
    final CountDownLatch prepareLatch = new CountDownLatch(2);
    final TestWriter writer = new TestWriter();
    TestObserver observer = new TestObserver();
    final ControlReader reader = new ControlReader();
    EntityStream es = EntityStreams.newEntityStream(writer);
    es.addObserver(observer);
    es.setReader(reader);
    reader.read(1000);
    executor.submit(new Callable<Object>()
    {
      @Override
      public Object call() throws Exception
      {
        while(writer.remaining() > 0)
        {
          prepareLatch.countDown();
          startLatch.await();
          writer.write();
        }
        finishLatch.countDown();
        return null;
      }
    });

    executor.submit(new Callable<Object>()
    {
      @Override
      public Object call() throws Exception
      {
        prepareLatch.countDown();
        startLatch.await();
        reader.cancel();
        finishLatch.countDown();
        return null;
      }
    });

    prepareLatch.await();
    startLatch.countDown();
    Assert.assertTrue(finishLatch.await(100, TimeUnit.MILLISECONDS));

    // in any case, reader shouldn't fail
    Assert.assertEquals(reader.errorTimes(), 0);
    Assert.assertEquals(writer.abortedTimes(), 1);
    Assert.assertEquals(reader.getChunkCount(), observer.getChunkCount());
    Assert.assertEquals(observer.errorTimes(), 1);
    Assert.assertEquals(observer.getLastEvent(), "onError");
  }

  @Test
  public void testRaceBetweenDoneAndCancel() throws Exception
  {
    ExecutorService executor = Executors.newFixedThreadPool(2);

    for (int i = 0; i < 1000; i ++)
    {
      testRaceBetweenDoneAndCancel(executor);
    }

    executor.shutdown();
  }

  @Test
  public void testReaderInitError() throws Exception
  {
    final CountDownLatch latch = new CountDownLatch(1);
    final TestWriter writer = new TestWriter();
    TestObserver observer = new TestObserver();
    final EntityStream es = EntityStreams.newEntityStream(writer);
    es.addObserver(observer);
    final ControlReader reader = new ControlReader()
    {
      @Override
      public void onInit(ReadHandle rh)
      {
        throw new RuntimeException();
      }
    };
    ExecutorService executor = Executors.newFixedThreadPool(1);
    executor.submit(new Runnable()
    {
      @Override
      public void run()
      {
        es.setReader(reader);
        latch.countDown();
      }
    });

    Assert.assertTrue(latch.await(100, TimeUnit.MILLISECONDS));
    Assert.assertEquals(reader.errorTimes(), 1);
    Assert.assertEquals(observer.errorTimes(), 1);
    Assert.assertEquals(writer.abortedTimes(), 1);
  }

  @Test
  public void testWriterInitError() throws Exception
  {
    final CountDownLatch latch = new CountDownLatch(1);
    final TestWriter writer = new TestWriter() {
      @Override
      public void onInit(WriteHandle wh)
      {
        throw new RuntimeException();
      }
    };
    TestObserver observer = new TestObserver();
    final EntityStream es = EntityStreams.newEntityStream(writer);
    es.addObserver(observer);
    final ControlReader reader = new ControlReader();

    ExecutorService executor = Executors.newFixedThreadPool(1);
    executor.submit(new Runnable()
    {
      @Override
      public void run()
      {
        es.setReader(reader);
        reader.read(5);
        latch.countDown();
      }
    });

    Assert.assertTrue(latch.await(100, TimeUnit.MILLISECONDS));
    Assert.assertEquals(reader.errorTimes(), 1);
    Assert.assertEquals(reader.getChunkCount(), 0);
    Assert.assertEquals(observer.errorTimes(), 1);
    Assert.assertEquals(reader.getChunkCount(), 0);
    Assert.assertEquals(writer.abortedTimes(), 1);
    Assert.assertEquals(writer.getWritePossibleCount(), 0);
  }

  @Test
  public void testWriterAndReaderInitError() throws Exception
  {
    final CountDownLatch latch = new CountDownLatch(1);
    final TestWriter writer = new TestWriter() {
      @Override
      public void onInit(WriteHandle wh)
      {
        throw new RuntimeException();
      }
    };
    TestObserver observer = new TestObserver();
    final EntityStream es = EntityStreams.newEntityStream(writer);
    es.addObserver(observer);
    final ControlReader reader = new ControlReader()
    {
      @Override
      public void onInit(ReadHandle rh)
      {
        throw new RuntimeException();
      }
    };

    ExecutorService executor = Executors.newFixedThreadPool(1);
    executor.submit(new Runnable()
    {
      @Override
      public void run()
      {
        es.setReader(reader);
        latch.countDown();
      }
    });

    Assert.assertTrue(latch.await(100, TimeUnit.MILLISECONDS));
    Assert.assertEquals(reader.errorTimes(), 1);
    Assert.assertEquals(reader.getChunkCount(), 0);
    Assert.assertEquals(observer.errorTimes(), 1);
    Assert.assertEquals(reader.getChunkCount(), 0);
    Assert.assertEquals(writer.abortedTimes(), 1);
    Assert.assertEquals(writer.getWritePossibleCount(), 0);
  }

  @Test
  public void testReaderInitErrorThrowInStreaming() throws Exception
  {
    final CountDownLatch latch = new CountDownLatch(1);
    final TestWriter writer = new TestWriter()
    {
      @Override
      public void onWritePossible()
      {
        super.onWritePossible();
        write();
      }
    };
    TestObserver observer = new TestObserver();
    final EntityStream es = EntityStreams.newEntityStream(writer);
    es.addObserver(observer);
    final ControlReader reader = new ControlReader()
    {
      @Override
      public void onInit(ReadHandle rh)
      {
        rh.request(1);
      }

      @Override
      public void onDataAvailable(ByteString data)
      {
        super.onDataAvailable(data);
        throw new RuntimeException();
      }
    };

    ExecutorService executor = Executors.newFixedThreadPool(1);
    executor.submit(new Runnable()
    {
      @Override
      public void run()
      {
        es.setReader(reader);
        latch.countDown();
      }
    });

    Assert.assertTrue(latch.await(100, TimeUnit.MILLISECONDS));
    Assert.assertEquals(reader.errorTimes(), 1);
    Assert.assertEquals(reader.getChunkCount(), 1);
    Assert.assertEquals(observer.errorTimes(), 1);
    Assert.assertEquals(reader.getChunkCount(), 1);
    Assert.assertEquals(writer.abortedTimes(), 1);
    Assert.assertEquals(writer.getWritePossibleCount(), 1);
  }

  private void testRaceBetweenDoneAndCancel(ExecutorService executor) throws Exception
  {
    final CountDownLatch startLatch = new CountDownLatch(1);
    final CountDownLatch finishLatch = new CountDownLatch(2);
    final CountDownLatch prepareLatch = new CountDownLatch(2);
    final TestWriter writer = new TestWriter();
    TestObserver observer = new TestObserver();
    final ControlReader reader = new ControlReader();
    EntityStream es = EntityStreams.newEntityStream(writer);
    es.addObserver(observer);
    es.setReader(reader);
    reader.read(1000);
    executor.submit(new Callable<Object>()
    {
      @Override
      public Object call() throws Exception
      {
        while(writer.remaining() > 100)
        {
          writer.write();
        }
        prepareLatch.countDown();
        startLatch.await();
        writer.done();
        finishLatch.countDown();
        return null;
      }
    });

    executor.submit(new Callable<Object>()
    {
      @Override
      public Object call() throws Exception
      {
        prepareLatch.countDown();
        startLatch.await();
        reader.cancel();
        finishLatch.countDown();
        return null;
      }
    });

    prepareLatch.await();
    startLatch.countDown();
    Assert.assertTrue(finishLatch.await(100, TimeUnit.MILLISECONDS));

    // in any case, reader shouldn't fail
    Assert.assertEquals(reader.errorTimes(), 0);

    // if done wins the race
    if (reader.doneTimes() > 0)
    {
      Assert.assertEquals(reader.doneTimes(), 1);
      Assert.assertEquals(observer.doneTimes(), 1);
      Assert.assertEquals(observer.errorTimes(), 0);
      Assert.assertEquals(writer.abortedTimes(), 0);
    }
    // if cancel wins the race
    else
    {
      Assert.assertEquals(observer.doneTimes(), 0);
      Assert.assertEquals(observer.errorTimes(), 1);
      Assert.assertEquals(writer.abortedTimes(), 1);
    }
  }

  @Test
  public void testRaceBetweenErrorAndCancel() throws Exception
  {
    ExecutorService executor = Executors.newFixedThreadPool(2);

    for (int i = 0; i < 1000; i ++)
    {
      testRaceBetweenErrorAndCancel(executor);
    }

    executor.shutdown();
  }

  private void testRaceBetweenErrorAndCancel(ExecutorService executor) throws Exception
  {
    final CountDownLatch startLatch = new CountDownLatch(1);
    final CountDownLatch finishLatch = new CountDownLatch(2);
    final CountDownLatch prepareLatch = new CountDownLatch(2);
    final TestWriter writer = new TestWriter();
    TestObserver observer = new TestObserver();
    final ControlReader reader = new ControlReader();
    EntityStream es = EntityStreams.newEntityStream(writer);
    es.addObserver(observer);
    es.setReader(reader);
    reader.read(1000);
    executor.submit(new Callable<Object>()
    {
      @Override
      public Object call() throws Exception
      {
        while(writer.remaining() > 100)
        {
          writer.write();
        }
        prepareLatch.countDown();
        startLatch.await();
        writer.error(new RuntimeException("writer has problem"));
        finishLatch.countDown();
        return null;
      }
    });

    executor.submit(new Callable<Object>()
    {
      @Override
      public Object call() throws Exception
      {
        prepareLatch.countDown();
        startLatch.await();
        reader.cancel();
        finishLatch.countDown();
        return null;
      }
    });

    prepareLatch.await();
    startLatch.countDown();
    Assert.assertTrue(finishLatch.await(100, TimeUnit.MILLISECONDS));

    // if error wins the race
    if (reader.errorTimes() > 0)
    {
      Assert.assertEquals(reader.doneTimes(), 0);
      Assert.assertEquals(observer.doneTimes(), 0);
      Assert.assertEquals(observer.errorTimes(), 1);
      Assert.assertEquals(writer.abortedTimes(), 0);
    }
    // if cancel wins the race
    else
    {
      Assert.assertEquals(observer.doneTimes(), 0);
      Assert.assertEquals(observer.errorTimes(), 1);
      Assert.assertEquals(writer.abortedTimes(), 1);
      Assert.assertEquals(reader.doneTimes(), 0);
      Assert.assertEquals(reader.errorTimes(), 0);
    }
  }

  @Test
  public void testRaceBetweenAbnormalAbortAndCancel() throws Exception
  {
    ExecutorService executor = Executors.newFixedThreadPool(2);

    for (int i = 0; i < 1000; i ++)
    {
      testRaceBetweenAbnormalAbortAndCancel(executor);
    }

    executor.shutdown();
  }

  private void testRaceBetweenAbnormalAbortAndCancel(ExecutorService executor) throws Exception
  {
    final CountDownLatch startLatch = new CountDownLatch(1);
    final CountDownLatch finishLatch = new CountDownLatch(2);
    final CountDownLatch prepareLatch = new CountDownLatch(2);
    final TestWriter writer = new TestWriter();
    TestObserver observer = new TestObserver();
    final ControlReader reader = new ControlReader() {

      @Override
      public void onDataAvailable(ByteString data)
      {
        try
        {
          prepareLatch.countDown();
          startLatch.await();
        }
        catch (Exception ex)
        {
          // ...
        }
        throw new RuntimeException("broken reader throws");
      }
    };

    EntityStream es = EntityStreams.newEntityStream(writer);
    es.addObserver(observer);
    es.setReader(reader);
    reader.read(1000);
    executor.submit(new Callable<Object>()
    {
      @Override
      public Object call() throws Exception
      {
        while(writer.remaining() > 0)
        {
          writer.write();
        }
        finishLatch.countDown();
        return null;
      }
    });

    executor.submit(new Callable<Object>()
    {
      @Override
      public Object call() throws Exception
      {
        prepareLatch.countDown();
        startLatch.await();
        reader.cancel();
        finishLatch.countDown();
        return null;
      }
    });

    prepareLatch.await();
    startLatch.countDown();
    Assert.assertTrue(finishLatch.await(100, TimeUnit.MILLISECONDS));

    // we should always fail because cancel on reader side wouldn't cause cancel action if
    // writer is already in the writing process
    Assert.assertEquals(writer.abortedTimes(), 1);
    Assert.assertEquals(observer.errorTimes(), 1);
    Assert.assertEquals(observer.getChunkCount(), 1);
    Assert.assertEquals(observer.getLastEvent(), "onError");
    Assert.assertEquals(reader.errorTimes(), 1);
    Assert.assertEquals(reader.getChunkCount(), 0);
  }


  private static class ControlReader extends TestObserver implements Reader
  {
    ReadHandle _rh;

    public void read(int n)
    {
      _rh.request(n);
    }

    public void cancel()
    {
      _rh.cancel();
    }

    @Override
    public void onInit(ReadHandle rh)
    {
      _rh = rh;
    }
  }

  private static class TestWriter implements Writer
  {
    private WriteHandle _wh;
    private volatile int _count = 0;
    private AtomicInteger _aborted = new AtomicInteger(0);

    public int getWritePossibleCount()
    {
      return _count;
    }

    public int remaining()
    {
      return _wh.remaining();
    }

    public void write()
    {
      _wh.write(ByteString.empty());
    }

    public void done()
    {
      _wh.done();
    }

    public void error(Throwable ex)
    {
      _wh.error(ex);
    }

    public int abortedTimes()
    {
      return _aborted.get();
    }

    @Override
    public void onInit(WriteHandle wh)
    {
      _wh = wh;
    }

    @Override
    public void onWritePossible()
    {
      _count++;
    }

    @Override
    public void onAbort(Throwable ex)
    {
      _aborted.incrementAndGet();
    }

  }

  private static class TestObserver implements Observer
  {
    private volatile int _count = 0;

    private AtomicInteger _isDone = new AtomicInteger(0);
    private AtomicInteger _error = new AtomicInteger(0);
    private AtomicReference<String> _lastEvent = new AtomicReference<String>();

    @Override
    public void onDataAvailable(ByteString data)
    {
      _lastEvent.set("onDataAvailable");
      _count++;
    }

    @Override
    public void onDone()
    {
      _lastEvent.set("onDone");
      _isDone.incrementAndGet();
    }

    @Override
    public void onError(Throwable e)
    {
      _lastEvent.set("onError");
      _error.incrementAndGet();
    }

    public int getChunkCount()
    {
      return _count;
    }

    public int doneTimes()
    {
      return _isDone.get();
    }

    public int errorTimes()
    {
      return _error.get();
    }

    public String getLastEvent()
    {
      return _lastEvent.get();
    }

  }
}
