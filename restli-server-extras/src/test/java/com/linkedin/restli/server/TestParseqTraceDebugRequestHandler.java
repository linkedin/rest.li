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


import com.linkedin.common.callback.Callback;
import com.linkedin.parseq.trace.Trace;
import com.linkedin.parseq.trace.codec.json.JsonTraceCodec;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.r2.message.rest.RestResponse;
import com.linkedin.restli.common.RestConstants;

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

import junit.framework.Assert;
import org.easymock.EasyMock;
import org.testng.annotations.Test;


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
        new Callback<RestResponse>()
        {
          @Override
          public void onError(Throwable e)
          {
            Assert.fail("Request execution failed unexpectedly.");
          }

          @Override
          public void onSuccess(RestResponse result)
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
        new Callback<RestResponse>()
        {
          @Override
          public void onError(Throwable e)
          {
            Assert.fail("Request execution failed unexpectedly.");
          }

          @Override
          public void onSuccess(RestResponse result)
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
          new Callback<RestResponse>()
          {
            @Override
            public void onError(Throwable e)
            {
              Assert.fail("Static content cannot be retrieved for " + uri.toString());
            }

            @Override
            public void onSuccess(RestResponse result)
            {
              Assert.assertEquals(result.getHeader(RestConstants.HEADER_CONTENT_TYPE), mimeType);
            }
          });
    }
  }

  private void executeRequestThroughParseqDebugHandler(URI uri, Callback<RestResponse> callback)
  {
    ParseqTraceDebugRequestHandler requestHandler = new ParseqTraceDebugRequestHandler();
    RestRequestBuilder requestBuilder = new RestRequestBuilder(uri);
    RestRequest request = requestBuilder.build();
    RequestContext requestContext = new RequestContext();

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

                                      callback.onSuccess(response, executionReportBuilder.build());
                                    }
                                  },
                                  callback);
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
