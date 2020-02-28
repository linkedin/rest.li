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

import com.linkedin.data.DataMap;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.validation.CoercionMode;
import com.linkedin.data.schema.validation.RequiredMode;
import com.linkedin.data.schema.validation.ValidateDataAgainstSchema;
import com.linkedin.data.schema.validation.ValidationOptions;
import com.linkedin.data.schema.validation.ValidationResult;
import com.linkedin.data.schema.validator.DataSchemaAnnotationValidator;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.jersey.api.uri.UriComponent;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.common.PathSegment.PathSegmentSyntaxException;
import com.linkedin.restli.internal.common.QueryParamsDataMap;
import com.linkedin.restli.internal.common.URIElementParser;
import com.linkedin.restli.internal.common.URIParamUtils;
import com.linkedin.restli.internal.common.URLEscaper.Escaping;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * The class represents a resource key consisting of a RecordTemplate-derived
 * key part and a RecordTemplate-derived parameters part. Creating derived complex key
 * classes from this class is not supported by the Rest.li infrastructure.
 *
 * @author adubman
 *
 * @param <K>
 * @param <P>
 */

public final class ComplexResourceKey<K extends RecordTemplate, P extends RecordTemplate>
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

    if (params != null && params.data() == null)
    {
      throw new IllegalArgumentException("Params part of the complex resource key has a null internal data map");
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

  /**
   * Only the key part is used here, as the params are not, strictly speaking, a part of the resource identifier.
   *
   * This returns a v1 style serialized key. It should not be used structurally.
   * @see #toString(com.linkedin.restli.internal.common.URLEscaper.Escaping)
   * @deprecated the output of this function may change in the future, but it is still acceptable to use for
   *             logging purposes.
   *             If you need a stringified version of a key to extract information from a batch response,
   *             you should use {@link BatchResponse#keyToString(Object, ProtocolVersion)}.
   *             Internal developers can use {@link com.linkedin.restli.internal.common.URIParamUtils#keyToString(Object, com.linkedin.restli.internal.common.URLEscaper.Escaping, com.linkedin.jersey.api.uri.UriComponent.Type, boolean, ProtocolVersion)},
   *             {@link com.linkedin.restli.internal.common.URIParamUtils#encodeKeyForBody(Object, boolean, ProtocolVersion)}, or {@link com.linkedin.restli.internal.common.URIParamUtils#encodeKeyForUri(Object, com.linkedin.jersey.api.uri.UriComponent.Type, ProtocolVersion)}
   *             as needed.
   */
  @Override
  @SuppressWarnings("deprecation")
  @Deprecated
  public String toString()
  {
    return toString(Escaping.NO_ESCAPING);
  }

  /**
   * @param escaping what type of escaping should be done.
   * @return a v1 style serialized key.
   * @deprecated If you need a stringified version of a key to extract information from a batch response,
   *             you should use {@link BatchResponse#keyToString(Object, ProtocolVersion)}.
   *             Internal developers can use {@link com.linkedin.restli.internal.common.URIParamUtils#keyToString(Object, com.linkedin.restli.internal.common.URLEscaper.Escaping, com.linkedin.jersey.api.uri.UriComponent.Type, boolean, ProtocolVersion)},
   *             {@link com.linkedin.restli.internal.common.URIParamUtils#encodeKeyForBody(Object, boolean, ProtocolVersion)}, or {@link com.linkedin.restli.internal.common.URIParamUtils#encodeKeyForUri(Object, com.linkedin.jersey.api.uri.UriComponent.Type, ProtocolVersion)}
   *             as needed.
   */
  @Deprecated
  public String toString(Escaping escaping)
  {
    return QueryParamsDataMap.dataMapToQueryString(key.data(), escaping);
  }

  /**
   * The entire contents of the key converted to String, for cases where it is desired,
   * such as when serializing the entire key, including the parameters in the request
   * builders.
   *
   * @return a String
   * @deprecated This can only return a v1 style serialized key. Developers should use one of
   *             {@link URIParamUtils#encodeKeyForBody(Object, boolean, ProtocolVersion)}, {@link URIParamUtils#encodeKeyForUri(Object, com.linkedin.jersey.api.uri.UriComponent.Type, ProtocolVersion)}
   *             {@link URIParamUtils#keyToString(Object, com.linkedin.restli.internal.common.URLEscaper.Escaping, com.linkedin.jersey.api.uri.UriComponent.Type, boolean, ProtocolVersion)}
   *             instead.
   */
  @Deprecated
  @SuppressWarnings("deprecation")
  public String toStringFull()
  {
    return toStringFull(Escaping.NO_ESCAPING);
  }

  /**
   * @param escaping what type of escaping should be done.
   * @return this ComplexResourceKey, serialized as a string, including $params.
   * @deprecated This can only return a v1 style serialized key. Developers should use one of
   *             {@link URIParamUtils#encodeKeyForBody(Object, boolean, ProtocolVersion)}, {@link URIParamUtils#encodeKeyForUri(Object, com.linkedin.jersey.api.uri.UriComponent.Type, ProtocolVersion)}
   *             {@link URIParamUtils#keyToString(Object, com.linkedin.restli.internal.common.URLEscaper.Escaping, com.linkedin.jersey.api.uri.UriComponent.Type, boolean, ProtocolVersion)}
   *             instead.
   */
  @Deprecated
  public String toStringFull(Escaping escaping)
  {
    return QueryParamsDataMap.dataMapToQueryString(toDataMap(), escaping);
  }

  public DataMap toDataMap()
  {
    final DataMap m = new DataMap(key.data());
    if (params != null)
    {
      m.put(COMPLEX_KEY_PARAMS, params.data());
    }
    return m;
  }

  /**
   * Returns whether this key is read only by checking the underlying {@link DataMap}s in the key and params.
   */
  public boolean isReadOnly()
  {
    boolean result = true;

    result = key.data().isReadOnly();

    if (params != null)
    {
      result &= params.data().isReadOnly();
    }

    return result;
  }

  /**
   * Makes this key read only by making the underlying {@link DataMap}s in the key and params read only.
   */
  public void makeReadOnly()
  {
    key.data().makeReadOnly();

    if (params != null)
    {
      params.data().makeReadOnly();
    }
  }

  protected final K           key;
  protected final P           params;

  private static final String COMPLEX_KEY_PARAMS = "$params";

  /**
   * Build complex key instance from an untyped datamap representing a complex key as
   * defined in {@link QueryParamsDataMap}
   *
   * @param keyDataMap untyped DataMap - all primitive values are represented as strings.
   * @param keyKeyClass Class of the key component of {@link ComplexResourceKey}
   * @param keyParamsClass Class of the params component of {@link ComplexResourceKey}
   * @return {@link ComplexResourceKey} initialized with id and param values specified in
   *         the input DataMap
   */
  public static ComplexResourceKey<RecordTemplate, RecordTemplate> buildFromDataMap(DataMap keyDataMap, Class<? extends RecordTemplate> keyKeyClass,
      Class<? extends RecordTemplate> keyParamsClass)
  {
    return buildFromDataMap(keyDataMap, ComplexKeySpec.forClassesMaybeNull(keyKeyClass, keyParamsClass), false);
  }

  /**
   * Build complex key instance from an untyped datamap representing a complex key as
   * defined in {@link QueryParamsDataMap}
   *
   * @param keyDataMap untyped DataMap - all primitive values are represented as strings.
   * @param complexKeyType type of {@link ComplexResourceKey}
   * @return {@link ComplexResourceKey} initialized with id and param values specified in
   *         the input DataMap
   */
  public static ComplexResourceKey<RecordTemplate, RecordTemplate> buildFromDataMap(DataMap keyDataMap,
                                                                                    ComplexKeySpec<?, ?> complexKeyType)
  {
    return buildFromDataMap(keyDataMap, complexKeyType, false);
  }

  /**
   * Build complex key instance from an untyped datamap representing a complex key as
   * defined in {@link QueryParamsDataMap}
   *
   * @param keyDataMap untyped DataMap - all primitive values are represented as strings.
   * @param complexKeyType type of {@link ComplexResourceKey}
   * @param enforceValidation if set true throws IllegalArgumentException on validation failure
   * @return {@link ComplexResourceKey} initialized with id and param values specified in
   *         the input DataMap
   */
  public static ComplexResourceKey<RecordTemplate, RecordTemplate> buildFromDataMap(DataMap keyDataMap,
                                                                                    ComplexKeySpec<?, ?> complexKeyType,
                                                                                    boolean enforceValidation)
  {
    // Copy in case the original is immutable
    keyDataMap = new DataMap(keyDataMap);

    // Separate key from its parameters (those are under "params" key in the total map)
    DataMap paramsDataMap = (DataMap) keyDataMap.remove(COMPLEX_KEY_PARAMS);
    if (paramsDataMap == null)
    {
      paramsDataMap = new DataMap();
    }
    RecordTemplate key = validateDataMap(keyDataMap, complexKeyType.getKeyType(), enforceValidation);
    RecordTemplate params = validateDataMap(paramsDataMap, complexKeyType.getParamsType(), enforceValidation);

    return new ComplexResourceKey<RecordTemplate, RecordTemplate>(key, params);
  }

  /**
   * @deprecated use {@link ComplexResourceKey#parseString(String, Class, Class, ProtocolVersion)} instead.
   */
  @Deprecated
  @SuppressWarnings("deprecation")
  public static ComplexResourceKey<RecordTemplate, RecordTemplate> parseFromPathSegment(String currentPathSegment,
                                                                                        Class<? extends RecordTemplate> keyKeyClass,
                                                                                        Class<? extends RecordTemplate> keyParamsClass) throws PathSegmentSyntaxException
  {
    return parseFromPathSegment(currentPathSegment, ComplexKeySpec.forClassesMaybeNull(keyKeyClass, keyParamsClass));
  }

  /**
   * @deprecated use {@link ComplexResourceKey#parseString(String, ComplexKeySpec, ProtocolVersion)} instead.
   */
  @Deprecated
  public static ComplexResourceKey<RecordTemplate, RecordTemplate> parseFromPathSegment(String currentPathSegment,
                                                                                        ComplexKeySpec<?, ?> complexKeyType) throws PathSegmentSyntaxException
  {
    Map<String, List<String>> queryParameters =
        UriComponent.decodeQuery(URI.create("?" + currentPathSegment), true);
    DataMap allParametersDataMap = QueryParamsDataMap.parseDataMapKeys(queryParameters);
    return buildFromDataMap(allParametersDataMap, complexKeyType, false);
  }

  /**
   * Parse the given {@link String} into a {@link ComplexResourceKey}.
   *
   * @param str the {@link String} to parse
   * @param keyKeyClass the {@link RecordTemplate} derived {@link Class} of the key part of the key
   * @param keyParamsClass the {@link RecordTemplate} derived {@link Class} of the param part of the key
   * @param version the {@link ProtocolVersion}
   * @return a {@link ComplexResourceKey}
   * @throws PathSegmentSyntaxException
   */
  public static ComplexResourceKey<RecordTemplate, RecordTemplate> parseString(String str,
                                                                               Class<? extends RecordTemplate> keyKeyClass,
                                                                               Class<? extends RecordTemplate> keyParamsClass,
                                                                               ProtocolVersion version) throws PathSegmentSyntaxException
  {
    return parseString(str, ComplexKeySpec.forClassesMaybeNull(keyKeyClass, keyParamsClass), version, false);
  }

  /**
   * Parse the given {@link String} into a {@link ComplexResourceKey}.
   *
   * @param str the {@link String} to parse
   * @param complexKeyType the {@link ComplexKeySpec} of the {@link ComplexResourceKey}
   * @param version the {@link ProtocolVersion}
   * @return a {@link ComplexResourceKey}
   * @throws PathSegmentSyntaxException
   */
  @SuppressWarnings("deprecation")
  public static ComplexResourceKey<RecordTemplate, RecordTemplate> parseString(String str,
                                                                               ComplexKeySpec<?, ?> complexKeyType,
                                                                               ProtocolVersion version)
    throws PathSegmentSyntaxException
  {
    return parseString(str, complexKeyType, version, false);
  }

  /**
   * Parse the given {@link String} into a {@link ComplexResourceKey}.
   *
   * @param str the {@link String} to parse
   * @param complexKeyType the {@link ComplexKeySpec} of the {@link ComplexResourceKey}
   * @param version the {@link ProtocolVersion}
   * @param throwErrorOnValidationFailure if set true throws IllegalArgumentException on validation failure for restli V2
   * @return a {@link ComplexResourceKey}
   * @throws PathSegmentSyntaxException
   */
  public static ComplexResourceKey<RecordTemplate, RecordTemplate> parseString(String str,
      ComplexKeySpec<?, ?> complexKeyType,
      ProtocolVersion version,
      boolean throwErrorOnValidationFailure)
      throws PathSegmentSyntaxException
  {
    if (version.compareTo(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion()) >= 0)
    {
      DataMap dataMap = (DataMap) URIElementParser.parse(str);
      return buildFromDataMap(dataMap, complexKeyType, throwErrorOnValidationFailure);
    }
    else
    {
      // v1 Complex Keys are always URI encoded, so we don't need to worry about errors from .decodeQuery
      return parseFromPathSegment(str, complexKeyType);
    }
  }

  private static RecordTemplate validateDataMap(DataMap dataMap,
                                                TypeSpec<? extends RecordTemplate> spec,
                                                boolean enforceValidation)
  {
    RecordTemplate recordTemplate = wrapWithSchema(dataMap, spec);
    // Validate against the class schema with FixupMode.STRING_TO_PRIMITIVE to parse the
    // strings into the
    // corresponding primitive types.
    DataSchemaAnnotationValidator validator = new DataSchemaAnnotationValidator(recordTemplate.schema());
    ValidationResult validationResult =
        ValidateDataAgainstSchema.validate(recordTemplate.data(), recordTemplate.schema(),
            new ValidationOptions(RequiredMode.CAN_BE_ABSENT_IF_HAS_DEFAULT, CoercionMode.STRING_TO_PRIMITIVE),
            validator);
    if (enforceValidation && !validationResult.isValid())
    {
      throw new IllegalArgumentException(
          String.format("Complex Key with value '%s' is invalid, reason: %s", recordTemplate.data(),
              validationResult.getMessages()));
    }
    return recordTemplate;
  }

  private static RecordTemplate wrapWithSchema(DataMap dataMap, TypeSpec<? extends RecordTemplate> spec)
  {
    Class<? extends RecordTemplate> clazz = spec.getType();
    return DataTemplateUtil.wrap(dataMap, clazz);
  }

  /** @see java.lang.Object#hashCode() */
  @Override
  public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    // Key cannot be null
    result = prime * result + key.hashCode();
    result = prime * result + ((params == null) ? 0 : params.hashCode());
    return result;
  }

  /** @see java.lang.Object#equals(java.lang.Object) */
  @Override
  public boolean equals(Object obj)
  {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    ComplexResourceKey<?, ?> other = (ComplexResourceKey<?, ?>) obj;
    // Key cannot be null
    return key.equals(other.key)
        && (params == null ? other.params == null : (params.equals(other.params)));
  }

  @SuppressWarnings("unchecked")
  public ComplexResourceKey<K, P> copy() throws CloneNotSupportedException
  {
    K copyKey = (K) key.copy();
    P copyParams = null;

    if (params != null)
    {
      copyParams = (P) params.copy();
    }

    return new ComplexResourceKey<K, P>(copyKey, copyParams);
  }
}
