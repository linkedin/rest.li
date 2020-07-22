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

package com.linkedin.restli.internal.common;

import com.linkedin.data.DataComplex;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.jersey.api.uri.UriBuilder;
import com.linkedin.jersey.api.uri.UriComponent;
import com.linkedin.jersey.api.uri.UriTemplate;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.RestConstants;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.regex.Pattern;


/**
 * A utility class for creating URI parameters in the rest.li 2.0 URI style.
 *
 * @see URIElementParser URIElementParser for parsing 2.0 URIs
 *
 * @author Moira Tagle
 * @version $Revision: $
 */
public class URIParamUtils
{
  private static final String[] _EMPTY_STRING_ARRAY = new String[0];
  private static final Pattern NORMALIZED_URI_PATTERN = Pattern.compile("(^/|/$)");
  private static final Pattern URI_SEPARATOR_PATTERN = Pattern.compile("/+");

  /**
   * Return the string encoded version of query parameters.
   * For projection parameters stored in dataMap, this function handles both cases when the value is a original string
   * or a structured {@link DataMap}
   *
   * @param dataMap the {@link DataMap} which represents the query parameters
   * @return a {@link Map} from query param key to value in encoded string
   */
  private static Map<String, String> dataMapToQueryParams(DataMap dataMap)
  {
    Map<String, String> flattenedMap = new HashMap<>();
    for (Map.Entry<String, Object> entry : dataMap.entrySet())
    {
      // Serialize the projection MaskTree values
      if (RestConstants.PROJECTION_PARAMETERS.contains(entry.getKey()))
      {
        Object projectionParameters = entry.getValue();
        if (projectionParameters instanceof String)
        {
          flattenedMap.put(entry.getKey(), (String) projectionParameters);
        }
        else if (projectionParameters instanceof DataMap)
        {
          flattenedMap.put(entry.getKey(), URIMaskUtil.encodeMaskForURI((DataMap) projectionParameters));
        }
        else
        {
          throw new IllegalArgumentException("Invalid projection field data type");
        }
      }
      else
      {

        String flattenedValue = encodeElement(entry.getValue(),
                                              URLEscaper.Escaping.URL_ESCAPING,
                                              UriComponent.Type.QUERY_PARAM);
        String encodedKey = encodeString(entry.getKey(), URLEscaper.Escaping.URL_ESCAPING, UriComponent.Type.QUERY_PARAM);
        flattenedMap.put(encodedKey, flattenedValue);
      }
    }
    return flattenedMap;
  }

  /* package private */ static String encodeElement(Object obj, URLEscaper.Escaping escaping, UriComponent.Type componentType)
  {
    StringBuilder builder = new StringBuilder();
    encodeDataObject(obj, escaping, componentType, builder);
    return builder.toString();
  }

  /**
   * Serialize the given key for use in a uri
   *
   * @param key the key
   * @param componentType the uri component type
   * @param version the {@link ProtocolVersion}
   * @return the serialized key
   */
  public static String encodeKeyForUri(Object key, UriComponent.Type componentType, ProtocolVersion version)
  {
    return keyToString(key, URLEscaper.Escaping.URL_ESCAPING, componentType, true, version);
  }

  public static Map<String, String> encodePathKeysForUri(Map<String, Object> pathKeys, ProtocolVersion version)
  {
    final Map<String, String> escapedKeys = new HashMap<String, String>();

    for (Map.Entry<String, Object> entry : pathKeys.entrySet())
    {
      final String value = URIParamUtils.encodeKeyForUri(entry.getValue(), UriComponent.Type.PATH_SEGMENT, version);
      if (value == null)
      {
        throw new IllegalArgumentException("Missing value for path key " + entry.getKey());
      }
      escapedKeys.put(entry.getKey(), value);
    }

    return escapedKeys;
  }

  /**
   * Serialize the given key for use in an header. Params are not included.
   *
   *
   * @param key the key
   * @param version the {@link com.linkedin.restli.common.ProtocolVersion}
   * @return the serialized key
   */
  public static String encodeKeyForHeader(Object key, ProtocolVersion version)
  {
    return encodeKeyForBody(key, false, version);
  }

