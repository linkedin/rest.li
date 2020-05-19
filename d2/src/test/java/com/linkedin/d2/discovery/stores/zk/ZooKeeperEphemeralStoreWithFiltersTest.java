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

package com.linkedin.d2.discovery.stores.zk;

import com.linkedin.test.util.retry.ThreeRetries;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.linkedin.common.callback.FutureCallback;
import com.linkedin.common.util.None;
import com.linkedin.d2.balancer.servers.AnnouncerHostPrefixGenerator;
import com.linkedin.d2.balancer.servers.ZookeeperPrefixChildFilter;
import com.linkedin.d2.discovery.stores.PropertySetStringMerger;
import com.linkedin.d2.discovery.stores.PropertySetStringSerializer;
import com.linkedin.d2.discovery.stores.PropertyStoreException;

import static org.testng.Assert.fail;
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Tests for the get and pur part of the EphemeralStore with filters and prefix, which is used by during markUp/markDown
 *
 * @author Nizar Mankulangara (nmankulangara@linkedin.com)
 */
public class ZooKeeperEphemeralStoreWithFiltersTest
{
  private ZKConnection _zkClient;
  private ZKServer _zkServer;
  private int _port;
  private ExecutorService _executor = Executors.newSingleThreadExecutor();

  @Test(dataProvider = "dataD2ClusterWithNumberOfChildren", groups = { "ci-flaky" })
  public void testPutWithoutPrefixAndFilter(String d2ClusterName, int numberOfChildren)
    throws IOException, InterruptedException, ExecutionException, PropertyStoreException
  {
    ZKConnection client = new ZKConnection("localhost:" + _port, 5000);
    client.start();

    final ZooKeeperEphemeralStore<Set<String>> store = getStore(client, null, null);

    // Add new 'numberOfChildren' to the store using put
    Set<String> addedChildren = new HashSet<>();
    for (int i = 0; i < numberOfChildren; i++)
    {
      Set<String> currentChild = new HashSet<>();
      String childName = "Child" + i;
      currentChild.add(childName);
      addedChildren.add(childName);
      store.put(d2ClusterName, currentChild);
    }

    // Read all the new children added to the store using get
    Set<String> childrenFromZK = store.get(d2ClusterName);

    // Verify all children added through put are read back in get
    Assert.assertEquals(childrenFromZK.size(), addedChildren.size());
    Assert.assertEquals(addedChildren, childrenFromZK);

    tearDown(store);
  }

  @Test(dataProvider = "dataD2ClusterWithNumberOfChildrenAndHashCode", retryAnalyzer = ThreeRetries.class)
  public void testPutAndGetWithPrefixAndFilter(String d2ClusterName, List<String> childrenNames, int expectedPrefixDuplicates,
                                               List<ZookeeperEphemeralPrefixGenerator> prefixGenerators)
    throws IOException, InterruptedException, ExecutionException, PropertyStoreException
  {
    ZKConnection client = new ZKConnection("localhost:" + _port, 5000);
    client.start();

    List<ZooKeeperEphemeralStore<Set<String>>> stores = new ArrayList<>();

    // Add the given list childrenNames to the store using children specific prefixGenerator
    Set<String> addedChildren = new HashSet<>();
    for (int i = 0; i < childrenNames.size(); i++)
    {
      String childName = childrenNames.get(i);
      ZookeeperEphemeralPrefixGenerator prefixGenerator = prefixGenerators.get(i);
      Set<String> currentChild = new HashSet<>();
      currentChild.add(childName);
      addedChildren.add(childName);
      final ZooKeeperEphemeralStore<Set<String>> store = getStore(client, new ZookeeperPrefixChildFilter(prefixGenerator), prefixGenerator);
      stores.add(store);

      store.put(d2ClusterName, currentChild);
    }

    // Verify for each children the get operation returns expected number of children
    for (int i = 0; i < childrenNames.size(); i++)
    {
      String childName = childrenNames.get(i);
      ZookeeperEphemeralPrefixGenerator prefixGenerator = prefixGenerators.get(i);

      Set<String> currentChild = new HashSet<>();
      currentChild.add(childName);

      final ZooKeeperEphemeralStore<Set<String>> store = getStore(client, new ZookeeperPrefixChildFilter(prefixGenerator), prefixGenerator);
      stores.add(store);

      // Read the data from store using get with the child specific prefixGenerator and filter
      Set<String> childrenFromZK = store.get(d2ClusterName);

      // Verify expectations
      Assert.assertNotNull(childrenFromZK);
      Assert.assertEquals(childrenFromZK.size(), expectedPrefixDuplicates);
      if (expectedPrefixDuplicates == 1) // expectedPrefixDuplicates = 1 when unique prefixGenerator is used per child
      {
        Assert.assertEquals(currentChild, childrenFromZK);
      }
    }

    if (expectedPrefixDuplicates > 1) // // expectedPrefixDuplicates = childrenNames.size() when shared prefixGenerator is used
    {
      final ZooKeeperEphemeralStore<Set<String>> store =
        getStore(client, new ZookeeperPrefixChildFilter(prefixGenerators.get(0)), prefixGenerators.get(0));
      stores.add(store);

      // Read the data from store using get with the shared prefixGenerator
      Set<String> childrenFromZK = store.get(d2ClusterName);

      // verify expectations
      Assert.assertEquals(childrenFromZK.size(), addedChildren.size());
      Assert.assertEquals(addedChildren, childrenFromZK);
    }

    for (ZooKeeperEphemeralStore<Set<String>> store : stores)
    {
      tearDown(store);
    }
  }

