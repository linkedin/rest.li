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

package com.linkedin.restli.internal.server.util;

import com.linkedin.data.DataMap;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaUtil;
import com.linkedin.data.schema.validation.CoercionMode;
import com.linkedin.data.schema.validation.RequiredMode;
import com.linkedin.data.schema.validation.ValidateDataAgainstSchema;
import com.linkedin.data.schema.validation.ValidationOptions;
import com.linkedin.data.schema.validation.ValidationResult;
import com.linkedin.data.schema.validator.DataSchemaAnnotationValidator;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.InvalidAlternativeKeyException;
import com.linkedin.data.template.KeyCoercer;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.data.transform.filter.request.MaskTree;
import com.linkedin.jersey.api.uri.UriComponent;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.common.TypeSpec;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.common.IllegalMaskException;
import com.linkedin.restli.internal.common.PathSegment.PathSegmentSyntaxException;
import com.linkedin.restli.internal.common.URIElementParser;
import com.linkedin.restli.internal.common.URIMaskUtil;
import com.linkedin.restli.internal.common.URLEscaper;
import com.linkedin.restli.internal.common.ValueConverter;
import com.linkedin.restli.internal.server.RestLiInternalException;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.model.ResourceModel;
import com.linkedin.restli.server.AlternativeKey;
import com.linkedin.restli.server.Key;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.RoutingException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Josh Walker
 * @version $Revision: $
 */

public class ArgumentUtils
{
  private static final Logger _log = LoggerFactory.getLogger(ArgumentUtils.class);
  private static final Pattern SIMPLE_KEY_DELIMETER_PATTERN =
          Pattern.compile(Pattern.quote(String.valueOf(RestConstants.SIMPLE_KEY_DELIMITER)));
  private static final Pattern LEGACY_SIMPLE_KEY_DELIMETER_PATTERN = Pattern.compile(Pattern.quote(";"));
  private static final Pattern KEY_VALUE_DELIMETER_PATTERN =
          Pattern.compile(Pattern.quote(String.valueOf(RestConstants.KEY_VALUE_DELIMITER)));
  private static final Pattern LEGACY_KEY_VALUE_DELIMETER_PATTERN = Pattern.compile(Pattern.quote(":"));

  /**
   * @param routingResult {@link RoutingResult}
   * @return key value of the resource addressed by this method
   */
  public static Object getResourceKey(final RoutingResult routingResult)
  {
    String keyName = routingResult.getResourceMethod().getResourceModel().getKeyName();
    return routingResult.getContext().getPathKeys().get(keyName);
  }

  /**
   * @param routingResult {@link RoutingResult}
   * @return whether the resource addressed by this method has a key
   */
  public static boolean hasResourceKey(final RoutingResult routingResult)
  {
    return routingResult.getResourceMethod().getResourceModel().getPrimaryKey() != null;
  }

  /**
   * @param routingResult {@link RoutingResult}
   * @return value class of the resource addressed by this method
   */
  public static Class<? extends RecordTemplate> getValueClass(final RoutingResult routingResult)
  {
    return routingResult.getResourceMethod().getResourceModel().getValueClass();
  }

  /**
   * @param invocableMethod {@link RoutingResult}
   * @return key class of the resource addressed by this method
   */
  public static Class<?> getKeyClass(final RoutingResult invocableMethod)
  {
    return invocableMethod.getResourceMethod().getResourceModel().getKeyClass();
  }

  /**
   * @param uri {@link URI} object
   * @return map of lists of parameter values keyed on parameter names.
   */
  public static Map<String, List<String>> getQueryParameters(final URI uri)
  {
    return UriComponent.decodeQuery(uri, true);
  }

