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

/* $Id$ */
package com.linkedin.r2.caprep.db;

import com.linkedin.data.ByteString;
import com.linkedin.r2.message.MessageHeaders;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.Response;
import com.linkedin.r2.message.MessageHeadersBuilder;
import com.linkedin.r2.message.rest.RestMessage;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.r2.transport.http.common.HttpConstants;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * The default serializer for messages. This serializer has two goals: 1) generate pseudo-HTTP 1.1
 * requests and responses (as defined in RFC-2616) and 2) make it convenient to work with the
 * resulting messages using standard unix tools. When #1 and #2 are in conflict, favor #2.<p/>
 *
 * Ultimately, TestDefaultMessageSerializer, in the test module, defines the behavior for this
 * class. If a particular case is not handled in TestDefaultMessageSerializer it is safe to assume
 * the behavior is undefined. The following paragraphs describe some of the behavior of this class.
 * <p/>
 *
 * Messages written out are nearly RFC-2616 conformant, with the following specifics:<p/>
 *
 * <ul>
 *  <li>RFC-2616 field-values can be split across multiple lines. We use a transform described in
 *      RFC-2616 to collapse multiple lines into a single line.</li>
 *  <li>We do no special handling for invalid character sequences in field-values.</li>
 *  <li>We insert a CRLF at the end of the entity to make it easier to use with unix tools and
 *      editors.</li>
 * </ul>
 *
 * Messages read in have the following specifics:<p/>
 *
 * <ul>
 *  <li>end-of-line is looser than the RFC-2616 spec: we only look for LF and we ignore all CR's.</li>
 *  <li>Leading and trailing whitespace in a field-value are stripped, which is allowed for in
 *      RFC-2616.</li>
 *  <li>A trailing LF or CRLF at the end of an entity will be stripped (see above where we add an
 *      extract CRLF).</li>
 * </ul>
 *
 * @author Chris Pettitt
 * @version $Revision$
 */
public class DefaultMessageSerializer implements MessageSerializer
{
  private static final Charset CHARSET = Charset.forName("ASCII");
  private static final char CR_CHAR = '\r';
  private static final char LF_CHAR = '\n';
  private static final String CR = Character.toString(CR_CHAR);
  private static final String LF = Character.toString(LF_CHAR);
  private static final String CRLF = CR + LF;
  private static final char SP_CHAR = ' ';
  private static final String SP = Character.toString(SP_CHAR);
  private static final String HT = "\t";
  private static final String POST = "POST";
  private static final String HTTP_1_1 = "HTTP/1.1";
  private static final String STATUS_200 = "200";

  @Override
  public void writeRequest(OutputStream out, RestRequest req) throws IOException
  {
    writeReqLine(out, req);

    writeHeaders(out, req, HttpConstants.REQUEST_COOKIE_HEADER_NAME);

    writeEntity(out, req);
  }

  @Override
  public void writeResponse(OutputStream out, RestResponse res) throws IOException
  {
    writeResLine(out, res);

    writeHeaders(out, res, HttpConstants.RESPONSE_COOKIE_HEADER_NAME);

    writeEntity(out, res);
  }

  @Override
  public RestRequest readRestRequest(InputStream in) throws IOException
  {
    final RestRequestBuilder builder = new RestRequestBuilder(URI.create(""));
    readReqLine(builder, in);
    readHeaders(builder, in, HttpConstants.REQUEST_COOKIE_HEADER_NAME);
    builder.setEntity(readEntity(in));

    return builder.build();
  }

  @Override
  public RestResponse readRestResponse(InputStream in) throws IOException
  {
    final RestResponseBuilder builder = new RestResponseBuilder();
    readResLine(builder, in);
    readHeaders(builder, in, HttpConstants.RESPONSE_COOKIE_HEADER_NAME);
    builder.setEntity(readEntity(in));
    return builder.build();
  }

  private void readReqLine(RestRequestBuilder builder, InputStream in) throws IOException
  {
    builder.setMethod(readUntil(SP_CHAR, in));
    builder.setURI(URI.create(readUntil(SP_CHAR, in)));
    readIgnore(HTTP_1_1, in);
    readIgnoreNewLine(in);
  }

