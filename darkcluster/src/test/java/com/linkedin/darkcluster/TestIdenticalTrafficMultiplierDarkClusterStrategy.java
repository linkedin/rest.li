package com.linkedin.darkcluster;

import com.linkedin.darkcluster.api.DarkClusterDispatcher;
import com.linkedin.darkcluster.api.DarkClusterStrategy;
import com.linkedin.darkcluster.impl.BaseDarkClusterDispatcherImpl;
import com.linkedin.darkcluster.impl.DefaultDarkClusterDispatcher;
import com.linkedin.darkcluster.impl.IdenticalTrafficMultiplierDarkClusterStrategy;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


public class TestIdenticalTrafficMultiplierDarkClusterStrategy
{

  private static class DarkClusterMetadata
  {
    String _darkClusterName;
    float _multiplier;
    boolean _darkRequestSent;

    public DarkClusterMetadata(String darkClusterName, float multiplier, boolean darkRequestSent)
    {
      _darkClusterName = darkClusterName;
      _multiplier = multiplier;
      _darkRequestSent = darkRequestSent;
    }
  }

  @Test(dataProvider = "getDarkClusters")
  public void testHandleRequest(List<DarkClusterMetadata> darkClusterMetadataList, float expectedRandomNumber)
  {
    String sourceClusterName = "sourceCluster";
    DarkClusterDispatcher darkClusterDispatcher = new DefaultDarkClusterDispatcher(new MockClient(false));
    Random random = Mockito.mock(Random.class);
    Mockito.when(random.nextFloat()).thenReturn(expectedRandomNumber);
    List<IdenticalTrafficMultiplierDarkClusterStrategy> strategies = darkClusterMetadataList.stream()
        .map(darkClusterMetadata ->
        {
          BaseDarkClusterDispatcherImpl baseDispatcher = new BaseDarkClusterDispatcherImpl(darkClusterMetadata._darkClusterName,
              darkClusterDispatcher,
              new DoNothingNotifier(),
              new CountingVerifierManager());
          MockClusterInfoProvider mockClusterInfoProvider = new MockClusterInfoProvider();
          mockClusterInfoProvider.putHttpsClusterCount(darkClusterMetadata._darkClusterName, 1);
          mockClusterInfoProvider.putHttpsClusterCount(sourceClusterName, 1);
          return new IdenticalTrafficMultiplierDarkClusterStrategy(sourceClusterName,
              darkClusterMetadata._darkClusterName, darkClusterMetadata._multiplier, baseDispatcher, new DoNothingNotifier(),
              mockClusterInfoProvider, random);
        }).collect(Collectors.toList());
    RestRequest dummyRestRequest = new RestRequestBuilder(URI.create("foo")).build();
    RestRequest dummyDarkRequest = new RestRequestBuilder(URI.create("darkfoo")).build();
    RequestContext dummyRequestContext = new RequestContext();
    IntStream.range(0, darkClusterMetadataList.size()).forEach(index ->
    {
      DarkClusterMetadata darkClusterMetadata = darkClusterMetadataList.get(index);
      DarkClusterStrategy strategy = strategies.get(index);
      boolean darkRequestSent = strategy.handleRequest(dummyRestRequest, dummyDarkRequest, dummyRequestContext);
      Assert.assertEquals(darkRequestSent, darkClusterMetadata._darkRequestSent);
    });
    Assert.assertEquals(dummyRequestContext.getLocalAttr("identicalTrafficMultiplier.randomNumber"), expectedRandomNumber);
    Mockito.verify(random).nextFloat();
  }

  @DataProvider(name = "getDarkClusters")
  public Object[][] getDarkClusters()
  {
    return new Object[][]
        {
            {
              Arrays.asList(
                  new DarkClusterMetadata("A", 0.1f, true),
                  new DarkClusterMetadata("B", 0.2f, true),
                  new DarkClusterMetadata("C", 0.3f, true)),
                0.05f
            },
            {
              Arrays.asList(
                  new DarkClusterMetadata("B", 0.2f, true),
                  new DarkClusterMetadata("C", 0.3f, true),
                  new DarkClusterMetadata("A", 0.1f, true)),
                0.05f
            },
            {
              Arrays.asList(
                  new DarkClusterMetadata("C", 0.2f, true),
                  new DarkClusterMetadata("A", 0.3f, true),
                  new DarkClusterMetadata("B", 0.1f, true)),
                0.05f
            },
            {
              Arrays.asList(
                  new DarkClusterMetadata("A", 0.1f, false),
                  new DarkClusterMetadata("B", 0.2f, true),
                  new DarkClusterMetadata("C", 0.3f, true)),
                0.15f
            },
            {
              Arrays.asList(
                  new DarkClusterMetadata("B", 0.2f, true),
                  new DarkClusterMetadata("C", 0.3f, true),
                  new DarkClusterMetadata("A", 0.1f, false)),
                0.15f
            },
            {
              Arrays.asList(
                  new DarkClusterMetadata("C", 0.3f, true),
                  new DarkClusterMetadata("A", 0.1f, false),
                  new DarkClusterMetadata("B", 0.2f, true)),
                0.15f
            },
            {
              Arrays.asList(
                  new DarkClusterMetadata("A", 0.1f, false),
                  new DarkClusterMetadata("B", 0.2f, false),
                  new DarkClusterMetadata("C", 0.3f, true)),
                0.25f
            },
            {
              Arrays.asList(
                  new DarkClusterMetadata("B", 0.2f, false),
                  new DarkClusterMetadata("C", 0.3f, true),
                  new DarkClusterMetadata("A", 0.1f, false)),
                0.25f
            },
            {
              Arrays.asList(
                  new DarkClusterMetadata("C", 0.3f, true),
                  new DarkClusterMetadata("A", 0.1f, false),
                  new DarkClusterMetadata("B", 0.2f, false)),
                0.25f
            },
            {
              Arrays.asList(
                  new DarkClusterMetadata("A", 0.1f, false),
                  new DarkClusterMetadata("B", 0.2f, false),
                  new DarkClusterMetadata("C", 0.3f, false)),
                0.35f
            },
            {
              Arrays.asList(
                  new DarkClusterMetadata("B", 0.2f, false),
                  new DarkClusterMetadata("C", 0.3f, false),
                  new DarkClusterMetadata("A", 0.1f, false)),
                0.35f
            },
            {
              Arrays.asList(
                  new DarkClusterMetadata("C", 0.3f, false),
                  new DarkClusterMetadata("A", 0.1f, false),
                  new DarkClusterMetadata("B", 0.2f, false)),
                0.35f
            }
        };
  }
}
