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
import com.linkedin.restli.internal.common.URLEscaper.Escaping;

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
    if (key == null || key.data() == null)
    {
      throw new IllegalArgumentException("Key part of the complex resource key is required");
    }
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
   * @return the params
   */
  public P getParams()
  {
    return params;
  }

  @Override
  /**
   * Only the key part is used here, as the params are not, strictly speaking, a part of the resource identifier
   */
  public String toString()
  {
    return QueryParamsDataMap.dataMapToQueryString(key.data(), Escaping.NO_ESCAPING);
  }

  /**
   * The entire contents of the key converted to String, for cases where it is desired,
   * such as when serializing the entire key, including the parameters in the request
   * builders.
   *
   * @return a String
   */
  public String toStringFull()
  {
    return toStringFull(Escaping.NO_ESCAPING);
  }

  public String toStringFull(Escaping escaping)
  {
    return QueryParamsDataMap.dataMapToQueryString(toDataMap(), escaping);
  }

  public DataMap toDataMap()
  {
    final DataMap m = new DataMap(key.data());
    if (params != null && params.data() != null)
    {
      m.put(COMPLEX_KEY_PARAMS, params.data());
    }
    return m;
  }

  protected final K           key;
  protected final P           params;

  private static final String COMPLEX_KEY_PARAMS = "$params";

  /**
   * Build complex key instance from an untyped datamap representing a complex key as
   * defined in {@link QueryParamsDataMap}
   *
   * @param dataMap untyped DataMap - all primitive values are represented as strings.
   * @param keyKeyClass Class of the key component of {@link ComplexResourceKey}
   * @param keyParamsClass Class of the params component of {@link ComplexResourceKey}
   * @return {@link ComplexResourceKey} initialized with id and param values specified in
   *         the input DataMap
   */
  public static ComplexResourceKey<RecordTemplate, RecordTemplate> buildFromDataMap(DataMap dataMap,
                                                                                    Class<? extends RecordTemplate> keyKeyClass,
                                                                                    Class<? extends RecordTemplate> keyParamsClass)
  {
    // Copy in case the original is immutable
    dataMap = new DataMap(dataMap);
    // Separate key from its parameters (those are under "params" key in the total map)
    DataMap paramsDataMap = (DataMap) dataMap.remove(COMPLEX_KEY_PARAMS);
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

  private static RecordTemplate validateDataMap(DataMap dataMap,
                                                Class<? extends RecordTemplate> clazz)
  {
    RecordTemplate recordTemplate = DataTemplateUtil.wrap(dataMap, clazz);
    // Validate against the class schema with FixupMode.STRING_TO_PRIMITIVE to parse the
    // strings into the
    // corresponding primitive types.
    ValidateDataAgainstSchema.validate(recordTemplate.data(),
                                       recordTemplate.schema(),
                                       new ValidationOptions(RequiredMode.CAN_BE_ABSENT_IF_HAS_DEFAULT,
                                                             CoercionMode.STRING_TO_PRIMITIVE));
    return recordTemplate;
  }
}
