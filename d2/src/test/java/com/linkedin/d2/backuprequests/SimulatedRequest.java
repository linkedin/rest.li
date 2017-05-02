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

public class SimulatedRequest
{
  private final long _start;
  private final long _end;
  private final State _state;
  private final SimulatedRequest _originalRequest;
  private boolean _overriddenByBackup = false;

  public enum State
  {
    scheduled,
    started
  }

  public SimulatedRequest(long start, long end, SimulatedRequest originalRequest, State state,
      boolean overriddenByBackup)
  {
    _start = start;
    _end = end;
    _originalRequest = originalRequest;
    _state = state;
    _overriddenByBackup = overriddenByBackup;
  }

  public long getStart()
  {
    return _start;
  }

  public long getEnd()
  {
    return _end;
  }

  public boolean isBackup()
  {
    return _originalRequest != null;
  }

  public State getState()
  {
    return _state;
  }

  public SimulatedRequest getOriginalRequest()
  {
    return _originalRequest;
  }

  public SimulatedRequest start()
  {
    if (_state == State.started)
    {
      throw new IllegalStateException("Request has already been started");
    }
    return new SimulatedRequest(_start, _end, _originalRequest, State.started, _overriddenByBackup);
  }

  public boolean isOverridenByBackup()
  {
    return _overriddenByBackup;
  }

  public void setIgnored(boolean ignored)
  {
    _overriddenByBackup = ignored;
  }

  @Override
  public String toString()
  {
    return "SimulatedRequest [start=" + _start / 1000000 + "ms, duration=" + (_end - _start) / 1000000
        + ", originalRequest=" + _originalRequest + ", overriddenByBackup=" + _overriddenByBackup + ", state=" + _state
        + "]";
  }

}
