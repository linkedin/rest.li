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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkedin.d2.balancer.strategies.RingFactory;
import com.linkedin.d2.balancer.util.hashing.Ring;
import com.linkedin.util.degrader.CallCompletion;
import com.linkedin.util.degrader.CallTracker;
import com.linkedin.util.degrader.CallTrackerImpl;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;
import org.knowm.xchart.SwingWrapper;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.markers.SeriesMarkers;


/**
 * A simulator for the consistent hash ring that implements the {@link Ring} interface.
 *
 * It takes in exactly one argument, the name of the json file that specifies the configurations of the simulator.
 * The configurable parameters are implemented in {@link ConsistentHashRingSimulatorConfig}.
 * See <a href="file:configs/example.json">configs/example.json</a> for a sample configuration.
 *
 * It runs request simulations on the test ring, and reports the distribution of the requests, i.e. consistency,
 * and the average latency per server, i.e. load balancing. After the simulation, it will also display a bar chart
 * visualizing the requests distribution on the test ring against the strict consistent hash ring, and two line
 * charts visualizing the CIR changes per server over time on test ring and strict consistent hash ring.
 *
 * @author Rick Zhou
 */
public class ConsistentHashRingSimulator
{
  private static final String CONFIG_RESOURCE_PATH =
      "d2/src/main/java/com/linkedin/d2/balancer/util/hashing/simulator/config/simulator.config";
  private static final int REQUEST_TIMEOUT_TIME = 1000;
  private static final int CIR_SNAPSHOT_INTERVAL = 20;

  private final List<Client> _clients;
  private final List<String> _servers;
  private final ConsistentHashRingState _testRingState;
  private final ConsistentHashRingState _consistentRingState;
  private final Map<String, ConsistentHashRingState> _clientState;
  private final Map<String, ConsistentHashRingState> _consistentClientState;
  private final int _serverCapacity;

  private final Map<String, Map<String, AtomicInteger>> _consistencyTracker;
  private final Map<String, List<Integer>> _testRingCIRTracker;
  private final Map<String, List<Integer>> _consistentRingCIRTracker;

  private static Random _random = new Random();
  private static AtomicInteger _consistencyCount = new AtomicInteger(0);
  private static AtomicInteger _callCount = new AtomicInteger(0);

  /**
   * Creates a {@link ConsistentHashRingSimulator} instance
   *
   * @param testRingFactory          The test ring factory
   * @param consistentRingFactory    The strict consistent hash ring factory
   * @param clients         List of clients sending requests to the rings
   * @param pointsMap       A map between object to store in the ring and its points. The more points
   *                        one has, the higher its weight is.
   * @param serverCapacity  The maximum capacity of the server. The latency of the server will be higher
   *                        when the load is reaching its capacity. Requests will time out when the server is full.
   *                        Here we assume that all the server has the same capacity.
   */
  public ConsistentHashRingSimulator(RingFactory<String> testRingFactory, RingFactory<String> consistentRingFactory,
      List<Client> clients, Map<String, Integer> pointsMap, int serverCapacity)
  {
    _clients = clients;
    _servers = new ArrayList<>(pointsMap.keySet());
    _serverCapacity = serverCapacity;

    _testRingState = initState(testRingFactory, pointsMap);
    _consistentRingState = initState(consistentRingFactory, pointsMap);
    _clientState = new ConcurrentHashMap<>();
    _consistentClientState = new ConcurrentHashMap<>();
    _consistencyTracker = new ConcurrentHashMap<>();
    _testRingCIRTracker = new ConcurrentHashMap<>();
    _consistentRingCIRTracker = new ConcurrentHashMap<>();

    clients.forEach(e -> _clientState.put(e.getName(), initState(testRingFactory, pointsMap)));
    clients.forEach(e -> _consistentClientState.put(e.getName(), initState(consistentRingFactory, pointsMap)));
    _servers.forEach(e ->
    {
      _testRingCIRTracker.put(e, new ArrayList<>());
      _consistentRingCIRTracker.put(e, new ArrayList<>());
    });
  }

  private ConsistentHashRingState initState(RingFactory<String> ringFactory, Map<String, Integer> pointsMap)
  {
    Map<String, CallTracker> callTrackerMap = new ConcurrentHashMap<>();
    Map<String, List<Integer>> latencyMap = new ConcurrentHashMap<>();

    for (String server : pointsMap.keySet())
    {
      CallTracker callTracker = new CallTrackerImpl(5000L);

      callTrackerMap.put(server, callTracker);
      latencyMap.put(server, new ArrayList<>());
    }

    Ring<String> ring = ringFactory.createRing(pointsMap, callTrackerMap);

    return new ConsistentHashRingState(ring, callTrackerMap, latencyMap);
  }

  private static ConsistentHashRingSimulator readFromJson(Path path)
  {
    ObjectMapper mapper = new ObjectMapper();
    ConsistentHashRingSimulatorConfig config;
    try
    {
      config = mapper.readValue(new File(path.toUri()), ConsistentHashRingSimulatorConfig.class);
      return config.toSimulator();
    }
    catch (IOException e)
    {
      e.printStackTrace();
      throw new RuntimeException("Error reading JSON file");
    }
  }

