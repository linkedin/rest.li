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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.util.Map;

import org.testng.annotations.Test;

import com.linkedin.d2.BackupRequestsConfiguration;
import com.linkedin.d2.BoundedCostBackupRequests;
import com.linkedin.d2.balancer.util.JacksonUtil;
import com.linkedin.data.codec.JacksonDataCodec;


public class TestBackupRequestsStrategyFactory
{

  @Test
  public void testBoundedCostBackupRequestsWithDefaultsDeser() throws IOException
  {
    BackupRequestsConfiguration brc = new BackupRequestsConfiguration();
    BoundedCostBackupRequests bcbr = new BoundedCostBackupRequests();
    bcbr.setCost(3);
    brc.setOperation("BATCH_GET");
    brc.setStrategy(BackupRequestsConfiguration.Strategy.create(bcbr));
    String json = new JacksonDataCodec().mapToString(brc.data());
    @SuppressWarnings("unchecked")
    Map<String, Object> map = JacksonUtil.getObjectMapper().readValue(json, Map.class);
    BackupRequestsStrategy strategy = BackupRequestsStrategyFactory.tryCreate(map);
    assertNotNull(strategy);
    assertTrue(strategy instanceof BoundedCostBackupRequestsStrategy);
    BoundedCostBackupRequestsStrategy boundedCostStrategy = (BoundedCostBackupRequestsStrategy) strategy;
    assertEquals(boundedCostStrategy.getHistoryLength(), (int) bcbr.getHistoryLength());
    assertEquals(boundedCostStrategy.getMinBackupDelayNano(), (long) bcbr.getMinBackupDelayMs() * 1000L * 1000L);
    assertEquals(boundedCostStrategy.getRequiredHistory(), (int) bcbr.getRequiredHistoryLength());
    assertEquals(boundedCostStrategy.getPercent(), (double) bcbr.getCost());
  }

  @Test
  public void testBoundedCostBackupRequestsDeser() throws IOException
  {
    BackupRequestsConfiguration brc = new BackupRequestsConfiguration();
    BoundedCostBackupRequests bcbr = new BoundedCostBackupRequests();
    bcbr.setCost(3);
    bcbr.setHistoryLength(4096);
    bcbr.setMaxBurst(16);
    bcbr.setMinBackupDelayMs(5);
    bcbr.setRequiredHistoryLength(65536);
    brc.setOperation("BATCH_GET");
    brc.setStrategy(BackupRequestsConfiguration.Strategy.create(bcbr));
    String json = new JacksonDataCodec().mapToString(brc.data());
    @SuppressWarnings("unchecked")
    Map<String, Object> map = JacksonUtil.getObjectMapper().readValue(json, Map.class);
    BackupRequestsStrategy strategy = BackupRequestsStrategyFactory.tryCreate(map);
    assertNotNull(strategy);
    assertTrue(strategy instanceof BoundedCostBackupRequestsStrategy);
    BoundedCostBackupRequestsStrategy boundedCostStrategy = (BoundedCostBackupRequestsStrategy) strategy;
    assertEquals(boundedCostStrategy.getHistoryLength(), (int) bcbr.getHistoryLength());
    assertEquals(boundedCostStrategy.getMinBackupDelayNano(), (long) bcbr.getMinBackupDelayMs() * 1000L * 1000L);
    assertEquals(boundedCostStrategy.getRequiredHistory(), (int) bcbr.getRequiredHistoryLength());
    assertEquals(boundedCostStrategy.getPercent(), (double) bcbr.getCost());
  }

}
