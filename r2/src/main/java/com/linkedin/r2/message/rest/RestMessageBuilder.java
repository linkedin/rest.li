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
package com.linkedin.r2.message.rest;


import com.linkedin.r2.message.MessageBuilder;

import java.util.List;
import java.util.Map;


/**
 * @author Chris Pettitt
 * @version $Revision$
 */
public interface RestMessageBuilder<B extends RestMessageBuilder<B>> extends MessageBuilder<B>
{
  /**
   * Replaces the headers in this message with a copy of the supplied headers. Changes to the
   * supplied headers will not be reflected in the headers on this message, and vice-versa.
   *
   * @param headers the headers to set for this message
   * @return this builder
   */
  B setHeaders(Map<String, String> headers);

  /**
   * Sets a header with the given name and value. If a header already exists with the given name
   * then its value is replaced by the supplied value.
   *
   * @param name the name of the header
   * @param value the new value for the header
   * @return this builder
   */
  B setHeader(String name, String value);

  /**
   * Adds a new value for a header name. If a value is already associated with the header name then
   * the new value is joined with the existing value by a comma as allowed by RFC-2616, section 4.2.
   * If a value is not already associated with the header name then the header is set to the
   * given value.<p/>
   *
   * Note that it is possible to append a value that itself includes a comma. This can occur when
   * parsing a header that does not have list semantics or when parsing a header that has already
   * had the list values concatenated. If the header is later treated as a list then this element
   * would also be split by commas yielding more values than there were calls to this method.
   *
   * @param name the name of the header
   * @param value the value to add to the header
   * @return this builder
   */
  B addHeaderValue(String name, String value);

  /**
   * Adds all specified headers to this message, while preserving non-overlapping headers that were
   * already set through this builder. When a header name appears both in this message and in the
   * supplied headers, the value of the supplied header replaces the header in this message.
   *
   * @param headers the map of headers to add to this message
   * @return this builder
   */
  B overwriteHeaders(Map<String, String> headers);

  /**
   * Adds a new {@code cookie} to this message. The content of the {@code cookie} is expected to
   * comply with RFC 2616 but not explicitly enforced.
   *
   * @param cookie the cookie to add to this message
   * @return this builder
   */
  B addCookie(String cookie);

  /**
   * Sets a new list of {@code cookies} as the cookies of this message. Previously added cookies are
   * replaced with the new list of {@code cookies}. The content of each cookie is expected to comply
   * with RFC 2616 but not explicitly enforced.
   *
   * @param cookies the list cookies to be set to this message
   * @return this builder
   */
  B setCookies(List<String> cookies);

  /**
   * Removes all headers from this message.
   *
   * @return this builder
   */
  B clearHeaders();

  /**
   * Remove all cookies from this message.
   *
   * @return this builder
   */
  B clearCookies();

  /**
   * Returns an unmodifiable view of the headers in this builder. Because this is a view of the
   * headers and not a copy, changes to the headers in this builder *may* be reflected in the
   * returned map.
   *
   * @return a view of the headers in this builder
   */
  Map<String, String> getHeaders();

  /**
   * Returns an unmodifiable view of the cookies in this builder.
   *
   * @return a view of the cookies in this builder
   */
  List<String> getCookies();

  /**
   * Gets the value of the header with the given name. If there is no header with the given name
   * then this method returns {@code null}.
   *
   * @param name name of the header
   * @return the value of the header or {@code null} if there is no header with the given name.
   */
  String getHeader(String name);

  /**
   * Treats the header with the given name as a multi-value header (see RFC 2616, section 4.2). Each
   * value for the header is a separate element in the returned list. If no header exists with the
   * supplied name then {@code null} is returned.
   *
   * @param name the name of the header
   * @return a list of values for the header or {@code null} if no values exist.
   */
  List<String> getHeaderValues(String name);

  /**
   * Constructs an immutable {@link RestMessage} using the settings configured in this builder.
   * Subsequent changes to this builder will not change this response. The concrete
   * type of this builder (for example {@link com.linkedin.r2.message.rest.RestResponseBuilder}) will
   * be used to build the appropriate concrete type.
   *
   * @return a RestMessage from the settings in this builder
   */
  @Override
  RestMessage build();

  /**
   * Similar to {@link #build}, but the returned Message is in canonical form.
   *
   * @return a RestMessage from the settings in this builder
   */
  @Override
  RestMessage buildCanonical();
}
