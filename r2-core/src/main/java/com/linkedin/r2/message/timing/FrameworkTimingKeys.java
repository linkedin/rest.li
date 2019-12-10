/*
   Copyright (c) 2019 LinkedIn Corp.

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

package com.linkedin.r2.message.timing;

/**
 * A collection of predefined {@link TimingKey} objects that represent various Rest.li framework code paths.
 *
 * @author Evan Williams
 */
public enum FrameworkTimingKeys
{
  // High-level metrics
  RESOURCE("resource", TimingImportance.HIGH),
  SERVER_REQUEST("server/request", TimingImportance.HIGH),
  SERVER_RESPONSE("server/response", TimingImportance.HIGH),
  CLIENT_REQUEST("client/request", TimingImportance.HIGH),
  CLIENT_RESPONSE("client/response", TimingImportance.HIGH),

  // Layer-specific metrics
  SERVER_REQUEST_R2("server/request/r2", TimingImportance.MEDIUM),
  SERVER_REQUEST_RESTLI("server/request/restli", TimingImportance.MEDIUM),
  SERVER_RESPONSE_R2("server/response/r2", TimingImportance.MEDIUM),
  SERVER_RESPONSE_RESTLI("server/response/restli", TimingImportance.MEDIUM),
  CLIENT_REQUEST_R2("client/request/r2", TimingImportance.MEDIUM),
  CLIENT_REQUEST_RESTLI("client/request/restli", TimingImportance.MEDIUM),
  CLIENT_RESPONSE_R2("client/response/r2", TimingImportance.MEDIUM),
  CLIENT_RESPONSE_RESTLI("client/response/restli", TimingImportance.MEDIUM),

  // Filter chain metrics
  SERVER_REQUEST_R2_FILTER_CHAIN("server/request/r2/filter_chain", TimingImportance.LOW),
  SERVER_REQUEST_RESTLI_FILTER_CHAIN("server/request/restli/filter_chain", TimingImportance.LOW),
  SERVER_RESPONSE_R2_FILTER_CHAIN("server/response/r2/filter_chain", TimingImportance.LOW),
  SERVER_RESPONSE_RESTLI_FILTER_CHAIN("server/response/restli/filter_chain", TimingImportance.LOW),
  CLIENT_REQUEST_R2_FILTER_CHAIN("client/request/r2/filter_chain", TimingImportance.LOW),
  CLIENT_RESPONSE_R2_FILTER_CHAIN("client/response/r2/filter_chain", TimingImportance.LOW),

  // Serialization/Deserialization metrics
  SERVER_REQUEST_RESTLI_DESERIALIZATION("server/request/restli/deserialization", TimingImportance.LOW),
  SERVER_RESPONSE_RESTLI_SERIALIZATION("server/response/restli/serialization", TimingImportance.LOW),
  SERVER_RESPONSE_RESTLI_ERROR_SERIALIZATION("server/response/restli/error_serialization", TimingImportance.LOW),
  CLIENT_REQUEST_RESTLI_SERIALIZATION("client/request/restli/serialization", TimingImportance.LOW),
  CLIENT_RESPONSE_RESTLI_DESERIALIZATION("client/response/restli/deserialization", TimingImportance.LOW),
  CLIENT_RESPONSE_RESTLI_ERROR_DESERIALIZATION("client/response/restli/error_deserialization", TimingImportance.LOW),

  // URI operation metrics (numbered suffixes correspond to protocol-specific code paths)
  SERVER_REQUEST_RESTLI_URI_PARSE_1("server/request/restli/uri_parse_1", TimingImportance.LOW),
  SERVER_REQUEST_RESTLI_URI_PARSE_2("server/request/restli/uri_parse_2", TimingImportance.LOW),
  CLIENT_REQUEST_RESTLI_URI_ENCODE("client/request/restli/uri_encode", TimingImportance.LOW),

  // Projection operation metrics
  SERVER_REQUEST_RESTLI_PROJECTION_DECODE("server/request/restli/projection_decode", TimingImportance.LOW),
  SERVER_RESPONSE_RESTLI_PROJECTION_APPLY("server/request/restli/projection_apply", TimingImportance.LOW),

  // Misc. metrics
  CLIENT_REQUEST_RESTLI_GET_PROTOCOL("client/request/restli/get_protocol", TimingImportance.LOW);

  public final static String KEY_PREFIX = "fwk/";

  private final TimingKey _timingKey;

  FrameworkTimingKeys(String name, TimingImportance timingImportance)
  {
    _timingKey = TimingKey.registerNewKey(KEY_PREFIX + name, timingImportance);
  }

  public TimingKey key()
  {
    return _timingKey;
  }
}
