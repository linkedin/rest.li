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

package com.linkedin.restli.internal.server.methods.response;


import com.linkedin.data.DataMap;
import com.linkedin.data.collections.CheckedUtil;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.restli.common.BatchResponse;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.ErrorResponse;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.common.URLEscaper;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.ServerResourceContext;
import com.linkedin.restli.server.ResourceContext;
import com.linkedin.restli.server.RestLiServiceException;
import java.io.IOException;
import java.util.Map;

/**
 * @author Josh Walker
 * @version $Revision: $
 */

public abstract class AbstractBatchResponseBuilder<V>
{
  ErrorResponseBuilder _errorResponseBuilder;

  protected AbstractBatchResponseBuilder(ErrorResponseBuilder errorResponseBuilder)
  {
    _errorResponseBuilder = errorResponseBuilder;
  }

  protected void populateErrors(final RestRequest request,
                                final RoutingResult routingResult,
                                final Map<?, RestLiServiceException> map,
                                final Map<String, String> headers,
                                final BatchResponse<? extends RecordTemplate> batchResponse) throws IOException
  {

    for (Map.Entry<?, RestLiServiceException> entry : ((ServerResourceContext) routingResult.getContext()).getBatchKeyErrors()
                                                                                                          .entrySet())
    {
      DataMap errorData =
          _errorResponseBuilder.buildResponse(request, routingResult, entry.getValue(), headers)
                 .getDataMap();
      batchResponse.getErrors().put(keyToString(entry.getKey()),
                                    new ErrorResponse(errorData));
    }

    for (Map.Entry<?, RestLiServiceException> entry : map.entrySet())
    {
      DataMap errorData =
          _errorResponseBuilder.buildResponse(request, routingResult, entry.getValue(), headers)
                 .getDataMap();
      batchResponse.getErrors().put(keyToString(entry.getKey()),
                                    new ErrorResponse(errorData));
    }
  }

  protected void populateResults(final BatchResponse<?> response,
                                 final Map<?, ? extends V> resultsMap,
                                 final Map<String, String> headers,
                                 final Class<? extends RecordTemplate> valueClass,
                                 final ResourceContext resourceContext)
  {
    DataMap dataMap = (DataMap) response.data().get(BatchResponse.RESULTS);

    for (Map.Entry<?, ? extends V> entry: resultsMap.entrySet())
    {
      DataMap data = buildResultRecord(entry.getValue(), resourceContext);
      CheckedUtil.putWithoutChecking(dataMap, keyToString(entry.getKey()), data);
    }
  }

  protected <T extends RecordTemplate> BatchResponse<T> createBatchResponse(final Class<T> clazz,
                                                                            final int resultsCapacity,
                                                                            final int errorsCapacity)
  {
    return new BatchResponse<T>(clazz, resultsCapacity, errorsCapacity);
  }

  /**
   * Subclasses must override this method to convert an application result into a
   * RecordTemplate representation to be sent in the BatchResponse.
   *
   * This method is called by populateResults for each value in the resultsMap
   *
   * @param o - the object to be converted
   * @return a RecordTemplate representation of the converted object
   */
  protected abstract DataMap buildResultRecord(V o, ResourceContext resourceContext);

  private static String keyToString(Object key)
  {
    String result;
    if (key == null)
    {
      result = null;
    }
    else if (key instanceof ComplexResourceKey)
    {
      result = ((ComplexResourceKey<?,?>)key).toString(URLEscaper.Escaping.URL_ESCAPING);
    }
    else if (key instanceof CompoundKey)
    {
      result = key.toString(); // already escaped
    }
    else
    {
      result = URLEscaper.escape(DataTemplateUtil.stringify(key), URLEscaper.Escaping.NO_ESCAPING);
    }
    return result;
  }
}
