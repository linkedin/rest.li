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

import com.linkedin.data.DataMap;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.GetMode;
import com.linkedin.data.template.RecordTemplate;


/**
 * A generic ErrorResponse
 */
public class ErrorResponse extends RecordTemplate
{

  private static final RecordDataSchema SCHEMA = (RecordDataSchema) DataTemplateUtil.parseSchema("{\"type\":\"record\",\"name\":\"ErrorResponse\",\"namespace\":\"com.linkedin.common.rest\",\"doc\":\"A generic ErrorResponse\",\"fields\":[{\"name\":\"status\",\"type\":\"int\",\"doc\":\"The HTTP status code\"},{\"name\":\"serviceErrorCode\",\"type\":\"int\",\"doc\":\"An service-specific error code (documented in prose)\"},{\"name\":\"message\",\"type\":\"string\",\"doc\":\"A human-readable explanation of the error\"},{\"name\":\"exceptionClass\",\"type\":\"string\",\"doc\":\"The FQCN of the exception thrown by the server (included the case of a server fault)\"},{\"name\":\"stackTrace\",\"type\":\"string\",\"doc\":\"The full (??) stack trace (included the case of a server fault)\"}]}");
  private static final RecordDataSchema.Field FIELD_Status = SCHEMA.getField("status");
  private static final RecordDataSchema.Field FIELD_ServiceErrorCode = SCHEMA.getField("serviceErrorCode");
  private static final RecordDataSchema.Field FIELD_Message = SCHEMA.getField("message");
  private static final RecordDataSchema.Field FIELD_ExceptionClass = SCHEMA.getField("exceptionClass");
  private static final RecordDataSchema.Field FIELD_StackTrace = SCHEMA.getField("stackTrace");
  private static final String ERROR_DETAILS = "errorDetails";

  /**
   * Initialize an empty ErrorResponse.
   */
  public ErrorResponse()
  {
    super(new DataMap(), SCHEMA);
  }

  /**
   * Initialize an ErrorResponse based on the given DataMap.
   *
   * @param data a DataMap
   */
  public ErrorResponse(DataMap data)
  {
    super(data, SCHEMA);
  }

  /**
   * @return true if the ErrorResponse has a http status value, and false otherwise
   */
  public boolean hasStatus()
  {
    return contains(FIELD_Status);
  }

  /**
   * Remove the status value from this ErrorResponse.
   */
  public void removeStatus()
  {
    remove(FIELD_Status);
  }

  /**
   * Returns the status of the ErrorResponse. If the value is not
   * present, behavior will be determined by the given {@link com.linkedin.data.template.GetMode mode}.
   *
   * @param mode determines behavior if the attribute is not present
   * @return an Integer
   * @see GetMode
   */
  public Integer getStatus(GetMode mode)
  {
    return obtainDirect(FIELD_Status, Integer.class, mode);
  }

  /**
   * Get the status of the ErrorResponse.  If the value is not found, act in
   * accordance with {@link com.linkedin.data.template.GetMode GetMode.STRICT}.
   *
   * @return an int
   */
  public int getStatus()
  {
    return getStatus(GetMode.STRICT).intValue();
  }

  /**
   * Set the status.
   * @param value status
   */
  public void setStatus(int value)
  {
    putDirect(FIELD_Status, Integer.class, value);
  }

  /**
   * @return true if the ErrorResponse has a service error code, and false otherwise.
   */
  public boolean hasServiceErrorCode()
  {
    return contains(FIELD_ServiceErrorCode);
  }

  /**
   * Remove the service error code from this ErrorResponse.
   */
  public void removeServiceErrorCode()
  {
    remove(FIELD_ServiceErrorCode);
  }

  /**
   * Returns the service error code of the ErrorResponse. If the value is not
   * present, behavior will be determined by the given {@link com.linkedin.data.template.GetMode mode}.
   *
   * @param mode determines behavior if the attribute is not present
   * @return an Integer
   * @see GetMode
   */
  public Integer getServiceErrorCode(GetMode mode)
  {
    return obtainDirect(FIELD_ServiceErrorCode, Integer.class, mode);
  }

  /**
   * Get the service error code of the ErrorResponse.  If the value is not found, act in
   * accordance with {@link com.linkedin.data.template.GetMode GetMode.STRICT}.
   *
   * @return an int
   */
  public int getServiceErrorCode()
  {
    return getServiceErrorCode(GetMode.STRICT).intValue();
  }

