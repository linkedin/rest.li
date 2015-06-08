package test.r2.perf.client;

import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.transport.common.Client;
import test.r2.perf.Generator;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @auther Zhenkai Zhu
 */

public class StreamClientRunnableFactory implements ClientRunnableFactory
{
  private final Client _client;
  private final Generator<StreamRequest> _reqGen;

  public StreamClientRunnableFactory(Client client, Generator<StreamRequest> reqGen)
  {
    _client = client;
    _reqGen = reqGen;
  }

  @Override
  public Runnable create(AtomicReference<Stats> stats, CountDownLatch startLatch)
  {
    return new StreamClientRunnable(_client, stats, startLatch, _reqGen);
  }

  @Override
  public void shutdown()
  {
    final FutureCallback<None> callback = new FutureCallback<None>();
    _client.shutdown(callback);

    try
    {
      callback.get();
    }
    catch (Exception e)
    {
      // Print out error and continue
      e.printStackTrace();
    }
  }
}
