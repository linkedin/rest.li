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
import com.linkedin.restli.internal.server.model.Parameter;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.internal.server.model.ResourceModel;
import com.linkedin.restli.server.ProjectionMode;
import com.linkedin.restli.server.TestServiceError;
import com.linkedin.restli.server.errors.ServiceError;
import com.linkedin.restli.server.filter.FilterRequestContext;
import com.linkedin.restli.server.filter.FilterResourceModel;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertNotSame;


public class TestFilterRequestContextInternalImpl
{
  @Mock
  private ServerResourceContext context;
  @Mock
  private ResourceMethodDescriptor resourceMethod;
  @Mock
  private ResourceModel resourceModel;
  @Mock
  private FilterResourceModel filterResourceModel;

  @BeforeTest
  protected void setUp()
  {
    MockitoAnnotations.initMocks(this);
  }

  @AfterMethod
  protected void resetSharedMocks()
  {
    Mockito.reset(context, resourceMethod, resourceModel, filterResourceModel);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testFilterRequestContextAdapter() throws Exception
  {
    final String resourceName = "resourceName";
    final String resourceNamespace = "resourceNamespace";
    final ResourceMethod methodType = ResourceMethod.GET;
    final DataMap customAnnotations = new DataMap();
    customAnnotations.put("foo", "Bar");
    final ProjectionMode projectionMode = ProjectionMode.AUTOMATIC;
    final MaskTree maskTree = new MaskTree();
    final MaskTree metadataMaskTree = new MaskTree();
    final MaskTree pagingMaskTree = new MaskTree();
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
    final String batchFinderName = UUID.randomUUID().toString();
    final String actionName = UUID.randomUUID().toString();

    final List<ServiceError> methodServiceErrors = Collections.singletonList(TestServiceError.METHOD_LEVEL_ERROR);
    final List<ServiceError> resourceServiceErrors = Collections.singletonList(TestServiceError.RESOURCE_LEVEL_ERROR);
    final List<Parameter<?>> methodParameters = Collections.singletonList(Mockito.mock(Parameter.class));

    when(resourceModel.getName()).thenReturn(resourceName);
    when(resourceModel.getNamespace()).thenReturn(resourceNamespace);
    when(filterResourceModel.getServiceErrors()).thenReturn(resourceServiceErrors);
    when(resourceMethod.getResourceModel()).thenReturn(resourceModel);
    when(resourceMethod.getMethodType()).thenReturn(methodType);
    when(resourceMethod.getFinderName()).thenReturn(finderName);
    when(resourceMethod.getBatchFinderName()).thenReturn(batchFinderName);
    when(resourceMethod.getActionName()).thenReturn(actionName);
    when(resourceMethod.getCustomAnnotationData()).thenReturn(customAnnotations);
    when(resourceMethod.getMethod()).thenReturn(null);
    when(resourceMethod.getParameters()).thenReturn(methodParameters);
    when(resourceMethod.getServiceErrors()).thenReturn(methodServiceErrors);

    when(context.getProjectionMode()).thenReturn(projectionMode);
    when(context.getProjectionMask()).thenReturn(maskTree);
    when(context.getMetadataProjectionMask()).thenReturn(metadataMaskTree);
    when(context.getPagingProjectionMask()).thenReturn(pagingMaskTree);
    when(context.getPathKeys()).thenReturn(pathKeys);
    when(context.getRequestHeaders()).thenReturn(requestHeaders);
    when(context.getRequestURI()).thenReturn(requestUri);
    when(context.getRestliProtocolVersion()).thenReturn(protoVersion);
    when(context.getParameters()).thenReturn(queryParams);
    when(context.getRawRequestContext()).thenReturn(r2RequestContext);

    FilterRequestContext filterContext = new FilterRequestContextInternalImpl(context, resourceMethod, null);

    filterContext.setProjectionMask(maskTree);
    filterContext.setMetadataProjectionMask(metadataMaskTree);
    filterContext.setPagingProjectionMask(pagingMaskTree);

    assertEquals(filterContext.getFilterResourceModel().getResourceName(), resourceName);
    assertEquals(filterContext.getFilterResourceModel().getResourceNamespace(), resourceNamespace);
    assertEquals(filterContext.getMethodType(), methodType);
    assertEquals(filterContext.getCustomAnnotations(), customAnnotations);
    assertEquals(filterContext.getProjectionMode(), projectionMode);
    assertEquals(filterContext.getProjectionMask(), maskTree);
    assertEquals(filterContext.getMetadataProjectionMask(), metadataMaskTree);
    assertEquals(filterContext.getPagingProjectionMask(), pagingMaskTree);
    assertEquals(filterContext.getPathKeys(), pathKeys);
    assertEquals(filterContext.getRequestHeaders(), requestHeaders);
    assertEquals(filterContext.getRequestURI(), requestUri);
    assertEquals(filterContext.getRestliProtocolVersion(), protoVersion);
    assertEquals(filterContext.getQueryParameters(), queryParams);
    assertEquals(filterContext.getActionName(), actionName);
    assertEquals(filterContext.getFinderName(), finderName);
    assertEquals(filterContext.getBatchFinderName(), batchFinderName);
    assertEquals(filterContext.getRequestContextLocalAttrs(), localAttrs);
    assertNull(filterContext.getMethod());
    assertEquals(filterContext.getMethodParameters(), methodParameters);
    assertNotSame(filterContext.getMethodParameters(), methodParameters);
    assertEquals(filterContext.getMethodServiceErrors(), methodServiceErrors);
    filterContext.getRequestHeaders().put("header2", "value2");
    assertEquals(requestHeaders.get("header2"), "value2");

    verify(resourceModel).getName();
    verify(resourceModel).getNamespace();
    verify(resourceMethod).getMethodType();
    verify(resourceMethod).getResourceModel();
    verify(resourceMethod).getCustomAnnotationData();
    verify(resourceMethod).getFinderName();
    verify(resourceMethod).getBatchFinderName();
    verify(resourceMethod).getActionName();
    verify(resourceMethod).getMethod();
    verify(resourceMethod, times(2)).getParameters();
    verify(resourceMethod).getServiceErrors();
    verify(context).getProjectionMode();
    verify(context).setProjectionMask(maskTree);
    verify(context).getProjectionMask();
    verify(context).setMetadataProjectionMask(metadataMaskTree);
    verify(context).getMetadataProjectionMask();
    verify(context).setPagingProjectionMask(pagingMaskTree);
    verify(context).getPagingProjectionMask();
    verify(context).getPathKeys();
    verify(context, times(2)).getRequestHeaders();
    verify(context).getRequestURI();
    verify(context).getRestliProtocolVersion();
    verify(context).getParameters();
    verify(context).getRawRequestContext();
    verify(resourceMethod).getCollectionCustomMetadataType();
    verifyNoMoreInteractions(context, resourceMethod, resourceModel);
  }

  @Test
  public void testFilterScratchpad()
  {
    FilterRequestContext filterContext = new FilterRequestContextInternalImpl(context, resourceMethod, null);
    Object spValue = new Object();
    String spKey = UUID.randomUUID().toString();
    filterContext.getFilterScratchpad().put(spKey, spValue);
    assertSame(filterContext.getFilterScratchpad().get(spKey), spValue);
  }

  @Test
  public void testCustomContextData()
  {
    FilterRequestContext filterContext = new FilterRequestContextInternalImpl(context, resourceMethod, null);
    filterContext.putCustomContextData("foo", "bar");
    filterContext.getCustomContextData("foo");
    filterContext.removeCustomContextData("foo");
    verify(context, times(1)).putCustomContextData("foo", "bar");
    verify(context, times(1)).getCustomContextData("foo");
    verify(context, times(1)).removeCustomContextData("foo");
  }

  @Test
  public void testGetActionReturnType()
  {
    when(resourceMethod.getMethodType()).thenReturn(ResourceMethod.ACTION);
    Mockito.doReturn(String.class).when(resourceMethod).getActionReturnType();
    FilterRequestContext filterContext = new FilterRequestContextInternalImpl(context, resourceMethod, null);
    Assert.assertEquals(filterContext.getActionReturnType(), String.class);

    when(resourceMethod.getMethodType()).thenReturn(ResourceMethod.GET);
    Assert.assertNull(filterContext.getActionReturnType());
  }
}