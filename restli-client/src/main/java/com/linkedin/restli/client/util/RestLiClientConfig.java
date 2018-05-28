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
package com.linkedin.restli.client.util;


import com.linkedin.restli.client.ScatterGatherStrategy;

/**
 * Configuration for rest.li clients.
 *
 * @author seliang
 */

public class RestLiClientConfig {
  private Boolean _useStreaming = false;
  private ScatterGatherStrategy _scatterGatherStrategy = null;

  public boolean isUseStreaming() {
    return _useStreaming;
  }

  public void setUseStreaming(boolean useStreaming) {
    _useStreaming = useStreaming;
  }

  public ScatterGatherStrategy getScatterGatherStrategy()
  {
    return _scatterGatherStrategy;
  }

  public void setScatterGatherStrategy(ScatterGatherStrategy scatterGatherStrategy)
  {
    _scatterGatherStrategy = scatterGatherStrategy;
  }

  @Override
  public boolean equals(Object obj)
  {
    if (obj == this)
    {
      return true;
    }
    if (!(obj instanceof RestLiClientConfig))
    {
      return false;
    }
    RestLiClientConfig c = (RestLiClientConfig) obj;
    return _useStreaming == c.isUseStreaming();
  }

  @Override
  public int hashCode()
  {
    int hashCode = _useStreaming.hashCode();
    return hashCode;
  }
}
