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

package com.linkedin.restli.internal.server.filter;


import com.linkedin.data.DataMap;
import com.linkedin.data.transform.filter.request.MaskTree;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.server.MutablePathKeys;
import com.linkedin.restli.internal.server.PathKeysImpl;
import com.linkedin.restli.internal.server.ServerResourceContext;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.internal.server.model.ResourceModel;
import com.linkedin.restli.server.ProjectionMode;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;


public class TestFilterRequestContextInternalImpl
{
  @Mock
  private ServerResourceContext context;
  @Mock
  private ResourceMethodDescriptor resourceMethod;
  @Mock
  private ResourceModel resourceModel;

  @BeforeTest
  protected void setUp() throws Exception
  {
    MockitoAnnotations.initMocks(this);
  }

  @AfterMethod
  protected void resetSharedMocks() throws Exception
  {
    Mockito.reset(context, resourceMethod, resourceModel);
  }

  @Test
  public void testFilterRequestContextAdapter() throws Exception
  {
    final String resourceName = "resourceName";
    final String resourceNamespace = "resourceNamespace";
    final ResourceMethod methodType = ResourceMethod.GET;
    final DataMap customAnnotations = new DataMap();
    customAnnotations.put("foo", "Bar");
    final ProjectionMode projectionMode = ProjectionMode.AUTOMATIC;
    final MaskTree maskTree = new MaskTree();
    final MutablePathKeys pathKeys = new PathKeysImpl();
    final Map<String, String> requestHeaders = new HashMap<String, String>();
    requestHeaders.put("Key1", "Value1");
    final URI requestUri = new URI("foo.bar.com");
    final ProtocolVersion protoVersion = AllProtocolVersions.BASELINE_PROTOCOL_VERSION;
    final DataMap queryParams = new DataMap();
    queryParams.put("Param1", "Val1");
    final Map<String, Object> localAttrs = new HashMap<>();
    localAttrs.put("Key1", "Val1");
    final RequestContext r2RequestContext = new RequestContext();
    r2RequestContext.putLocalAttr("Key1", "Val1");

    final String finderName = UUID.randomUUID().toString();
    final String actionName = UUID.randomUUID().toString();

    when(resourceModel.getName()).thenReturn(resourceName);
    when(resourceModel.getNamespace()).thenReturn(resourceNamespace);
    when(resourceMethod.getResourceModel()).thenReturn(resourceModel);
    when(resourceMethod.getMethodType()).thenReturn(methodType);
    when(resourceMethod.getFinderName()).thenReturn(finderName);
    when(resourceMethod.getActionName()).thenReturn(actionName);
    when(resourceMethod.getCustomAnnotationData()).thenReturn(customAnnotations);
    when(resourceMethod.getMethod()).thenReturn(null);

    when(context.getProjectionMode()).thenReturn(projectionMode);
    when(context.getProjectionMask()).thenReturn(maskTree);
    when(context.getPathKeys()).thenReturn(pathKeys);
    when(context.getRequestHeaders()).thenReturn(requestHeaders);
    when(context.getRequestURI()).thenReturn(requestUri);
    when(context.getRestliProtocolVersion()).thenReturn(protoVersion);
    when(context.getParameters()).thenReturn(queryParams);
    when(context.getRawRequestContext()).thenReturn(r2RequestContext);

    FilterRequestContextInternalImpl filterContext = new FilterRequestContextInternalImpl(context, resourceMethod);

    assertEquals(filterContext.getFilterResourceModel().getResourceName(), resourceName);
    assertEquals(filterContext.getFilterResourceModel().getResourceNamespace(), resourceNamespace);
    assertEquals(filterContext.getMethodType(), methodType);
    assertEquals(filterContext.getCustomAnnotations(), customAnnotations);
    assertEquals(filterContext.getProjectionMode(), projectionMode);
    assertEquals(filterContext.getProjectionMask(), maskTree);
    assertEquals(filterContext.getPathKeys(), pathKeys);
    assertEquals(filterContext.getRequestHeaders(), requestHeaders);
    assertEquals(filterContext.getRequestURI(), requestUri);
    assertEquals(filterContext.getRestliProtocolVersion(), protoVersion);
    assertEquals(filterContext.getQueryParameters(), queryParams);
    assertEquals(filterContext.getActionName(), actionName);
    assertEquals(filterContext.getFinderName(), finderName);
    assertEquals(filterContext.getRequestContextLocalAttrs(), localAttrs);
    assertNull(filterContext.getMethod());
    filterContext.getRequestHeaders().put("header2", "value2");
    assertEquals(requestHeaders.get("header2"), "value2");

    verify(resourceModel).getName();
    verify(resourceModel).getNamespace();
    verify(resourceMethod).getMethodType();
    verify(resourceMethod).getResourceModel();
    verify(resourceMethod).getCustomAnnotationData();
    verify(resourceMethod).getFinderName();
    verify(resourceMethod).getActionName();
    verify(resourceMethod).getMethod();
    verify(context).getProjectionMode();
    verify(context).getProjectionMask();
    verify(context).getPathKeys();
    verify(context, times(2)).getRequestHeaders();
    verify(context).getRequestURI();
    verify(context).getRestliProtocolVersion();
    verify(context).getParameters();
    verify(context).getRawRequestContext();
    verify(resourceMethod).getFinderMetadataType();
    verifyNoMoreInteractions(context, resourceMethod, resourceModel);
  }

  @Test
  public void testFilterScratchpad() throws Exception
  {
    FilterRequestContextInternalImpl filterContext = new FilterRequestContextInternalImpl(context, resourceMethod);
    Object spValue = new Object();
    String spKey = UUID.randomUUID().toString();
    filterContext.getFilterScratchpad().put(spKey, spValue);
    assertTrue(filterContext.getFilterScratchpad().get(spKey) == spValue);
  }

  @Test
  public void testCustomContextData() throws Exception
  {
    FilterRequestContextInternalImpl filterContext = new FilterRequestContextInternalImpl(context, resourceMethod);
    filterContext.putCustomContextData("foo", "bar");
    filterContext.getCustomContextData("foo");
    filterContext.removeCustomContextData("foo");
    verify(context, times(1)).putCustomContextData("foo", "bar");
    verify(context, times(1)).getCustomContextData("foo");
    verify(context, times(1)).removeCustomContextData("foo");
  }
}