  /**
   * Parse {@link MaskTree} from a projection parameter string.
   *
   * @param projectionParam projection parameter string to parse
   * @return {@link MaskTree} based on the projection parameter
   * @throws RestLiSyntaxException if projection parameter value is invalid
   */
  public static MaskTree parseProjectionParameter(final String projectionParam) throws
          RestLiSyntaxException
  {
    if (projectionParam == null)
    {
      return new MaskTree();
    }
    else
    {
      return decodeMaskUriFormat(projectionParam);
    }
  }

  /**
   * Same as {@link #parseProjectionParameter(String)} but assumes the uri param is not
   * null.
   *
   * @param uriParam cannot be null
   * @return {@link MaskTree} based on the projection parameter
   * @throws RestLiSyntaxException if projection parameter value is invalid
   */
  public static MaskTree decodeMaskUriFormat(final String uriParam) throws RestLiSyntaxException
  {
    try
    {
      return URIMaskUtil.decodeMaskUriFormat(uriParam);
    }
    catch (IllegalMaskException e)
    {
      throw new RestLiSyntaxException("error parsing mask", e);
    }
  }

  public static CompoundKey parseCompoundKey(final String urlString,
                                             final Collection<Key> keys,
                                             final ProtocolVersion version) throws IllegalArgumentException,
                                                                                    PathSegmentSyntaxException
  {
    return parseCompoundKey(urlString, keys, version, false);
  }

  /**
   * The method parses out runtime-typesafe simple keys for the compound key based on the
   * provided key set for the resource.
   *
   *
   * @param urlString a string representation of the compound key.
   * @param keys a set of {@link com.linkedin.restli.server.Key} objects specifying
   *          names and types of the constituent simple keys
   * @param validateKey if set throws RoutingException on validation failure
   * @return a runtime-typesafe CompoundKey
   * @throws IllegalArgumentException if there are unexpected key parts in the urlString that are not in keys,
   *         or any error in {@link ProtocolVersion} 1.0
   * @throws PathSegmentSyntaxException if the given string is not a valid encoded compound key
   */
  public static CompoundKey parseCompoundKey(final String urlString,
                                             final Collection<Key> keys,
                                             final ProtocolVersion version,
                                             boolean validateKey) throws IllegalArgumentException,
                                                                                   PathSegmentSyntaxException
  {
    if (urlString == null || urlString.trim().isEmpty())
    {
      return null;
    }

    if (version.compareTo(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion()) >= 0)
    {
      return parseCompoundKeyV2(urlString, keys, validateKey);
    }
    else
    {
      //There are two compound key syntaxes potentially in use by clients, depending on the version
      //of rest.li being used.  The syntaxes use different delimiters: ";" and ":" for the legacy
      //syntax, and "&" and "=" for the newer syntax.  When parsing compound keys, we do not
      //know which syntax the client used, and we cannot rely on correct percent-encoding of
      //delimiter characters for both syntaxes.  Therefore we simulate parsing using each syntax in
      //turn, and choose the best match.
      StringBuilder legacyParseError = new StringBuilder();
      StringBuilder currentParseError = new StringBuilder();
      CompoundKey legacyParsedKey = parseCompoundKey(urlString,
                                                     keys,
                                                     legacyParseError,
                                                     LEGACY_SIMPLE_KEY_DELIMETER_PATTERN,
                                                     LEGACY_KEY_VALUE_DELIMETER_PATTERN,
                                                     validateKey);
      CompoundKey currentParsedKey = parseCompoundKey(urlString,
                                                      keys,
                                                      currentParseError,
                                                      SIMPLE_KEY_DELIMETER_PATTERN,
                                                      KEY_VALUE_DELIMETER_PATTERN,
                                                      validateKey);
      if (legacyParsedKey != null && currentParsedKey != null)
      {
        boolean legacy = legacyParsedKey.getNumParts() > currentParsedKey.getNumParts();
        _log.warn("Ambiguous compound key syntax, using heuristic decision for '{}', legacy: {}",
                  urlString, String.valueOf(legacy));
        return legacy ? legacyParsedKey : currentParsedKey;
      }
      else if (legacyParsedKey == null && currentParsedKey == null)
      {
        throw new IllegalArgumentException(currentParseError.toString());
      }
      else
      {
        return currentParsedKey == null ? legacyParsedKey : currentParsedKey;
      }
    }
  }