  /**
   * Serialize the given key for use in a body, such as in a batch response.
   *
   *
   * @param key the key
   * @param full encode the full key, including params
   * @param version the {@link com.linkedin.restli.common.ProtocolVersion}
   * @return the serialized key
   */
  public static String encodeKeyForBody(Object key, boolean full, ProtocolVersion version)
  {
    if (key instanceof ComplexResourceKey && !full && version.compareTo(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion()) <= 0)
    {
      /**
       * in v1, ComplexResourceKeys that are sent over the wire as a response are all URI encoded
       * and do not contain params. They are all URI encoded because v1 ComplexResourceKeys can only
       * be properly parsed if they are URI encoded.
       *
       * ComplexResourceKeys in request bodies are full, and are not URI encoded because the key
       * decoding is done from the URI itself.
       */
      return keyToString(key, URLEscaper.Escaping.URL_ESCAPING, null, full, version);
    }
    else
    {
      return keyToString(key, URLEscaper.Escaping.NO_ESCAPING, null, full, version);
    }
  }

  /**
   * Universal function for serializing Keys to Strings.
   *
   * @param key the key
   * @param escaping determines if the resulting string should be URI escaped or not.
   * @param componentType if this key is to be encoded for a URI, the URI component Type of the final result.
   *                      this can be null if you are not encoding for a URI.
   * @param full if false, ComplexResourceKey inputs will not have their parameters represented in
   *             the final string result. Except in the case of response bodies, it should normally
   *             be true.
   * @param version the protocol version.
   * @return a stringified version of the key, suitable for insertion into a URI or json body.
   *
   * @see #encodeKeyForUri(Object, com.linkedin.jersey.api.uri.UriComponent.Type, com.linkedin.restli.common.ProtocolVersion)
   * @see #encodeKeyForBody(Object, boolean, com.linkedin.restli.common.ProtocolVersion)
   */
  public static String keyToString(Object key,
                                   URLEscaper.Escaping escaping,
                                   UriComponent.Type componentType,
                                   boolean full,
                                   ProtocolVersion version)
  {
    if (version.compareTo(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion()) >= 0)
    {
      return keyToStringV2(key, escaping, componentType, full);
    }
    else
    {
      return keyToStringV1(key, escaping, full);
    }
  }

  private static String keyToStringV1(Object key, URLEscaper.Escaping escaping, boolean full)
  {
    String result;
    if (key == null)
    {
      result = null;
    }
    else if (key instanceof ComplexResourceKey)
    {
      ComplexResourceKey<?, ?> complexKey = (ComplexResourceKey<?, ?>) key;
      if (full)
      {
        result = QueryParamsDataMap.dataMapToQueryString(complexKey.toDataMap(), escaping);
      }
      else
      {
        result = QueryParamsDataMap.dataMapToQueryString(complexKey.getKey().data(), escaping);
      }

    }
    else if (key instanceof CompoundKey)
    {
      result = compoundKeyToStringV1((CompoundKey)key);
    }
    else
    {
      result = URLEscaper.escape(DataTemplateUtil.stringify(key), escaping);
    }
    return result;
  }

  private static String compoundKeyToStringV1(CompoundKey key)
  {
    List<String> keyList = new ArrayList<String>(key.getPartKeys());
    Collections.sort(keyList);

    StringBuilder b = new StringBuilder();
    boolean delimit=false;
    for (String keyPart : keyList)
    {
      if (delimit)
      {
        b.append(RestConstants.SIMPLE_KEY_DELIMITER);
      }
      try
      {
        b.append(URLEncoder.encode(keyPart, RestConstants.DEFAULT_CHARSET_NAME));
        b.append(RestConstants.KEY_VALUE_DELIMITER);
        b.append(URLEncoder.encode(DataTemplateUtil.stringify(key.getPart(keyPart)), RestConstants.DEFAULT_CHARSET_NAME));
      }
      catch (UnsupportedEncodingException e)
      {
        throw new RuntimeException("UnsupportedEncodingException while trying to encode the key", e);
      }
      delimit = true;
    }
    return b.toString();
  }

  private static String keyToStringV2(Object key,
                                      URLEscaper.Escaping escaping,
                                      UriComponent.Type componentType,
                                      boolean full)
  {
    if (key == null)
    {
      return null;
    }
    if (key instanceof ComplexResourceKey)
    {
      Object convertedKey;
      ComplexResourceKey<?, ?> complexResourceKey = (ComplexResourceKey<?, ?>) key;
      if (full)
      {
        convertedKey = complexResourceKey.toDataMap();
      }
      else
      {
        convertedKey = complexResourceKey.getKey().data();
      }
      return URIParamUtils.encodeElement(convertedKey, escaping, componentType);
    }
    else if (key instanceof CompoundKey)
    {
      return URIParamUtils.encodeElement(URIParamUtils.compoundKeyToDataMap((CompoundKey) key), escaping, componentType);
    }
    else
    {
      return simpleKeyToStringV2(key, escaping, componentType);
    }
  }

