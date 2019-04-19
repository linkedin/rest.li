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

package test.r2.integ.clientserver.providers.client;

import com.linkedin.r2.filter.FilterChain;
import com.linkedin.r2.sample.Bootstrap;
import com.linkedin.r2.transport.common.Client;
import java.util.Map;


public class Http1ClientProvider implements ClientProvider
{
  private final boolean _clientROS;
  private boolean _usePipelineV2;

  public Http1ClientProvider(boolean clientROS)
  {
    this(clientROS,false);
  }

  public Http1ClientProvider(boolean clientROS, boolean usePipelineV2)
  {
    _clientROS = clientROS;
    _usePipelineV2 = usePipelineV2;
  }

  @Override
  public Client createClient(FilterChain filters)
  {
    return Bootstrap.createHttpClient(filters, _clientROS, _usePipelineV2);
  }

  @Override
  public Client createClient(FilterChain filters, Map<String, Object> clientProperties)
  {
    return Bootstrap.createHttpClient(filters, _clientROS, _usePipelineV2, clientProperties);
  }

  @Override
  public boolean getUsePipelineV2()
  {
    return _usePipelineV2;
  }

  @Override
  public String toString()
  {
    return "[" + getClass().getName() + ", stream=" + _clientROS +", _usePipelineV2=" + _usePipelineV2 + "]";
  }
}
