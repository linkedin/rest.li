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

package com.linkedin.restli.tools.errors;

import com.linkedin.data.DataMap;
import com.linkedin.data.schema.Name;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.ErrorDetails;
import com.linkedin.restli.server.errors.ServiceError;


/**
 * Dummy RecordTemplates for service error resources.
 *
 * @author Evan Williams
 */
public class ServiceErrorTestDataModels
{
  /**
   * A dummy record to be used as the value class for service error test resources.
   */
  public static class DummyRecord extends RecordTemplate
  {
    private static final RecordDataSchema SCHEMA =
        new RecordDataSchema(new Name("com.linkedin.restli.tools.errors.DummyRecord", new StringBuilder(10)), RecordDataSchema.RecordType.RECORD);

    public DummyRecord()
    {
      super(new DataMap(), SCHEMA);
    }

    public DummyRecord(DataMap map)
    {
      super(map, SCHEMA);
    }
  }

  /**
   * Service error-related data and interfaces for service error test resources.
   */
  public enum DummyServiceError implements ServiceError
  {
    RESOURCE_LEVEL_ERROR(400, "Wow, this is such a resource-level error", ErrorDetails.class),
    METHOD_LEVEL_ERROR(400, "And this is such a method-level error", ErrorDetails.class),
    YET_ANOTHER_RESOURCE_LEVEL_ERROR(403, "Wow, yet another one!", ErrorDetails.class),
    YET_ANOTHER_METHOD_LEVEL_ERROR(403, "I can't believe there's another one", ErrorDetails.class),
    ILLEGAL_ACTION(451, "You can't do that, you're going to Rest.li prison", DummyRecord.class),
    NO_MESSAGE_ERROR(400, null, ErrorDetails.class),
    NO_DETAIL_TYPE_ERROR(400, "The error detail type... where is it?", null);

    DummyServiceError(int status, String message, Class<? extends RecordTemplate> errorDetailType)
    {
      _status = status;
      _message = message;
      _errorDetailType = errorDetailType;
    }

    public interface Codes
    {
      String RESOURCE_LEVEL_ERROR = "RESOURCE_LEVEL_ERROR";
      String METHOD_LEVEL_ERROR = "METHOD_LEVEL_ERROR";
      String YET_ANOTHER_RESOURCE_LEVEL_ERROR = "YET_ANOTHER_RESOURCE_LEVEL_ERROR";
      String YET_ANOTHER_METHOD_LEVEL_ERROR = "YET_ANOTHER_METHOD_LEVEL_ERROR";
      String ILLEGAL_ACTION = "ILLEGAL_ACTION";
      String NO_MESSAGE_ERROR = "NO_MESSAGE_ERROR";
      String NO_DETAIL_TYPE_ERROR = "NO_DETAIL_TYPE_ERROR";
    }

    private final int _status;
    private final String _message;
    private final Class<? extends RecordTemplate> _errorDetailType;

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
  }
}
