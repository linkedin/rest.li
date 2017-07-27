package com.linkedin.d2.balancer.config;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.linkedin.d2.BackupRequestsConfiguration;
import com.linkedin.d2.BackupRequestsConfiguration.Strategy;
import com.linkedin.d2.BackupRequestsConfigurationArray;
import com.linkedin.d2.BoundedCostBackupRequests;

public class BackupRequestsConverterTest {

  @Test
  public void testBackupRequestsConverterEmpty()
  {
    BackupRequestsConfigurationArray config = new BackupRequestsConfigurationArray();

    //round trip conversion test
    Assert.assertEquals(BackupRequestsConverter.toConfig(BackupRequestsConverter.toProperties(config)), config);
  }

  @Test
  public void testBackupRequestsConverter()
  {
    BackupRequestsConfigurationArray configArray = new BackupRequestsConfigurationArray();
    BackupRequestsConfiguration config = new BackupRequestsConfiguration();
    config.setOperation("myOperation");
    BoundedCostBackupRequests boundedCostBackupRequests = new BoundedCostBackupRequests();
    boundedCostBackupRequests.setCost(5);
    boundedCostBackupRequests.setHistoryLength(4096);
    boundedCostBackupRequests.setMaxBurst(45);
    boundedCostBackupRequests.setMinBackupDelayMs(50);
    boundedCostBackupRequests.setRequiredHistoryLength(456);
    config.setStrategy(Strategy.create(boundedCostBackupRequests));

    //round trip conversion test
    Assert.assertEquals(BackupRequestsConverter.toConfig(BackupRequestsConverter.toProperties(configArray)), configArray);
  }

}