  static List<Request> getRequest(ConsistentHashRingSimulatorConfig.Request request)
  {
    List<Request> requests = new ArrayList<>();

    int id = request.getId();
    int randomIdentifier = new Random(id).nextInt();

    switch (request.getRandomStrategy())
    {
      case GAUSSIAN:
        for (int i = 0; i < request.getNumber(); i++)
        {
          int internalID = (id == -1) ? _random.nextInt() : randomIdentifier;
          requests.add(new Request(internalID,
              getNormal(request.getMinLatency(), request.getMaxLatency(), request.getStddev())));
        }
        break;
      case UNIFORM:
        for (int i = 0; i < request.getNumber(); i++)
        {
          int internalID = (id == -1) ? _random.nextInt() : randomIdentifier;
          requests.add(new Request(internalID,
              _random.nextInt(request.getMaxLatency() - request.getMinLatency()) + request.getMinLatency()));
        }
        break;
    }
    return requests;
  }

  /**
   * Get a random value with truncated normal distribution {@see https://en.wikipedia.org/wiki/Truncated_normal_distribution}
   *
   * @param lower   lower bound
   * @param upper   upper bound
   * @param stddev  standard deviation of the distribution
   * @return  a truncated normal random value within the interval (lower, upper)
   */
  static int getNormal(int lower, int upper, double stddev)
  {
    int mean = lower + (upper - lower) / 2;
    int x = (int) (_random.nextGaussian() * stddev + mean);

    while (x > upper || x < lower)
    {
      x = (int) (_random.nextGaussian() * stddev + mean);
    }

    return x;
  }

  private void setActualLatency(Request request, int serverLoad, int serverCapacity, boolean isTestRing)
  {
    double utilRatio = Double.min(0.9, (double) serverLoad / serverCapacity);
    int actualLatency = (int) ((utilRatio / (1 - utilRatio)) * request.getLatency() * 0.9);
    actualLatency = Integer.max(request.getLatency(), Integer.min(REQUEST_TIMEOUT_TIME, actualLatency));

    if (isTestRing)
    {
      request.setActualLatency(actualLatency);
    }
    else
    {
      request.setConsistentActualLatency(actualLatency);
    }
  }

  private synchronized CallCompletion startCall(String server, ConsistentHashRingState state)
  {
    CallTracker callTracker = state.getCallTrackerMap().get(server);
    return callTracker.startCall();
  }

  private synchronized void endCall(CallCompletion callCompletion, String server, ConsistentHashRingState state,
      int latency)
  {
    callCompletion.endCall();
    state.getLatencyMap().get(server).add(latency);
  }

  private Thread runRequest(String clientName, Request request)
  {
    return new Thread(() ->
    {
      String server = _clientState.get(clientName).getRing().get(request.getId());

      String consistentServer = _consistentClientState.get(clientName).getRing().get(request.getId());

      if (server != null && server.equals(consistentServer))
      {
        _consistencyCount.incrementAndGet();
      }

      if (!_consistencyTracker.containsKey(server))
      {
        _consistencyTracker.put(server, new ConcurrentHashMap<>());
      }

      if (!_consistencyTracker.get(server).containsKey(consistentServer))
      {
        _consistencyTracker.get(server).put(consistentServer, new AtomicInteger(0));
      }

      _consistencyTracker.get(server).get(consistentServer).incrementAndGet();

      CallCompletion testRingCompletion = startCall(server, _testRingState);
      CallCompletion consistentRingCompletion = startCall(consistentServer, _consistentRingState);
      CallCompletion clientCompletion = startCall(server, _clientState.get(clientName));

      setActualLatency(request, _testRingState.getPendingRequestsNum().get(server), _serverCapacity, true);
      setActualLatency(request, _consistentRingState.getPendingRequestsNum().get(consistentServer), _serverCapacity,
          false);
      printRequestInfo(_callCount.incrementAndGet(), request, server, consistentServer,
          _testRingState.getPendingRequestsNum());

      try
      {
        Thread.sleep(request.getActualLatency());
      }
      catch (InterruptedException e)
      {
        e.printStackTrace();
      }

      endCall(testRingCompletion, server, _testRingState, request.getActualLatency());
      endCall(consistentRingCompletion, consistentServer, _consistentRingState, request.getConsistentActualLatency());
      endCall(clientCompletion, server, _clientState.get(clientName), request.getActualLatency());
    });
  }

  private void printRequestInfo(int id, Request request, String server, String origServer, Map<String, Integer> loadMap)
  {
    synchronized (System.out)
    {
      System.out.printf("Request #%d is sent to %s. Most consistent server: %s, Latency: %d, Actual latency: %d\n", id,
          server, origServer, request.getLatency(), request.getActualLatency());
      System.out.print("\t Current server loads: ");

      if (!loadMap.isEmpty())
      {
        loadMap.forEach((k, v) -> System.out.printf("%s : %d\t", k, v));
      }

      System.out.println();
      System.out.println();
    }
  }

