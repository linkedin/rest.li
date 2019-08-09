/*
   Copyright (c) 2019 LinkedIn Corp.

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

package com.linkedin.d2.balancer.util.hashing.simulator;

/**
 * The class mocks a request with id and latency.
 */
public class Request
{
  private final int _latency;
  private final int _id;

  private int _actualLatency;   // Actual latency calculated on the test ring
  private int _consistentActualLatency;   // Actual latency calculated on the consistent hash ring

  /**
   * Creates a request
   *
   * @param id    Request with the same id will be considered as the same request by the simulator.
   * @param latency   Latency of the request
   */
  public Request(int id, int latency)
  {
    _id = id;
    _latency = latency;
  }

  public int getId()
  {
    return _id;
  }

  public int getLatency()
  {
    return _latency;
  }

  public int getActualLatency()
  {
    return _actualLatency;
  }

  public void setActualLatency(int actualLatency)
  {
    _actualLatency = actualLatency;
  }

  public int getConsistentActualLatency()
  {
    return _consistentActualLatency;
  }

  public void setConsistentActualLatency(int consistentActualLatency)
  {
    _consistentActualLatency = consistentActualLatency;
  }

  @Override
  public String toString()
  {
    return Integer.toString(_id);
  }
}
