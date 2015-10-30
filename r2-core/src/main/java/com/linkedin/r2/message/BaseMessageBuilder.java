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

/* $Id$ */
package com.linkedin.r2.message;

import com.linkedin.r2.message.rest.RestUtil;
import com.linkedin.util.ArgumentUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


/**
 * Abstract base class for {@link com.linkedin.r2.message.MessageHeaders} builders.
 *
 * @author Chris Pettitt
 * @version $Revision$
 */
public abstract class BaseMessageBuilder<B extends BaseMessageBuilder<B>>
    implements MessageHeadersBuilder<B>
{
  private static final String CANONICAL_REGEX = "[ \t\n\r]+";

  private static final String CANONICAL_REPLACEMENT = " ";

  private Map<String, String> _headers;

  private List<String> _cookies;

  /**
   * Constructs a new builder with no initial values.
   */
  public BaseMessageBuilder()
  {
    setHeaders(Collections.<String, String>emptyMap());
    setCookies(Collections.<String>emptyList());
  }

  /**
   * Copies the values from the supplied message. Changes to this builder will not be reflected
   * in the original message.
   *
   * @param message the message to copy
   */
  public BaseMessageBuilder(MessageHeaders message)
  {
    setHeaders(message.getHeaders());
    setCookies(message.getCookies());
  }

  @Override
  public B setHeaders(Map<String, String> headers)
  {
    ArgumentUtil.notNull(headers, "headers");
    validateFieldNames(headers.keySet());
    return unsafeSetHeaders(headers);
  }

  @Override
  public B setHeader(String name, String value)
  {
    validateFieldName(name);
    return unsafeSetHeader(name, value);
  }

  @Override
  public B addHeaderValue(String name, String value)
  {
    validateFieldName(name);
    return unsafeAddHeaderValue(name, value);
  }

  @Override
  public B overwriteHeaders(Map<String, String> headers)
  {
    ArgumentUtil.notNull(headers, "headers");
    validateFieldNames(headers.keySet());
    return unsafeOverwriteHeaders(headers);
  }

  @Override
  public B addCookie(String cookie)
  {
    _cookies.add(cookie);
    return thisBuilder();
  }

  @Override
  public B setCookies(List<String> cookies)
  {
    _cookies = new ArrayList<String>(cookies);
    return thisBuilder();
  }

  @Override
  public B clearHeaders()
  {
    _headers.clear();
    return thisBuilder();
  }

  @Override
  public B clearCookies()
  {
    _cookies.clear();
    return thisBuilder();
  }

  @Override
  public Map<String, String> getHeaders()
  {
    return Collections.unmodifiableMap(_headers);
  }

  @Override
  public List<String> getCookies()
  {
    return Collections.unmodifiableList(_cookies);
  }

  @Override
  public String getHeader(String name)
  {
    return _headers.get(name);
  }

  @Override
  public List<String> getHeaderValues(String name)
  {
    final String headerVal = getHeader(name);
    if (headerVal == null)
    {
      return null;
    }
    return RestUtil.getHeaderValues(headerVal);
  }

  /**
   * Sets the given header without doing any validation. This method should only be used when the
   * headers are already known to be properly validated.
   *
   * @param name the name of the header
   * @param value the value of the header
   * @return this builder
   */
  public B unsafeSetHeader(String name, String value)
  {
    _headers.put(name, value);
    return thisBuilder();
  }

  /**
   * Appends a value to a header. This method should only be used when the headers are already known
   * to be properly validated.
   *
   * @param name  the name of the header
   * @param value the value to append to the header
   * @return this builder
   */
  public B unsafeAddHeaderValue(String name, String value)
  {
    // This is "safe" because we explicitly state in StreamMessageBuilder that the builder is not thread
    // safe and proper external synchronization must be used to use instances across threads.
    final String current = _headers.get(name);

    if (current == null)
    {
      _headers.put(name, value);
    }
    else
    {
      StringBuilder builder = new StringBuilder();
      builder.append(current).append(',').append(value);
      _headers.put(name, builder.toString());
    }

    return thisBuilder();
  }

  /**
   * Sets the given headers without doing any validation. This method should only be used when the
   * headers are already known to be properly validated.
   *
   * @param headers the headers to set
   * @return this builder
   */
  public B unsafeSetHeaders(Map<String, String> headers)
  {
    _headers = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
    return unsafeOverwriteHeaders(headers);
  }

  /**
   * Adds the given headers without doing any validation. This method should only be used when the
   * headers are already known to be properly validated.
   *
   * @param headers the headers to add
   * @return this builder
   */
  public B unsafeOverwriteHeaders(Map<String, String> headers)
  {
    _headers.putAll(headers);
    return thisBuilder();
  }

  /**
   * Validates {@code name} is not a cookie related header (i.e. Cookie or Set-Cookie).
   *
   * @param name the name to test for conformation.
   */
  protected abstract void validateCookieHeader(String name);

  /**
   * Strictly validates the given fieldNames to ensure that they conform to the field-name
   * specification in RFC 2616, section 2.2. In addition, validates {@code name} is not a
   * cookie related header.
   *
   * @param fieldNames the field names to validate
   */
  private void validateFieldNames(Collection<String> fieldNames)
  {
    for (String fieldName : fieldNames)
    {
      validateFieldName(fieldName);
    }
  }

  /**
   * Strictly validates {@code name} conforms to RFC 2616, section 2.2.
   *
   * @param name the name to test for conformation with RFC 2616, section 2.2.
   */
  private void validateFieldName(String name)
  {
    validateCookieHeader(name);

    if (name.isEmpty())
    {
      throw new IllegalArgumentException("header names must contain at least one character");
    }

    for (int i = 0; i < name.length(); i++)
    {
      final char ch = name.charAt(i);
      if (ch <= 32 || ch >= 127)
      {
        throw new IllegalArgumentException("header name does not conform to RFC 2616, section 2.2: " + name);
      }

      switch (ch)
      {
        case '(':
        case ')':
        case '<':
        case '>':
        case '@':
        case ',':
        case ';':
        case ':':
        case '\\':
        case '"':
        case '/':
        case '[':
        case ']':
        case '?':
        case '=':
        case '{':
        case '}':
          throw new IllegalArgumentException("header name does not conform to RFC 2616, section 2.2: " + name);
      }
    }
  }

  protected Map<String, String> getCanonicalHeaders()
  {
    final Map<String, String> orig = getHeaders();
    final Map<String, String> headers = new HashMap<String, String>(orig.size());
    for (Map.Entry<String, String> entry : orig.entrySet())
    {
      final String key = entry.getKey().toLowerCase();

      // Note: we don't handle null list elements because we don't know if the header is a list
      // or not.
      final String value = entry.getValue().trim().replaceAll(CANONICAL_REGEX, CANONICAL_REPLACEMENT);
      headers.put(key, value);
    }

    return Collections.unmodifiableMap(headers);
  }

  protected List<String> getCanonicalCookies()
  {
    final List<String> orig = getCookies();
    final List<String> cookies = new ArrayList<String>(orig.size());
    for (String entry : orig)
    {
      final String value = entry.trim().replaceAll(CANONICAL_REGEX, CANONICAL_REPLACEMENT);
      cookies.add(value);
    }

    return Collections.unmodifiableList(cookies);
  }

  @SuppressWarnings("unchecked")
  protected B thisBuilder()
  {
    return (B)this;
  }
}
