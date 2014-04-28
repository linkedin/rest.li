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
 */
public class MockSuccessfulResponseFutureBuilder<T extends RecordTemplate> extends MockAbstractResponseFutureBuilder<T>
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
  public MockSuccessfulResponseFutureBuilder<T> setEntity(T entity)
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
  public MockSuccessfulResponseFutureBuilder<T> setStatus(int status)
  {
    if (status < 200 || status >= 300)
    {
      throw new IllegalArgumentException("Status must be a 2xx HTTP status!");
    }
    super.setStatus(status);
    return this;
  }

  /**
   * Set the ID. This will be returned by
   * {@link com.linkedin.restli.client.response.CreateResponse#getId()}
   * and also by {@link com.linkedin.restli.common.IdResponse#getId()}
   *
   * This ID is stored in the header of the {@link Response}.
   *
   * If the Rest.li 1.0 protocol is being used the header key is
   * {@link com.linkedin.restli.common.RestConstants#HEADER_ID}
   *
   * If the Rets.li 2.0 protocol is being used the header key is
   * {@link com.linkedin.restli.common.RestConstants#HEADER_RESTLI_ID}
   *
   * @param id the ID we want to set
   * @return
   */
  @Override
  public MockSuccessfulResponseFutureBuilder<T> setId(String id)
  {
    super.setId(id);
    return this;
  }

  /**
   * Sets the headers. This will be returned by {@link com.linkedin.restli.client.Response#getHeaders()}
   *
   * @param headers the headers we want to set
   * @return
   */
  @Override
  public MockSuccessfulResponseFutureBuilder<T> setHeaders(Map<String, String> headers)
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
  public MockSuccessfulResponseFutureBuilder<T> setProtocolVersion(ProtocolVersion protocolVersion)
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
  public ResponseFuture<T> build()
  {
    MockResponseBuilder<T> responseBuilder = new MockResponseBuilder<T>();
    Response<T> response = responseBuilder
        .setEntity(getEntity())
        .setStatus(getStatus())
        .setId(getId())
        .setHeaders(getHeaders())
        .setProtocolVersion(getProtocolVersion())
        .build();

    return new ResponseFutureImpl<T>(buildFuture(response, null));
  }
}