  @DataProvider
  public Object[][] dataD2ClusterWithNumberOfChildren()
  {
    Object[][] data = new Object[25][2];
    for (int i = 0; i < 25; i++)
    {
      data[i][0] = "D2Test1Cluster" + i;
      data[i][1] = ThreadLocalRandom.current().nextInt(25) + 1;
    }

    return data;
  }

  @DataProvider
  public Object[][] dataD2ClusterWithNumberOfChildrenAndHashCode()
  {
    Object[][] data = new Object[50][4];

    // 25 test cases with shared prefix generator
    for (int i = 0; i < 25; i++)
    {
      int numChildren = ThreadLocalRandom.current().nextInt(25) + 1;
      List<String> children = new ArrayList<>();
      List<ZookeeperEphemeralPrefixGenerator> prefixGenerators = new ArrayList<>();
      AnnouncerHostPrefixGenerator generator = new AnnouncerHostPrefixGenerator("test-machine.subdomain1.subdomain2.com");
      for (int j = 0; j < numChildren; j++)
      {
        children.add("Child" + i + j + 1);
        prefixGenerators.add(generator);
      }

      data[i][0] = "D2Test2Cluster" + i;
      data[i][1] = children;
      data[i][2] = numChildren;
      data[i][3] = prefixGenerators;
    }

    // 25 test cases with unique prefix generator
    for (int i = 25; i < 50; i++)
    {
      int numChildren = ThreadLocalRandom.current().nextInt(25) + 1;
      List<String> children = new ArrayList<>();
      List<ZookeeperEphemeralPrefixGenerator> prefixGenerators = new ArrayList<>();
      for (int j = 0; j < numChildren; j++)
      {
        String childName = "Child" + i + j + 1;
        children.add(childName);
        String fqdn = "test-machine" + i + j+ ".subdomain1.subdomain2.com";
        prefixGenerators.add(new AnnouncerHostPrefixGenerator(fqdn));
      }

      data[i][0] = "D2Test2Cluster" + i;
      data[i][1] = children;
      data[i][2] = 1;
      data[i][3] = prefixGenerators;
    }

    return data;
  }

  private void tearDown(ZooKeeperEphemeralStore<Set<String>> store)
  {
    final FutureCallback<None> callback = new FutureCallback<>();
    store.shutdown(callback);
    try
    {
      callback.get(5, TimeUnit.SECONDS);
    }
    catch (InterruptedException | ExecutionException | TimeoutException e)
    {
      fail("unable to shut down store");
    }
  }

  @BeforeSuite
  public void setup()
    throws InterruptedException
  {
    try
    {
      _zkServer = new ZKServer();
      _zkServer.startup();
      _port = _zkServer.getPort();
      _zkClient = new ZKConnection("localhost:" + _port, 5000);
      _zkClient.start();
    }
    catch (IOException e)
    {
      Assert.fail("unable to instantiate real zk server on port " + _port);
    }
  }

  @AfterSuite
  public void tearDown()
    throws IOException, InterruptedException
  {
    _zkClient.shutdown();
    _zkServer.shutdown();
    _executor.shutdown();
  }

  public ZooKeeperEphemeralStore<Set<String>> getStore(ZKConnection client, ZookeeperChildFilter filter,
                                                       ZookeeperEphemeralPrefixGenerator prefixGenerator)
    throws InterruptedException, ExecutionException
  {
    ZooKeeperEphemeralStore<Set<String>> store =
      new ZooKeeperEphemeralStore<>(client, new PropertySetStringSerializer(), new PropertySetStringMerger(), "/test-path", false, true, null, null,
                                    0, filter, prefixGenerator);

    FutureCallback<None> callback = new FutureCallback<>();
    store.start(callback);
    callback.get();
    return store;
  }
}