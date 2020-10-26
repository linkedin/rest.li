package com.linkedin.d2.balancer.util.hashing.simulator.subsetting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.CategoryChartBuilder;
import org.knowm.xchart.SwingWrapper;


public class D2RandomSubsettingSimulator
{
  private final Map<Integer, Integer> _hostDistribution;

  private final int _numberOfHosts;
  private final int _numberOfClients;
  private final int _subsetSize;
  private final boolean _withReplacement;

  public D2RandomSubsettingSimulator(int numberOfHosts, int numberOfClients, int subsetSize, boolean withReplacement)
  {
    this._hostDistribution = new HashMap<>();
    this._numberOfHosts = numberOfHosts;
    this._numberOfClients = numberOfClients;
    this._subsetSize = subsetSize;
    this._withReplacement = withReplacement;
  }

  private static List<Integer> randomPick(int numberOfHosts, int subsetSize, boolean withReplacement)
  {
    IntStream stream = withReplacement ?
        ThreadLocalRandom.current()
            .ints(0, numberOfHosts) :
        ThreadLocalRandom.current()
            .ints(0, numberOfHosts)
            .distinct();

    return stream.limit(subsetSize)
        .boxed()
        .collect(Collectors.toList());
  }

  public void run()
  {
    for (int i = 0; i < _numberOfClients; i++)
    {
      List<Integer> hostSubset = randomPick(_numberOfHosts, _subsetSize, _withReplacement);
      Collections.sort(hostSubset);

      System.out.println(hostSubset);

      for (Integer hostId: hostSubset)
      {
        _hostDistribution.put(hostId, _hostDistribution.getOrDefault(hostId, 0) + 1);
      }
    }

    showChart();
  }

  private void showChart()
  {
    CategoryChart chart = new CategoryChartBuilder()
        .width(800)
        .height(600)
        .title(String.format("Number of hosts: %d, Number of Clients: %d, Subset size: %d", _numberOfHosts, _numberOfClients, _subsetSize))
        .xAxisTitle("Host")
        .yAxisTitle("Number of clients")
        .build();

    chart.getStyler().setLegendVisible(false);
    chart.getStyler().setPlotGridVerticalLinesVisible(false);

    List<Integer> x_data = IntStream.range(0, _numberOfHosts).boxed().collect(Collectors.toList());
    List<Integer> y_data = new ArrayList<>(_hostDistribution.values());
    int data_len = y_data.size();

    for (int i = 0; i < _numberOfHosts - data_len; i++)
    {
      y_data.add(0);
    }

    Collections.sort(y_data);

    chart.addSeries("Test", x_data, y_data);
    new SwingWrapper<>(chart).displayChart();
  }

  public static void main(String[] args) {
    if (args.length < 4)
    {
      System.out.println("Usage: D2RandomSubsettingSimulator <numberOfHosts> <numberOfClients> <subsetSize> <withReplacement>");
      return;
    }

    int numberOfHosts = Integer.parseInt(args[0]);
    int numberOfClients = Integer.parseInt(args[1]);
    int subsetSize = Integer.parseInt(args[2]);
    boolean withReplacement = Boolean.parseBoolean(args[3]);

    D2RandomSubsettingSimulator d2RandomSubsettingSimulator = new D2RandomSubsettingSimulator(numberOfHosts, numberOfClients, subsetSize, withReplacement);
    d2RandomSubsettingSimulator.run();
  }
}
