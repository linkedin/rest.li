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


import com.linkedin.restli.server.filter.FilterResourceModel;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;


public class TestResourceModelImpl
{
  @Mock
  private com.linkedin.restli.internal.server.model.ResourceModel _resourceModel;
  @Mock
  private com.linkedin.restli.internal.server.model.ResourceModel _parentResourceModel;

  @BeforeTest
  protected void setUp() throws Exception
  {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testResourceModelImpl() throws Exception
  {
    final String resourceName = "resourceName";
    final String parentResourceName = "parentResourceName";
    final String resourceNamespace = "resourceNamespace";
    final String keyName = "keyName";

    when(_resourceModel.getKeyName()).thenReturn(keyName);
    when(_resourceModel.getName()).thenReturn(resourceName);
    when(_resourceModel.getNamespace()).thenReturn(resourceNamespace);
    doReturn(TestResourceModelImpl.class).when(_resourceModel).getResourceClass();
    when(_resourceModel.getParentResourceModel()).thenReturn(_parentResourceModel);
    when(_parentResourceModel.getName()).thenReturn(parentResourceName);

    FilterResourceModel model = new FilterResourceModelImpl(_resourceModel);

    assertEquals(model.getKeyName(), keyName);
    assertEquals(model.getResourceClass(), TestResourceModelImpl.class);
    assertEquals(model.getResourceName(), resourceName);
    assertEquals(model.getParentResourceModel().getResourceName(), parentResourceName);
    assertEquals(model.getResourceNamespace(), resourceNamespace);

    verify(_resourceModel).getKeyName();
    verify(_resourceModel).getName();
    verify(_resourceModel).getNamespace();
    verify(_resourceModel).getResourceClass();
    verify(_resourceModel, times(2)).getParentResourceModel();
    verify(_parentResourceModel).getName();
    verifyNoMoreInteractions(_resourceModel, _parentResourceModel);
  }
}