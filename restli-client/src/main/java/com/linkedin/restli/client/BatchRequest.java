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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.client.RestResponseDecoder;

/**
 * A type-bound Batch Request for a resource.
 *
 * @param <T> response entity template class
 *
 * @author adubman
 */
public class BatchRequest<T> extends Request<T>
{
  /**
   * @param uri
   * @param method
   * @param input
   * @param headers
   * @param decoder
   * @param resourceSpec
   * @param queryParams
   */
  public BatchRequest(URI uri,
                      ResourceMethod method,
                      RecordTemplate input,
                      Map<String, String> headers,
                      RestResponseDecoder<T> decoder,
                      ResourceSpec resourceSpec,
                      DataMap queryParams)
  {
    super(uri, method, input, headers, decoder, resourceSpec, queryParams);
  }

  /**
   * This method is to be exposed in the extending classes when appropriate
   */
  public Set<Object> getIds()
  {
    Set<Object> result = new HashSet<Object>();
    DataList idsList = (DataList) getQueryParams().get(RestConstants.QUERY_BATCH_IDS_PARAM);
    if (idsList == null || idsList.isEmpty())
    {
      return result;
    }
    result.addAll(idsList);
    return result;
  }
}
