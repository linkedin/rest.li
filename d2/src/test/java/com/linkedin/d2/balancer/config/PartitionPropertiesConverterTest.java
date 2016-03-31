/*
   Copyright (c) 2016 LinkedIn Corp.

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

package com.linkedin.d2.balancer.config;

import com.linkedin.d2.D2ClusterPartitionConfiguration;
import com.linkedin.d2.PartitionTypeEnum;
import com.linkedin.d2.balancer.properties.HashBasedPartitionProperties;
import com.linkedin.d2.balancer.properties.NullPartitionProperties;
import com.linkedin.d2.balancer.properties.PartitionProperties;
import com.linkedin.d2.balancer.properties.RangeBasedPartitionProperties;
import com.linkedin.d2.rangedPartitionProperties;
import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * @author Ang Xu
 */
public class PartitionPropertiesConverterTest
{

  @Test
  public void testNullPartitionProperties()
  {
    PartitionProperties partitionProperties = NullPartitionProperties.getInstance();
    D2ClusterPartitionConfiguration partitionConfig =
        new D2ClusterPartitionConfiguration().setType(PartitionTypeEnum.NONE);

    Assert.assertEquals(PartitionPropertiesConverter.toProperties(partitionConfig), partitionProperties);
    Assert.assertEquals(PartitionPropertiesConverter.toConfig(partitionProperties), partitionConfig);
  }

  @Test
  public void testRangePartitionProperties()
  {
    final String partitionKeyRegex = "/foo/bar/(\\d+)";
    final long keyRangeStart = 1;
    final long paritionSize = 1024;
    final int partitionCount = 32;

    PartitionProperties partitionProperties =
        new RangeBasedPartitionProperties(partitionKeyRegex, keyRangeStart, paritionSize, partitionCount);

    D2ClusterPartitionConfiguration.PartitionTypeSpecificData data =
        new D2ClusterPartitionConfiguration.PartitionTypeSpecificData();
    data.setRangedPartitionProperties(
        new rangedPartitionProperties()
            .setKeyRangeStart(keyRangeStart)
            .setPartitionSize(paritionSize)
    );
    D2ClusterPartitionConfiguration partitionConfig =
        new D2ClusterPartitionConfiguration()
            .setType(PartitionTypeEnum.RANGE)
            .setPartitionKeyRegex(partitionKeyRegex)
            .setPartitionCount(partitionCount)
            .setPartitionTypeSpecificData(data);

    Assert.assertEquals(PartitionPropertiesConverter.toProperties(partitionConfig), partitionProperties);
    Assert.assertEquals(PartitionPropertiesConverter.toConfig(partitionProperties), partitionConfig);
  }

  @Test
  public void testHashMD5PartitionProperties()
  {
    final String partitionKeyRegex = "/foo/bar/(\\d+)";
    final int partitionCount = 8;
    final HashBasedPartitionProperties.HashAlgorithm hashAlgorithm =
        HashBasedPartitionProperties.HashAlgorithm.MD5;

    PartitionProperties partitionProperties =
        new HashBasedPartitionProperties(partitionKeyRegex, partitionCount, hashAlgorithm);

    D2ClusterPartitionConfiguration.PartitionTypeSpecificData data
        = new D2ClusterPartitionConfiguration.PartitionTypeSpecificData();
    data.setHashAlgorithm(com.linkedin.d2.hashAlgorithm.MD5);
    D2ClusterPartitionConfiguration partitionConfig =
        new D2ClusterPartitionConfiguration()
            .setType(PartitionTypeEnum.HASH)
            .setPartitionKeyRegex(partitionKeyRegex)
            .setPartitionCount(partitionCount)
            .setPartitionTypeSpecificData(data);


    Assert.assertEquals(PartitionPropertiesConverter.toProperties(partitionConfig), partitionProperties);
    Assert.assertEquals(PartitionPropertiesConverter.toConfig(partitionProperties), partitionConfig);
  }

  @Test
  public void testHashModuloPartitionProperties()
  {
    final String partitionKeyRegex = "/foo/bar/(\\d+)";
    final int partitionCount = 16;
    final HashBasedPartitionProperties.HashAlgorithm hashAlgorithm =
        HashBasedPartitionProperties.HashAlgorithm.MODULO;

    PartitionProperties partitionProperties =
        new HashBasedPartitionProperties(partitionKeyRegex, partitionCount, hashAlgorithm);

    D2ClusterPartitionConfiguration.PartitionTypeSpecificData data
        = new D2ClusterPartitionConfiguration.PartitionTypeSpecificData();
    data.setHashAlgorithm(com.linkedin.d2.hashAlgorithm.MODULO);
    D2ClusterPartitionConfiguration partitionConfig =
        new D2ClusterPartitionConfiguration()
            .setType(PartitionTypeEnum.HASH)
            .setPartitionKeyRegex(partitionKeyRegex)
            .setPartitionCount(partitionCount)
            .setPartitionTypeSpecificData(data);

    Assert.assertEquals(PartitionPropertiesConverter.toConfig(partitionProperties), partitionConfig);
    Assert.assertEquals(PartitionPropertiesConverter.toProperties(partitionConfig), partitionProperties);
  }
}