  private void readResLine(RestResponseBuilder builder, InputStream in) throws IOException
  {
    readIgnore(HTTP_1_1, in);
    readIgnore(SP, in);
    try
    {
      final String statusStr = readUntil(SP_CHAR, in);
      builder.setStatus(Integer.parseInt(statusStr));
    }
    catch (NumberFormatException e)
    {
      throw new IOException("Failed to parse HTTP status code", e);
    }
    readIgnoreLine(in);
  }

  private void readHeaders(MessageHeadersBuilder<?> builder, InputStream in, String cookieHeader) throws IOException
  {
    String line = readLine(in);
    while (!line.isEmpty())
    {
      final int sep = line.indexOf(':');
      if (sep == -1)
      {
        throw new IOException("Parsing header failed. Expected ':' in: " + printable(line));
      }
      final String key = line.substring(0, sep);
      final StringBuilder valueBuilder = new StringBuilder(line.substring(sep + 1));

      // Is next line a continuation?
      for (line = readLine(in); line.startsWith(SP) || line.startsWith(HT); line = readLine(in))
      {
        valueBuilder.append(line);
      }

      if (key.equalsIgnoreCase(cookieHeader)) {
        builder.addCookie(valueBuilder.toString().trim());
      } else {
        builder.addHeaderValue(key, valueBuilder.toString().trim());
      }
    }
  }

  private ByteString readEntity(InputStream in) throws IOException
  {
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    final byte[] buf = new byte[1024];
    int bytesRead;
    while ((bytesRead = in.read(buf)) != -1)
    {
      out.write(buf, 0, bytesRead);
    }

    final byte[] eb = out.toByteArray();
    // Strip off the last CRLF. We added this in writeEntity for convenience when working with
    // other tools.
    final ByteString entity;
    if (eb.length >= 2 && eb[eb.length-2] == CR_CHAR && eb[eb.length-1] == LF_CHAR)
    {
      entity = ByteString.copy(eb, 0, eb.length - 2);
    }
    else
    {
      entity = ByteString.copy(eb);
    }

    return entity;
  }

  private void writeReqLine(OutputStream out, Request req) throws IOException
  {
    if (req instanceof RestRequest)
    {
      write(out, ((RestRequest)req).getMethod());
    }
    else
    {
      write(out, POST);
    }

    write(out, SP);
    write(out, req.getURI().toASCIIString());
    write(out, SP);
    write(out, HTTP_1_1);
    write(out, CRLF);
  }

  private void writeResLine(OutputStream out, Response res) throws IOException
  {
    write(out, HTTP_1_1);
    write(out, SP);

    int statusCode;
    if (res instanceof RestResponse)
    {
      statusCode = ((RestResponse) res).getStatus();
      write(out, Integer.toString(statusCode));
    }
    else
    {
      statusCode = 200;
      write(out, STATUS_200);
    }
    write(out, SP);


    write(out, toReasonPhrase(statusCode));
    write(out, CRLF);
  }

  /**
   * Converts all status codes defined by the official http spec to their official reason phrase.  Unrecognized status
   * codes will be converted to "No Reason Phrase", which is safe to do per RFC-2616.
   */
  private String toReasonPhrase(int status)
  {
    switch(status)
    {
      case 200: return "OK";
      case 201: return "Created";
      case 202: return "Accepted";
      case 203: return "Non-Authoritative Information";
      case 204: return "No Content";
      case 205: return "Reset Content";
      case 206: return "Partial Content";
      case 300: return "Multiple Choices";
      case 301: return "Moved Permanently";
      case 302: return "Found";
      case 303: return "See Other";
      case 304: return "Not Modified";
      case 305: return "Use Proxy";
      case 306: return "Switch Proxy";
      case 307: return "Temporary Redirect";
      case 400: return "Bad Request";
      case 401: return "Unauthorized";
      case 402: return "Payment Required";
      case 403: return "Forbidden";
      case 404: return "Not Found";
      case 405: return "Method Not Allowed";
      case 406: return "Not Acceptable";
      case 407: return "Proxy Authentication Required";
      case 408: return "Request Timeout";
      case 409: return "Conflict";
      case 410: return "Gone";
      case 411: return "Length Required";
      case 412: return "Precondition Failed";
      case 413: return "Request Entity Too Large";
      case 414: return "Request-URI Too Long";
      case 415: return "Unsupported Media Type";
      case 416: return "Requested Range Not Satisfiable";
      case 417: return "Expectation Failed";
      case 500: return "Internal Server Error";
      case 501: return "Not Implemented";
      case 502: return "Bad Gateway";
      case 503: return "Service Unavailable";
      case 504: return "Gateway Timeout";
      case 505: return "HTTP Version Not Supported";
      default: return "No Reason Phrase"; // For any cases not covered, this is okay, since we can write whatever we like here per RFC-2616
    }
  }

