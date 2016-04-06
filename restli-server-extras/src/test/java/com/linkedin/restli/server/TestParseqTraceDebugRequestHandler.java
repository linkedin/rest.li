/*
   Copyright (c) 2014 LinkedIn Corp.

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


import com.linkedin.data.ByteString;
import com.linkedin.multipart.MultiPartMIMEReader;
import com.linkedin.multipart.MultiPartMIMEReaderCallback;
import com.linkedin.multipart.MultiPartMIMEStreamRequestFactory;
import com.linkedin.multipart.MultiPartMIMEWriter;
import com.linkedin.parseq.trace.Trace;
import com.linkedin.parseq.trace.codec.json.JsonTraceCodec;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestException;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.r2.message.stream.StreamRequest;
import com.linkedin.r2.message.stream.entitystream.ByteStringWriter;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.common.attachments.RestLiAttachmentReader;
import com.linkedin.restli.internal.common.AttachmentUtils;
import com.linkedin.restli.internal.testutils.RestLiTestAttachmentDataSource;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.testng.annotations.Test;

import junit.framework.Assert;
import org.easymock.EasyMock;


public class TestParseqTraceDebugRequestHandler
{
  private static final String TEST_TRACE =
      "{" +
          "\"traces\":" +
            "[" +
              "{" +
                "\"id\":0," +
                "\"name\":\"com.linkedin.restli.examples.greetings.server.GreetingsResourceTask$1@20fc40ae\"," +
                "\"resultType\":\"SUCCESS\"," +
                "\"hidden\":false," +
                "\"systemHidden\":false," +
                "\"value\":\"{message=Good morning!, id=1, tone=FRIENDLY}\"," +
                "\"startNanos\":1738823237485217," +
                "\"pendingNanos\":1738823238898104," +
                "\"endNanos\":1738823239389873" +
              "}" +
            "]," +
          "\"relationships\":[]" +
      "}";
  private static final String HEADER_VALUE_TEXT_HTML = "text/html";
  private static final String HEADER_VALUE_TEXT_CSS = "text/css";
  private static final String HEADER_VALUE_APPLICATION_JS = "application/javascript";
  private static final String HEADER_VALUE_APPLICATION_JSON = "application/json";
  private static final String HEADER_VALUE_IMAGE_PNG = "image/png";

  /**
   * Tests tracevis visualization debug request.
   */
  @Test
  public void testTracevisRequest()
  {
    executeRequestThroughParseqDebugHandler(
            URI.create("http://host/abc/12/__debug/parseqtrace/tracevis"),
            new RequestExecutionCallback<RestResponse>()
            {
              @Override
              public void onError(Throwable e, RequestExecutionReport executionReport,
                                  RestLiAttachmentReader requestAttachmentReader,
                                  RestLiResponseAttachments responseAttachments)
              {
                Assert.fail("Request execution failed unexpectedly.");
              }

              @Override
              public void onSuccess(RestResponse result, RequestExecutionReport executionReport,
                                    RestLiResponseAttachments responseAttachments)
              {
                Assert.assertEquals(result.getHeader(RestConstants.HEADER_CONTENT_TYPE), HEADER_VALUE_TEXT_HTML);
              }
            });
  }

  /**
   * Tests raw debug request
   */
  @Test
  public void testRawRequest()
  {
    executeRequestThroughParseqDebugHandler(
            URI.create("http://host/abc/12/__debug/parseqtrace/raw"),
            new RequestExecutionCallback<RestResponse>()
            {
              @Override
              public void onError(Throwable e, RequestExecutionReport executionReport,
                                  RestLiAttachmentReader requestAttachmentReader,
                                  RestLiResponseAttachments responseAttachments)
              {
                Assert.fail("Request execution failed unexpectedly.");
              }

              @Override
              public void onSuccess(RestResponse result, RequestExecutionReport executionReport,
                                    RestLiResponseAttachments responseAttachments)
              {
                Assert.assertEquals(result.getHeader(RestConstants.HEADER_CONTENT_TYPE), HEADER_VALUE_APPLICATION_JSON);
                String traceJson = result.getEntity().asString(Charset.forName("UTF-8"));
                Assert.assertEquals(traceJson, TEST_TRACE);
              }
            });
  }

  /**
   * Tests the static content retrieval from the parseq trace debug request handler. It enumerates through all
   * files imported into the JAR containing the parseq trace debug request handler, skips the ones that should
   * not be served and verifies the rest can be retrieved. This test makes sure all files we import are actually
   * servicable by the parseq trace debug request handler.
   * @throws IOException
   */
  @Test
  public void testStaticContent() throws IOException
  {
    ClassLoader classLoader = ParseqTraceDebugRequestHandler.class.getClassLoader();

    //Collect all files under tracevis folder in the jar containing the parseq trace debug request handler.
    Enumeration<URL> resources = classLoader.getResources(
        ParseqTraceDebugRequestHandler.class.getName().replace('.', '/') + ".class");
    List<String> files = new ArrayList<String>();

    while (resources.hasMoreElements())
    {
      URL url = resources.nextElement();
      URLConnection urlConnection = url.openConnection();

      if (urlConnection instanceof JarURLConnection)
      {
        JarURLConnection jarURLConnection = (JarURLConnection) urlConnection;
        JarFile jar = jarURLConnection.getJarFile();

        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements())
        {
          JarEntry currentEntry = entries.nextElement();

          if (!currentEntry.isDirectory())
          {
            String entry = currentEntry.getName();
            if (entry.startsWith("tracevis/"))
            {
              files.add(entry);
            }
          }
        }
      }
    }

    Assert.assertTrue(files.size() > 0);

    //Go through the tracevis files and skip over the known metadata files
    // All other files should be retrievable from the parseq trace debug request handler.
    for (String file : files)
    {
      final String mimeType = determineMediaType(file);
      final URI uri = URI.create("http://host/abc/12/__debug/parseqtrace/" +
                               file.substring(file.indexOf('/') + 1));

      executeRequestThroughParseqDebugHandler(
              uri,
              new RequestExecutionCallback<RestResponse>()
              {
                @Override
                public void onError(Throwable e, RequestExecutionReport executionReport,
                                    RestLiAttachmentReader requestAttachmentReader,
                                    RestLiResponseAttachments responseAttachments)
                {
                  Assert.fail("Static content cannot be retrieved for " + uri.toString());
                }

                @Override
                public void onSuccess(RestResponse result, RequestExecutionReport executionReport,
                                      RestLiResponseAttachments responseAttachments)
                {
                  Assert.assertEquals(result.getHeader(RestConstants.HEADER_CONTENT_TYPE), mimeType);
                }
              });
    }
  }

  private void executeRequestThroughParseqDebugHandler(URI uri, RequestExecutionCallback<RestResponse> callback)
  {
    ParseqTraceDebugRequestHandler requestHandler = new ParseqTraceDebugRequestHandler();
    RestRequestBuilder requestBuilder = new RestRequestBuilder(uri);
    RestRequest request = requestBuilder.build();
    RequestContext requestContext = new RequestContext();

    requestHandler.handleRequest(request, requestContext, new RestLiDebugRequestHandler.ResourceDebugRequestHandler()
                                 {
                                   @Override
                                   public void handleRequest(RestRequest request, RequestContext requestContext,
                                                             RequestExecutionCallback<RestResponse> callback)
                                   {
                                     RestResponse response = EasyMock.createMock(RestResponse.class);
                                     RequestExecutionReportBuilder executionReportBuilder =
                                         new RequestExecutionReportBuilder();
                                     JsonTraceCodec jsonTraceCodec = new JsonTraceCodec();
                                     Trace t = null;

                                     try
                                     {
                                       t = jsonTraceCodec.decode(TEST_TRACE);
                                       executionReportBuilder.setParseqTrace(t);
                                     }
                                     catch (IOException exc)
                                     {
                                       //test will fail later
                                     }

                                     callback.onSuccess(response, executionReportBuilder.build(), null);
                                   }
                                 }, null, callback);
  }

  @Test
  public void testResponseStreamingAttachmentsForbidden()
  {
    //This test verifies that the ParseqTraceDebugRequestHandler aborts any potential outgoing response attachments
    //set by a resource method.

    final URI uri = URI.create("http://host/abc/12/__debug/parseqtrace/raw");
    ParseqTraceDebugRequestHandler requestHandler = new ParseqTraceDebugRequestHandler();
    RestRequestBuilder requestBuilder = new RestRequestBuilder(uri);
    RestRequest request = requestBuilder.build();
    RequestContext requestContext = new RequestContext();

    final RequestExecutionCallback<RestResponse> callback = new RequestExecutionCallback<RestResponse>()
    {
      @Override
      public void onError(Throwable e, RequestExecutionReport executionReport,
                          RestLiAttachmentReader requestAttachmentReader,
                          RestLiResponseAttachments responseAttachments)
      {
        Assert.fail("Request execution failed unexpectedly.");
      }

      @Override
      public void onSuccess(RestResponse result, RequestExecutionReport executionReport,
                            RestLiResponseAttachments responseAttachments)
      {
      }
    };

    final RestLiTestAttachmentDataSource testAttachmentDataSource = RestLiTestAttachmentDataSource.createWithRandomPayload("1");

    final RestLiResponseAttachments responseAttachments =
            new RestLiResponseAttachments.Builder().appendSingleAttachment(testAttachmentDataSource).build();
    requestHandler.handleRequest(request,
            requestContext,
            new RestLiDebugRequestHandler.ResourceDebugRequestHandler()
            {
              @Override
              public void handleRequest(RestRequest request,
                                        RequestContext requestContext,
                                        RequestExecutionCallback<RestResponse> callback)
              {
                RestResponse response = EasyMock.createMock(RestResponse.class);

                //Provide some attachments that should be aborted.
                callback.onSuccess(response, new RequestExecutionReportBuilder().build(), responseAttachments);
              }
            },
            null,
            callback);

    Assert.assertTrue(testAttachmentDataSource.dataSourceAborted());
  }

  @Test
  public void testErrorStreamingAbsorbRequestAbortResponse() throws Exception
  {
    //This test verifies that in the face of an error, the ParseqTraceDebugRequestHandler aborts any potential outgoing
    //response attachments and absorbs and drops on the ground any incoming request attachments.
    final URI uri = URI.create("http://host/abc/12/__debug/parseqtrace/raw");
    ParseqTraceDebugRequestHandler requestHandler = new ParseqTraceDebugRequestHandler();
    RestRequestBuilder requestBuilder = new RestRequestBuilder(uri);
    RestRequest request = requestBuilder.build();
    RequestContext requestContext = new RequestContext();

    final RequestExecutionCallback<RestResponse> callback = new RequestExecutionCallback<RestResponse>()
    {
      @Override
      public void onError(Throwable e, RequestExecutionReport executionReport,
                          RestLiAttachmentReader requestAttachmentReader,
                          RestLiResponseAttachments responseAttachments)
      {
        //Even though we call callback.onError() below to simulate a failed rest.li request execution, the
        //ParseqTraceDebugRequestHandler eventually treats this as a success.
        Assert.fail("Request execution passed unexpectedly.");
      }

      @Override
      public void onSuccess(RestResponse result, RequestExecutionReport executionReport,
                            RestLiResponseAttachments responseAttachments)
      {
      }
    };

    //Create request and response attachments. This is not the wire protocol for rest.li streaming, but
    //makes this test easier to write and understand.

    //Response attachment.
    final RestLiTestAttachmentDataSource responseDataSource = RestLiTestAttachmentDataSource.createWithRandomPayload("1");

    final RestLiResponseAttachments responseAttachments =
            new RestLiResponseAttachments.Builder().appendSingleAttachment(responseDataSource).build();

    //Request attachment. We need to create a reader here.
    final RestLiTestAttachmentDataSource requestAttachment = RestLiTestAttachmentDataSource.createWithRandomPayload("2");

    final MultiPartMIMEWriter.Builder builder = new MultiPartMIMEWriter.Builder();
    AttachmentUtils.appendSingleAttachmentToBuilder(builder, requestAttachment);
    final ByteStringWriter byteStringWriter =
            new ByteStringWriter(ByteString.copyString("Normal rest.li request payload", Charset.defaultCharset()));
    final MultiPartMIMEWriter writer = AttachmentUtils.createMultiPartMIMEWriter(byteStringWriter, "application/json", builder);

    final StreamRequest streamRequest =
            MultiPartMIMEStreamRequestFactory.generateMultiPartMIMEStreamRequest(new URI(""), "related", writer);

    final MultiPartMIMEReader reader = MultiPartMIMEReader.createAndAcquireStream(streamRequest);
    final RestLiAttachmentReader attachmentReader = new RestLiAttachmentReader(reader);

    //Absorb the first part as rest.li server does and then give the beginning of the next part to the
    //ParseqTraceDebugRequestHandler.
    final MultiPartMIMEReaderCallback multiPartMIMEReaderCallback = new MultiPartMIMEReaderCallback()
    {
      int partCounter = 0;
      @Override
      public void onNewPart(MultiPartMIMEReader.SinglePartMIMEReader singlePartMIMEReader)
      {
        if (partCounter == 0)
        {
          singlePartMIMEReader.drainPart();
          partCounter ++;
        }
        else
        {
          //When the first part is read in, at the beginning of the 2nd part, we move to the debug request handler.
          requestHandler.handleRequest(request,
                  requestContext,
                  new RestLiDebugRequestHandler.ResourceDebugRequestHandler()
                  {
                    @Override
                    public void handleRequest(RestRequest request,
                                              RequestContext requestContext,
                                              RequestExecutionCallback<RestResponse> callback)
                    {
                      callback.onError(RestException.forError(500, "An error has occurred"),
                                       new RequestExecutionReportBuilder().build(),
                                       attachmentReader, responseAttachments);
                    }
                  },
                  attachmentReader,
                  callback);
        }
      }

      @Override
      public void onFinished()
      {
        Assert.fail();
      }

      @Override
      public void onDrainComplete()
      {
        //Eventually this should occur.
      }

      @Override
      public void onStreamError(Throwable throwable)
      {
        Assert.fail();
      }
    };

    reader.registerReaderCallback(multiPartMIMEReaderCallback);

    //The response data source should have been aborted.
    Assert.assertTrue(responseDataSource.dataSourceAborted());

    //The request attachment should have been absorbed and finished.
    Assert.assertTrue(requestAttachment.finished());
  }

  private static String determineMediaType(String path)
  {
    int extensionIndex = path.lastIndexOf('.');

    if (extensionIndex != -1)
    {
      String extension = path.substring(extensionIndex);

      if (extension.equals(".html"))
      {
        return HEADER_VALUE_TEXT_HTML;
      }
      else if (extension.equals(".js"))
      {
        return HEADER_VALUE_APPLICATION_JS;
      }
      else if (extension.equals(".css"))
      {
        return HEADER_VALUE_TEXT_CSS;
      }
      else if (extension.equals(".png"))
      {
        return HEADER_VALUE_IMAGE_PNG;
      }
    }

    return null;
  }
}