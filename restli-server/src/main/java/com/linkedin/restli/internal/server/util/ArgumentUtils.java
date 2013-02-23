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

package com.linkedin.restli.internal.server.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaUtil;
import com.linkedin.restli.server.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.linkedin.data.DataMap;
import com.linkedin.data.schema.TyperefDataSchema;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.data.transform.filter.request.MaskTree;
import com.linkedin.jersey.api.uri.UriComponent;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.restli.common.BatchRequest;
import com.linkedin.restli.common.ComplexResourceKey;
import com.linkedin.restli.common.CompoundKey;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.common.IllegalMaskException;
import com.linkedin.restli.internal.common.PathSegment.PathSegmentSyntaxException;
import com.linkedin.restli.internal.common.URIMaskUtil;
import com.linkedin.restli.internal.common.ValueConverter;
import com.linkedin.restli.internal.server.RestLiInternalException;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.model.ResourceModel;
import com.linkedin.restli.server.RoutingException;

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
    return routingResult.getContext().getPathKeys().get(
            routingResult.getResourceMethod().getResourceModel().getKeyName());
  }

  /**
   * @param request {@link RestRequest}
   * @param recordClass resource value class
   * @param <V> resource value type which is a subclass of {@link RecordTemplate}
   * @return resource value
   */
  public static <V extends RecordTemplate> V extractEntity(final RestRequest request,
                                                           final Class<V> recordClass)
  {
    try
    {
      return DataMapUtils.read(request, recordClass);
    }
    catch (IOException e)
    {
      throw new RoutingException("Error parsing entity body: " + e.getMessage(),
                                 HttpStatus.S_400_BAD_REQUEST.getCode());
    }
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
   * Convert a DataMap representation of a BatchRequest (string->record) into a Java Map
   * appropriate for passing into application code.  Note that compound/complex keys are
   * represented as their string encoding in the DataMap.  Since we have already parsed
   * these keys, we simply try to match the string representations, rather than re-parsing.
   *
   *
   * @param data - the input DataMap to be converted
   * @param valueClass - the RecordTemplate type of the values
   * @param ids - the parsed batch ids from the request URI
   * @return a map using appropriate key and value classes
   */
  @SuppressWarnings({ "unchecked" })
  public static Map buildBatchRequestMap(final DataMap data,
                                         final Class<? extends RecordTemplate> valueClass,
                                         final Set<?> ids)
  {
    BatchRequest<RecordTemplate> batchRequest = new BatchRequest(data, valueClass);

    Map<String, Object> parsedKeyMap = new HashMap<String, Object>();
    for (Object o : ids)
    {
      parsedKeyMap.put(o instanceof ComplexResourceKey
                           ? ((ComplexResourceKey<?, ?>) o).toStringFull() : o.toString(),
                       o);
    }

    Map<Object, RecordTemplate> result =
        new HashMap<Object, RecordTemplate>(batchRequest.getEntities().size());
    for (Map.Entry<String, RecordTemplate> entry : batchRequest.getEntities().entrySet())
    {
      Object key = parsedKeyMap.get(entry.getKey());
      if (key == null)
      {
        throw new RoutingException(
                String.format("Batch request mismatch, URI keys: '%s'  Entity keys: '%s'",
                              ids.toString(),
                              result.keySet().toString()),
                HttpStatus.S_400_BAD_REQUEST.getCode());
      }
      RecordTemplate value = DataTemplateUtil.wrap(entry.getValue().data(), valueClass);
      result.put(key, value);
    }
    if (!ids.equals(result.keySet()))
    {
      throw new RoutingException(
              String.format("Batch request mismatch, URI keys: '%s'  Entity keys: '%s'",
                            ids.toString(),
                            result.keySet().toString()),
              HttpStatus.S_400_BAD_REQUEST.getCode());
    }
    return result;
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
      return URIMaskUtil.decodeMaskUriFormat(new StringBuilder(uriParam));
    }
    catch (IllegalMaskException e)
    {
      throw new RestLiSyntaxException("error parsing mask", e);
    }
  }

  /**
   * The method parses out runtime-typesafe simple keys for the compound key based on the
   * provided key set for the resource.
   *
   * @param urlString a string representation of the compound key in the form:
   *          name1:value1;name2:value2...
   * @param partialKeys a set of {@link com.linkedin.restli.server.Key} objects specifying
   *          names and types of the constituent simple keys
   * @return a runtime-typesafe CompoundKey
   *
   * @deprecated Should take a Collection of fully formed {@link Key}s.
   */
  @Deprecated
  public static CompoundKey parseCompoundKey(final String urlString,
                                             final Map<String, Class<?>> partialKeys)
  {
    Collection<Key> keys = new ArrayList<Key>(partialKeys.size());
    for(Map.Entry<String, Class<?>> entry : partialKeys.entrySet())
    {
      keys.add(new Key(entry.getKey(), entry.getValue()));
    }
    return parseCompoundKey(urlString, keys);
  }

  /**
   * The method parses out runtime-typesafe simple keys for the compound key based on the
   * provided key set for the resource.
   *
   * @param urlString a string representation of the compound key in the form:
   *          name1:value1;name2:value2...
   * @param keys a set of {@link com.linkedin.restli.server.Key} objects specifying
   *          names and types of the constituent simple keys
   * @return a runtime-typesafe CompoundKey
   */
  public static CompoundKey parseCompoundKey(final String urlString,
                                             final Collection<Key> keys)
  {
    if (urlString == null || urlString.trim().isEmpty())
    {
      return null;
    }

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
                                                   LEGACY_KEY_VALUE_DELIMETER_PATTERN);
    CompoundKey currentParsedKey = parseCompoundKey(urlString,
                                                    keys,
                                                    currentParseError,
                                                    SIMPLE_KEY_DELIMETER_PATTERN,
                                                    KEY_VALUE_DELIMETER_PATTERN);
    if (legacyParsedKey != null && currentParsedKey != null)
    {
      boolean legacy = legacyParsedKey.getNumParts() > currentParsedKey.getNumParts();
      _log.warn("Ambiguous compound key syntax, using heuristic decision for '{}', legacy: {}",
                urlString, String.valueOf(legacy));
      return legacy ? legacyParsedKey : currentParsedKey;
    }
    else if (legacyParsedKey == null && currentParsedKey == null)
    {
      throw new RoutingException(currentParseError.toString(),
                                 HttpStatus.S_400_BAD_REQUEST.getCode());
    }
    else
    {
      return currentParsedKey == null ? legacyParsedKey : currentParsedKey;
    }
  }

  /**
   * Parse {@link CompoundKey} from its String representation.
   *
   * @param urlString {@link CompoundKey} string representation
   * @param partialKeys {@link CompoundKey} constituent keys' classes keyed on their names
   * @param errorMessageBuilder {@link StringBuilder} to build error message if necessary
   * @param simpleKeyDelimiterPattern delimiter of constituent keys in the compound key
   * @param keyValueDelimiterPattern delimiter of key and value in a constituent key
   * @return {@link CompoundKey} parsed from the input string
   *
   * @deprecated Should take a Collection of fully formed {@link Key}s.
   */
  @Deprecated
  public static CompoundKey parseCompoundKey(final String urlString,
                                             final Map<String, Class<?>> partialKeys,
                                             final StringBuilder errorMessageBuilder,
                                             final Pattern simpleKeyDelimiterPattern,
                                             final Pattern keyValueDelimiterPattern)
          throws RoutingException
  {
    Collection<Key> keys = new ArrayList<Key>(partialKeys.size());
    for(Map.Entry<String, Class<?>> entry : partialKeys.entrySet())
    {
      keys.add(new Key(entry.getKey(), entry.getValue()));
    }
    return parseCompoundKey(urlString, keys, errorMessageBuilder, simpleKeyDelimiterPattern, keyValueDelimiterPattern);
  }

  /**
   * Parse {@link CompoundKey} from its String representation.
   *
   * @param urlString {@link CompoundKey} string representation
   * @param keys {@link CompoundKey} constituent keys' classes keyed on their names
   * @param errorMessageBuilder {@link StringBuilder} to build error message if necessary
   * @param simpleKeyDelimiterPattern delimiter of constituent keys in the compound key
   * @param keyValueDelimiterPattern delimiter of key and value in a constituent key
   * @return {@link CompoundKey} parsed from the input string
   */
  public static CompoundKey parseCompoundKey(final String urlString,
                                             final Collection<Key> keys,
                                             final StringBuilder errorMessageBuilder,
                                             final Pattern simpleKeyDelimiterPattern,
                                             final Pattern keyValueDelimiterPattern)
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

      compoundKey.append(name, convertSimpleValue(decodedStringValue, currentKey.getDataSchema(), currentKey.getType(), false));
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

  /**
   * The method parses out and returns the correct type of the key out of the Object. It
   * can possibly call parseCompoundKey which, in turn, can call this method. While this
   * may appear recursive, in practice the recursion does not happen, since nested
   * compound keys are not supported. It is functionally equivalent to
   * {@link #parseOptionalKey(String, ResourceModel)} with the exception that it
   * translates its various exceptions into {@link RoutingException}
   *
   * @param value key value string representation to parse
   * @param resource {@link ResourceModel} containing the key type
   * @return parsed key value in the correct type for the key
   */
  public static Object parseKeyIntoCorrectType(final String value,
                                               final ResourceModel resource)
  {
    try
    {
      return parseOptionalKey(value, resource);
    }
    catch (NumberFormatException e)
    {
      // thrown from Integer.valueOf or Long.valueOf
      throw new RoutingException(String.format("Key value '%s' must be of type '%s'",
                                               value,
                                               resource.getKeyClass().getName()),
                                 HttpStatus.S_400_BAD_REQUEST.getCode());
    }
    catch (IllegalArgumentException e)
    {
      // thrown from Enum.valueOf
      throw new RoutingException(String.format("Key parameter value '%s' is invalid",
                                               value),
                                 HttpStatus.S_400_BAD_REQUEST.getCode());
    }
    catch (PathSegmentSyntaxException e)
    {
      throw new RoutingException(String.format("Key parameter value '%s' is invalid",
                                               value),
                                 HttpStatus.S_400_BAD_REQUEST.getCode());
    }
  }

  /**
   * Similar to {@link #parseKeyIntoCorrectType(String, ResourceModel)} but throws
   * different exceptions.
   *
   * @param value key value string representation to parse
   * @param resource {@link ResourceModel} containing the key type
   * @return parsed key value in the correct type for the key
   * @throws PathSegmentSyntaxException if cannot parse {@link ComplexResourceKey}
   */
  public static Object parseOptionalKey(final String value, final ResourceModel resource) throws PathSegmentSyntaxException
  {
    if (CompoundKey.class.isAssignableFrom(resource.getKeyClass()))
    {
      return parseCompoundKey(value, resource.getKeys());
    }
    else if (ComplexResourceKey.class.isAssignableFrom(resource.getKeyClass()))
    {
      return ComplexResourceKey.parseFromPathSegment(value,
                                                     resource.getKeyKeyClass(),
                                                     resource.getKeyParamsClass());
    }
    else
    {
      Key key = resource.getPrimaryKey();
      return convertSimpleValue(value, key.getDataSchema(), key.getType(), true);
    }
  }

  /**
   * Parse the parameter value string representation according to the parameter type and
   * return in that type.
   *
   * @param value string to parse
   * @param keyClass key class
   * @return parameter value in the correct type.
   * @throws RoutingException if value cannot be correctly converted to the keyClass or keyClass
   * does not represent a primitive or enum.
   */
  public static Object parseSimpleKey(final String value, final Class<?> keyClass)
          throws RoutingException
  {
    if (CompoundKey.class.isAssignableFrom(keyClass)
        || ComplexResourceKey.class.isAssignableFrom(keyClass))
    {
      throw new RoutingException("Passing a complex key in place of a simple key",
                                 HttpStatus.S_500_INTERNAL_SERVER_ERROR.getCode());
    }

    return parseBasicValue(value, keyClass);
  }

  private static Object parseBasicValue(String value, Class<?> targetClass)
          throws RoutingException
  {
    try
    {
      return ValueConverter.coerceString(value, targetClass);
    }
    catch (NumberFormatException e)
    {
      // thrown from Integer.valueOf or Long.valueOf
      throw new RoutingException(String.format("Value '%s' must be of type '%s'",
                                               value,
                                               targetClass.getName()),
                                 HttpStatus.S_400_BAD_REQUEST.getCode());
    }
    catch (IllegalArgumentException e)
    {
      // thrown from Enum.valueOf
      throw new RoutingException(String.format("Parameter value '%s' is invalid",
                                               value),
                                 HttpStatus.S_400_BAD_REQUEST.getCode());
    }
  }

  /**
   *
   * @param value the stringified value
   * @param schema the schema of the type
   * @param type a non-complex type to convert to
   * @param optionalValue if the value is optional or not.
   * @return the converted value
   * @throws RoutingException if optionalValue is false and the value cannot be converted.
   */
  public static Object convertSimpleValue(final String value,
                                          final DataSchema schema,
                                          final Class<?> type,
                                          final boolean optionalValue)
          throws RoutingException
  {
    DataSchema.Type dereferencedType = schema.getDereferencedType();

    Object underlyingValue;
    if (schema.getDereferencedDataSchema().isComplex())
    {
      underlyingValue = value;
    }
    else if (optionalValue)
    {
      underlyingValue = ValueConverter.coerceString(value,
                                                    DataSchemaUtil.dataSchemaTypeToPrimitiveDataSchemaClass(dereferencedType));
    }
    else
    {
      underlyingValue = parseBasicValue(value, DataSchemaUtil.dataSchemaTypeToPrimitiveDataSchemaClass(dereferencedType));
    }

    return DataTemplateUtil.coerceOutput(underlyingValue, type);
  }

  /**
   * Cast input to string or throw RestLiInternalException if it is not a String.
   *
   * @param obj value to cast to string
   * @param paramName param name to return in the exception
   * @return input value cast to String
   */
  public static String argumentAsString(final Object obj, final String paramName)
  {
    if (obj != null && !(obj instanceof String))
    {
      throw new RestLiInternalException("Invalid value type for parameter " + paramName);
    }
    return (String) obj;
  }

  public static String getJavaClassNameFromSchema(final TyperefDataSchema schema)
  {
    Object o = schema.getProperties().get("java");
    if (o == null || !(o instanceof Map))
    {
      return null;
    }

    Map map = (Map)o;
    Object o2 = map.get("class");

    if (o2 == null || !(o2 instanceof String))
    {
      return null;
    }

    return (String)o2;
  }

  public static String getCoercerClassFromSchema(final TyperefDataSchema schema)
  {
    Object o = schema.getProperties().get("java");
    if (o == null || !(o instanceof Map))
    {
      return null;
    }

    Map map = (Map) o;
    Object o2 = map.get("coercerClass");

    if (o2 == null || !(o2 instanceof String))
    {
      return null;
    }

    return (String) o2;

  }

}
