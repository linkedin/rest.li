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


package test.r2.integ.clientserver.providers.client;

import com.linkedin.common.callback.Callback;
import com.linkedin.common.util.None;
import com.linkedin.r2.filter.FilterChain;
import com.linkedin.r2.sample.Bootstrap;
import com.linkedin.r2.transport.common.Client;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.r2.util.NamedThreadFactory;
import io.netty.channel.nio.NioEventLoopGroup;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * @author Nizar Mankulangara
 */
public abstract class AbstractClientProvider implements ClientProvider
{
  protected final boolean _clientROS;
  protected boolean _usePipelineV2;
  private List<HttpClientFactory> _httpClientFactoryList;
  private final static NioEventLoopGroup _nioEventLoopGroup = new NioEventLoopGroup(5, new NamedThreadFactory("R2 Nio EventLoop Integration Test"));

  protected AbstractClientProvider(boolean clientROS)
  {
    this(clientROS, false);
  }

  protected AbstractClientProvider(boolean clientROS, boolean usePipelineV2)
  {
    _clientROS = clientROS;
    _usePipelineV2 = usePipelineV2;
    _httpClientFactoryList = new ArrayList<>();
  }

  @Override
  public Client createClient(FilterChain filters) throws Exception
  {
    return createClient(createHttpClientFactory(filters), null);
  }

  @Override
  public Client createClient(FilterChain filters, Map<String, Object> clientProperties) throws Exception
  {
    return createClient(createHttpClientFactory(filters), clientProperties);
  }

  @Override
  public boolean getUsePipelineV2()
  {
    return _usePipelineV2;
  }

  @Override
  public void tearDown()
  {
    for(HttpClientFactory factory : _httpClientFactoryList)
    {
      factory.shutdown(new Callback<None>() {
        @Override
        public void onError(Throwable e) {
        }

        @Override
        public void onSuccess(None result) {

        }
      });
    }
  }

  @Override
  public String toString()
  {
    return "[" + getClass().getName() + ", stream=" + _clientROS +", _usePipelineV2=" + _usePipelineV2 + "]";
  }

  protected abstract Client createClient(HttpClientFactory httpClientFactory, Map<String, Object> clientProperties)
                                        throws Exception;

  private HttpClientFactory createHttpClientFactory(FilterChain filters)
  {
    HttpClientFactory httpClientFactory = Bootstrap.createHttpClientFactory(filters, _usePipelineV2, _nioEventLoopGroup);
    _httpClientFactoryList.add(httpClientFactory);
    return httpClientFactory;
  }
}
