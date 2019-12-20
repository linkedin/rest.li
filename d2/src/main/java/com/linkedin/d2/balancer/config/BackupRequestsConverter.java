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

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.linkedin.d2.BackupRequestsConfigurationArray;
import com.linkedin.d2.balancer.util.JacksonUtil;
import com.linkedin.data.codec.JacksonDataCodec;
import com.linkedin.data.schema.validation.CoercionMode;
import com.linkedin.data.schema.validation.RequiredMode;
import com.linkedin.data.schema.validation.ValidateDataAgainstSchema;
import com.linkedin.data.schema.validation.ValidationOptions;


/**
 * This class converts {@link BackupRequestsConfigurationArray} into a {@link List}
 * that can be stored in zookeeper and vice versa.
 *
 * @author Jaroslaw Odzga (jodzga@linkedin.com)
 */
public class BackupRequestsConverter
{
  private static final JacksonDataCodec CODEC = new JacksonDataCodec();
  private static final ValidationOptions VALIDATION_OPTIONS =
      new ValidationOptions(RequiredMode.CAN_BE_ABSENT_IF_HAS_DEFAULT, CoercionMode.STRING_TO_PRIMITIVE);

  @SuppressWarnings("unchecked")
  public static List<Map<String, Object>> toProperties(BackupRequestsConfigurationArray config)
  {
    if (config == null)
    {
      return Collections.emptyList();
    }
    else
    {
      try {
        String json = CODEC.listToString(config.data());
        return JacksonUtil.getObjectMapper().readValue(json, List.class);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public static BackupRequestsConfigurationArray toConfig(List<Map<String, Object>> properties)
  {
    try {
      String json = JacksonUtil.getObjectMapper().writeValueAsString(properties);
      BackupRequestsConfigurationArray brca = new BackupRequestsConfigurationArray(CODEC.stringToList(json));
      //fixes are applied in place
      ValidateDataAgainstSchema.validate(brca, VALIDATION_OPTIONS);
      return brca;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