  private static String simpleKeyToStringV2(Object key,
                                            URLEscaper.Escaping escaping,
                                            UriComponent.Type componentType)
  {
    if (escaping == URLEscaper.Escaping.URL_ESCAPING)
    {
      return URIParamUtils.encodeElement(key, escaping, componentType);
    }
    else
    {
      return DataTemplateUtil.stringify(key);
    }
  }

  private static void encodeDataObject(Object obj, URLEscaper.Escaping escaping, UriComponent.Type componentType, StringBuilder stringBuilder)
  {
    if (obj instanceof DataComplex)
    {
      if (obj instanceof DataMap)
      {
        DataMap dataMap = (DataMap) obj;
        stringBuilder.append(URIConstants.OBJ_START);
        if (!dataMap.isEmpty())
        {
          List<String> keys = new ArrayList<String>(dataMap.keySet());
          Collections.sort(keys);
          ListIterator<String> iterator = keys.listIterator();

          String currentKey = iterator.next();
          mapEncodingHelper(currentKey, dataMap.get(currentKey), escaping, componentType, stringBuilder);
          while (iterator.hasNext())
          {
            stringBuilder.append(URIConstants.ITEM_SEP);
            currentKey = iterator.next();
            mapEncodingHelper(currentKey, dataMap.get(currentKey), escaping, componentType, stringBuilder);
          }
        }
        stringBuilder.append(URIConstants.OBJ_END);
      }
      else if (obj instanceof DataList)
      {
        DataList dataList = (DataList) obj;
        stringBuilder.append(URIConstants.LIST_PREFIX);
        stringBuilder.append(URIConstants.OBJ_START);
        if (!dataList.isEmpty())
        {
          ListIterator<Object> iterator = dataList.listIterator();
          encodeDataObject(iterator.next(), escaping, componentType, stringBuilder);
          while(iterator.hasNext())
          {
            stringBuilder.append(URIConstants.ITEM_SEP);
            encodeDataObject(iterator.next(), escaping, componentType, stringBuilder);
          }
        }
        stringBuilder.append(URIConstants.OBJ_END);
      }
      else
      {
        throw new IllegalArgumentException(obj.getClass() + " is an unknown subtype of dataComplex.");
      }
    }
    else
    {
      stringBuilder.append(encodeString(DataTemplateUtil.stringify(obj), escaping, componentType));
    }
  }

  /**
   * encodes an individual map element into the given StringBuilder
   */
  private static void mapEncodingHelper(String key,
                                        Object value,
                                        URLEscaper.Escaping escaping,
                                        UriComponent.Type componentType,
                                        StringBuilder stringBuilder)
  {
    stringBuilder.append(encodeString(key, escaping, componentType));
    stringBuilder.append(URIConstants.KEY_VALUE_SEP);
    encodeDataObject(value, escaping, componentType, stringBuilder);
  }

  private static final AsciiHexEncoding CODEC = new AsciiHexEncoding('%', URIConstants.RESERVED_CHARS);

  /**
   * encodes the given string
   * @param toEscape
   * @param escaping whether to encode the string for URLs or not.
   * @param componentType if encoding for URLs, where in the URL it will be located
   *                      if escaping is {@link URLEscaper.Escaping#NO_ESCAPING}, this can be null
   * @return the encoded String
   */
  private static String encodeString(String toEscape, URLEscaper.Escaping escaping, UriComponent.Type componentType)
  {
    if (toEscape.isEmpty())
    {
      return URIConstants.EMPTY_STRING_REP;
    }
    else if (escaping.equals(URLEscaper.Escaping.URL_ESCAPING))
    {
      // do internal encoding, which will encode %.
      String internalEncoding = CODEC.encode(toEscape);
      // do external encoding, as signified by componentType
      return UriComponent.contextualEncode(internalEncoding, componentType);
    }
    else
    {
      return CODEC.encode(toEscape);
    }
  }

