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


package com.linkedin.restli.server;

import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.ErrorDetails;
import com.linkedin.restli.server.errors.ServiceError;


/**
 * Service errors to be used in unit tests.
 *
 * @author Gevorg Kurghinyan
 * @author Evan Williams
 */
public enum TestServiceError implements ServiceError
{
  RESOURCE_LEVEL_ERROR(400, "resource-level error"),
  METHOD_LEVEL_ERROR(400, "method-level error"),
  RESOURCE_LEVEL_ERROR_WITH_ERROR_DETAILS(400, "resource-level error with error details", EmptyRecord.class),
  METHOD_LEVEL_ERROR_WITH_ERROR_DETAILS(400, "method-level error with error details", EmptyRecord.class),
  ERROR_NOT_DEFINED_ON_RESOURCE_AND_METHOD(400, "service error");

  private final int _status;
  private final String _message;
  private final Class<? extends RecordTemplate> _errorDetailType;

  TestServiceError(int status, String message)
  {
    this(status, message, ErrorDetails.class);
  }

  TestServiceError(int status, String message, Class<? extends RecordTemplate> errorDetailType)
  {
    _status = status;
    _message = message;
    _errorDetailType = errorDetailType;
  }

  @Override
  public int httpStatus()
  {
    return _status;
  }

  @Override
  public String code()
  {
    return name();
  }

  @Override
  public String message()
  {
    return _message;
  }

  @Override
  public Class<? extends RecordTemplate> errorDetailType()
  {
    return _errorDetailType;
  }

  public interface Codes
  {
    String RESOURCE_LEVEL_ERROR = "RESOURCE_LEVEL_ERROR";
    String METHOD_LEVEL_ERROR = "METHOD_LEVEL_ERROR";
    String RESOURCE_LEVEL_ERROR_WITH_ERROR_DETAILS = "RESOURCE_LEVEL_ERROR_WITH_ERROR_DETAILS";
    String METHOD_LEVEL_ERROR_WITH_ERROR_DETAILS = "METHOD_LEVEL_ERROR_WITH_ERROR_DETAILS";
    String ERROR_NOT_DEFINED_ON_RESOURCE_AND_METHOD = "ERROR_NOT_DEFINED_ON_RESOURCE_AND_METHOD";
  }
}