  /**
   *
   * @param urlString {@link String} representation of the v2 compound key
   * @param keys the {@link Key}s representing each part of the compound key.
   * @param validateKey if set throws RoutingException on validation failure
   * @return a {@link CompoundKey}
   * @throws IllegalArgumentException if there are unexpected key parts in the urlString that are not in keys.
   * @throws PathSegmentSyntaxException if the given string is not a valid encoded v2 compound key
   */
  private static CompoundKey parseCompoundKeyV2(final String urlString,
                                                final Collection<Key> keys,
                                                boolean validateKey) throws PathSegmentSyntaxException,
                                                                                   IllegalArgumentException
  {
    DataMap dataMap;
    // dataMap looks like keyName1: keyValue1, keyName2: keyValue2, ...
    Object parsedObject = URIElementParser.parse(urlString);
    if (parsedObject instanceof DataMap)
    {
      dataMap = (DataMap) parsedObject;
      return dataMapToCompoundKey(dataMap, keys, validateKey);
    }
    else
    {
      throw new PathSegmentSyntaxException(String.format("input '%s' is not a valid CompoundKey",
                                                         urlString));
    }
  }

  public static CompoundKey dataMapToCompoundKey(DataMap dataMap, Collection<Key> keys) throws IllegalArgumentException
  {
    return dataMapToCompoundKey(dataMap, keys, false);
  }

  public static CompoundKey dataMapToCompoundKey(DataMap dataMap, Collection<Key> keys, boolean validateKey) throws IllegalArgumentException
  {
    CompoundKey compoundKey = new CompoundKey();
    for (Key key : keys)
    {
      String name = key.getName();

      // may be a partial compound key
      String value = dataMap.getString(name);
      if (value != null)
      {
        dataMap.remove(name);
        compoundKey.append(name, convertSimpleValue(value, key.getDataSchema(), key.getType(), validateKey), keyToTypeInfo(key));
      }
    }
    if (!dataMap.isEmpty())
    {
      StringBuilder errorMessageBuilder = new StringBuilder();
      for (String leftOverKey: dataMap.keySet())
      {
        errorMessageBuilder.append("Unknown key part named '");
        errorMessageBuilder.append(leftOverKey);
        errorMessageBuilder.append("'");
      }
      throw new IllegalArgumentException(errorMessageBuilder.toString());
    }

    return compoundKey;
  }

  private static CompoundKey.TypeInfo keyToTypeInfo(Key key) {
    TypeSpec<?> typeSpec = new TypeSpec<>(key.getType(), key.getDataSchema());
    return new CompoundKey.TypeInfo(typeSpec, typeSpec);
  }

  public static CompoundKey parseCompoundKey(final String urlString,
      final Collection<Key> keys,
      final StringBuilder errorMessageBuilder,
      final Pattern simpleKeyDelimiterPattern,
      final Pattern keyValueDelimiterPattern)
      throws RoutingException
  {
    return parseCompoundKey(urlString, keys, errorMessageBuilder, simpleKeyDelimiterPattern, keyValueDelimiterPattern, false);
  }

