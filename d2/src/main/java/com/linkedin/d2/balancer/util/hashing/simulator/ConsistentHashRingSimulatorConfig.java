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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.linkedin.d2.balancer.strategies.DelegatingRingFactory;
import com.linkedin.d2.balancer.strategies.RingFactory;
import com.linkedin.d2.balancer.strategies.degrader.DegraderLoadBalancerStrategyConfig;
import com.linkedin.d2.balancer.strategies.degrader.DegraderLoadBalancerStrategyV3;
import com.linkedin.util.degrader.DegraderImpl;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ConsistentHashRingSimulatorConfig
{

  // The underlying hashing algorithm to use, see DegraderRingFactory for available hashing algorithms
  @JsonProperty("hashingAlgorithm")
  private String _hashingAlgorithm;

  // Configurable parameters for bounded-load consistent hashing
  @JsonProperty("boundedLoadBalancingFactor")
  private double _boundedLoadBalancingFactor;

  // Specifies the number of points per server
  @JsonProperty("servers")
  private Server[] _servers;

  // The maximum capacity of the server. The latency of the server will be higher when the load
  // is reaching its capacity. Requests will time out when the server is full.
  // Here we assume that all the server has the same capacity.
  @JsonProperty("serverCapacity")
  private int _serverCapacity;

  @JsonProperty("clients")
  private Client[] _clients;

  // Whether to shuffle the requests. Defaults to true.
  @JsonProperty("shuffleRequests")
  private boolean _shuffleRequests = true;

  /**
   * The distribution used to generate random numbers from given intervals.
   */
  public enum RandomStrategy
  {
    UNIFORM, GAUSSIAN
  }

  /**
   * Arrival rate of {@link Request}.
   */
  public static class Arrival
  {
    @JsonProperty("minInterval")
    private int _minInterval;
    @JsonProperty("maxInterval")
    private int _maxInterval;
    @JsonProperty("stddev")
    private double _stddev = 1;
    @JsonProperty("randomStrategy")
    private RandomStrategy _randomStrategy = RandomStrategy.GAUSSIAN;

    public RandomStrategy getRandomStrategy()
    {
      return _randomStrategy;
    }

    public int getMaxInterval()
    {
      return _maxInterval;
    }

    public int getMinInterval()
    {
      return _minInterval;
    }

    public double getStddev()
    {
      return _stddev;
    }
  }

  public static class Request
  {
    // When id is specified, the requests with the same id will be treated as the same request.
    // If id is not specified, it will default to -1, and the request will be assigned a random id in the simulator.
    @JsonProperty("id")
    private int _id = -1;

    // Number of requests of this type
    @JsonProperty("number")
    private int _number;

    @JsonProperty("minLatency")
    private int _minLatency;
    @JsonProperty("maxLatency")
    private int _maxLatency;
    @JsonProperty("stddev")
    private double _stddev = 1;
    @JsonProperty("randomStrategy")
    private RandomStrategy _randomStrategy = RandomStrategy.GAUSSIAN;

    public int getId()
    {
      return _id;
    }

    public int getNumber()
    {
      return _number;
    }

    public int getMinLatency()
    {
      return _minLatency;
    }

    public int getMaxLatency()
    {
      return _maxLatency;
    }

    public RandomStrategy getRandomStrategy()
    {
      return _randomStrategy;
    }

    public double getStddev()
    {
      return _stddev;
    }
  }

  public static class Client
  {
    // Number of clients of this type
    @JsonProperty("number")
    private int _number;

    @JsonProperty("requests")
    private Request[] _requests;
    @JsonProperty("arrival")
    private Arrival _arrival;

    public int getNumber()
    {
      return _number;
    }

    public Request[] getRequests()
    {
      return _requests;
    }

    public Arrival getArrival()
    {
      return _arrival;
    }
  }

  public static class Server
  {
    // Number of servers of this type
    @JsonProperty("number")
    private int _number;

    // Number of points
    @JsonProperty("points")
    private int _points;

    public int getNumber()
    {
      return _number;
    }

    public int getPoints()
    {
      return _points;
    }
  }

  public String getHashingAlgorithm()
  {
    return _hashingAlgorithm;
  }

  public double getBoundedLoadBalancingFactor()
  {
    return _boundedLoadBalancingFactor;
  }

  public Client[] getClients()
  {
    return _clients;
  }

  public Server[] getServers()
  {
    return _servers;
  }

  public int getServerCapacity()
  {
    return _serverCapacity;
  }

  public boolean getShuffleRequests()
  {
    return _shuffleRequests;
  }

  /**
   * Creates a {@link ConsistentHashRingSimulator} from the config
   *
   * @return A ConsistentHashRingSimulator instance
   */
  public ConsistentHashRingSimulator toSimulator()
  {
    String hashingAlgorithm = getHashingAlgorithm();
    double balancingFactor = getBoundedLoadBalancingFactor();

    DegraderLoadBalancerStrategyConfig degraderLoadBalancerStrategyConfig =
        getConfig(hashingAlgorithm, balancingFactor);
    RingFactory<String> testFactory = new DelegatingRingFactory<>(degraderLoadBalancerStrategyConfig);

    Map<String, Integer> pointsMap = new HashMap<>();

    int serverID = 0;

    for (Server server : getServers())
    {
      for (int i = 0; i < server.getNumber(); i++)
      {
        pointsMap.put("Server" + serverID, server.getPoints());
        serverID += 1;
      }
    }

    DegraderLoadBalancerStrategyConfig consistentConfig = getConfig(hashingAlgorithm, Double.POSITIVE_INFINITY);
    RingFactory<String> consistentFactory = new DelegatingRingFactory<>(consistentConfig);

    List<com.linkedin.d2.balancer.util.hashing.simulator.Client> clients = new ArrayList<>();

    int clientID = 0;

    for (ConsistentHashRingSimulatorConfig.Client client : getClients())
    {
      for (int i = 0; i < client.getNumber(); i++)
      {
        clients.add(new com.linkedin.d2.balancer.util.hashing.simulator.Client("Client" + clientID, client,
            getShuffleRequests()));
        clientID++;
      }
    }

    int serverCapacity = getServerCapacity();

    return new ConsistentHashRingSimulator(testFactory, consistentFactory, clients, pointsMap, serverCapacity);
  }

  private static DegraderLoadBalancerStrategyConfig getConfig(String hashingAlgorithm, double balancingFactor)
  {
    return new DegraderLoadBalancerStrategyConfig(1000,
        DegraderLoadBalancerStrategyConfig.DEFAULT_UPDATE_ONLY_AT_INTERVAL, 100,
        DegraderLoadBalancerStrategyV3.HASH_METHOD_URI_REGEX, Collections.<String, Object>emptyMap(),
        DegraderLoadBalancerStrategyConfig.DEFAULT_CLOCK,
        DegraderLoadBalancerStrategyConfig.DEFAULT_INITIAL_RECOVERY_LEVEL,
        DegraderLoadBalancerStrategyConfig.DEFAULT_RAMP_FACTOR,
        DegraderLoadBalancerStrategyConfig.DEFAULT_HIGH_WATER_MARK,
        DegraderLoadBalancerStrategyConfig.DEFAULT_LOW_WATER_MARK,
        DegraderLoadBalancerStrategyConfig.DEFAULT_GLOBAL_STEP_UP,
        DegraderLoadBalancerStrategyConfig.DEFAULT_GLOBAL_STEP_DOWN,
        DegraderLoadBalancerStrategyConfig.DEFAULT_CLUSTER_MIN_CALL_COUNT_HIGH_WATER_MARK,
        DegraderLoadBalancerStrategyConfig.DEFAULT_CLUSTER_MIN_CALL_COUNT_LOW_WATER_MARK,
        DegraderLoadBalancerStrategyConfig.DEFAULT_HASHRING_POINT_CLEANUP_RATE, hashingAlgorithm,
        DegraderLoadBalancerStrategyConfig.DEFAULT_NUM_PROBES,
        DegraderLoadBalancerStrategyConfig.DEFAULT_POINTS_PER_HOST, balancingFactor, null,
        DegraderLoadBalancerStrategyConfig.DEFAULT_QUARANTINE_MAXPERCENT, null, null,
        DegraderLoadBalancerStrategyConfig.DEFAULT_QUARANTINE_METHOD, null, DegraderImpl.DEFAULT_LOW_LATENCY, null,
        DegraderLoadBalancerStrategyConfig.DEFAULT_LOW_EVENT_EMITTING_INTERVAL,
        DegraderLoadBalancerStrategyConfig.DEFAULT_HIGH_EVENT_EMITTING_INTERVAL,
        DegraderLoadBalancerStrategyConfig.DEFAULT_CLUSTER_NAME);
  }
}
