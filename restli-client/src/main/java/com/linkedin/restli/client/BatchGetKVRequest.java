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

package com.linkedin.restli.client;

import java.net.URI;
import java.util.Map;
import java.util.Set;

import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.client.response.BatchKVResponse;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.internal.client.RestResponseDecoder;


/**
 * A request for reading a resource batch.
 *
 * @param <K> resource key class
 * @param <V> entity template class
 *
 * @author Eran Leshem
 */
public class BatchGetKVRequest<K, V extends RecordTemplate> extends Request<BatchKVResponse<K, V>>
{
  private final URI _baseURI;
  private final Set<String> _ids;
  private final Set<PathSpec> _fields;

  BatchGetKVRequest(URI uri,
                    ResourceMethod method,
                    Map<String, String> headers,
                    RestResponseDecoder<BatchKVResponse<K, V>> decoder,
                    URI baseURI,
                    Set<String> ids,
                    Set<PathSpec> fields,
                    ResourceSpec resourceSpec)
  {
    super(uri, method, null, headers, decoder, resourceSpec);
    _baseURI = baseURI;
    _ids = ids;
    _fields = fields;
  }

  public URI getBaseURI()
  {
    return _baseURI;
  }

  public Set<String> getIds()
  {
    return _ids;
  }

  public Set<PathSpec> getFields()
  {
    return _fields;
  }
}