  /**
   * Parse {@link CompoundKey} from its String representation.
   *
   * @param urlString {@link CompoundKey} string representation
   * @param keys {@link CompoundKey} constituent keys' classes keyed on their names
   * @param errorMessageBuilder {@link StringBuilder} to build error message if necessary
   * @param simpleKeyDelimiterPattern delimiter of constituent keys in the compound key
   * @param keyValueDelimiterPattern delimiter of key and value in a constituent key
   * @param validateKey if set throws RoutingException on validation failure
   * @return {@link CompoundKey} parsed from the input string
   */
  public static CompoundKey parseCompoundKey(final String urlString,
                                             final Collection<Key> keys,
                                             final StringBuilder errorMessageBuilder,
                                             final Pattern simpleKeyDelimiterPattern,
                                             final Pattern keyValueDelimiterPattern,
                                             boolean validateKey)
          throws RoutingException
  {
    String[] simpleKeys = simpleKeyDelimiterPattern.split(urlString.trim());
    CompoundKey compoundKey = new CompoundKey();
    for (String simpleKey : simpleKeys)
    {
      String[] nameValuePair = keyValueDelimiterPattern.split(simpleKey.trim());
      if (simpleKey.trim().length() == 0 || nameValuePair.length != 2)
      {
        errorMessageBuilder.append("Bad key format '");
        errorMessageBuilder.append(urlString);
        errorMessageBuilder.append("'");
        return null;
      }

      // Simple key names and values are URL-encoded prior to being included in the URL on
      // the client, to prevent collision with any of the delimiter characters (bulk,
      // compound key and simple key-value). So, must decode them
      String name;
      try
      {
        name = URLDecoder.decode(nameValuePair[0], RestConstants.DEFAULT_CHARSET_NAME);
      }
      catch (UnsupportedEncodingException e)
      {
        //should not happen, since we are using "UTF-8" as the encoding
        throw new RestLiInternalException(e);
      }
      // Key is not found in the set defined for the resource
      Key currentKey = getKeyWithName(keys, name);
      if (currentKey == null)
      {
        errorMessageBuilder.append("Unknown key part named '");
        errorMessageBuilder.append(name);
        errorMessageBuilder.append("'");
        return null;
      }

      String decodedStringValue;
      try
      {
        decodedStringValue =
                URLDecoder.decode(nameValuePair[1], RestConstants.DEFAULT_CHARSET_NAME);
      }
      catch (UnsupportedEncodingException e)
      {
        //should not happen, since we are using "UTF-8" as the encoding
        throw new RestLiInternalException(e);
      }

      compoundKey.append(name, convertSimpleValue(decodedStringValue, currentKey.getDataSchema(),
          currentKey.getType(), validateKey), keyToTypeInfo(currentKey));
    }
    return compoundKey;
  }

  private static Key getKeyWithName(Collection<Key> keys, String keyName)
  {
    for (Key key: keys)
    {
      if (key.getName().equals(keyName))
      {
        return key;
      }
    }
    return null;
  }

  public static Object parseSimplePathKey(final String value,
      final ResourceModel resource,
      final ProtocolVersion version) throws IllegalArgumentException
  {
    return parseSimplePathKey(value, resource, version, false);
  }

  /**
   * The method parses out and returns the correct simple type of the key out of the Object.
   * It does not handle {@link CompoundKey}s or {@link ComplexResourceKey}s.
   *
   * @param value key value string representation to parse
   * @param resource {@link com.linkedin.restli.internal.server.model.ResourceModel} containing the key type
   * @param version the {@link com.linkedin.restli.common.ProtocolVersion}
   * @param validateKey if set throws RoutingException on validation failure
   * @return parsed key value in the correct type for the key
   * @throws IllegalArgumentException
   * @throws NumberFormatException
   */
  public static Object parseSimplePathKey(final String value,
                                          final ResourceModel resource,
                                          final ProtocolVersion version,
                                          boolean validateKey) throws IllegalArgumentException

  {
    Key key = resource.getPrimaryKey();
    String decodedValue;
    if (version.compareTo(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion()) >= 0)
    {
      decodedValue = UriComponent.decode(value, UriComponent.Type.PATH_SEGMENT);
    }
    else
    {
      decodedValue = URLEscaper.unescape(value, URLEscaper.Escaping.URL_ESCAPING);
    }
    return convertSimpleValue(decodedValue, key.getDataSchema(), key.getType(), validateKey);
  }

