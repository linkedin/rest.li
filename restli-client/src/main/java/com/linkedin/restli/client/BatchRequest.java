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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.ComplexResourceKey;
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
   * @param resourcePath
   */
  public BatchRequest(URI uri,
                      ResourceMethod method,
                      RecordTemplate input,
                      Map<String, String> headers,
                      RestResponseDecoder<T> decoder,
                      ResourceSpec resourceSpec,
                      DataMap queryParams,
                      List<String> resourcePath)
  {
    super(uri, method, input, headers, decoder, resourceSpec, queryParams, resourcePath);
  }

  /**
   * Return ids of this batch request as object. At the query params datamap level complex
   * keys are represented as datamaps. Need to convert them back to ComplexResourceKeys
   * before returning.
   */
  public Set<Object> getIdObjects()
  {
    DataList idsList =
        (DataList) getQueryParams().get(RestConstants.QUERY_BATCH_IDS_PARAM);
    if (idsList == null || idsList.isEmpty())
    {
      return Collections.emptySet();
    }

    Set<Object> result = new HashSet<Object>(idsList.size());

    if (getResourceSpec().getKeyClass() == ComplexResourceKey.class)
    {
      for (Object key : idsList)
      {
        assert (key instanceof DataMap);
        result.add(ComplexResourceKey.buildFromDataMap((DataMap) key,
                                                       getResourceSpec().getKeyKeyClass(),
                                                       getResourceSpec().getKeyParamsClass()));

      }
    }
    else
    {
      result.addAll(idsList);
    }
    return result;
  }

  @Deprecated
  public Set<String> getIds()
  {
    Set<Object> idObjects = getIdObjects();
    if (idObjects.isEmpty())
    {
      return Collections.emptySet();
    }
    HashSet<String> result = new HashSet<String>(idObjects.size());
    for (Object obj : idObjects)
    {
      result.add(obj.toString());
    }
    return result;
  }
}
