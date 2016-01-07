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

package com.linkedin.multipart;


import com.linkedin.data.ByteString;
import com.linkedin.multipart.exceptions.MultiPartIllegalFormatException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;


/**
 * General purpose utility methods.
 *
 * @author Karim Vidhani
 */
public final class MultiPartMIMEUtils
{
  //R2 uses a case insensitive TreeMap so the casing here for the Content-Type header does not matter
  public static final String CONTENT_TYPE_HEADER = "Content-Type";
  public static final String MULTIPART_PREFIX = "multipart/";
  public static final String BOUNDARY_PARAMETER = "boundary";
  public static final byte SPACE_BYTE = 32;
  public static final byte TAB_BYTE = 9;

  public static final String CRLF_STRING = "\r\n";
  public static final byte[] CRLF_BYTES = "\r\n".getBytes();
  public static final byte[] CONSECUTIVE_CRLFS_BYTES = "\r\n\r\n".getBytes();
  public static final ByteString BYTE_STRING_CRLF_BYTES = ByteString.copy(CRLF_BYTES);
  public static final ByteString BYTE_STRING_CONSECUTIVE_CRLFS_BYTES = ByteString.copy(CONSECUTIVE_CRLFS_BYTES);

  private static final char[] MULTIPART_CHARS =
      "-_1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

  static void serializeHeaders(final Map<String, String> headers, final ByteArrayOutputStream outputStream)
      throws IOException
  {

    final StringBuilder headerBuffer = new StringBuilder();
    for (final Map.Entry<String, String> header : headers.entrySet())
    {
      headerBuffer.append(formattedHeader(header.getKey(), header.getValue()));
    }

    //Headers should always be 7 bit ASCII according to the RFC. If characters provided in the header
    //are do not constitute a valid ASCII character, then this Charset will place (U+FFFD) or the
    //replacement character which is used to replace an unknown or unrepresentable character.
    outputStream.write(headerBuffer.toString().getBytes(Charset.forName("US-ASCII")));
  }

  static String formattedHeader(final String name, final String value)
  {
    return ((name == null ? "" : name) + ": " + (null == value ? "" : value) + CRLF_STRING);
  }

  static String generateBoundary()
  {
    final StringBuilder buffer = new StringBuilder();
    final Random rand = new Random();
    //The RFC limit is 70 characters, so we will create a boundary that is randomly
    //between 50 to 60 characters. This should ensure that we never see the boundary within the request
    final int count = rand.nextInt(11) + 50; // a random size from 50 to 60
    for (int i = 0; i < count; i++)
    {
      buffer.append(MULTIPART_CHARS[rand.nextInt(MULTIPART_CHARS.length)]);
    }
    //RFC 2046 states a limited character set for the boundary but we don't have to explicitly encode to ASCII
    //since Unicode is backward compatible with ASCII
    return buffer.toString();
  }

  static String buildMIMEContentTypeHeader(final String mimeType, final String boundary,
      final Map<String, String> contentTypeParameters)
  {
    final StringBuilder contentTypeBuilder = new StringBuilder();
    contentTypeBuilder.append(MULTIPART_PREFIX).append(mimeType);
    //As per the RFC, parameters of the Content-Type header are separated by semi colons
    contentTypeBuilder.append("; ").append(BOUNDARY_PARAMETER).append("=").append(boundary);

    for (final Map.Entry<String, String> parameter : contentTypeParameters.entrySet())
    {
      //Note we ignore the provided boundary parameter
      if (!parameter.getKey().trim().equalsIgnoreCase(BOUNDARY_PARAMETER))
      {
        contentTypeBuilder.append("; ").append(parameter.getKey().trim()).append("=")
            .append(parameter.getValue().trim());
      }
    }

    return contentTypeBuilder.toString();
  }

  static String extractBoundary(final String contentTypeHeader) throws MultiPartIllegalFormatException
  {
    if (!contentTypeHeader.toLowerCase().startsWith(MultiPartMIMEUtils.MULTIPART_PREFIX))
    {
      throw new MultiPartIllegalFormatException(
          "Malformed multipart mime request. Not a valid multipart mime header.");
    }

    if (!contentTypeHeader.contains(";"))
    {
      throw new MultiPartIllegalFormatException(
          "Malformed multipart mime request. Improperly formatted Content-Type header. "
              + "Expected at least one parameter in addition to the content type.");
    }

    final String[] contentTypeParameters = contentTypeHeader.split(";");

    //In case someone used something like bOuNdArY
    final Map<String, String> parameterMap = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
    for (final String parameter : contentTypeParameters)
    {
      //We don't need the first bit here.
      if (parameter.startsWith(MULTIPART_PREFIX))
      {
        continue;
      }

      final String trimmedParameter = parameter.trim();
      //According to the RFC, there could be an '=' character in the boundary so we can't just split on =.
      //We find the first equals and then go from there. It should also be noted that the RFC does allow
      //boundaries to start and end with quotes.
      final int firstEquals = trimmedParameter.indexOf("=");
      //We throw an exception if there is no equals sign, or if the equals is the first character or if the
      //equals is the last character.
      if (firstEquals == 0 || firstEquals == -1 || firstEquals == trimmedParameter.length() - 1)
      {
        throw new MultiPartIllegalFormatException("Invalid parameter format.");
      }

      final String parameterKey = trimmedParameter.substring(0, firstEquals);
      String parameterValue = trimmedParameter.substring(firstEquals + 1, trimmedParameter.length());
      if (parameterValue.charAt(0) == '"')
      {
        if (parameterValue.charAt(parameterValue.length() - 1) != '"')
        {
          throw new MultiPartIllegalFormatException("Invalid parameter format.");
        }
        //Remove the leading and trailing '"'
        parameterValue = parameterValue.substring(1, parameterValue.length() - 1);
      }

      //The purpose of storing all of the parameters we have seen in the map is so that we can detect the case when
      //there are multiple boundary parameters.
      if (parameterMap.containsKey(parameterKey))
      {
        throw new MultiPartIllegalFormatException(
            "Invalid parameter format. Multiple declarations of the same parameter!");
      }
      parameterMap.put(parameterKey, parameterValue);
    }

    final String boundaryValue = parameterMap.get(BOUNDARY_PARAMETER);

    if (boundaryValue == null)
    {
      throw new MultiPartIllegalFormatException("No boundary parameter found!");
    }

    return boundaryValue;
  }

  //Note: This could be optimized to pass in a ByteArrayOutputStream so that we don't reallocate a new one
  //each time.
  static ByteString serializeBoundaryAndHeaders(final byte[] normalEncapsulationBoundary,
      final MultiPartMIMEDataSourceWriter dataSource) throws IOException
  {
    final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

    //Write the headers out for this new part
    byteArrayOutputStream.write(normalEncapsulationBoundary);
    byteArrayOutputStream.write(MultiPartMIMEUtils.CRLF_BYTES);

    if (!dataSource.dataSourceHeaders().isEmpty())
    {
      //Serialize the headers
      serializeHeaders(dataSource.dataSourceHeaders(), byteArrayOutputStream);
    }

    //Regardless of whether or not there were headers the RFC calls for another CRLF here.
    //If there were no headers we end up with two CRLFs after the boundary
    //If there were headers CRLF_BYTES we end up with one CRLF after the boundary and one after the last header
    byteArrayOutputStream.write(MultiPartMIMEUtils.CRLF_BYTES);

    return ByteString.unsafeWrap(byteArrayOutputStream.toByteArray());
  }
}