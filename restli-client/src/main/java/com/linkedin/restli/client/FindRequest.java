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
import java.util.List;
import java.util.Map;

import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.internal.client.CollectionResponseDecoder;


/**
 * A request for reading a resource collection.
 *
 * @param <T> entity template class
 *
 * @author Eran Leshem
 */
public class FindRequest<T extends RecordTemplate>
                extends Request<CollectionResponse<T>>
{
  @Deprecated
  FindRequest(URI uri, Map<String, String> headers, Class<T> templateClass,
              ResourceSpec resourceSpec, List<String> resourcePath)
  {
    super(uri, ResourceMethod.FINDER, null, headers, new CollectionResponseDecoder<T>(templateClass),
          resourceSpec, resourcePath);
  }

  FindRequest(URI uri, Map<String, String> headers, Class<T> templateClass,
              ResourceSpec resourceSpec, List<String> resourcePath, String name)
  {
    super(uri,
          ResourceMethod.FINDER,
          null,
          headers,
          new CollectionResponseDecoder<T>(templateClass),
          resourceSpec,
          null,
          resourcePath,
          name);
  }
}
