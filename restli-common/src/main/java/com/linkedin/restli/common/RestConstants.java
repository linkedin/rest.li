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


package com.linkedin.restli.common;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


public interface RestConstants
{
  int DEFAULT_START = 0;
  int DEFAULT_COUNT = 10;

  String HEADER_RESTLI_REQUEST_METHOD = "X-RestLi-Method";
  String HEADER_LINKEDIN_ERROR_RESPONSE = "X-LinkedIn-Error-Response"; // we are deprecating all X-Linkedin header prefixes and replacing them with X-RestLi
  String HEADER_RESTLI_ERROR_RESPONSE = "X-RestLi-Error-Response"; // replacement for X-LinkedIn-Error-Response for when it is removed
  String HEADER_VALUE_ERROR = "true";
  String HEADER_ID = "X-LinkedIn-Id"; // we are deprecating all X-Linkedin header prefixes and replacing them with X-RestLi
  String HEADER_RESTLI_ID = "X-RestLi-Id"; // replacement for X-LinkedIn-Id for when it is removed
  String HEADER_LOCATION = "Location";
  String HEADER_ACCEPT = "Accept";
  String HEADER_CONTENT_TYPE = "Content-Type";
  String HEADER_VALUE_APPLICATION_JSON = "application/json";
  String HEADER_VALUE_APPLICATION_PSON = "application/x-pson";
  String HEADER_VALUE_MULTIPART_RELATED = "multipart/related";
  String HEADER_VALUE_ACCEPT_ANY = "*/*";
  String HEADER_RESTLI_PROTOCOL_VERSION = "X-RestLi-Protocol-Version";
  String HEADER_CONTENT_ID = "Content-ID";

  List<String> SUPPORTED_MIME_TYPES = Arrays.asList(HEADER_VALUE_APPLICATION_PSON, HEADER_VALUE_APPLICATION_JSON);

  String START_PARAM = "start";
  String COUNT_PARAM = "count";
  String ACTION_PARAM = "action";
  String QUERY_TYPE_PARAM = "q";
  String QUERY_BATCH_IDS_PARAM = "ids";
  String FIELDS_PARAM = "fields";
  String ALT_KEY_PARAM = "altkey";
  String METADATA_FIELDS_PARAM = "metadataFields";
  String PAGING_FIELDS_PARAM = "pagingFields";
  Set<String> PROJECTION_PARAMETERS = Collections.unmodifiableSet(new LinkedHashSet<String>(
      Arrays.asList(FIELDS_PARAM, METADATA_FIELDS_PARAM, PAGING_FIELDS_PARAM)));

  /** delimiter used for separating (name=value) parts of compound key */
  char   SIMPLE_KEY_DELIMITER = '&';
  /** delimiter used for separating name from value in a name-value pair forming a part of compound key */
  char   KEY_VALUE_DELIMITER = '=';
  String DEFAULT_CHARSET_NAME = "UTF-8";
  Charset DEFAULT_CHARSET = Charset.forName(DEFAULT_CHARSET_NAME);

  String RESOURCE_MODEL_FILENAME_EXTENSION = ".restspec.json";
  String SNAPSHOT_FILENAME_EXTENTION = ".snapshot.json";
  Set<ResourceMethod> SIMPLE_RESOURCE_METHODS = Collections.unmodifiableSet(
      new HashSet<ResourceMethod>(
        Arrays.asList(
            ResourceMethod.ACTION,
            ResourceMethod.DELETE,
            ResourceMethod.GET,
            ResourceMethod.PARTIAL_UPDATE,
            ResourceMethod.UPDATE)));

  String RESTLI_PROTOCOL_VERSION_PROPERTY = "restli.protocol";
  String RESTLI_PROTOCOL_VERSION_PERCENTAGE_PROPERTY = "restli.protocol.percentage";
  String RESTLI_FORCE_USE_NEXT_VERSION_OVERRIDE = "restli.forceUseNextVersionOverride";
}
