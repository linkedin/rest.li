/*
   Copyright (c) 2015 LinkedIn Corp.

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

package com.linkedin.restli.internal.common;


import com.linkedin.restli.common.RestConstants;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;


/**
 * Utility used to get content type based on the mime type
 */
public class ContentTypeUtil
{
  /**
   * Type of content supported by Restli
   */
  public enum ContentType
  {
    PSON,
    JSON
  }

  /**
   * Get content type based on the given mime type
   * @param contentTypeHeaderValue value of Content-Type header.
   * @return type of content Restli supports.  Currently only JSON and PSON are supported.
   * @throws MimeTypeParseException throws this exception when content type is not parsable.
   */
  public static ContentType getContentType(String contentTypeHeaderValue) throws MimeTypeParseException
  {
    if (contentTypeHeaderValue == null)
    {
      return ContentType.JSON;
    }
    MimeType parsedMimeType = new MimeType(contentTypeHeaderValue);
    if (parsedMimeType.getBaseType().equalsIgnoreCase(RestConstants.HEADER_VALUE_APPLICATION_PSON))
    {
      return ContentType.PSON;
    }
    else
    {
      return ContentType.JSON;
    }
  }
}
