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

package com.linkedin.restli.common;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.linkedin.data.DataMap;
import com.linkedin.data.schema.validation.CoercionMode;
import com.linkedin.data.schema.validation.RequiredMode;
import com.linkedin.data.schema.validation.ValidateDataAgainstSchema;
import com.linkedin.data.schema.validation.ValidationOptions;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.jersey.api.uri.UriComponent;
import com.linkedin.restli.internal.common.PathSegment.PathSegmentSyntaxException;
import com.linkedin.restli.internal.common.QueryParamsDataMap;

/**
 * The class represents a resource key consisting of a RecordTemplate-derived
 * key part and a RecordTemplate-derived parameters part
 * 
 * @author adubman
 *
 * @param <K>
 * @param <P>
 */

public class ComplexResourceKey<K extends RecordTemplate, P extends RecordTemplate>
{
  /**
   * Initialize a ComplexResourceKey with the given key and parameters.
   *
   * @param key the key component of theComplexResourceKey
   * @param params the parameter component of the ComplexResourceKey
   */
  public ComplexResourceKey(K key, P params)
  {
    super();
    this.key = key;
    this.params = params;
  }
  
  /**
   * @return the key
   */
  public K getKey()
  {
    return key;
  }
  /**
   * @param key the key to set
   */
  public void setKey(K key)
  {
    this.key = key;
  }
  /**
   * @return the params
   */
  public P getParams()
  {
    return params;
  }
  /**
   * @param params the params to set
   */
  public void setParams(P params)
  {
    this.params = params;
  }
  
  @Override
  /**
   * Only the key part is used here, as the params are not, strictly speaking, a part of the resource identifier
   */
  public String toString()
  {
    if (key == null || key.data() == null)
    {
      return "";
    }
    
    return dataMapToString(key.data());
  }
  
  /**
   * The entire contents of the key converted to String, for cases where it is desired, such as
   * when serializing the entire key, including the parameters in the request builders.
   *
   * @return a String
   */
  public String toStringFull()
  {
    // Params aren't meaningful without the key, so return empty string if the key is null or empty
    if (key == null || key.data() == null)
    {
      return "";
    }
    DataMap keyDataMap = key.data();
    if (params != null && params.data() != null)
    {
      keyDataMap.put(RestConstants.COMPLEX_KEY_PARAMS, params.data());
    }
    return dataMapToString(keyDataMap);
  }
  
  /**
   * Helper method to convert a DataMap into a string by concatenating key-value pairs with "&"
   *
   * @return a String
   */
  private static String dataMapToString(DataMap dataMap)
  {
    Map<String, String> queryStringParamsMap = QueryParamsDataMap.queryString(dataMap);
    StringBuilder sb = new StringBuilder();
    for (Entry<String, String> entry : queryStringParamsMap.entrySet())
    {
      sb.append(entry.getKey()).append('=').append(entry.getValue()).append('&');
    }
    return sb.deleteCharAt(sb.length()-1).toString();
  }
  
  protected K key;
  protected P params;
  
  /**
   * Build complex key instance from an untyped datamap representing a complex key as
   * defined in {@link QueryParamsDataMap}
   * 
   * @param dataMap
   *          untyped DataMap - all primitive values are represented as strings.
   * @param keyKeyClass
   *          Class of the key component of {@link ComplexResourceKey}
   * @param keyParamsClass
   *          Class of the params component of {@link ComplexResourceKey}
   * @return {@link ComplexResourceKey} initialized with id and param values specified in
   *         the input DataMap
   */
  public static ComplexResourceKey<RecordTemplate, RecordTemplate> buildFromDataMap(DataMap dataMap,
                                                                                   Class<? extends RecordTemplate> keyKeyClass,
                                                                                   Class<? extends RecordTemplate> keyParamsClass)
  {
    // Separate key from its parameters (those are under "params" key in the total map)
    DataMap paramsDataMap = (DataMap) dataMap.remove(RestConstants.COMPLEX_KEY_PARAMS);
    RecordTemplate key = validateDataMap(dataMap, keyKeyClass);
    RecordTemplate params = validateDataMap(paramsDataMap, keyParamsClass);

    return new ComplexResourceKey<RecordTemplate, RecordTemplate>(key, params);
  }
  
  public static ComplexResourceKey<RecordTemplate, RecordTemplate> parseFromPathSegment(String currentPathSegment,
                                                                                                  Class<? extends RecordTemplate> keyKeyClass,
                                                                                                  Class<? extends RecordTemplate> keyParamsClass) throws PathSegmentSyntaxException
  {
    Map<String, List<String>> queryParameters =
        UriComponent.decodeQuery(URI.create("?" + currentPathSegment), true);
    DataMap allParametersDataMap = QueryParamsDataMap.parseDataMapKeys(queryParameters);
    return buildFromDataMap(allParametersDataMap, keyKeyClass, keyParamsClass);
  }
  
  private static RecordTemplate validateDataMap(DataMap dataMap, Class<? extends RecordTemplate> clazz)
  {
    RecordTemplate recordTemplate = DataTemplateUtil.wrap(dataMap, clazz);
    // Validate against the class schema with FixupMode.STRING_TO_PRIMITIVE to parse the strings into the
    // corresponding primitive types.
    ValidateDataAgainstSchema.validate(recordTemplate.data(),
                                       recordTemplate.schema(),
                                       new ValidationOptions(RequiredMode.CAN_BE_ABSENT_IF_HAS_DEFAULT,
                                                             CoercionMode.STRING_TO_PRIMITIVE));
    return recordTemplate;
  }
}
