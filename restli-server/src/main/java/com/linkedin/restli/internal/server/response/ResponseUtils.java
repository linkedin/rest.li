/*
   Copyright (c) 2015 LinkedIn Corp.

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

package com.linkedin.restli.internal.server.response;

import com.linkedin.data.ByteString;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.codec.entitystream.StreamDataCodec;
import com.linkedin.data.collections.CheckedUtil;
import com.linkedin.data.schema.ArrayDataSchema;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.MapDataSchema;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.TyperefDataSchema;
import com.linkedin.data.schema.UnionDataSchema;
import com.linkedin.entitystream.EntityStream;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.r2.message.stream.StreamException;
import com.linkedin.r2.message.stream.StreamResponse;
import com.linkedin.r2.message.stream.StreamResponseBuilder;
import com.linkedin.r2.message.stream.entitystream.adapter.EntityStreamAdapters;
import com.linkedin.restli.common.ContentType;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.common.CookieUtil;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.ServerResourceContext;
import com.linkedin.restli.internal.server.model.ResourceModel;
import com.linkedin.restli.internal.server.util.AlternativeKeyCoercerException;
import com.linkedin.restli.internal.server.util.ArgumentUtils;
import com.linkedin.restli.internal.server.util.DataMapUtils;
import com.linkedin.restli.restspec.ResourceEntityType;
import com.linkedin.restli.server.RestLiServiceException;

import java.net.URI;
import java.util.Map;
import javax.activation.MimeTypeParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author mtagle
 */
