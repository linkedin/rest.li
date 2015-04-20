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

import com.linkedin.common.stats.LongStats;

import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Chris Pettitt
 * @version $Revision$
 */
public class PrintResultsTask implements Runnable
{
  private final AtomicReference<Stats> _statsRef;

  public PrintResultsTask(AtomicReference<Stats> statsRef)
  {
    _statsRef = statsRef;
  }

  public void run()
  {
    final Stats stats = _statsRef.get();
    LongStats snapshot = stats.getLatencyStats();
    long elapsedTime = stats.getElapsedTime();
    double timePerReq = stats.getSuccessCount() != 0 ? elapsedTime/(double)stats.getSuccessCount() : 0;
    double reqPerSec = timePerReq != 0 ? 1000.0 / timePerReq : 0;

    System.out.println();
    System.out.println();
    System.out.println("DONE");
    System.out.println();
    System.out.println("Results");
    System.out.println("-------");
    System.out.println("    Total Requests: " + stats.getSentCount());
    System.out.println("    Elapsed: " + elapsedTime);
    System.out.println("    Mean latency (in millis): " + snapshot.getAverage() / 10E6);
    System.out.println("    Reqs / Sec: " + reqPerSec);
    System.out.println("    Errors: " + stats.getErrorCount());
    System.out.println("    Min latency: " + snapshot.getMinimum() / 10E6);
    System.out.println("    50% latency: " + snapshot.get50Pct() / 10E6);
    System.out.println("    90% latency: " + snapshot.get90Pct() / 10E6);
    System.out.println("    95% latency: " + snapshot.get95Pct() / 10E6);
    System.out.println("    99% latency: " + snapshot.get99Pct() / 10E6);
    System.out.println("    Max latency: " + snapshot.getMaximum() / 10E6);
  }
}
