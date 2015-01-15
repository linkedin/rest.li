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


import com.linkedin.r2.message.Message;

import java.util.List;
import java.util.Map;


/**
 * An object that represents a REST message, either a request or a response.<p/>
 *
 * Instances of RestMessage are immutable and thread-safe. It is possible to clone an existing
 * RestMessage, modify details in the copy, and create a new RestMessage instance that has the
 * concrete type of the original message (request or response) using the {@link #restBuilder()}
 * method.
 *
 * @see RestRequest
 * @see RestResponse
 * @author Chris Pettitt
 * @version $Revision$
 */
public interface RestMessage extends Message
{
  /**
   * Gets the value of the header with the given name. If there is no header with the given name
   * then this method returns {@code null}. If the header has multiple values then this method
   * returns the list joined with commas as allowed by RFC-2616, section 4.2.
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
   * Gets the values of cookies as specified in the Cookie or Set-Cookies HTTP headers in the
   * HTTP request and response respectively. Each Cookie or Set-Cookie header is a separate element in
   * the returned elements. If no cookie exists then an empty list is returned.
   *
   * @return cookies specified in the Cookie or Set-Cookie HTTP headers.
   */
  List<String> getCookies();

  /**
   * Returns an unmodifiable view of the headers in this builder. Because this is a view of the
   * headers and not a copy, changes to the headers in this builder *may* be reflected in the
   * returned map.
   *
   * @return a view of the headers in this builder
   */
  Map<String, String> getHeaders();

  /**
   * Returns a {@link RestMessageBuilder}, which provides a means of constructing a new message using
   * this message as a starting point. Changes made with the builder are not reflected by this
   * message instance.
   *
   * @return a builder for this message
   */
  RestMessageBuilder<? extends RestMessageBuilder<?>> restBuilder();
}