public class ResponseUtils
{
  private static final Logger log = LoggerFactory.getLogger(ResponseUtils.class);
  /**
   * If needed, translate a given canonical key to its alternative format.
   *
   * @param canonicalKey the canonical key
   * @param routingResult the routing result
   * @return the canonical key if the request did not use or ask for alternative keys, the alternative key otherwise.
   */
  static Object translateCanonicalKeyToAlternativeKeyIfNeeded(Object canonicalKey, RoutingResult routingResult)
  {
    if (routingResult.getContext().hasParameter(RestConstants.ALT_KEY_PARAM))
    {
      String altKeyName = routingResult.getContext().getParameter(RestConstants.ALT_KEY_PARAM);
      ResourceModel resourceModel = routingResult.getResourceMethod().getResourceModel();
      try
      {
        return ArgumentUtils.translateToAlternativeKey(canonicalKey, altKeyName, resourceModel);
      }
      catch (AlternativeKeyCoercerException e)
      {
        throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR,
                String.format("Unexpected Error when coercing canonical key '%s' to alternative key type '%s'", canonicalKey, altKeyName),
                e);
      }
    }
    else
    {
      return canonicalKey;
    }
  }

  /**
   * @param schema schema for the companion data map
   * @param dataWithoutDefault data map that is response for a restli request
   * @return data object that filled in with default values on the field with default value set on the schema
   */
  public static Object fillInDataDefault(DataSchema schema, Object dataWithoutDefault)
  {
    try
    {
      switch (schema.getType())
      {
        case RECORD:
          return fillInDefaultOnRecord((RecordDataSchema) schema, (DataMap) dataWithoutDefault);
        case TYPEREF:
          return fillInDefaultOnTyperef((TyperefDataSchema) schema, dataWithoutDefault);
        case MAP:
          return fillInDefaultOnMap((MapDataSchema) schema, (DataMap) dataWithoutDefault);
        case UNION:
          return fillInDefaultOnUnion((UnionDataSchema) schema, (DataMap) dataWithoutDefault);
        case ARRAY:
          return fillInDefaultOnArray((ArrayDataSchema) schema, (DataList) dataWithoutDefault);
        default:
          return dataWithoutDefault;
      }
    }
    catch (CloneNotSupportedException ex)
    {
      throw new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR, ex);
    }
  }

  private static DataMap fillInDefaultOnRecord(RecordDataSchema schema, DataMap dataMap) throws CloneNotSupportedException
  {
    DataMap dataWithDefault = dataMap.clone();
    for (RecordDataSchema.Field field : schema.getFields())
    {
      if (dataMap.containsKey(field.getName()) || field.getDefault() != null)
      {
        Object fieldData = dataMap.containsKey(field.getName()) ? dataMap.get(field.getName()) : field.getDefault();
        CheckedUtil.putWithoutChecking(dataWithDefault, field.getName(), fillInDataDefault(field.getType(), fieldData));
      }
    }
    return dataWithDefault;
  }

  private static DataMap fillInDefaultOnMap(MapDataSchema schema, DataMap dataMap) throws CloneNotSupportedException
  {
    DataSchema valueSchema = schema.getValues();
    DataMap dataWithDefault = dataMap.clone();
    for (Map.Entry<String, Object> entry : dataMap.entrySet())
    {
      CheckedUtil.putWithoutChecking(dataWithDefault, entry.getKey(), fillInDataDefault(valueSchema, entry.getValue()));
    }
    return dataWithDefault;
  }

  private static DataList fillInDefaultOnArray(ArrayDataSchema schema, DataList dataList)
  {
    DataSchema itemDataSchema = schema.getItems();
    DataList dataListWithDefault = new DataList(dataList.size());
    for (Object o : dataList)
    {
      CheckedUtil.addWithoutChecking(dataListWithDefault, fillInDataDefault(itemDataSchema, o));
    }
    return dataListWithDefault;
  }

  private static DataMap fillInDefaultOnUnion(UnionDataSchema schema, DataMap dataMap) throws CloneNotSupportedException
  {
    DataMap dataWithDefault = dataMap.clone();
    if (dataWithDefault.size() == 1)
    {
      for (Map.Entry<String, Object> entry: dataWithDefault.entrySet())
      {
        String memberTypeKey = entry.getKey();
        DataSchema memberDataSchema = schema.getTypeByMemberKey(memberTypeKey);
        if (memberDataSchema == null)
        {
          return dataWithDefault;
        }
        CheckedUtil.putWithoutChecking(dataWithDefault, memberTypeKey, fillInDataDefault(memberDataSchema, entry.getValue()));
      }
    }
    return dataWithDefault;
  }

  private static Object fillInDefaultOnTyperef(TyperefDataSchema typerefDataSchema, Object data) throws CloneNotSupportedException
  {
    DataSchema dataSchema = typerefDataSchema.getDereferencedDataSchema();
    return fillInDataDefault(dataSchema, data);
  }

  public static RestResponse buildResponse(RoutingResult routingResult, RestLiResponse restLiResponse)
  {
    RestResponseBuilder builder = new RestResponseBuilder()
        .setHeaders(restLiResponse.getHeaders())
        .setCookies(CookieUtil.encodeSetCookies(restLiResponse.getCookies()))
        .setStatus(restLiResponse.getStatus().getCode());

    ServerResourceContext context = routingResult.getContext();
    ResourceEntityType resourceEntityType = routingResult.getResourceMethod()
                                                         .getResourceModel()
                                                         .getResourceEntityType();
    if (restLiResponse.hasData() && ResourceEntityType.STRUCTURED_DATA == resourceEntityType)
    {
      DataMap dataMap = restLiResponse.getDataMap();
      String mimeType = context.getResponseMimeType();
      URI requestUri = context.getRequestURI();
      Map<String, String> requestHeaders = context.getRequestHeaders();
      builder = encodeResult(mimeType, requestUri, requestHeaders, builder, dataMap);
    }
    return builder.build();
  }

  private static RestResponseBuilder encodeResult(String mimeType,
      URI requestUri,
      Map<String, String> requestHeaders,
      RestResponseBuilder builder,
      DataMap dataMap)
  {
    try
    {
      ContentType type = ContentType.getResponseContentType(mimeType, requestUri, requestHeaders).orElseThrow(
          () -> new RestLiServiceException(HttpStatus.S_406_NOT_ACCEPTABLE,
              "Requested mime type for encoding is not supported. Mimetype: " + mimeType));
      assert type != null;
      builder.setHeader(RestConstants.HEADER_CONTENT_TYPE, type.getHeaderKey());
      builder.setEntity(DataMapUtils.mapToByteString(dataMap, type.getCodec()));
    }
    catch (MimeTypeParseException e)
    {
      throw new RestLiServiceException(HttpStatus.S_406_NOT_ACCEPTABLE, "Invalid mime type: " + mimeType);
    }

    return builder;
  }

  public static RestException buildRestException(RestLiResponseException restLiResponseException)
  {
    return buildRestException(restLiResponseException, true);
  }

  public static RestException buildRestException(RestLiResponseException restLiResponseException, boolean writableStackTrace)
  {
    RestLiResponse restLiResponse = restLiResponseException.getRestLiResponse();
    RestResponseBuilder responseBuilder = new RestResponseBuilder()
        .setHeaders(restLiResponse.getHeaders())
        .setCookies(CookieUtil.encodeSetCookies(restLiResponse.getCookies()))
        .setStatus(restLiResponse.getStatus() == null ? HttpStatus.S_500_INTERNAL_SERVER_ERROR.getCode()
            : restLiResponse.getStatus().getCode());

    if (restLiResponse.hasData() && restLiResponse.getStatus() != HttpStatus.S_204_NO_CONTENT)
    {
      responseBuilder.setEntity(DataMapUtils.mapToByteString(restLiResponse.getDataMap(), responseBuilder.getHeaders()));
    }

    RestResponse restResponse = responseBuilder.build();
    Throwable cause = restLiResponseException.getCause();
    return new RestException(restResponse, cause==null ? null : cause.toString(), cause, writableStackTrace);
  }

  public static StreamException buildStreamException(RestLiResponseException restLiResponseException, ContentType contentType)
  {
    RestLiResponse restLiResponse = restLiResponseException.getRestLiResponse();
    StreamResponseBuilder responseBuilder = new StreamResponseBuilder()
        .setHeaders(restLiResponse.getHeaders())
        .setHeader(RestConstants.HEADER_CONTENT_TYPE, contentType.getHeaderKey())
        .setCookies(CookieUtil.encodeSetCookies(restLiResponse.getCookies()))
        .setStatus(restLiResponse.getStatus() == null ? HttpStatus.S_500_INTERNAL_SERVER_ERROR.getCode()
            : restLiResponse.getStatus().getCode());

    EntityStream<ByteString> entityStream = contentType.getStreamCodec().encodeMap(restLiResponse.getDataMap());
    StreamResponse response = responseBuilder.build(EntityStreamAdapters.fromGenericEntityStream(entityStream));
    return new StreamException(response, restLiResponseException.getCause());
  }
}