  public static DataMap parseUriParams(Map<String, List<String>> queryParameters) throws PathSegment.PathSegmentSyntaxException
  {
    DataMap dataMap = new DataMap();
    for (Map.Entry<String, List<String>> entry : queryParameters.entrySet())
    {
      String key = entry.getKey();
      Object value;

      List<String> values = entry.getValue();
      if (values.size() > 1)
      {
        throw new PathSegment.PathSegmentSyntaxException("unexpected repeated query param in URI: " + key);
      }
      String encodedValue =  values.get(0);

      if (RestConstants.PROJECTION_PARAMETERS.contains(key))
      {
        // Don't decode it
        value = encodedValue;
      }
      else
      {
        try
        {
          value = URIElementParser.parse(encodedValue);
        }
        catch (PathSegment.PathSegmentSyntaxException e)
        {
          throw new PathSegment.PathSegmentSyntaxException("error while parsing query param '" + key + "'\n" + e.getMessage());
        }

      }
      dataMap.put(key, value);
    }
    return dataMap;
  }

  /**
   * Add the given parameters to the UriBuilder, in sorted order.
   *
   * @param uriBuilder the {@link UriBuilder}
   * @param params The {@link DataMap} representing the parameters
   * @param version The {@link ProtocolVersion}
   */
  public static void addSortedParams(UriBuilder uriBuilder, DataMap params, ProtocolVersion version)
  {
    if(version.compareTo(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion()) >= 0)
    {
      addSortedParams(uriBuilder, params);
    }
    else
    {
      QueryParamsDataMap.addSortedParams(uriBuilder, params);
    }
  }

  /**
   * replace the values of the given queryParam with new ones
   * @param uri initial URI
   * @param queryParam name of the queryParam
   * @param values values of the queryParam
   * @param parameters all parameters
   * @param version The {@link ProtocolVersion}
   */

  public static URI replaceQueryParam(URI uri, String queryParam, DataComplex values, DataMap parameters, ProtocolVersion version)
  {
    UriBuilder builder = UriBuilder.fromPath(uri.getPath());
    DataMap newQueryParams = new DataMap();
    newQueryParams.putAll(parameters);
    newQueryParams.put(queryParam, values);
    URIParamUtils.addSortedParams(builder, newQueryParams, version);

    return builder.build();
  }

  /**
   * Add the given parameters to the UriBuilder, in sorted order.
   *
   * @param uriBuilder the {@link UriBuilder}
   * @param params The {@link DataMap} representing the parameters
   */
  public static void addSortedParams(UriBuilder uriBuilder, DataMap params)
  {
    Map<String, String> map = dataMapToQueryParams(params);
    addSortedParams(uriBuilder, map);
  }

  // params must already be escaped.
  private static void addSortedParams(UriBuilder uriBuilder, Map<String, String> params)
  {
    List<String> keysList = new ArrayList<String>(params.keySet());
    Collections.sort(keysList);

    for (String key: keysList)
    {
      uriBuilder.queryParam(key, params.get(key));
    }
  }

  /**
   * Create a DataMap representation of this CompoundKey.  If any of its fields are CustomTypes,
   * they will be coerced down to their base type before being placed into the map.
   * It is distinct from {@link CompoundKey#toDataMap(java.util.Map)} because we may not know the
   * field types when we need to do this transition internally. As a result, it may be slightly slower.
   *
   * @return a {@link DataMap} representation of this {@link CompoundKey}
   * @see CompoundKey#toDataMap(java.util.Map)
   */
  public static DataMap compoundKeyToDataMap(CompoundKey compoundKey)
  {
    DataMap dataMap = new DataMap(compoundKey.getNumParts());
    for (String key : compoundKey.getPartKeys())
    {
      Object value = compoundKey.getPart(key);
      Class<?> valueClass = value.getClass();
      if (DataTemplateUtil.hasCoercer(valueClass) || valueClass.isEnum())
      {
        @SuppressWarnings("unchecked")
        Object coercedValue = DataTemplateUtil.coerceInput(value, (Class<Object>) valueClass, Object.class);
        dataMap.put(key, coercedValue);
      }
      else
      {
        dataMap.put(key, value);
      }
    }
    return dataMap;
  }

  public static String[] extractPathComponentsFromUriTemplate(String uriTemplate)
  {
    final String normalizedUriTemplate = NORMALIZED_URI_PATTERN.matcher(uriTemplate).replaceAll("");
    final UriTemplate template = new UriTemplate(normalizedUriTemplate);
    final String uri = template.createURI(_EMPTY_STRING_ARRAY);
    return URI_SEPARATOR_PATTERN.split(uri);
  }
}
