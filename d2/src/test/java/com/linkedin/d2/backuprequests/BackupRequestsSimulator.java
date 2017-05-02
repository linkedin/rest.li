/*
   Copyright (c) 2017 LinkedIn Corp.

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
package com.linkedin.d2.backuprequests;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.TreeMap;

import org.HdrHistogram.Histogram;

import com.linkedin.d2.backuprequests.BackupRequestsStrategy;
import com.linkedin.d2.backuprequests.SimulatedRequest.State;


public class BackupRequestsSimulator
{

  //contains requests in flight, indexed by completion time
  private final TreeMap<Long, List<SimulatedRequest>> _inFlight = new TreeMap<>();

  private final EventsArrival _arrivalProcess;
  private final ResponseTimeDistribution _responseTimeDistribution;
  private final BackupRequestsStrategy _backupRequestsStrategy;

  private final Histogram _responseTimeWithoutBackupRequestsHistogram =
      new Histogram(BoundedCostBackupRequestsStrategy.LOW, BoundedCostBackupRequestsStrategy.HIGH, 3);

  private final Histogram _responseTimeWithBackupRequestsHistogram =
      new Histogram(BoundedCostBackupRequestsStrategy.LOW, BoundedCostBackupRequestsStrategy.HIGH, 3);

  private long _numberOfBackupRequestsMade = 0;

  public BackupRequestsSimulator(EventsArrival arrivalProcess, ResponseTimeDistribution responseTimeDistribution,
      BackupRequestsStrategy backupRequestsStrategy)
  {
    _arrivalProcess = arrivalProcess;
    _responseTimeDistribution = responseTimeDistribution;
    _backupRequestsStrategy = backupRequestsStrategy;
  }

  public void simulate(int numberOfEvents)
  {
    long time = 0;
    for (int i = 0; i < numberOfEvents; i++)
    {
      long nextRequest = time + _arrivalProcess.nanosToNextEvent();
      SimulatedRequest request = new SimulatedRequest(nextRequest,
          nextRequest + _responseTimeDistribution.responseTimeNanos(), null, State.scheduled, false);

      inFlightPut(request.getStart(), request);
      processUntil(nextRequest);
      time = nextRequest;
    }
    drainInFlight();
  }

  private void processUntil(long time)
  {
    while (true)
    {
      try
      {
        long key = _inFlight.firstKey();
        if (key <= time)
        {
          _inFlight.get(key).forEach(request -> processRequest(request));
          _inFlight.remove(key);
        } else
        {
          break;
        }
      } catch (NoSuchElementException e)
      {
        break;
      }
    }
  }

  private void processRequest(SimulatedRequest request)
  {
    if (request.getState() == State.scheduled)
    {
      processScheduledRequest(request);
    } else
    {
      processStartedRequest(request);
    }
  }

  private long sanitize(long duration) {
    if (duration < BoundedCostBackupRequestsStrategy.LOW)
      duration = BoundedCostBackupRequestsStrategy.LOW;
    if (duration > BoundedCostBackupRequestsStrategy.HIGH)
      duration = BoundedCostBackupRequestsStrategy.HIGH;
    return duration;
  }

  private void processStartedRequest(SimulatedRequest request)
  {
    if (request.isBackup())
    {
      if (request.getOriginalRequest().isOverridenByBackup())
      {
        _responseTimeWithBackupRequestsHistogram
            .recordValue(sanitize(request.getEnd() - request.getOriginalRequest().getStart()));
      }
    } else
    {
      _responseTimeWithoutBackupRequestsHistogram.recordValue(sanitize(request.getEnd() - request.getStart()));
      if (!request.isOverridenByBackup())
      {
        _responseTimeWithBackupRequestsHistogram.recordValue(sanitize(request.getEnd() - request.getStart()));
      }
    }
    _backupRequestsStrategy.recordCompletion(request.getEnd() - request.getStart());
  }

  private void processScheduledRequest(SimulatedRequest request)
  {
    if (request.isBackup())
    {
      if (_backupRequestsStrategy.isBackupRequestAllowed())
      {
        _numberOfBackupRequestsMade++;
        inFlightPut(request.getEnd(), request.start());
      }
    } else
    {
      Optional<Long> backup = _backupRequestsStrategy.getTimeUntilBackupRequestNano();
      if (backup.isPresent() && request.getEnd() > (request.getStart() + backup.get()))
      {
        SimulatedRequest backupRequest = new SimulatedRequest(request.getStart() + backup.get(),
            request.getStart() + backup.get() + _responseTimeDistribution.responseTimeNanos(), request, State.scheduled,
            false);
        inFlightPut(backupRequest.getStart(), backupRequest);
        if (backupRequest.getEnd() < request.getEnd())
        {
          request.setIgnored(true);
        }
      }
      inFlightPut(request.getEnd(), request.start());
    }
  }

  private void inFlightPut(Long key, SimulatedRequest value)
  {
    if (_inFlight.containsKey(key))
    {
      _inFlight.get(key).add(value);
    } else
    {
      List<SimulatedRequest> l = new ArrayList<>();
      l.add(value);
      _inFlight.put(key, l);
    }
  }

  private void drainInFlight()
  {
    while (true)
    {
      try
      {
        long key = _inFlight.firstKey();
        _inFlight.get(key).forEach(request -> processRequest(request));
        _inFlight.remove(key);
      } catch (NoSuchElementException e)
      {
        break;
      }
    }
  }

  public long getNumberOfBackupRequestsMade()
  {
    return _numberOfBackupRequestsMade;
  }

  public Histogram getResponseTimeWithoutBackupRequestsHistogram()
  {
    return _responseTimeWithoutBackupRequestsHistogram;
  }

  public Histogram getResponseTimeWithBackupRequestsHistogram()
  {
    return _responseTimeWithBackupRequestsHistogram;
  }
}