  private Thread runClient(String clientName, List<Request> requests, Arrival arrival)
  {
    return new Thread(() ->
    {
      List<Thread> threads = new ArrayList<>();

      for (Request request : requests)
      {
        Thread thread = runRequest(clientName, request);
        thread.start();
        threads.add(thread);

        try
        {
          Thread.sleep(arrival.getNextInterval());
        }
        catch (InterruptedException e)
        {
          e.printStackTrace();
        }
      }

      for (Thread thread : threads)
      {
        try
        {
          thread.join();
        }
        catch (InterruptedException e)
        {
          e.printStackTrace();
        }
      }
    });
  }

  private void run() throws InterruptedException
  {
    List<Thread> threads = new ArrayList<>();

    Timer timer = new Timer();
    TimerTask monitorTask = new TimerTask()
    {
      @Override
      public void run()
      {
        _testRingState.getPendingRequestsNum().forEach((k, v) -> _testRingCIRTracker.get(k).add(v));
        _consistentRingState.getPendingRequestsNum().forEach((k, v) -> _consistentRingCIRTracker.get(k).add(v));
      }
    };

    timer.schedule(monitorTask, 0, CIR_SNAPSHOT_INTERVAL);

    for (Client client : _clients)
    {
      Thread thread = runClient(client.getName(), client.getRequests(), client.getArrival());
      thread.start();
      threads.add(thread);
    }

    for (Thread thread : threads)
    {
      thread.join();
    }

    timer.cancel();
    timer.purge();

    printSummary();
    showChart();
  }

  private void printSummary()
  {
    System.out.println();
    System.out.println("****** SUMMARY ******");
    System.out.println("Request distribution on the testing hash ring: ");
    for (String server : _servers)
    {
      System.out.printf("%s : %d\n", server, _testRingState.getTotalRequestsNum().get(server));
    }

    System.out.println();

    System.out.println("Request distribution on the consistent hash ring: ");
    for (String server : _servers)
    {
      System.out.printf("%s : %d\n", server, _consistentRingState.getTotalRequestsNum().get(server));
    }

    System.out.println();

    System.out.println("Average latency (actual) on the testing hash ring: ");
    for (String server : _servers)
    {
      Integer averageLatency = _testRingState.getAverageLatency().get(server);
      System.out.printf("%s, %d\n", server, averageLatency);
    }

    System.out.println();

    System.out.println("Average latency (actual) on the consistent hash ring: ");
    for (String server : _servers)
    {
      Integer averageLatency = _consistentRingState.getAverageLatency().get(server);
      System.out.printf("%s, %d\n", server, averageLatency);
    }

    System.out.println();

    System.out.printf("Percentage of consistent requests: %.2f", (double) _consistencyCount.get() / _callCount.get());
  }

  private XYChart getCIRChart(Map<String, List<Integer>> CIRTracker, String title)
  {
    XYChart chart =
        new XYChartBuilder().title(title).xAxisTitle("Time (ms)").yAxisTitle("CIR").width(600).height(400).build();

    for (Map.Entry<String, List<Integer>> entry : CIRTracker.entrySet())
    {
      List<Integer> xData = IntStream.range(0, entry.getValue().size())
          .mapToObj(i -> i * CIR_SNAPSHOT_INTERVAL)
          .collect(Collectors.toList());

      XYSeries series = chart.addSeries(entry.getKey(), xData, entry.getValue());
      series.setMarker(SeriesMarkers.NONE);
    }

    return chart;
  }

  private void showChart()
  {
    List<XYChart> charts = new ArrayList<>();
    CategoryChart chart = new CategoryChartBuilder().width(800)
        .height(600)
        .title("Consistency of hashing algorithm")
        .xAxisTitle("Distribution of requests on test ring")
        .yAxisTitle("Number of requests")
        .build();

    chart.getStyler().setPlotGridVerticalLinesVisible(false);
    chart.getStyler().setStacked(true);

    for (String server : _servers)
    {
      List<Integer> yData = new ArrayList<>();
      _servers.forEach(orig -> yData.add(
          _consistencyTracker.getOrDefault(orig, new HashMap<>()).getOrDefault(server, new AtomicInteger(0)).get()));

      chart.addSeries(server, _servers, yData);
    }

    charts.add(getCIRChart(_testRingCIRTracker, "CIR changes over time on test ring"));
    charts.add(getCIRChart(_consistentRingCIRTracker, "CIR changes over time on consistent hash ring"));

    new SwingWrapper<>(chart).displayChart();
    new SwingWrapper<>(charts).displayChartMatrix();
  }

  /**
   * Main function to run the simulator using the config file in com.linkedin.d2.balancer.util.hashing.simulator.config
   *
   * @param args  No arguments needed
   * @throws InterruptedException
   */
  public static void main(String[] args) throws InterruptedException
  {
    ConsistentHashRingSimulator simulator = readFromJson(Paths.get(CONFIG_RESOURCE_PATH));
    simulator.run();
  }
}
