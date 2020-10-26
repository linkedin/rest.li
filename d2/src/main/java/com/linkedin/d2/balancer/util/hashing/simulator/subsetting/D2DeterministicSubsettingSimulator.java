package com.linkedin.d2.balancer.util.hashing.simulator.subsetting;

import com.linkedin.d2.balancer.util.hashing.simulator.subsetting.deterministic.Coordinate;
import com.linkedin.d2.balancer.util.hashing.simulator.subsetting.deterministic.DeterministicAperture;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.jboss.netty.util.internal.ThreadLocalRandom;
import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;
import org.knowm.xchart.SwingWrapper;


public class D2DeterministicSubsettingSimulator
{
  private final int _numberOfHosts;
  private final List<Integer> _clientsMap;
  private final Map<Integer, Double> _hostDistribution;

  public D2DeterministicSubsettingSimulator(List<Integer> clientsMap, int numHosts)
  {
    _clientsMap = clientsMap;
    _numberOfHosts = numHosts;
    _hostDistribution = new HashMap<>();
  }

  public void run()
  {
    List<Integer> trackerClients = IntStream.range(0, _numberOfHosts).boxed().collect(Collectors.toList());
    for (int numInstances : _clientsMap)
    {
      runClient(numInstances, new ArrayList<>(trackerClients));
    }

    showChart();
  }

  private void runClient(int numInstances, List<Integer> trackerClients)
  {
    Collections.shuffle(trackerClients, new ThreadLocalRandom());
    System.out.println(trackerClients);

    for(int i = 0; i < numInstances; i++)
    {
      Coordinate coordinate = Coordinate.fromInstaceId(i, numInstances);
      DeterministicAperture deterministicAperture = new DeterministicAperture(trackerClients, coordinate);
      Map<Integer, Double> subset = deterministicAperture.getTrackerClientsSubset();
      System.out.println(subset);

      for (Map.Entry<Integer, Double> entry : subset.entrySet())
      {
        _hostDistribution.put(entry.getKey(), _hostDistribution.getOrDefault(entry.getKey(), 0.0) + entry.getValue());
      }
    }
  }

  private void showChart()
  {
    CategoryChart chart = new CategoryChartBuilder()
        .width(800)
        .height(600)
        .title(String.format("Number of hosts: %d, Number of instances in each client cluster: %s", _numberOfHosts, _clientsMap.toString()))
        .xAxisTitle("Host")
        .yAxisTitle("Number of clients")
        .build();

    chart.getStyler().setLegendVisible(false);
    chart.getStyler().setPlotGridVerticalLinesVisible(false);

    List<Integer> x_data = IntStream.range(0, _numberOfHosts).boxed().collect(Collectors.toList());
    List<Double> y_data = new ArrayList<>(_hostDistribution.values());
    int data_len = y_data.size();

    for (int i = 0; i < _numberOfHosts - data_len; i++)
    {
      y_data.add(0.0);
    }

    Collections.sort(y_data);

    chart.addSeries("Test", x_data, y_data);
    new SwingWrapper<>(chart).displayChart();
  }

  public static void main(String[] args) {
    Integer[] clientsMap = {1, 5};
    D2DeterministicSubsettingSimulator d2DeterministicSubsettingSimulator = new D2DeterministicSubsettingSimulator(
        Arrays.asList(clientsMap), 8);
    d2DeterministicSubsettingSimulator.run();
  }

}
