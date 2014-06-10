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


import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.common.ResourceSpec;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.client.QueryParamsUtil;
import com.linkedin.restli.internal.client.RestResponseDecoder;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
   *
   * @param method
   * @param input
   * @param headers
   * @param decoder
   * @param resourceSpec
   * @param queryParams
   * @param baseUriTemplate
   * @param pathKeys
   */
  BatchRequest(ResourceMethod method,
                      RecordTemplate input,
                      Map<String, String> headers,
                      RestResponseDecoder<T> decoder,
                      ResourceSpec resourceSpec,
                      Map<String, Object> queryParams,
                      String baseUriTemplate,
                      Map<String, Object> pathKeys,
                      RestliRequestOptions requestOptions)
  {
    super(method, input, headers, decoder, resourceSpec, queryParams, null, baseUriTemplate, pathKeys, requestOptions);
  }

  /**
   * @deprecated Please use {@link #getObjectIds()} instead. {@link #getObjectIds()} returns IDs as their original,
   *             non-coerced type.
   *
   * Return ids of this batch request as object. At the query params datamap level complex
   * keys are represented as datamaps. Need to convert them back to ComplexResourceKeys
   * before returning.
   */
  @Deprecated
  public Set<Object> getIdObjects()
  {
    DataMap dataMap = QueryParamsUtil.convertToDataMap(getQueryParamsObjects());
    DataList idsList =
        (DataList) dataMap.get(RestConstants.QUERY_BATCH_IDS_PARAM);
    if (idsList == null || idsList.isEmpty())
    {
      return Collections.emptySet();
    }

    Set<Object> result = new HashSet<Object>(idsList.size());

    if (getResourceSpec().getKeyType() != null && getResourceSpec().getKeyType().getType() == ComplexResourceKey.class)
    {
      for (Object key : idsList)
      {
        assert (key instanceof DataMap);
        result.add(ComplexResourceKey.buildFromDataMap((DataMap) key,
                                                       getResourceSpec().getComplexKeyType()));

      }
    }
    else
    {
      result.addAll(idsList);
    }
    return result;
  }

  /**
   * @return the IDs of the objects in this request. The IDs are the keys with their original types (non-coerced)
   */
  public Set<Object> getObjectIds()
  {
    @SuppressWarnings({"unchecked"})
    Collection<Object> ids = (Collection<Object>) getQueryParamsObjects().get(RestConstants.QUERY_BATCH_IDS_PARAM);
    if (ids == null || ids.isEmpty())
    {
      return Collections.emptySet();
    }
    return new HashSet<Object>(ids);
  }

  /**
   * @deprecated Please use {@link #getObjectIds()} instead
   * @return
   */
  @SuppressWarnings("deprecation")
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