  public static <K> Object parseAlternativeKey(final String value,
      final String altKeyName,
      final ResourceModel resource,
      final ProtocolVersion version) throws IllegalArgumentException
  {
    return parseAlternativeKey(value, altKeyName, resource, version, false);
  }

    /**
     * Parse a serialized alternative key into a deserialized alternative key.
     *
     * @param value The serialized alternative key.
     * @param altKeyName The name of the type of the alternative key.
     * @param resource The {@link com.linkedin.restli.internal.server.model.ResourceModel} of the resource.
     * @param version The {@link com.linkedin.restli.common.ProtocolVersion}.
     * @param validateKey if set throws RoutingException on validation failure
     * @return The deserialized alternative key.
     */
  public static <K> Object parseAlternativeKey(final String value,
                                               final String altKeyName,
                                               final ResourceModel resource,
                                               final ProtocolVersion version,
                                               boolean validateKey) throws IllegalArgumentException
  {
    if (!resource.getAlternativeKeys().containsKey(altKeyName))
    {
      throw new IllegalArgumentException("Resource '" + resource.getName() + "' does not contain alternative key named '" + altKeyName + "'.");
    }
    @SuppressWarnings("unchecked")
    AlternativeKey<?, K> alternativeKey = (AlternativeKey<?, K>)resource.getAlternativeKeys().get(altKeyName);
    String decodedValue;
    if (version.compareTo(AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion()) >= 0)
    {
      decodedValue = UriComponent.decode(value, UriComponent.Type.PATH_SEGMENT);
    }
    else
    {
      decodedValue = URLEscaper.unescape(value, URLEscaper.Escaping.URL_ESCAPING);
    }
    return convertSimpleValue(decodedValue, alternativeKey.getDataSchema(), alternativeKey.getType(), validateKey);
  }

  /**
   * Convert the alternative key to its primary key format.
   *
   * @param altKey The alternative key.
   * @param altKeyName The name of the type of the alternative key to translate to.
   * @param resource The {@link com.linkedin.restli.internal.server.model.ResourceModel} of the resource.
   * @return The primary key.
   * @throws InvalidAlternativeKeyException
   * @throws AlternativeKeyCoercerException
   */
  public static <T, K> K translateFromAlternativeKey(final T altKey,
                                                     final String altKeyName,
                                                     final ResourceModel resource) throws InvalidAlternativeKeyException,
          AlternativeKeyCoercerException
  {
    AlternativeKey<T, K> alternativeKey = getAltKeyOrError(altKeyName, resource);
    KeyCoercer<T, K> keyCoercer = alternativeKey.getKeyCoercer();

    try
    {
      return keyCoercer.coerceToKey(altKey);
    }
    catch (InvalidAlternativeKeyException invalidAlternativeKeyException)
    {
      // just rethrow
      throw invalidAlternativeKeyException;
    }
    catch (Exception e)
    {
      throw new AlternativeKeyCoercerException(e);
    }
  }

  /**
   * Translate a primary key to an alternative key
   *
   * @param key The primary key.
   * @param altKeyName The name of the alternative key to translate to.
   * @param resource The {@link com.linkedin.restli.internal.server.model.ResourceModel} of the resource.
   * @return The alternative key.
   * @throws AlternativeKeyCoercerException
   */
  public static <T, K> T translateToAlternativeKey(final K key,
                                                   final String altKeyName,
                                                   final ResourceModel resource) throws AlternativeKeyCoercerException
  {
    AlternativeKey<T, K> alternativeKey = getAltKeyOrError(altKeyName, resource);
    KeyCoercer<T, K> keyCoercer = alternativeKey.getKeyCoercer();

    try
    {
      return keyCoercer.coerceFromKey(key);
    }
    catch (Exception e)
    {
      throw new AlternativeKeyCoercerException(e);
    }

  }

