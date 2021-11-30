/*
   Copyright (c) 2013 LinkedIn Corp.

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

package com.linkedin.r2.filter;


/**
 * @author kparikh
 */
public class R2Constants
{
  public static final String OPERATION = "OPERATION";

  /**
   * The address and port of the remote client or last proxy that sent the http request.
   * Typically this value is set by servlets and accessible for server side code.
   **/
  public static final String REMOTE_ADDR = "REMOTE_ADDR";
  public static final String REMOTE_PORT = "REMOTE_PORT";


  /**
   * The address and port of the remote server that received the http request.
   * Typically this value is set by transport client and accessible for
   * client side code after the request is sent
   * */
  public static final String REMOTE_SERVER_ADDR = "REMOTE_SERVER_ADDR";
  public static final String REMOTE_SERVER_PORT = "REMOTE_SERVER_PORT";

  /**
   * The HTTP protocol that the client and server are communicated under.
   */
  public static final String HTTP_PROTOCOL_VERSION = "HTTP_PROTOCOL_VERSION";
  public static final String IS_SECURE = "IS_SECURE";
  public static final String CLIENT_CERT = "CLIENT_CERT";
  public static final String CIPHER_SUITE = "CIPHER_SUITE";
  public static final String REQUEST_COMPRESSION_OVERRIDE = "REQUEST_COMPRESSION_OVERRIDE";
  public static final String RESPONSE_COMPRESSION_OVERRIDE = "RESPONSE_COMPRESSION_OVERRIDE";
  public static final String IS_QUERY_TUNNELED = "IS_QUERY_TUNNELED";
  public static final String FORCE_QUERY_TUNNEL = "FORCE_QUERY_TUNNEL";
  public static final String RESPONSE_DECOMPRESSION_OFF = "RESPONSE_DECOMPRESSION_OFF";
  public static final String IS_FULL_REQUEST = "IS_FULL_REQUEST";
  public static final int DEFAULT_DATA_CHUNK_SIZE = 8192;
  public static final boolean DEFAULT_REST_OVER_STREAM = false;
  public static final String RETRY_MESSAGE_ATTRIBUTE_KEY = "RETRY";
  public static final String BACKUP_REQUEST_BUFFERED_BODY = "BACKUP_REQUEST_BUFFERED_BODY";
  @Deprecated
  public static final String EXPECTED_SERVER_CERT_PRINCIPAL_NAME = "EXPECTED_SERVER_CERT_PRINCIPAL_NAME";
  public static final String REQUESTED_SSL_SESSION_VALIDATOR = "REQUESTED_SSL_SESSION_VALIDATOR";
  public static final String REQUEST_TIMEOUT = "REQUEST_TIMEOUT";
  /**
   * Ignore overriding the REQUEST_TIMEOUT if it is higher than the current value in the LB
   */
  public static final String REQUEST_TIMEOUT_IGNORE_IF_HIGHER_THAN_DEFAULT = "REQUEST_TIMEOUT_IGNORE_IF_HIGHER";
  /**
   * CLIENT_REQUEST_TIMEOUT_VIEW should only be set when per request timeout is lower than the default value
   */
  public static final String CLIENT_REQUEST_TIMEOUT_VIEW = "CLIENT_REQUEST_TIMEOUT_VIEW";
  public static final String PREEMPTIVE_TIMEOUT_RATE = "PREEMPTIVE_TIMEOUT_RATE";
  public static final String PROJECTION_INFO = "PROJECTION_INFO";
  public static final String RESTLI_INFO = "RESTLI_INFO";

  /**
   * Client uses this key to designate a request as belonging to a group of requests that will be monitored in a
   * single aggregation and monitored from client's perspective.
   */
  public static final String CLIENT_REQUEST_METRIC_GROUP_NAME = "CLIENT_REQUEST_METRIC_GROUP_NAME";

  /**
   * Server-side request finalizer manager.
   *
   * @see com.linkedin.r2.util.finalizer.RequestFinalizerManager
   * @see com.linkedin.r2.util.finalizer.RequestFinalizerDispatcher
   */
  public static final String SERVER_REQUEST_FINALIZER_MANAGER_REQUEST_CONTEXT_KEY = "SERVER_REQUEST_FINALIZER_MANAGER";

  /**
   * Client-side request finalizer manager.
   *
   * @see com.linkedin.r2.util.finalizer.RequestFinalizerManager
   * @see com.linkedin.r2.filter.ClientRequestFinalizerFilter
   */
  public static final String CLIENT_REQUEST_FINALIZER_MANAGER_REQUEST_CONTEXT_KEY = "CLIENT_REQUEST_FINALIZER_MANAGER";
}
