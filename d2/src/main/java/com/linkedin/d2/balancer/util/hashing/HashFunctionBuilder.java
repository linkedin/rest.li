/*
   Copyright (c) 2018 LinkedIn Corp.

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

package com.linkedin.d2.balancer.util.hashing;

import com.linkedin.common.util.MapUtil;
import com.linkedin.d2.balancer.properties.PropertyKeys;
import com.linkedin.r2.message.Request;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.linkedin.d2.balancer.properties.PropertyKeys.HASH_SEED;


/**
 * Build up hashFunction according to the configured method and configs
 */

public class HashFunctionBuilder
{
  private static final Logger _log = LoggerFactory.getLogger(HashFunctionBuilder.class);
  private static final long DEFAULT_SEED = 123456789L;

  private String _hashMethod;

  public HashFunctionBuilder setHashMethod(String hashMethod)
  {
    _hashMethod = hashMethod;
    return this;
  }

  public HashFunctionBuilder setHashConfig(Map<String, Object> hashConfig)
  {
    _hashConfig = hashConfig;
    return this;
  }

  private Map<String, Object> _hashConfig = new HashMap<>();

  public HashFunction<Request> build()
  {
    if (_hashMethod == null || _hashMethod.equals(PropertyKeys.HASH_METHOD_NONE))
    {
      return _hashConfig.containsKey(HASH_SEED)
          ? new SeededRandomHash(MapUtil.getWithDefault(_hashConfig, HASH_SEED, DEFAULT_SEED)) : new RandomHash();
    }
    else if (PropertyKeys.HASH_METHOD_URI_REGEX.equals(_hashMethod))
    {
      return new URIRegexHash(_hashConfig);
    }
    else
    {
      _log.warn("Unknown hash method {}, falling back to random", _hashMethod);
      return new RandomHash();
    }
  }
}