  private void writeHeaders(OutputStream out, MessageHeaders msg, String cookieHeader) throws IOException
  {
    for (Map.Entry<String, String> hdr : msg.getHeaders().entrySet())
    {
      writeHeader(out, hdr.getKey(), hdr.getValue());
    }
    for (String cookie : msg.getCookies())
    {
      writeHeader(out, cookieHeader, cookie);
    }
    write(out, CRLF);
  }

  private void writeHeader(OutputStream out, String key, String value) throws IOException
  {
    write(out, key);

    write(out, ":");
    // Not required per spec, but improves readability
    write(out, SP);

    // Replace CR/LF with SP, acceptable per RFC-2616
    write(out, value.replaceAll("[\n\r]+", " "));

    write(out, CRLF);
  }

  private void readIgnore(String expectedStr, InputStream in) throws IOException
  {
    final int bytesExpected = expectedStr.getBytes(CHARSET).length;
    final byte[] actualBytes = new byte[bytesExpected];
    final int bytesRead = in.read(actualBytes);
    final String actualStr = new String(actualBytes, 0, bytesRead, CHARSET);

    if (!actualStr.equalsIgnoreCase(expectedStr))
    {
      // Fancier parser would give line number and column...
      throw new IOException("Parse failed. Expected: " + printable(expectedStr) + ". Actual: " + printable(actualStr));
    }
  }

  private String readUntil(char ch, InputStream in) throws IOException
  {
    // This is safe only because we assume ch is ASCII
    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    int nextByte;
    while ((nextByte = in.read()) != ch)
    {
      if (nextByte == -1)
      {
        throw new IOException("Parse failed. End of stream before reaching expected char: " + printable(ch));
      }

      out.write(nextByte);
    }
    return new String(out.toByteArray(), CHARSET);
  }

  private void readIgnoreNewLine(InputStream in) throws IOException
  {
    // Per RFc-2616 everything except the entity must use CRLF to end a line. We relax that
    // requirement to make it easier to manipulate capture files in various Mac and Linux editors.
    char ch = (char)in.read();
    if (ch == CR_CHAR)
    {
      ch = (char)in.read();
    }
    if (ch == -1)
    {
      throw new IOException("Expected end-of-line but got EOF");
    }
    if (ch != LF_CHAR)
    {
      throw new IOException("Expected LF (0x0A) but got EOF");
    }
  }

  private void readIgnoreLine(InputStream in) throws IOException
  {
    readLine(in);
  }

  private String readLine(InputStream in) throws IOException
  {
    // Our strategy for passing lines is to read until we hit LF and ignore any CR's along the way.
    // This is not strictly valid HTTP/1.1 (except for entities), but it makes life easier when
    // editing capture files in most editors on Mac and Linux.
    return readUntil(LF_CHAR, in).replaceAll(CR, "");
  }

  private void writeEntity(OutputStream out, RestMessage res) throws IOException
  {
    res.getEntity().write(out);

    // Append final CRLF to make it easier to modify the files in a text editor. This is not
    // compatible with the HTTP spec, but should work for most of our use cases.
    write(out, CRLF);
  }

  private void write(OutputStream out, String text) throws IOException
  {
    out.write(text.getBytes(CHARSET));
  }

  private String printable(String str)
  {
    final StringBuilder sb = new StringBuilder();
    for (byte b : str.getBytes(CHARSET))
    {
      sb.append(printable((char)b));
    }
    return sb.toString();
  }

  private String printable(char ch)
  {
    if (ch >= 32 && ch <= 126)
    {
      return Character.toString(ch);
    }
    return " (0x" + Integer.toHexString(ch) + ") ";
  }
}
