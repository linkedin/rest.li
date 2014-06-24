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

package com.linkedin.restli.client.testutils;


import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.ResponseFuture;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.internal.client.ResponseFutureImpl;

import java.util.Map;


/**
 * Build a {@link ResponseFuture} representing a successful request. The {@link ResponseFuture} wraps a
 * {@link Response}.
 *
 * @author kparikh
 *
 * @param <K> key type of the mocked response
 * @param <V> entity type of the mocked response
 */
public class MockSuccessfulResponseFutureBuilder<K, V extends RecordTemplate> extends MockAbstractResponseFutureBuilder<K, V>
{
  private static final int DEFAULT_HTTP_STATUS = 200;

  @Override
  protected int getStatus()
  {
    int status = super.getStatus();
    return (status == 0) ? DEFAULT_HTTP_STATUS : status;
  }

  /**
   * Set the entity. This is the object that will be returned by {@link com.linkedin.restli.client.Response#getEntity()}
   *
   * @param entity the entity to set
   * @return
   */
  @Override
  public MockSuccessfulResponseFutureBuilder<K, V> setEntity(V entity)
  {
    super.setEntity(entity);
    return this;
  }

  /**
   * Set the HTTP status. This is the status that will be returned by
   * {@link com.linkedin.restli.client.Response#getStatus()}
   *
   * An {@link IllegalArgumentException} is thrown if the status is not in the range [200, 300).
   *
   * @param status the HTTP status we want to set
   * @return
   */
  @Override
  public MockSuccessfulResponseFutureBuilder<K, V> setStatus(int status)
  {
    if (status < 200 || status >= 300)
    {
      throw new IllegalArgumentException("Status must be a 2xx HTTP status!");
    }
    super.setStatus(status);
    return this;
  }

  /**
   * Sets the headers. This will be returned by {@link com.linkedin.restli.client.Response#getHeaders()}
   *
   * @param headers the headers we want to set
   * @return
   */
  @Override
  public MockSuccessfulResponseFutureBuilder<K, V> setHeaders(Map<String, String> headers)
  {
    super.setHeaders(headers);
    return this;
  }

  /**
   * Set the {@link ProtocolVersion}
   *
   * @param protocolVersion the {@link ProtocolVersion} to set
   * @return
   */
  @Override
  public MockSuccessfulResponseFutureBuilder<K, V> setProtocolVersion(ProtocolVersion protocolVersion)
  {
    super.setProtocolVersion(protocolVersion);
    return this;
  }

  /**
   * Builds a {@link ResponseFuture} that has been constructed using the above setters.
   * If the HTTP status was not set then {@link #DEFAULT_HTTP_STATUS} is used as the status.
   *
   * @return a {@link ResponseFuture} that has been constructed using the above setters.
   */
  @Override
  public ResponseFuture<V> build()
  {
    MockResponseBuilder<K, V> responseBuilder = new MockResponseBuilder<K, V>();
    Response<V> response = responseBuilder
        .setEntity(getEntity())
        .setStatus(getStatus())
        .setHeaders(getHeaders())
        .setProtocolVersion(getProtocolVersion())
        .build();

    return new ResponseFutureImpl<V>(buildFuture(response, null));
  }
}
