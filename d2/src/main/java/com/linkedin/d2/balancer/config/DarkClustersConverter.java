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

package com.linkedin.d2.balancer.config;

import com.linkedin.d2.DarkClusterConfigMap;
import com.linkedin.d2.balancer.util.JacksonUtil;
import com.linkedin.data.codec.JacksonDataCodec;
import com.linkedin.data.schema.validation.CoercionMode;
import com.linkedin.data.schema.validation.RequiredMode;
import com.linkedin.data.schema.validation.ValidateDataAgainstSchema;
import com.linkedin.data.schema.validation.ValidationOptions;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;


/**
 * This class converts {@link DarkClusterConfigMap} into a Map
 * that can be stored in zookeeper and vice versa.
 *
 * @author David Hoa (dhoa@linkedin.com)
 */
public class DarkClustersConverter
{
  private static final JacksonDataCodec CODEC = new JacksonDataCodec();
  private static final ValidationOptions VALIDATION_OPTIONS =
      new ValidationOptions(RequiredMode.FIXUP_ABSENT_WITH_DEFAULT, CoercionMode.STRING_TO_PRIMITIVE);

  @SuppressWarnings("unchecked")
  public static Map<String, Object> toProperties(DarkClusterConfigMap config)
  {
    if (config == null)
    {
      return Collections.emptyMap();
    }
    else
    {
      try
      {
        String json = CODEC.mapToString(config.data());
        return JacksonUtil.getObjectMapper().readValue(json, Map.class);
      }
      catch (IOException e)
      {
        throw new RuntimeException(e);
      }
    }
  }

  public static DarkClusterConfigMap toConfig(Map<String, Object> properties)
  {
    try
    {
      String json = JacksonUtil.getObjectMapper().writeValueAsString(properties);
      DarkClusterConfigMap darkClusterConfigMap = new DarkClusterConfigMap(CODEC.stringToMap(json));
      //fixes are applied in place
      ValidateDataAgainstSchema.validate(darkClusterConfigMap, VALIDATION_OPTIONS);

      return darkClusterConfigMap;
    }
    catch (IOException e)
    {
      throw new RuntimeException(e);
    }
  }
}
