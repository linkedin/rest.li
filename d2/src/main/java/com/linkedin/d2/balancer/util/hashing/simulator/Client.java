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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * The class mocks a client with a name, requests to send, and the arrival pattern of requests.
 */
public class Client
{
  private final String _name;
  private final List<Request> _requests;
  private final Arrival _arrival;

  public Client(String name, ConsistentHashRingSimulatorConfig.Client client, boolean shuffleRequests)
  {
    _name = name;
    _requests = new ArrayList<>();

    for (ConsistentHashRingSimulatorConfig.Request request : client.getRequests())
    {
      _requests.addAll(ConsistentHashRingSimulator.getRequest(request));
    }

    _arrival = new Arrival(client.getArrival());

    if (shuffleRequests)
    {
      Collections.shuffle(_requests);
    }
  }

  /**
   * Creates a mock client.
   *
   * @param name     Name of the client
   * @param requests    List of {@link Request}
   * @param arrival   {@link Arrival} pattern of the requests
   * @param shuffleRequests   Whether or not to shuffle the requests
   */
  public Client(String name, List<Request> requests, Arrival arrival, boolean shuffleRequests)
  {
    _name = name;
    _requests = requests;
    _arrival = arrival;

    if (shuffleRequests)
    {
      Collections.shuffle(_requests);
    }
  }

  public String getName()
  {
    return _name;
  }

  public Arrival getArrival()
  {
    return _arrival;
  }

  public List<Request> getRequests()
  {
    return _requests;
  }
}
