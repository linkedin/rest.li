/*
   Copyright (c) 2012 LinkedIn Corp.

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

/* $Id$ */
package test.r2.perf.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Chris Pettitt
 * @version $Revision$
 */
public class PerfClient
{
  private final ClientRunnableFactory _runnableFactory;
  private final int _numThreads;

  public PerfClient(ClientRunnableFactory runnableFactory, int numThreads)
  {
    _runnableFactory = runnableFactory;
    _numThreads = numThreads;
  }

  public void run() throws Exception
  {
    final AtomicReference<Stats> statsRef = new AtomicReference<Stats>();
    statsRef.set(new Stats(System.currentTimeMillis()));
    final CountDownLatch startLatch = new CountDownLatch(1);
    final List<Thread> workers = new ArrayList<Thread>();
    for (int i = 0; i < _numThreads; i++)
    {
      final Thread t = new Thread(_runnableFactory.create(statsRef,
                                                         startLatch));
      t.start();
      workers.add(t);
    }

    final PrintResultsTask resultsTask = new PrintResultsTask(statsRef);
    Thread shutdownTask = new Thread() {
      @Override
      public void run()
      {
        resultsTask.run();
      }
    };

    Runtime.getRuntime().addShutdownHook(shutdownTask);
    startLatch.countDown();

    Timer statsTimer = new Timer(true);
    statsTimer.schedule(new TimerTask()
    {
      @Override
      public void run()
      {
        printStats(statsRef.get());
      }
    }, 1000, 1000);

    try
    {
      Thread.sleep(15000);
    }
    catch (InterruptedException e)
    {
      // TODO
    }

    // Reset the stats after the warmup period
    statsRef.set(new Stats(System.currentTimeMillis(), true));

    for (Thread worker : workers)
    {
      worker.join();
    }

    statsTimer.cancel();
    Runtime.getRuntime().removeShutdownHook(shutdownTask);
    resultsTask.run();
  }

  public void shutdown()
  {
    _runnableFactory.shutdown();
  }

  private void printStats(Stats stats)
  {
    final Exception lastError = stats.getLastError();
    final String errorMsg = lastError != null ? lastError.toString() : "";

    final long sentCount = stats.getSentCount();
    final long successCount = stats.getSuccessCount();
    final long errorCount = stats.getErrorCount();

    System.out.printf("Sent %8d   Processed: %8d   Errors: %8d   Last Error: %s\n",
            sentCount,
            successCount,
            errorCount,
            errorMsg);

    /*
    final com.linkedin.util.stats.LongStats snapshot = stats.getLatencyStats();

    System.out.printf("  %04d %04d %04d %04d %04d %04d\n",
                      snapshot.getMinimum(),
                      snapshot.get50Pct(),
                      snapshot.get90Pct(),
                      snapshot.get95Pct(),
                      snapshot.get99Pct(),
                      snapshot.getMaximum());
                      */
  }
}
