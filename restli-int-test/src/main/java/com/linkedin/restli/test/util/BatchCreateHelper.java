/*
   Copyright (c) 2014 LinkedIn Corp.

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

package com.linkedin.restli.test.util;


import com.linkedin.data.template.RecordTemplate;
import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.restli.client.BatchCreateIdRequest;
import com.linkedin.restli.client.BatchCreateIdRequestBuilder;
import com.linkedin.restli.client.BatchCreateRequest;
import com.linkedin.restli.client.BatchCreateRequestBuilder;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.RestClient;
import com.linkedin.restli.common.BatchCreateIdResponse;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.common.CreateIdStatus;
import com.linkedin.restli.common.CreateStatus;

import java.util.ArrayList;
import java.util.List;


/**
 * Functions for dealing with differences between v1 and v2 request builders batchCreate.
 * @author Moira Tagle
 */
public class BatchCreateHelper
{
  public static <K, V extends RecordTemplate> List<CreateIdStatus<K>> batchCreate(RestClient restClient, RootBuilderWrapper<K, V>  builders, List<V> entities, boolean addParams)
    throws RemoteInvocationException
  {
    RootBuilderWrapper.MethodBuilderWrapper<K, V, CollectionResponse<CreateStatus>> batchCreateWrapper = builders.batchCreate();
    if (batchCreateWrapper.isRestLi2Builder())
    {
      Object obj = batchCreateWrapper.getBuilder();
      @SuppressWarnings("unchecked")
      BatchCreateIdRequestBuilder<K, V> builder = (BatchCreateIdRequestBuilder<K, V>) obj;
      if (addParams) {
        builder.addParam("useless", "param");
        builder.addParam("foo", 2);
      }
      return batchCreateNewBuilders(restClient, builder, entities);
    }
    else
    {
      @SuppressWarnings("unchecked")
      BatchCreateRequestBuilder<K, V> builder = (BatchCreateRequestBuilder<K, V>) batchCreateWrapper.getBuilder();
      if (addParams) {
        builder.addParam("useless", "param");
        builder.addParam("foo", 2);
      }
      return batchCreateOldBuilders(restClient, builder, entities);
    }
  }

  private static <K, V extends RecordTemplate> List<CreateIdStatus<K>> batchCreateOldBuilders(RestClient restClient, BatchCreateRequestBuilder<K, V> builder, List<V> entities)
    throws RemoteInvocationException
  {
    BatchCreateRequest<V> request = builder.inputs(entities).build();
    Response<CollectionResponse<CreateStatus>> response = restClient.sendRequest(request).getResponse();
    List<CreateStatus> elements = response.getEntity().getElements();
    List<CreateIdStatus<K>> result = new ArrayList<CreateIdStatus<K>>(elements.size());
    for (CreateStatus status : elements)
    {
      @SuppressWarnings("unchecked")
      CreateIdStatus<K> createIdStatus = (CreateIdStatus<K>) status;
      result.add(createIdStatus);
    }
    return result;
  }

  private static <K, V extends RecordTemplate> List<CreateIdStatus<K>> batchCreateNewBuilders(RestClient restClient,  BatchCreateIdRequestBuilder<K, V> builder, List<V> entities)
    throws RemoteInvocationException
  {
    BatchCreateIdRequest<K, V> request = builder.inputs(entities).build();
    Response<BatchCreateIdResponse<K>> response = restClient.sendRequest(request).getResponse();
    return response.getEntity().getElements();
  }
}
