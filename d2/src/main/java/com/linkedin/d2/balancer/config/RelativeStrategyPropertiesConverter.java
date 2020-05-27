/*
   Copyright (c) 2020 LinkedIn Corp.

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

import com.linkedin.d2.D2RelativeStrategyProperties;
import com.linkedin.d2.HashConfig;
import com.linkedin.d2.balancer.util.JacksonUtil;
import com.linkedin.d2.balancer.util.hashing.URIRegexHash;
import com.linkedin.data.codec.JacksonDataCodec;
import com.linkedin.data.schema.validation.CoercionMode;
import com.linkedin.data.schema.validation.RequiredMode;
import com.linkedin.data.schema.validation.ValidateDataAgainstSchema;
import com.linkedin.data.schema.validation.ValidationOptions;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * Converter for {@link com.linkedin.d2.D2RelativeStrategyProperties}.
 */
public class RelativeStrategyPropertiesConverter
{
  private static final JacksonDataCodec CODEC = new JacksonDataCodec();
  private static final ValidationOptions VALIDATION_OPTIONS =
    new ValidationOptions(RequiredMode.FIXUP_ABSENT_WITH_DEFAULT, CoercionMode.STRING_TO_PRIMITIVE);

  @SuppressWarnings("unchecked")
  public static Map<String, String> toMap(D2RelativeStrategyProperties properties)
  {
    if (properties == null)
    {
      return Collections.emptyMap();
    }

    try
    {
      String json = CODEC.mapToString(properties.data());
      return JacksonUtil.getObjectMapper().readValue(json, Map.class);
    }
    catch (IOException e)
    {
      throw new RuntimeException(e);
    }
  }

  public static D2RelativeStrategyProperties toProperties(Map<String, Object> properties)
  {
    if (properties == null)
    {
      return new D2RelativeStrategyProperties();
    }

    try
    {
      String json = JacksonUtil.getObjectMapper().writeValueAsString(properties);
      D2RelativeStrategyProperties relativeStrategyProperties = new D2RelativeStrategyProperties(CODEC.stringToMap(json));

      ValidateDataAgainstSchema.validate(relativeStrategyProperties, VALIDATION_OPTIONS);

      return relativeStrategyProperties;
    }
    catch (IOException e)
    {
      throw new RuntimeException(e);
    }
  }

  public static Map<String, Object> convertHashConfigToMap(HashConfig hashConfig) {
    Map<String, Object> hashConfigProperties = new HashMap<>();
    if (hashConfig.hasUriRegexes())
    {
      hashConfigProperties.put(URIRegexHash.KEY_REGEXES, hashConfig.getUriRegexes().stream().collect(Collectors.toList()));
    }
    if (hashConfig.hasFailOnNoMatch()) {
      hashConfigProperties.put(URIRegexHash.KEY_FAIL_ON_NO_MATCH, hashConfig.isFailOnNoMatch().toString());
    }
    if (hashConfig.hasWarnOnNoMatch()) {
      hashConfigProperties.put(URIRegexHash.KEY_WARN_ON_NO_MATCH, hashConfig.isWarnOnNoMatch().toString());
    }
    return hashConfigProperties;
  }
}
