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
import com.linkedin.r2.message.Message;
import com.linkedin.r2.message.MessageBuilder;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.RequestBuilder;
import com.linkedin.r2.message.Response;
import com.linkedin.r2.message.ResponseBuilder;
import com.linkedin.r2.message.rest.RestMessage;
import com.linkedin.r2.message.rest.RestMessageBuilder;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.rest.RestResponseBuilder;
import com.linkedin.r2.message.rpc.RpcRequest;
import com.linkedin.r2.message.rpc.RpcRequestBuilder;
import com.linkedin.r2.message.rpc.RpcResponse;
import com.linkedin.r2.message.rpc.RpcResponseBuilder;

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
  public void writeRequest(OutputStream out, Request req) throws IOException
  {
    writeReqLine(out, req);
    if (req instanceof RestRequest)
    {
      writeHeaders(out, (RestRequest)req);
    }
    else
    {
      write(out, CRLF);
    }
    writeEntity(out, req);
  }

  @Override
  public void writeResponse(OutputStream out, Response res) throws IOException
  {
    writeResLine(out, res);
    if (res instanceof RestResponse)
    {
      writeHeaders(out, (RestResponse)res);
    }
    else
    {
      write(out, CRLF);
    }
    writeEntity(out, res);
  }

  @Override
  public RestRequest readRestRequest(InputStream in) throws IOException
  {
    final RestRequestBuilder builder = new RestRequestBuilder(URI.create(""));
    readReqLine(builder, in);
    readHeaders(builder, in);
    readEntity(builder, in);
    return builder.build();
  }

  @Override
  public RpcRequest readRpcRequest(InputStream in) throws IOException
  {
    final RpcRequestBuilder builder = new RpcRequestBuilder(URI.create(""));
    readReqLine(builder, in);
    readIgnoreNewLine(in);
    readEntity(builder, in);
    return builder.build();
  }

  @Override
  public RestResponse readRestResponse(InputStream in) throws IOException
  {
    final RestResponseBuilder builder = new RestResponseBuilder();
    readResLine(builder, in);
    readHeaders(builder, in);
    readEntity(builder, in);
    return builder.build();
  }

  @Override
  public RpcResponse readRpcResponse(InputStream in) throws IOException
  {
    final RpcResponseBuilder builder = new RpcResponseBuilder();
    readResLine(builder, in);
    readIgnoreNewLine(in);
    readEntity(builder, in);
    return builder.build();
  }

  private void readReqLine(RequestBuilder builder, InputStream in) throws IOException
  {
    if (builder instanceof RestRequestBuilder)
    {
      ((RestRequestBuilder) builder).setMethod(readUntil(SP_CHAR, in));
    }
    else
    {
      readIgnore(POST, in);
      readIgnore(SP, in);
    }

    builder.setURI(URI.create(readUntil(SP_CHAR, in)));
    readIgnore(HTTP_1_1, in);
    readIgnoreNewLine(in);
  }

  private void readResLine(ResponseBuilder builder, InputStream in) throws IOException
  {
    readIgnore(HTTP_1_1, in);
    readIgnore(SP, in);
    if (builder instanceof RestResponseBuilder)
    {
      final String statusStr = readUntil(SP_CHAR, in);
      try
      {
        ((RestResponseBuilder)builder).setStatus(Integer.parseInt(statusStr));
      }
      catch (NumberFormatException e)
      {
        throw new IOException("Failed to parse HTTP status code", e);
      }
    }
    else
    {
      readIgnore(STATUS_200, in);
      readIgnore(SP, in);
    }
    readIgnoreLine(in);
  }

  private void readHeaders(RestMessageBuilder builder, InputStream in) throws IOException
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
      builder.addHeaderValue(key, valueBuilder.toString());
    }
  }

  private void readEntity(MessageBuilder builder, InputStream in) throws IOException
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
      entity = ByteString.copyString(new String(eb, 0, eb.length - 2), CHARSET);
    }
    else
    {
      entity = ByteString.copy(eb);
    }

    builder.setEntity(entity);
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

    if (res instanceof RestResponse)
    {
      write(out, Integer.toString(((RestResponse)res).getStatus()));
    }
    else
    {
      write(out, STATUS_200);
    }
    write(out, SP);

    // We can write whatever we like here per RFC-2616
    write(out, "No Reason Phrase");
    write(out, CRLF);
  }

  private void writeHeaders(OutputStream out, RestMessage msg) throws IOException
  {
    for (Map.Entry<String, String> hdr : msg.getHeaders().entrySet())
    {
      write(out, hdr.getKey());

      write(out, ":");
      // Not required per spec, but improves readability
      write(out, SP);

      // Replace CR/LF with SP, acceptable per RFC-2616
      write(out, hdr.getValue().replaceAll("[\n\r]+", " "));

      write(out, CRLF);
    }
    write(out, CRLF);
  }

  private void readIgnore(String expectedStr, InputStream in) throws IOException
  {
    // This is safe only because we've baked in the assumption of the ASCII charset.
    final int bytesExpected = expectedStr.length();
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
    final StringBuilder sb = new StringBuilder();
    int nextByte;
    while ((nextByte = in.read()) != ch)
    {
      if (nextByte == -1)
      {
        throw new IOException("Parse failed. End of stream before reaching expected char: " + printable(ch));
      }

      // Safe only because we assume ASCII charset.
      sb.append((char)nextByte);
    }
    return sb.toString();
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

  private void writeEntity(OutputStream out, Message res) throws IOException
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
    for (byte b : str.getBytes())
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
