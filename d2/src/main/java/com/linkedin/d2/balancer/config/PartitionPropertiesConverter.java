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
import com.linkedin.d2.HashAlgorithm;
import com.linkedin.d2.PartitionAccessorList;
import com.linkedin.d2.PartitionTypeEnum;
import com.linkedin.d2.balancer.properties.CustomizedPartitionProperties;
import com.linkedin.d2.balancer.properties.HashBasedPartitionProperties;
import com.linkedin.d2.balancer.properties.NullPartitionProperties;
import com.linkedin.d2.balancer.properties.PartitionProperties;
import com.linkedin.d2.balancer.properties.RangeBasedPartitionProperties;
import com.linkedin.d2.RangedPartitionProperties;
import com.linkedin.data.template.StringArray;


/**
 * This class converts {@link D2ClusterPartitionConfiguration} into
 * {@link PartitionProperties} and vice versa.
 * @author Ang Xu
 */
public class PartitionPropertiesConverter
{
  public static PartitionProperties toProperties(D2ClusterPartitionConfiguration config)
  {
    final PartitionProperties partitionProperties;
    switch (config.getType())
    {
      case RANGE:
      {
        RangedPartitionProperties rangedPartitionProperties =
            config.getPartitionTypeSpecificData().getRangedPartitionProperties();
        partitionProperties =
            new RangeBasedPartitionProperties(config.getPartitionKeyRegex(),
                rangedPartitionProperties.getKeyRangeStart(),
                rangedPartitionProperties.getPartitionSize(),
                config.getPartitionCount());
        break;
      }
      case HASH:
        HashBasedPartitionProperties.HashAlgorithm algorithm;
        switch (config.getPartitionTypeSpecificData().getHashAlgorithm())
        {
          case MODULO:
            algorithm = HashBasedPartitionProperties.HashAlgorithm.MODULO;
            break;
          case MD5:
            algorithm = HashBasedPartitionProperties.HashAlgorithm.MD5;
            break;
          case XXHASH:
            algorithm = HashBasedPartitionProperties.HashAlgorithm.XXHASH;
            break;
          default:
            throw new IllegalArgumentException("Unsupported hash algorithm: " +
                config.getPartitionTypeSpecificData().getHashAlgorithm());
        }
        partitionProperties =
            new HashBasedPartitionProperties(config.getPartitionKeyRegex(),
                config.getPartitionCount(),
                algorithm);
        break;
      case CUSTOM:
        partitionProperties = new CustomizedPartitionProperties(config.getPartitionCount(),
          config.getPartitionTypeSpecificData().getPartitionAccessorList().getClassNames());
        break;
      case NONE:
        partitionProperties = NullPartitionProperties.getInstance();
        break;
      default:
        throw new IllegalArgumentException("Unsupported partitionType: " + config.getType());
    }
    return partitionProperties;
  }

  public static D2ClusterPartitionConfiguration toConfig(PartitionProperties property)
  {
    final D2ClusterPartitionConfiguration config;
    final D2ClusterPartitionConfiguration.PartitionTypeSpecificData specificData;
    switch (property.getPartitionType())
    {
      case RANGE:
        RangeBasedPartitionProperties range = (RangeBasedPartitionProperties) property;
        config = new D2ClusterPartitionConfiguration();
        config.setType(PartitionTypeEnum.RANGE);
        config.setPartitionKeyRegex(range.getPartitionKeyRegex());
        config.setPartitionCount(range.getPartitionCount());

        specificData = new D2ClusterPartitionConfiguration.PartitionTypeSpecificData();
        RangedPartitionProperties rangedPartitionProperties = new RangedPartitionProperties();
        rangedPartitionProperties.setKeyRangeStart(range.getKeyRangeStart());
        rangedPartitionProperties.setPartitionSize(range.getPartitionSize());
        specificData.setRangedPartitionProperties(rangedPartitionProperties);
        config.setPartitionTypeSpecificData(specificData);
        break;
      case HASH:
        HashBasedPartitionProperties hash = (HashBasedPartitionProperties) property;
        config = new D2ClusterPartitionConfiguration();
        config.setType(PartitionTypeEnum.HASH);
        config.setPartitionKeyRegex(hash.getPartitionKeyRegex());
        config.setPartitionCount(hash.getPartitionCount());

        specificData = new D2ClusterPartitionConfiguration.PartitionTypeSpecificData();
        specificData.setHashAlgorithm(HashAlgorithm.valueOf(hash.getHashAlgorithm().name()));
        config.setPartitionTypeSpecificData(specificData);
        break;
      case CUSTOM:
      {
        CustomizedPartitionProperties properties = (CustomizedPartitionProperties) property;
        config = new D2ClusterPartitionConfiguration();
        config.setType(PartitionTypeEnum.CUSTOM);
        config.setPartitionCount(properties.getPartitionCount());

        specificData = new D2ClusterPartitionConfiguration.PartitionTypeSpecificData();
        PartitionAccessorList partitionList = new PartitionAccessorList();
        partitionList.setClassNames(new StringArray(properties.getPartitionAccessorList()));
        specificData.setPartitionAccessorList(partitionList);
        config.setPartitionTypeSpecificData(specificData);
        break;
      }
      case NONE:
        config = new D2ClusterPartitionConfiguration();
        config.setType(PartitionTypeEnum.NONE);
        break;
      default:
        throw new IllegalArgumentException("Unsupported partitionType: " + property.getPartitionType());
    }
    return config;
  }
}
