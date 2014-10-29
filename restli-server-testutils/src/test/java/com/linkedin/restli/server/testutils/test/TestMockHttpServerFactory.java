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

package com.linkedin.restli.server.testutils.test;


import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.r2.transport.common.bridge.client.TransportClient;
import com.linkedin.r2.transport.common.bridge.client.TransportClientAdapter;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.r2.transport.http.server.HttpServer;
import com.linkedin.restli.client.RestClient;
import com.linkedin.restli.example.Album;
import com.linkedin.restli.example.Photo;
import com.linkedin.restli.example.impl.AlbumDatabaseImpl;
import com.linkedin.restli.example.impl.AlbumEntryDatabaseImpl;
import com.linkedin.restli.example.impl.AlbumResource;
import com.linkedin.restli.example.impl.PhotoDatabase;
import com.linkedin.restli.example.impl.PhotoDatabaseImpl;
import com.linkedin.restli.example.impl.PhotoResource;
import com.linkedin.restli.example.photos.AlbumsRequestBuilders;
import com.linkedin.restli.example.photos.PhotosRequestBuilders;
import com.linkedin.restli.server.testutils.MockHttpServerFactory;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * @author kparikh
 */
public class TestMockHttpServerFactory
{
  private static final int PORT = 7777;
  private static final PhotosRequestBuilders PHOTOS_BUILDERS = new PhotosRequestBuilders();
  private static final AlbumsRequestBuilders ALBUMS_BUILDERS = new AlbumsRequestBuilders();
  private static final TransportClient TRANSPORT_CLIENT =
      new HttpClientFactory().getClient(Collections.<String, Object>emptyMap());
  private static final RestClient REST_CLIENT =
      new RestClient(new TransportClientAdapter(TRANSPORT_CLIENT), "http://localhost:" + PORT + "/");

  @Test
  public void testCreateUsingClassNames()
      throws IOException, RemoteInvocationException
  {
    Set<Class<?>> resourceClasses = new HashSet<Class<?>>();
    resourceClasses.add(PhotoResource.class);
    resourceClasses.add(AlbumResource.class);

    Map<String, Object> beans = getBeans();

    boolean[] enableAsyncOptions = {true, false};
    for (boolean enableAsync: enableAsyncOptions)
    {
      HttpServer server = MockHttpServerFactory.create(PORT, resourceClasses, beans, enableAsync);

      runTest(server);
    }
  }

  @Test
  public void testCreateUsingPackageNames()
      throws IOException, RemoteInvocationException
  {
    Map<String, Object> beans = getBeans();

    boolean[] enableAsyncOptions = {true, false};
    for (boolean enableAsync: enableAsyncOptions)
    {
      HttpServer server = MockHttpServerFactory.create(PORT,
                                                       new String[]{"com.linkedin.restli.example.impl"},
                                                       beans,
                                                       enableAsync);

      runTest(server);
    }
  }

  /**
   * Returns beans that are needed by the resource classes
   *
   * @return
   */
  private Map<String, Object> getBeans()
  {
    Map<String, Object> beans = new HashMap<String, Object>();
    final PhotoDatabase photoDb = new PhotoDatabaseImpl(10);
    beans.put("photoDb", photoDb);
    beans.put("albumDb", new AlbumDatabaseImpl(5));
    beans.put("albumEntryDb", new AlbumEntryDatabaseImpl(photoDb, 5));
    return beans;
  }

  /**
   * Starts the server, makes a request, runs assertions, stops the server
   *
   * @param server the test server
   * @throws IOException
   * @throws RemoteInvocationException
   */
  private void runTest(HttpServer server)
      throws IOException, RemoteInvocationException
  {
    server.start();

    Photo photoEntity = REST_CLIENT.sendRequest(PHOTOS_BUILDERS.get().id(1L).build()).getResponseEntity();
    Assert.assertEquals(photoEntity.getTitle(), "Photo 1");

    Album albumEntity = REST_CLIENT.sendRequest(ALBUMS_BUILDERS.get().id(1L).build()).getResponseEntity();
    Assert.assertEquals(albumEntity.getTitle(), "Awesome Album #1");

    server.stop();
  }
}