  /**
   * Set the service error code.
   *
   * @param value service error code
   */
  public void setServiceErrorCode(int value)
  {
    putDirect(FIELD_ServiceErrorCode, Integer.class, value);
  }

  /**
   * @return true if the ErrorResponse has a message value, and false otherwise.
   */
  public boolean hasMessage()
  {
    return contains(FIELD_Message);
  }

  /**
   * Remove the message from this ErrorResponse.
   */
  public void removeMessage()
  {
    remove(FIELD_Message);
  }

  /**
   * Returns the message of the ErrorResponse. If the value is not
   * present, behavior will be determined by the given {@link com.linkedin.data.template.GetMode mode}.
   *
   * @param mode determines behavior if the attribute is not present
   * @return a String
   * @see GetMode
   */
  public String getMessage(GetMode mode)
  {
    return obtainDirect(FIELD_Message, String.class, mode);
  }

  /**
   * Get the message of the ErrorResponse.  If the value is not found, act in
   * accordance with {@link com.linkedin.data.template.GetMode GetMode.STRICT}.
   *
   * @return a String
   */
  public String getMessage()
  {
    return getMessage(GetMode.STRICT);
  }

  /**
   * Set the message.
   * @param value message
   */
  public void setMessage(String value)
  {
    putDirect(FIELD_Message, String.class, value);
  }

  /**
   * @return true if the ErrorResponse has an exception class, and false otherwise.
   */
  public boolean hasExceptionClass()
  {
    return contains(FIELD_ExceptionClass);
  }

  /**
   * Remove the exception class from this ErrorResponse.
   */
  public void removeExceptionClass()
  {
    remove(FIELD_ExceptionClass);
  }

  /**
   * Returns the exception class of the ErrorResponse. If the value is not
   * present, behavior will be determined by the given {@link com.linkedin.data.template.GetMode mode}.
   *
   * @param mode determines behavior if the attribute is not present
   * @return a String
   * @see GetMode
   */
  public String getExceptionClass(GetMode mode)
  {
    return obtainDirect(FIELD_ExceptionClass, String.class, mode);
  }

  /**
   * Get the exception class of the ErrorResponse.  If the value is not found, act in
   * accordance with {@link com.linkedin.data.template.GetMode GetMode.STRICT}.
   *
   * @return a String
   */
  public String getExceptionClass()
  {
    return getExceptionClass(GetMode.STRICT);
  }

  /**
   * Set the exception class.
   *
   * @param value exception class
   */
  public void setExceptionClass(String value)
  {
    putDirect(FIELD_ExceptionClass, String.class, value);
  }

  /**
   * @return true if the ErrorResponse has a stack trace, and false otherwise.
   */
  public boolean hasStackTrace()
  {
    return contains(FIELD_StackTrace);
  }

  /**
   * Remove the stack trace from this ErrorResponse.
   */
  public void removeStackTrace()
  {
    remove(FIELD_StackTrace);
  }

  /**
   * Returns the stack trace of the ErrorResponse. If the value is not
   * present, behavior will be determined by the given {@link com.linkedin.data.template.GetMode mode}.
   *
   * @param mode determines behavior if the attribute is not present
   * @return a String
   * @see GetMode
   */
  public String getStackTrace(GetMode mode)
  {
    return obtainDirect(FIELD_StackTrace, String.class, mode);
  }

  /**
   * Get the stack trace of the ErrorResponse.  If the value is not found, act in
   * accordance with {@link com.linkedin.data.template.GetMode GetMode.STRICT}.
   *
   * @return a String
   */
  public String getStackTrace()
  {
    return getStackTrace(GetMode.STRICT);
  }

  /**
   * Set the stack trace.
   * @param value stack trace
   */
  public void setStackTrace(String value)
  {
    putDirect(FIELD_StackTrace, String.class, value);
  }

  /**
   * @return true if the ErrorResponse has error details, and false otherwise.
   */
  public boolean hasErrorDetails()
  {
    return data().get(ERROR_DETAILS) != null;
  }

  /**
   * Remove the error details from this ErrorResponse.
   */
  public void removeErrorDetails()
  {
    data().remove(ERROR_DETAILS);
  }

  /**
   * @return the error details
   */
  public DataMap getErrorDetails()
  {
    return (DataMap)data().get(ERROR_DETAILS);
  }

  /**
   * Set the error details.
   *
   * @param value error details
   */
  public void setErrorDetails(DataMap value)
  {
    data().put(ERROR_DETAILS, value);
  }


}
