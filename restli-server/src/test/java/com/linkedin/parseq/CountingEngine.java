/*
   Copyright (c) 2016 LinkedIn Corp.

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

package com.linkedin.parseq;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import com.linkedin.parseq.internal.FIFOPriorityQueue;

import org.slf4j.ILoggerFactory;


public class CountingEngine extends Engine
{
  private final AtomicInteger _plansStarted = new AtomicInteger();
  private final Engine _engine;

  public CountingEngine(Executor taskExecutor, DelayedExecutor timerExecutor, ILoggerFactory loggerFactory,
      Map<String, Object> properties, Engine engine)
  {
    super(taskExecutor, timerExecutor, loggerFactory, properties, planContext -> {}, planContext -> {}, FIFOPriorityQueue::new);
    _engine = engine;
  }

  @Override
  public void run(Task<?> task)
  {
    _plansStarted.incrementAndGet();
    _engine.run(task);
  }

  public int plansStarted()
  {
    return _plansStarted.get();
  }
}
