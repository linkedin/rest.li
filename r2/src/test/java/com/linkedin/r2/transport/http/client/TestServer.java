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

/**
 * $Id: $
 */

package com.linkedin.r2.transport.http.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.StringTokenizer;

/**
 * @author Steven Ihde
 * @version $Revision: $
 */

public class TestServer
{
  private final Thread _thread;
  private final ServerSocket _serverSocket;
  private volatile String _lastRequest;

  public TestServer() throws IOException
  {
    _serverSocket = new ServerSocket();
    _serverSocket.bind(null);
    _thread = new Thread()
    {
      @Override
      public void run()
      {
        try
        {
          for (;;)
          {
            // Catch IOException from accept() in the OUTER loop so it terminates the thread
            Socket s = _serverSocket.accept();
            _lastRequest = null;
            try
            {
              // Use 8859-1 because it can reversibly encode/decode any sequence of octets.
              BufferedReader r = new BufferedReader(new InputStreamReader(s.getInputStream(), Charset.forName("ISO-8859-1")));
              String line = r.readLine();
              String response = line==null ? null : getResponse(line);

              if (response != null)
              {
                s.getOutputStream().write(response.getBytes());

                // Close the server->client half of the connection
                s.shutdownOutput();

                // Keep reading from the client->server half of the connection; if we close
                // it prematurely (before client finishes writing the request) client may receive
                // a RST; gather all the input in case the client wants it.
                StringBuilder sb = new StringBuilder(line);
                while ((line = r.readLine()) != null)
                  sb.append(line);
                _lastRequest = sb.toString();
                s.close();
              }
            }
            catch (IOException e)
            {
              // Probably unexpected
              e.printStackTrace();
            }
          }
        }
        catch (IOException e)
        {
          // Means we're shutting down
        }
      }
    };
    _thread.start();
  }

  public int getPort()
  {
    return _serverSocket.getLocalPort();
  }

  public URI getRequestURI()
  {
    return URI.create("http://localhost:" + getPort());
  }

  public URI getNoResponseURI()
  {
    return URI.create("http://localhost:" + getPort() + "/?noresponse");
  }

  public URI getResponseOfSizeURI(int size)
  {
    return URI.create("http://localhost:" + getPort() + "/?responseSize="+size);
  }

  public String getLastRequest()
  {
    return _lastRequest;
  }

  public void shutdown() throws InterruptedException, IOException
  {
    _serverSocket.close();
    _thread.join(30000);
  }

  private String getResponse(String line)
  {
    try
    {
      StringTokenizer tokenizer = new StringTokenizer(line);
      tokenizer.nextToken();
      String uriString = tokenizer.nextToken();
      URI uri = URI.create(uriString);
      String q = uri.getQuery();
      if ("noresponse".equals(q))
      {
        return null;
      }
      if (q != null && q.startsWith("responseSize"))
      {
        int size = Integer.parseInt(q.replace("responseSize=", ""));
        String longString = "This is a semi-long string that is being used to test the response " +
                "size limitation of NettyClient, and it will be concatenated together to form a very " +
                "long response. ";
        StringBuilder sb = new StringBuilder(size + 2*longString.length());
        sb.append(longString);
        while(sb.length() < size)
        {
          sb.append(longString);
        }
        return "HTTP/1.0 200 OK\r\n" +
                "Content-Length: " + size + "\r\n" +
                "\r\n" +
                sb.substring(0,size);
      }
      return "HTTP/1.0 200 OK\r\n" +
             "\r\n";
    }
    catch (RuntimeException e)
    {
      throw new RuntimeException("Failed to parse line: " + line, e);
    }
  }

}
