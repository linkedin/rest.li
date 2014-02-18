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


import com.linkedin.restli.client.Response;
import com.linkedin.restli.client.RestLiResponseException;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.client.ResponseImpl;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import java.util.HashMap;
import java.util.Map;


/**
 * Builds a {@link Response} that can be used for tests.
 *
 * @author jflorencio
 * @author kparikh
 */
public class MockResponseBuilder<T>
{
  private T _entity;
  private int _status;
  private String _id;
  private Map<String, String> _headers;
  private RestLiResponseException _restLiResponseException;
  private ProtocolVersion _protocolVersion;

  private static final int DEFAULT_HTTP_STATUS = 200;

  /**
   * Set the entity
   *
   * @param entity the entity for the {@link Response}
   * @return
   */
  public MockResponseBuilder<T> setEntity(T entity)
  {
    _entity = entity;
    return this;
  }

  /**
   * Set the HTTP status code for the {@link Response}
   *
   * @param status the status code for the {@link Response}
   * @return
   */
  public MockResponseBuilder<T> setStatus(int status)
  {
    _status = status;
    return this;
  }

  /**
   * Set the ID for the {@link Response}
   *
   * This ID is stored in the header of the {@link Response}.
   *
   * If the Rest.li 1.0 protocol is being used the header key is
   * {@link com.linkedin.restli.common.RestConstants#HEADER_ID}
   *
   * If the Rets.li 2.0 protocol is being used the header key is
   * {@link com.linkedin.restli.common.RestConstants#HEADER_RESTLI_ID}
   *
   * @param id the ID for the {@link Response}
   * @return
   */
  public MockResponseBuilder<T> setId(String id)
  {
    _id = id;
    return this;
  }

  /**
   * Set the headers for the {@link Response}
   *
   * @param headers the headers for the {@link Response}
   * @return
   */
  public MockResponseBuilder<T> setHeaders(Map<String, String> headers)
  {
    _headers = headers;
    return this;
  }

  /**
   * Set the {@link RestLiResponseException} for the {@link Response}
   *
   * @param restLiResponseException the {@link RestLiResponseException} for the {@link Response}
   * @return
   */
  public MockResponseBuilder<T> setRestLiResponseException(RestLiResponseException restLiResponseException)
  {
    _restLiResponseException = restLiResponseException;
    return this;
  }

  /**
   * Set the {@link ProtocolVersion} for the {@link Response}
   *
   * @param protocolVersion the {@link ProtocolVersion} for the {@link Response}
   * @return
   */
  public MockResponseBuilder<T> setProtocolVersion(ProtocolVersion protocolVersion)
  {
    _protocolVersion = protocolVersion;
    return this;
  }

  /**
   * Builds a {@link Response} that has been constructed using the setters in this class.
   *
   * @return the constructed {@link Response}
   */
  public Response<T> build()
  {
    Map<String, String> headers = new HashMap<String, String>();
    if (_headers != null)
    {
      headers.putAll(_headers);
    }

    ProtocolVersion protocolVersion = (_protocolVersion == null) ?
        AllProtocolVersions.BASELINE_PROTOCOL_VERSION : _protocolVersion;

    addIdAndProtocolVersionHeaders(headers, _id, protocolVersion);

    int status = (_status == 0) ? DEFAULT_HTTP_STATUS : _status;

    return new ResponseImpl<T>(status, headers, _entity, _restLiResponseException);
  }

  /**
   * Adds the ID and protocol version to the headers
   *
   * @param headers existing headers. The ID and protocol version will be added to this
   * @param id the ID
   * @param protocolVersion the protocol version
   */
  static void addIdAndProtocolVersionHeaders(Map<String, String> headers, String id, ProtocolVersion protocolVersion)
  {
    headers.put(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, protocolVersion.toString());

    if (id != null)
    {
      if (protocolVersion.equals(AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion()))
      {
        // use protocol 1.0 ID header
        headers.put(RestConstants.HEADER_ID, id);
      }
      else
      {
        // use protocol 2.0 ID header
        headers.put(RestConstants.HEADER_RESTLI_ID, id);
      }
    }
  }
}
