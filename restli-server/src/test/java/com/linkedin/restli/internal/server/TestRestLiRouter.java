/*
   Copyright (c) 2021 LinkedIn Corp.

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

package com.linkedin.restli.internal.server;

import com.linkedin.data.DataMap;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.restli.common.EmptyRecord;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.internal.server.model.ResourceModel;
import com.linkedin.restli.internal.server.model.RestLiAnnotationReader;
import com.linkedin.restli.server.RestLiConfig;
import com.linkedin.restli.server.RoutingException;
import com.linkedin.restli.server.annotations.PathKeyParam;
import com.linkedin.restli.server.annotations.RestLiCollection;
import com.linkedin.restli.server.annotations.RestMethod;
import com.linkedin.restli.server.resources.CollectionResourceTemplate;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.Test;

import static com.linkedin.restli.internal.common.AllProtocolVersions.*;
import static org.mockito.Mockito.*;


public class TestRestLiRouter
{

  @Test
  public void succeedsOnRootResourceGet() throws URISyntaxException
  {
    final TestSetup setup = new TestSetup();
    setup.mockContextForRootResourceGetRequest(setup._rootPath + "/12345");
    final RestLiRouter router = setup._router;
    final ServerResourceContext context = setup._context;

    final ResourceMethodDescriptor method = router.process(context);

    Assert.assertNotNull(method);
  }

  // ----------------------------------------------------------------------
  // negative cases
  // ----------------------------------------------------------------------

  @Test
  public void failsOnChildResourceNotFound() throws URISyntaxException
  {
    final TestSetup setup = new TestSetup();
    setup.mockContextForRootResourceGetRequest(setup._rootPath + "/12345" + setup._childPath + "/54321");
    final RestLiRouter router = setup._router;
    final ServerResourceContext context = setup._context;

    final RoutingException e = runAndCatch(() -> router.process(context), RoutingException.class);

    Assert.assertEquals(e.getStatus(), HttpStatus.S_404_NOT_FOUND.getCode());
  }

  @Test
  public void failsOnRootResourceMethodNotFound() throws URISyntaxException
  {
    final TestSetup setup = new TestSetup();
    setup.mockContextForMethodNotFound(setup._rootPath);
    final RestLiRouter router = setup._router;
    final ServerResourceContext context = setup._context;

    final RoutingException e = runAndCatch(() -> router.process(context), RoutingException.class);

    Assert.assertEquals(e.getStatus(), HttpStatus.S_400_BAD_REQUEST.getCode());
  }

  @Test
  public void failsOnRootResourceOperationNotFound() throws URISyntaxException
  {
    final TestSetup setup = new TestSetup();
    setup.mockContextForOperationNotFound(setup._rootPath);
    final RestLiRouter router = setup._router;
    final ServerResourceContext context = setup._context;

    final RoutingException e = runAndCatch(() -> router.process(context), RoutingException.class);

    Assert.assertEquals(e.getStatus(), HttpStatus.S_400_BAD_REQUEST.getCode());
  }

  @Test
  public void failsOnRootResourceNotFound() throws URISyntaxException
  {
    final TestSetup setup = new TestSetup();
    final RestLiRouter router = setup._router;
    final ServerResourceContext context = setup._context;

    doReturn(new URI("/root")).when(context).getRequestURI();

    final RoutingException e = runAndCatch(() -> router.process(context), RoutingException.class);

    Assert.assertEquals(e.getStatus(), HttpStatus.S_404_NOT_FOUND.getCode());
  }

  @Test
  public void failsOnVeryShortUriPath() throws URISyntaxException
  {
    final TestSetup setup = new TestSetup();
    final RestLiRouter router = setup._router;
    final ServerResourceContext context = setup._context;

    doReturn(new URI("/")).when(context).getRequestURI();

    final RoutingException e = runAndCatch(() -> router.process(context), RoutingException.class);

    Assert.assertEquals(e.getStatus(), HttpStatus.S_404_NOT_FOUND.getCode());
  }

  // ----------------------------------------------------------------------
  // helper members
  // ----------------------------------------------------------------------

  @SuppressWarnings("unchecked")
  private static <X extends Throwable> X runAndCatch(ThrowingRunnable<X> runnable, Class<X> clazz) {
    try {
      runnable.run();
    } catch (Throwable t) {
      if (clazz.isAssignableFrom(t.getClass())) {
        return (X) t;
      }
      throw new IllegalStateException(
          String.format("Expected an exception of type '%s' but caught a different one: %s",
              clazz.getSimpleName(), t.getMessage()), t);
    }
    throw new IllegalStateException(
        String.format("Expected an exception of type '%s' but none was caught", clazz.getSimpleName()));
  }

  @RestLiCollection(name = "root", keyName = "rootId")
  private static class RootResource extends CollectionResourceTemplate<Long, EmptyRecord>
  {
    @RestMethod.Get
    public EmptyRecord get(@PathKeyParam("rootId") Long id)
    {
      return new EmptyRecord();
    }
  }

  private static final class TestSetup
  {
    private final String _childPath;
    private final RestLiConfig _config;
    private final ServerResourceContext _context;
    private final Class<?> _keyClass;
    private final String _keyName;
    private final DataMap _parameters;
    private final MutablePathKeys _pathKeys;
    private final Map<String, ResourceModel> _pathToModelMap;
    private final String _resourceName;
    private final RequestContext _requestContext;
    private final ResourceModel _rootModel;
    private final String _rootPath;
    private final RestLiRouter _router;

    private TestSetup() {
      _keyName = "rootId";
      _keyClass = Long.class;
      _resourceName = "root";
      _childPath = "/child";
      _rootPath = "/root";
      _parameters = new DataMap();
      _context = mock(ServerResourceContext.class);
      _pathKeys = mock(MutablePathKeys.class);
      _rootModel = RestLiAnnotationReader.processResource(RootResource.class);
      _pathToModelMap = new HashMap<>();
      _config = new RestLiConfig();
      _router = new RestLiRouter(_pathToModelMap, _config);
      _requestContext = new RequestContext();
    }

    private void mockCommon() {
      doReturn(_parameters).when(_context).getParameters();
      doReturn(_pathKeys).when(_context).getPathKeys();
      doReturn(RESTLI_PROTOCOL_2_0_0.getProtocolVersion()).when(_context).getRestliProtocolVersion();
      doReturn(_requestContext).when(_context).getRawRequestContext();
      _pathToModelMap.put(_rootPath, _rootModel);
    }

    private void mockContextForRootResourceGetRequest(String path) throws URISyntaxException
    {
      mockCommon();
      doReturn(null).when(_pathKeys).getBatchIds();
      doReturn(new URI(path)).when(_context).getRequestURI();
      doReturn("GET").when(_context).getRequestMethod(); // http method
      doReturn("GET").when(_context).getRestLiRequestMethod(); // from the X-RestLi-Method header
      doReturn(null).when(_context).getMethodName(eq(ResourceMethod.GET));
    }

    private void mockContextForOperationNotFound(String path) throws URISyntaxException
    {
      mockCommon();
      doReturn(null).when(_pathKeys).getBatchIds();
      doReturn(new URI(path)).when(_context).getRequestURI();
      doReturn("POST").when(_context).getRequestMethod(); // http method
      doReturn("CREATE").when(_context).getRestLiRequestMethod(); // from the X-RestLi-Method header
      doReturn(null).when(_context).getMethodName(eq(ResourceMethod.CREATE));
    }

    private void mockContextForMethodNotFound(String path) throws URISyntaxException
    {
      mockCommon();
      doReturn(null).when(_pathKeys).getBatchIds();
      doReturn(new URI(path)).when(_context).getRequestURI();
      doReturn("POST").when(_context).getRequestMethod(); // http method
      doReturn("CREATE").when(_context).getRestLiRequestMethod(); // from the X-RestLi-Method header
      doReturn("create").when(_context).getMethodName(eq(ResourceMethod.CREATE));
    }
  }

  interface ThrowingRunnable<X extends Throwable> {
    void run() throws X;
  }
}
