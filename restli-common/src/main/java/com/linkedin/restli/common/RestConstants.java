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

public interface RestConstants
{
  int DEFAULT_START = 0;
  int DEFAULT_COUNT = 10;

  String HEADER_LINKEDIN_TYPE = "X-LinkedIn-Type";
  String HEADER_LINKEDIN_SUB_TYPE = "X-LinkedIn-Sub-Type";
  String HEADER_RESTLI_REQUEST_METHOD = "X-RestLi-Method";
  String HEADER_LINKEDIN_ERROR_RESPONSE = "X-LinkedIn-Error-Response";
  String HEADER_VALUE_ERROR_PREPROCESSING = "FWK-PRE";
  String HEADER_VALUE_ERROR_POSTPROCESSING = "FWK-POST";
  String HEADER_VALUE_ERROR_APPLICATION = "APP";
  String HEADER_ID = "X-LinkedIn-Id";
  String HEADER_LOCATION = "Location";
  String HEADER_CONTENT_TYPE = "Content-Type";
  String HEADER_VALUE_APPLICATION_JSON = "application/json";
  
  String START_PARAM = "start";
  String COUNT_PARAM = "count";
  String ACTION_PARAM = "action";
  String QUERY_TYPE_PARAM = "q";
  String QUERY_BATCH_IDS_PARAM = "ids";
  String FIELDS_PARAM = "fields";
  /** delimiter used for separating (name=value) parts of compound key */
  char   SIMPLE_KEY_DELIMITER = '&';
  /** delimiter used for separating name from value in a name-value pair forming a part of compound key */
  char   KEY_VALUE_DELIMITER = '=';
  String DEFAULT_CHARSET_NAME = "UTF-8";
  Charset DEFAULT_CHARSET = Charset.forName(DEFAULT_CHARSET_NAME);

  String RESOURCE_MODEL_FILENAME_EXTENSION = ".restspec.json";
}
