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

package com.linkedin.restli.example.impl;

import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.ErrorDetails;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.example.AlbumError;
import com.linkedin.restli.server.errors.ServiceError;


/**
 * Service error definition for the {@link AlbumEntryResource} resource.
 *
 * @author Karthik Balasubramanian
 * @author Gevorg Kurghinyan
 */
public enum AlbumServiceError implements ServiceError
{
  // Service Level error
  BAD_REQUEST(HttpStatus.S_422_UNPROCESSABLE_ENTITY, "Input failed validation", AlbumError.class),
  // Method level error
  INVALID_PERMISSIONS(HttpStatus.S_403_FORBIDDEN, "User does not have valid permissions", AlbumError.class),
  // Parameter error
  INVALID_ALBUM_ID(HttpStatus.S_422_UNPROCESSABLE_ENTITY, "Album id cannot be less than 0", AlbumError.class);

  AlbumServiceError(HttpStatus status, String message, Class<? extends RecordTemplate> errorDetailType)
  {
    _status = status;
    _message = message;
    _errorDetailType = errorDetailType;
  }

  public interface Codes
  {
    String BAD_REQUEST = "BAD_REQUEST";
    String INVALID_PERMISSIONS = "INVALID_PERMISSIONS";
    String INVALID_ALBUM_ID = "INVALID_ALBUM_ID";
  }

  private final HttpStatus _status;
  private final String _message;
  private final Class<? extends RecordTemplate> _errorDetailType;

  @Override
  public HttpStatus httpStatus()
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
}
