/*
   Copyright (c) 2012 LinkedIn Corp.

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

/**
 * $Id: $
 */

package com.linkedin.restli.client;


import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.client.uribuilders.RestliUriBuilderUtil;
import com.linkedin.restli.common.CollectionRequest;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.common.CreateStatus;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.internal.client.CollectionResponseDecoder;
import java.net.URI;
import java.util.Map;

/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class BatchCreateRequest<T extends RecordTemplate> extends Request<CollectionResponse<CreateStatus>>
{
  private URI _baseUri = null;

  BatchCreateRequest(Map<String, String> headers,
                     CollectionRequest<T> input,
                     ResourceSpec resourceSpec,
                     Map<String, Object> queryParams,
                     String baseUriTemplate,
                     Map<String, Object> pathKeys)
  {
    super(ResourceMethod.BATCH_CREATE,
          input,
          headers,
          new CollectionResponseDecoder<CreateStatus>(CreateStatus.class),
          resourceSpec,
          queryParams,
          null,
          baseUriTemplate,
          pathKeys);
  }

  /**
   * @deprecated Please use {@link com.linkedin.restli.client.uribuilders.RestliUriBuilder#buildBaseUri()} instead
   */
  @Deprecated
  public URI getBaseURI()
  {
    if (_baseUri == null)
    {
      _baseUri = RestliUriBuilderUtil.createUriBuilder(this).buildBaseUri();
    }
    return _baseUri;
  }
}