  private static <T, K> AlternativeKey<T, K> getAltKeyOrError(final String altKeyName,
                                                              final ResourceModel resource) throws IllegalArgumentException
  {
    if (!resource.getAlternativeKeys().containsKey(altKeyName))
    {
      throw new IllegalArgumentException(String.format("Resource '%s' does not contain alternative key named '%s'.", resource.getName(), altKeyName));
    }
    else
    {
      @SuppressWarnings("unchecked")
      AlternativeKey<T, K> alternativeKey = (AlternativeKey<T, K>)resource.getAlternativeKeys().get(altKeyName);
      return alternativeKey;
    }
  }

  public static Object convertSimpleValue(final String value,
                                          final DataSchema schema,
                                          final Class<?> type)
  {
    return convertSimpleValue(value, schema, type, false);
  }

  /**
   *
   * @param value the stringified value
   * @param schema the schema of the type
   * @param type a non-complex type to convert to
   * @param validateKey if set throws RoutingException on validation failure
   * @return the converted value
   */
  public static Object convertSimpleValue(final String value,
                                          final DataSchema schema,
                                          final Class<?> type,
                                          boolean validateKey)
  {
    DataSchema.Type dereferencedType = schema.getDereferencedType();

    Object underlyingValue;
    if (schema.getDereferencedDataSchema().isComplex())
    {
      underlyingValue = value;
    }
    else
    {
      underlyingValue = ValueConverter.coerceString(value, DataSchemaUtil.dataSchemaTypeToPrimitiveDataSchemaClass(dereferencedType));
    }
    validateDataAgainstSchema(underlyingValue, schema, validateKey);
    return DataTemplateUtil.coerceOutput(underlyingValue, type);
  }

  /**
   * Cast input to string or throw RestLiInternalException if it is not a String.
   *
   * @param obj value to cast to string
   * @param paramName param name to return in the exception
   * @return input value cast to String
   *
   * @deprecated Deprecated with no recommended replacement. This method will be removed in the later major versions.
   */
  @Deprecated
  public static String argumentAsString(final Object obj, final String paramName)
  {
    if (obj != null && !(obj instanceof String))
    {
      throw new RestLiInternalException("Invalid value type for parameter " + paramName);
    }
    return (String) obj;
  }

  /**
   * Parse the "return entity" parameter of a request. This strictly expects a true or false value.
   *
   * @param value the "return entity" query parameter.
   * @return the parsed value of the "return entity" query parameter.
   */
  public static boolean parseReturnEntityParameter(final String value)
  {
    switch (value.toLowerCase()) {
      case "true":
        return true;
      case "false":
        return false;
      default:
        throw new RestLiServiceException(HttpStatus.S_400_BAD_REQUEST, String.format("Invalid \"%s\" parameter: %s", RestConstants.RETURN_ENTITY_PARAM, value));
    }
  }

  /**
   * Validates the value/dataMap against Schema, parses the value and converts string to primitive when required.
   * Throws Routing exception with HTTP status code 400 if there is a validation failure.
   *
   * @param value the entity to be validated.
   * @param schema DataSchema which defines validation rules for the value
   * @param enforceValidation if enabled throws 400 bad request RoutingException in case there is a validation failure
   */
  public static void validateDataAgainstSchema(Object value, DataSchema schema, boolean enforceValidation)
  {
    // Validate against the class schema with FixupMode.STRING_TO_PRIMITIVE to parse the
    // strings into the corresponding primitive types.
    ValidationResult result = ValidateDataAgainstSchema.validate(value, schema,
        new ValidationOptions(RequiredMode.CAN_BE_ABSENT_IF_HAS_DEFAULT, CoercionMode.STRING_TO_PRIMITIVE),
        schema != null ? new DataSchemaAnnotationValidator(schema) : null);
    if (enforceValidation && !result.isValid())
    {
      throw new RoutingException(String.format("Input field validation failure, reason: %s", result.getMessages()),
          HttpStatus.S_400_BAD_REQUEST.getCode());
    }
  }
}
