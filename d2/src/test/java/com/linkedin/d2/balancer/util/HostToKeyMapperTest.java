package com.linkedin.d2.balancer.util;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Created by xzhu on 2/5/15.
 */
public class HostToKeyMapperTest
{
  @Test
  public void verifyMapKeyResultWithHost() throws URISyntaxException
  {
    final URI foo1 = new URI("http://foo1.com");
    final URI foo2 = new URI("http://foo2.com");
    final URI foo3 = new URI("http://foo3.com");
    final URI foo4 = new URI("http://foo4.com");
    final URI foo5 = new URI("http://foo5.com");
    final URI foo6 = new URI("http://foo6.com");

    Map<Integer, KeysAndHosts<Integer>> partitionInfoMap = new HashMap<Integer, KeysAndHosts<Integer>>();
    KeysAndHosts<Integer> keyAndHosts0 = new KeysAndHosts<Integer>(new ArrayList<Integer>(Arrays.asList(1, 2, 3)),
            new ArrayList<URI>(Arrays.asList(foo1, foo3)));
    KeysAndHosts<Integer> keyAndHosts1 = new KeysAndHosts<Integer>(new ArrayList<Integer>(Arrays.asList(4)),
            new ArrayList<URI>(Arrays.asList(foo4, foo5)));
    KeysAndHosts<Integer> keyAndHosts2 = new KeysAndHosts<Integer>(new ArrayList<Integer>(Arrays.asList(9)),
          new ArrayList<URI>());
    KeysAndHosts<Integer> keyAndHosts3 = new KeysAndHosts<Integer>(new ArrayList<Integer>(Arrays.asList(10)),
            new ArrayList<URI>(Arrays.asList(foo2)));
    KeysAndHosts<Integer> keyAndHosts4 = new KeysAndHosts<Integer>(new ArrayList<Integer>(Arrays.asList(13, 15)),
            new ArrayList<URI>(Arrays.asList(foo2)));
    partitionInfoMap.put(0, keyAndHosts0);
    partitionInfoMap.put(1, keyAndHosts1);
    partitionInfoMap.put(2, keyAndHosts2);
    partitionInfoMap.put(3, keyAndHosts3);
    partitionInfoMap.put(4, keyAndHosts4);

    HostToKeyMapper<Integer> result = new HostToKeyMapper<Integer>(new ArrayList<Integer>(Arrays.asList(16)), partitionInfoMap, 2, 5, new HashMap<Integer, Integer>());

    Assert.assertNotNull(result);

    // Test the first iteration

    HostToKeyResult<Integer> firstIteration = result.getResult(0);
    Assert.assertEquals(firstIteration.getUnmappedKeys().size(), 2);

    Assert.assertTrue(firstIteration.getUnmappedKeys().contains(new HostToKeyResult.UnmappedKey<Integer>(9,
            HostToKeyResult.ErrorType.NO_HOST_AVAILABLE_IN_PARTITION)));
    Assert.assertTrue(firstIteration.getUnmappedKeys().contains(new HostToKeyResult.UnmappedKey<Integer>(16,
            HostToKeyResult.ErrorType.FAIL_TO_FIND_PARTITION)));

    Map<URI, Collection<Integer>> mapResult = firstIteration.getMapResult();
    Assert.assertNotNull(mapResult);
    Assert.assertTrue(mapResult.size() == 3);
    Collection<Integer> keys0 = new HashSet<Integer>();
    keys0.add(1);
    keys0.add(2);
    keys0.add(3);
    Collection<Integer> keys1 = new HashSet<Integer>();
    keys1.add(4);
    Collection<Integer> keys2 = new HashSet<Integer>();
    keys2.add(10);
    keys2.add(13);
    keys2.add(15);

    for (Map.Entry<URI, Collection<Integer>> entry: mapResult.entrySet())
    {
      if (entry.getKey().equals(foo1))
      {
        Assert.assertTrue(entry.getValue().containsAll(keys0));
      }
      else if (entry.getKey().equals(foo4))
      {
        Assert.assertTrue(entry.getValue().containsAll(keys1));
      }
      else if (entry.getKey().equals(foo2))
      {
        Assert.assertTrue(entry.getValue().containsAll(keys2));
      }
      else
      {
        Assert.fail("Values should be either for partition 0,1 or 3 and 4 merged");
      }
    }
    //test second iteration

    HostToKeyResult<Integer> secondIteration = result.getResult(1);
    Assert.assertEquals(secondIteration.getUnmappedKeys().size(), 5);
    Assert.assertTrue(secondIteration.getUnmappedKeys().contains(new HostToKeyResult.UnmappedKey<Integer>(9,
            HostToKeyResult.ErrorType.NO_HOST_AVAILABLE_IN_PARTITION)));
    Assert.assertTrue(secondIteration.getUnmappedKeys().contains(new HostToKeyResult.UnmappedKey<Integer>(10,
            HostToKeyResult.ErrorType.NO_HOST_AVAILABLE_IN_PARTITION)));
    Assert.assertTrue(secondIteration.getUnmappedKeys().contains(new HostToKeyResult.UnmappedKey<Integer>(13,
            HostToKeyResult.ErrorType.NO_HOST_AVAILABLE_IN_PARTITION)));
    Assert.assertTrue(secondIteration.getUnmappedKeys().contains(new HostToKeyResult.UnmappedKey<Integer>(15,
            HostToKeyResult.ErrorType.NO_HOST_AVAILABLE_IN_PARTITION)));
    Assert.assertTrue(secondIteration.getUnmappedKeys().contains(new HostToKeyResult.UnmappedKey<Integer>(16,
            HostToKeyResult.ErrorType.FAIL_TO_FIND_PARTITION)));

    mapResult = secondIteration.getMapResult();
    for (Map.Entry<URI, Collection<Integer>> entry: mapResult.entrySet())
    {
      if (entry.getKey().equals(foo3))
      {
        Assert.assertTrue(entry.getValue().containsAll(keys0));
      }
      else if (entry.getKey().equals(foo5))
      {
        Assert.assertTrue(entry.getValue().containsAll(keys1));
      }
      else
      {
        Assert.fail("Values should be either for partition 0,1 or 3 and 4 merged");
      }
    }

    //test third iteration
    HostToKeyResult<Integer> thirdIteration = result.getResult(2);
    Assert.assertNull(thirdIteration);

    //test getResult with subset of keys
    Collection<Integer> subsetKeys = new HashSet<Integer>();
    subsetKeys.add(10);
    subsetKeys.add(13);
    subsetKeys.add(9);
    subsetKeys.add(16);
    HostToKeyResult<Integer> subsetKeyResult = result.getResult(0, subsetKeys);
    Assert.assertNotNull(subsetKeyResult);
    Assert.assertEquals(subsetKeyResult.getUnmappedKeys().size(), 2);
    Assert.assertTrue(subsetKeyResult.getUnmappedKeys().contains(new HostToKeyResult.UnmappedKey<Integer>(9,
            HostToKeyResult.ErrorType.NO_HOST_AVAILABLE_IN_PARTITION)));
    Assert.assertTrue(subsetKeyResult.getUnmappedKeys().contains(new HostToKeyResult.UnmappedKey<Integer>(16,
            HostToKeyResult.ErrorType.FAIL_TO_FIND_PARTITION)));
    mapResult = subsetKeyResult.getMapResult();
    Assert.assertEquals(mapResult.size(), 1);
    for (Map.Entry<URI, Collection<Integer>> entry: mapResult.entrySet())
    {
      Assert.assertEquals(entry.getKey(), foo2);
      Assert.assertTrue(entry.getValue().contains(new Integer(10)));
      Assert.assertTrue(entry.getValue().contains(new Integer(13)));
    }
  }
}
