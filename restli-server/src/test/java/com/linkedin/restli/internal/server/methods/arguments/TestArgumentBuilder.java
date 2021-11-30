/*
   Copyright (c) 2016 LinkedIn Corp.

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

package com.linkedin.restli.internal.server.methods.arguments;


import com.linkedin.common.callback.Callback;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.schema.DataSchemaConstants;
import com.linkedin.data.template.DataTemplate;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.DynamicRecordTemplate;
import com.linkedin.data.template.StringArray;
import com.linkedin.data.transform.filter.request.MaskTree;
import com.linkedin.parseq.Context;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.common.attachments.RestLiAttachmentReader;
import com.linkedin.restli.internal.server.MutablePathKeys;
import com.linkedin.restli.internal.server.ServerResourceContext;
import com.linkedin.restli.internal.server.model.AnnotationSet;
import com.linkedin.restli.internal.server.model.Parameter;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.server.UnstructuredDataWriter;
import com.linkedin.restli.server.PagingContext;
import com.linkedin.restli.server.PathKeys;
import com.linkedin.restli.server.ResourceContext;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.RoutingException;
import com.linkedin.restli.server.TestRecord;
import com.linkedin.restli.server.TestRecordArray;
import com.linkedin.restli.server.annotations.UnstructuredDataWriterParam;
import com.linkedin.restli.server.annotations.HeaderParam;

import com.linkedin.restli.server.config.ResourceMethodConfig;
import java.io.ByteArrayOutputStream;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


/**
 * @author Oby Sumampouw
 */
public class TestArgumentBuilder
{
  private ResourceMethodDescriptor getMockResourceMethod(List<Parameter<?>> parameters)
  {
    ResourceMethodDescriptor resourceMethodDescriptor = EasyMock.createMock(ResourceMethodDescriptor.class);
    EasyMock.expect(resourceMethodDescriptor.getParameters()).andReturn(parameters);
    EasyMock.replay(resourceMethodDescriptor);
    return resourceMethodDescriptor;
  }

  private ResourceMethodConfig getMockResourceMethodConfig(boolean shouldValidateParams) {
    ResourceMethodConfig mockResourceMethodConfig = EasyMock.createMock(ResourceMethodConfig.class);
    EasyMock.expect(mockResourceMethodConfig.shouldValidateResourceKeys()).andReturn(shouldValidateParams).anyTimes();
    EasyMock.expect(mockResourceMethodConfig.shouldValidateQueryParams()).andReturn(shouldValidateParams).anyTimes();
    EasyMock.replay(mockResourceMethodConfig);
    return mockResourceMethodConfig;
  }

  @Test
  public void testBuildArgsHappyPath()
  {
    //test integer association key integer
    String param1Key = "param1";
    Parameter<Integer> param1 = new Parameter<>(param1Key, Integer.class, DataTemplateUtil.getSchema(Integer.class),
                                          false, null, Parameter.ParamType.ASSOC_KEY_PARAM, false, AnnotationSet.EMPTY);
    Integer param1Value = 123;

    //test regular string argument
    String param2Key = "param2";
    Parameter<String> param2 = new Parameter<>(param2Key, String.class, DataTemplateUtil.getSchema(String.class),
                                          true, null, Parameter.ParamType.QUERY, true, AnnotationSet.EMPTY);
    String param2Value = "param2Value";

    //test data template argument array with more than element
    String param3Key = "param3";
    Parameter<StringArray> param3 = new Parameter<>(param3Key, StringArray.class, DataTemplateUtil.getSchema(StringArray.class),
                                      true, null, Parameter.ParamType.QUERY, true, AnnotationSet.EMPTY);

    DataList param3Value = new DataList(Arrays.asList("param3a", "param3b"));
    StringArray param3Final = new StringArray(param3Value);

    //test data template argument array with only one element
    String param4Key = "param4";
    Parameter<StringArray> param4 = new Parameter<>(param4Key, StringArray.class, DataTemplateUtil.getSchema(StringArray.class),
                                          true, null, Parameter.ParamType.QUERY, true, AnnotationSet.EMPTY);
    String param4Value = "param4Value";
    StringArray param4Final = new StringArray(param4Value);

    // test record template
    String param5Key = "param5";
    Parameter<TestRecord> param5 = new Parameter<>(param5Key, TestRecord.class, DataTemplateUtil.getSchema(TestRecord.class),
                                                             true, null, Parameter.ParamType.QUERY, true, AnnotationSet.EMPTY);
    DataMap param5Value = new DataMap();
    param5Value.put("doubleField", "5.5");
    param5Value.put("floatField", "5");
    param5Value.put("intField", "5");
    param5Value.put("longField", "5");

    TestRecord param5Final = new TestRecord();
    param5Final.setDoubleField(5.5);
    param5Final.setFloatField(5F);
    param5Final.setIntField(5);
    param5Final.setLongField(5);

    // test record template array
    String param6Key = "param6";
    Parameter<TestRecordArray> param6 = new Parameter<>(param6Key, TestRecordArray.class, DataTemplateUtil.getSchema(TestRecordArray.class),
                                                                       true, null, Parameter.ParamType.QUERY, true, AnnotationSet.EMPTY);
    DataList param6Value = new DataList();
    DataMap testRecordDataMap1 = new DataMap();
    testRecordDataMap1.put("doubleField", "6.6");
    testRecordDataMap1.put("floatField", "6");
    testRecordDataMap1.put("intField", "6");
    testRecordDataMap1.put("longField", "6");
    DataMap testRecordDataMap2 = new DataMap();
    testRecordDataMap2.put("doubleField", "66.6");
    testRecordDataMap2.put("floatField", "66");
    testRecordDataMap2.put("intField", "66");
    testRecordDataMap2.put("longField", "66");
    param6Value.add(testRecordDataMap1);
    param6Value.add(testRecordDataMap2);

    TestRecordArray param6Final = new TestRecordArray();
    TestRecord testRecord1 = new TestRecord();
    testRecord1.setDoubleField(6.6);
    testRecord1.setFloatField(6);
    testRecord1.setIntField(6);
    testRecord1.setLongField(6);
    TestRecord testRecord2 = new TestRecord();
    testRecord2.setDoubleField(66.6);
    testRecord2.setFloatField(66);
    testRecord2.setIntField(66);
    testRecord2.setLongField(66);
    param6Final.add(testRecord1);
    param6Final.add(testRecord2);


    List<Parameter<?>> parameters = new ArrayList<>();
    parameters.add(param1);
    parameters.add(param2);
    parameters.add(param3);
    parameters.add(param4);
    parameters.add(param5);
    parameters.add(param6);
    Object[] positionalArguments = new Object[0];

    Capture<String> param1Capture = EasyMock.newCapture();
    Capture<String> param2Capture = EasyMock.newCapture();
    Capture<String> param3Capture = EasyMock.newCapture();
    Capture<String> param4Capture = EasyMock.newCapture();
    Capture<String> param5Capture = EasyMock.newCapture();
    Capture<String> param6Capture = EasyMock.newCapture();

    ServerResourceContext mockResourceContext = EasyMock.createMock(ServerResourceContext.class);
    EasyMock.expect(mockResourceContext.getRequestAttachmentReader()).andReturn(null);
    MutablePathKeys mockPathKeys = EasyMock.createMock(MutablePathKeys.class);
    ResourceMethodDescriptor mockResourceMethodDescriptor = getMockResourceMethod(parameters);

    ResourceMethodConfig mockResourceMethodConfig = EasyMock.createMock(ResourceMethodConfig.class);
    EasyMock.expect(mockResourceMethodConfig.shouldValidateResourceKeys()).andReturn(true).times(5);
    EasyMock.expect(mockResourceMethodConfig.shouldValidateQueryParams()).andReturn(false).times(5);
    EasyMock.replay(mockResourceMethodConfig);

    //easy mock for processing param1
    EasyMock.expect(mockPathKeys.get(EasyMock.capture(param1Capture))).andReturn(param1Value);
    EasyMock.expect(mockResourceContext.getPathKeys()).andReturn(mockPathKeys);
    //easy mock for processing param2
    EasyMock.expect(mockResourceContext.hasParameter(EasyMock.capture(param2Capture))).andReturn(true);
    EasyMock.expect(mockResourceContext.getParameter(EasyMock.capture(param2Capture))).andReturn(param2Value);
    //easy mock for processing param3
    EasyMock.expect(mockResourceContext.getStructuredParameter(EasyMock.capture(param3Capture))).andReturn(param3Value);
    //easy mock for processing param4
    EasyMock.expect(mockResourceContext.getStructuredParameter(EasyMock.capture(param4Capture))).andReturn(param4Value);
    //easy mock for processing param5
    EasyMock.expect(mockResourceContext.getStructuredParameter(EasyMock.capture(param5Capture))).andReturn(param5Value);
    //easy mock for processing param6
    EasyMock.expect(mockResourceContext.getStructuredParameter(EasyMock.capture(param6Capture))).andReturn(param6Value);
    EasyMock.replay(mockResourceContext, mockPathKeys);

    Object[] results = ArgumentBuilder.buildArgs(positionalArguments, mockResourceMethodDescriptor, mockResourceContext, null, mockResourceMethodConfig);

    EasyMock.verify(mockPathKeys, mockResourceContext);
    Assert.assertEquals(param1Capture.getValue(), param1Key);
    Assert.assertEquals(param2Capture.getValue(), param2Key);
    Assert.assertEquals(param3Capture.getValue(), param3Key);
    Assert.assertEquals(param4Capture.getValue(), param4Key);
    Assert.assertEquals(param5Capture.getValue(), param5Key);
    Assert.assertEquals(param6Capture.getValue(), param6Key);

    Assert.assertEquals(results[0], param1Value);
    Assert.assertEquals(results[1], param2Value);
    Assert.assertEquals(results[2], param3Final);
    Assert.assertEquals(results[3], param4Final);
    Assert.assertEquals(results[4], param5Final);
    Assert.assertEquals(results[5], param6Final);
  }

  @Test
  public void testHeaderParamType()
  {
    String testParamKey = "testParam";
    String expectedTestParamValue = "testParamValue";

    ServerResourceContext mockResourceContext = EasyMock.createMock(ServerResourceContext.class);
    HeaderParam annotation = EasyMock.createMock(HeaderParam.class);
    EasyMock.expect(annotation.value()).andReturn(testParamKey);
    AnnotationSet annotationSet = EasyMock.createMock(AnnotationSet.class);
    EasyMock.expect(annotationSet.getAll()).andReturn(new Annotation[]{});
    EasyMock.expect(annotationSet.get(HeaderParam.class)).andReturn(annotation);
    Map<String, String> headers = new HashMap<>();
    headers.put(testParamKey, expectedTestParamValue);
    EasyMock.expect(mockResourceContext.getRequestHeaders()).andReturn(headers);
    EasyMock.expect(mockResourceContext.getRequestAttachmentReader()).andReturn(null);
    EasyMock.replay(mockResourceContext, annotation, annotationSet);

    Parameter<String> param = new Parameter<>(testParamKey, String.class, DataSchemaConstants.STRING_DATA_SCHEMA,
        false, null, Parameter.ParamType.HEADER, false, annotationSet);
    List<Parameter<?>> parameters = Collections.singletonList(param);

    Object[] results = ArgumentBuilder.buildArgs(new Object[0], getMockResourceMethod(parameters), mockResourceContext, null, getMockResourceMethodConfig(false));
    Assert.assertEquals(results[0], expectedTestParamValue);
  }

  @DataProvider(name = "noOpParameterData")
  @SuppressWarnings("deprecation")
  private Object[][] noOpParameterData()
  {
    return new Object[][]
        {
            {
                Callback.class,
                Parameter.ParamType.CALLBACK
            },
            {
                Context.class,
                Parameter.ParamType.PARSEQ_CONTEXT_PARAM
            },
            {
                Context.class,
                Parameter.ParamType.PARSEQ_CONTEXT
            }
        };
  }

  @Test(dataProvider = "noOpParameterData")
  public void testNoOpParamType(Class<?> dataType, Parameter.ParamType paramType)
  {
    String paramKey = "testParam";

    ServerResourceContext mockResourceContext = EasyMock.createMock(ServerResourceContext.class);

    @SuppressWarnings({"unchecked","rawtypes"})
    Parameter<?> param = new Parameter(paramKey, dataType, null, false, null, paramType,
        false, AnnotationSet.EMPTY);
    List<Parameter<?>> parameters = Collections.singletonList(param);

    Object[] results = ArgumentBuilder.buildArgs(new Object[0], getMockResourceMethod(parameters), mockResourceContext, null, getMockResourceMethodConfig(false));
    Assert.assertEquals(results[0], null);
  }

  @DataProvider(name = "projectionParameterData")
  @SuppressWarnings("deprecation")
  private Object[][] projectionParameterData()
  {
    return new Object[][]
        {
            {
                Parameter.ParamType.PROJECTION
            },
            {
                Parameter.ParamType.PROJECTION_PARAM
            },
            {
                Parameter.ParamType.METADATA_PROJECTION_PARAM
            },
            {
                Parameter.ParamType.PAGING_PROJECTION_PARAM
            }
        };
  }

  @Test(dataProvider = "projectionParameterData")
  @SuppressWarnings("deprecation")
  public void testProjectionParamType(Parameter.ParamType paramType)
  {
    String testParamKey = "testParam";

    ServerResourceContext mockResourceContext = EasyMock.createMock(ServerResourceContext.class);
    MaskTree mockMask = EasyMock.createMock(MaskTree.class);
    if (paramType == Parameter.ParamType.PROJECTION_PARAM || paramType == Parameter.ParamType.PROJECTION)
    {
      EasyMock.expect(mockResourceContext.getProjectionMask()).andReturn(mockMask);
    }
    else if (paramType == Parameter.ParamType.METADATA_PROJECTION_PARAM)
    {
      EasyMock.expect(mockResourceContext.getMetadataProjectionMask()).andReturn(mockMask);
    }
    else
    {
      EasyMock.expect(mockResourceContext.getPagingProjectionMask()).andReturn(mockMask);
    }
    EasyMock.expect(mockResourceContext.getRequestAttachmentReader()).andReturn(null);
    EasyMock.replay(mockResourceContext);

    Parameter<MaskTree> param = new Parameter<>(testParamKey, MaskTree.class, null, false, null, paramType,
        false, AnnotationSet.EMPTY);
    List<Parameter<?>> parameters = Collections.singletonList(param);

    Object[] results = ArgumentBuilder.buildArgs(new Object[0], getMockResourceMethod(parameters), mockResourceContext, null, getMockResourceMethodConfig(false));
    Assert.assertEquals(results[0], mockMask);
  }

  @Test
  @SuppressWarnings("deprecation")
  public void testPagingContextParamType()
  {
    String testParamKey = "testParam";

    ServerResourceContext mockResourceContext = EasyMock.createMock(ServerResourceContext.class);
    PagingContext pagingContext = new PagingContext(RestConstants.DEFAULT_START, RestConstants.DEFAULT_COUNT, false, false);
    EasyMock.expect(mockResourceContext.getParameter(RestConstants.START_PARAM)).andReturn(null).anyTimes();
    EasyMock.expect(mockResourceContext.getParameter(RestConstants.COUNT_PARAM)).andReturn(null).anyTimes();
    EasyMock.expect(mockResourceContext.getRequestAttachmentReader()).andReturn(null);
    EasyMock.replay(mockResourceContext);

    List<Parameter<?>> parameters = new ArrayList<>();
    Parameter<PagingContext> param1 = new Parameter<>(testParamKey, PagingContext.class, null,
        false, null, Parameter.ParamType.PAGING_CONTEXT_PARAM, false, AnnotationSet.EMPTY);
    Parameter<PagingContext> param2 = new Parameter<>(testParamKey, PagingContext.class, null,
        false, null, Parameter.ParamType.CONTEXT, false, AnnotationSet.EMPTY);
    parameters.add(param1);
    parameters.add(param2);

    Object[] results = ArgumentBuilder.buildArgs(new Object[0], getMockResourceMethod(parameters), mockResourceContext, null, getMockResourceMethodConfig(false));
    Assert.assertEquals(results[0], pagingContext);
    Assert.assertEquals(results[1], pagingContext);
  }

  @Test
  public void testUnstructuredDataWriterParam()
  {
    ServerResourceContext mockResourceContext = EasyMock.createMock(ServerResourceContext.class);

    mockResourceContext.setResponseEntityStream(EasyMock.anyObject());
    EasyMock.expectLastCall().once();
    EasyMock.expect(mockResourceContext.getRequestAttachmentReader()).andReturn(null);
    EasyMock.replay(mockResourceContext);

    @SuppressWarnings({"unchecked","rawtypes"})
    final Parameter<UnstructuredDataWriterParam> param = new Parameter("RestLi Unstructured Data Writer",
                                                                       UnstructuredDataWriter.class, null, false, null,
                                                                       Parameter.ParamType.UNSTRUCTURED_DATA_WRITER_PARAM, false,
                                                                       AnnotationSet.EMPTY);

    List<Parameter<?>> parameters = Collections.singletonList(param);

    Object[] results = ArgumentBuilder.buildArgs(new Object[0], getMockResourceMethod(parameters), mockResourceContext, null, getMockResourceMethodConfig(false));

    UnstructuredDataWriter result = (UnstructuredDataWriter) results[0];
    Assert.assertNotNull(result);
    Assert.assertTrue(result.getOutputStream() instanceof ByteArrayOutputStream);
    EasyMock.verify(mockResourceContext);
  }

  @Test
  public void testRestLiAttachmentsParam()
  {
    ServerResourceContext mockResourceContext = EasyMock.createMock(ServerResourceContext.class);

    final RestLiAttachmentReader restLiAttachmentReader = new RestLiAttachmentReader(null);

    EasyMock.expect(mockResourceContext.getRequestAttachmentReader()).andReturn(restLiAttachmentReader);
    EasyMock.replay(mockResourceContext);

    @SuppressWarnings({"unchecked","rawtypes"})
    final Parameter<RestLiAttachmentReader> param = new Parameter("RestLi Attachment Reader",
                                                                  RestLiAttachmentReader.class, null, false, null,
                                                                  Parameter.ParamType.RESTLI_ATTACHMENTS_PARAM, false,
                                                                  AnnotationSet.EMPTY);

    List<Parameter<?>> parameters = Collections.singletonList(param);

    Object[] results = ArgumentBuilder.buildArgs(new Object[0], getMockResourceMethod(parameters), mockResourceContext, null, getMockResourceMethodConfig(false));
    Assert.assertEquals(results[0], restLiAttachmentReader);
  }

  @Test
  public void testRestLiAttachmentsParamResourceExpectNotPresent()
  {
    //This test makes sure that a resource method that expects attachments, but none are present in the request,
    //is given a null for the RestLiAttachmentReader.
    ServerResourceContext mockResourceContext = EasyMock.createMock(ServerResourceContext.class);

    EasyMock.expect(mockResourceContext.getRequestAttachmentReader()).andReturn(null);
    EasyMock.replay(mockResourceContext);

    @SuppressWarnings({"unchecked","rawtypes"})
    final Parameter<RestLiAttachmentReader> param = new Parameter("RestLi Attachment Reader",
                                                                  RestLiAttachmentReader.class, null, false, null,
                                                                  Parameter.ParamType.RESTLI_ATTACHMENTS_PARAM, false,
                                                                  AnnotationSet.EMPTY);

    List<Parameter<?>> parameters = Collections.singletonList(param);

    Object[] results = ArgumentBuilder.buildArgs(new Object[0], getMockResourceMethod(parameters), mockResourceContext, null, getMockResourceMethodConfig(false));
    Assert.assertEquals(results[0], null);
  }

  @Test
  public void testRestLiAttachmentsParamResourceNotExpect()
  {
    //This test makes sure that if the resource method did not expect attachments but there were attachments present
    //in the request, that an exception is thrown.
    ServerResourceContext mockResourceContext = EasyMock.createMock(ServerResourceContext.class);

    final RestLiAttachmentReader restLiAttachmentReader = new RestLiAttachmentReader(null);

    EasyMock.expect(mockResourceContext.getRequestAttachmentReader()).andReturn(restLiAttachmentReader);
    EasyMock.replay(mockResourceContext);

    List<Parameter<?>> parameters = Collections.emptyList();

    try
    {
      ArgumentBuilder.buildArgs(new Object[0], getMockResourceMethod(parameters), mockResourceContext, null, getMockResourceMethodConfig(false));
      Assert.fail();
    }
    catch (RestLiServiceException restLiServiceException)
    {
      Assert.assertEquals(restLiServiceException.getStatus(), HttpStatus.S_400_BAD_REQUEST);
      Assert.assertEquals(restLiServiceException.getMessage(), "Resource method endpoint invoked does not accept any request attachments.");
    }
  }

  @Test
  @SuppressWarnings("deprecation")
  public void testPathKeyParameterType()
  {
    String testParamKey = "testParam";
    String expectedTestParamValue = "testParamValue";

    // mock setup
    ServerResourceContext mockResourceContext = EasyMock.createMock(ServerResourceContext.class);
    MutablePathKeys mockPathKeys = EasyMock.createMock(MutablePathKeys.class);
    EasyMock.expect(mockPathKeys.get(testParamKey)).andReturn(expectedTestParamValue).anyTimes();
    EasyMock.expect(mockResourceContext.getPathKeys()).andReturn(mockPathKeys).anyTimes();
    EasyMock.expect(mockResourceContext.getRequestAttachmentReader()).andReturn(null);
    EasyMock.replay(mockResourceContext, mockPathKeys);

    List<Parameter<?>> parameters = new ArrayList<>();
    Parameter<String> param1 = new Parameter<>(testParamKey, String.class, null,
      false, null, Parameter.ParamType.KEY, false, AnnotationSet.EMPTY);
    Parameter<String> param2 = new Parameter<>(testParamKey, String.class, null,
        false, null, Parameter.ParamType.ASSOC_KEY_PARAM, false, AnnotationSet.EMPTY);
    Parameter<PathKeys> param3 = new Parameter<>(testParamKey, PathKeys.class, null,
        false, null, Parameter.ParamType.PATH_KEYS, false, AnnotationSet.EMPTY);
    Parameter<PathKeys> param4 = new Parameter<>(testParamKey, PathKeys.class, null,
        false, null, Parameter.ParamType.PATH_KEYS_PARAM, false, AnnotationSet.EMPTY);
    parameters.add(param1);
    parameters.add(param2);
    parameters.add(param3);
    parameters.add(param4);

    Object[] results = ArgumentBuilder.buildArgs(new Object[0], getMockResourceMethod(parameters), mockResourceContext, null, getMockResourceMethodConfig(false));
    Assert.assertEquals(results[0], expectedTestParamValue);
    Assert.assertEquals(results[1], expectedTestParamValue);
    Assert.assertEquals(results[2], mockPathKeys);
    Assert.assertEquals(results[3], mockPathKeys);
  }

  @Test
  @SuppressWarnings("deprecation")
  public void testResourceContextParameterType()
  {
    String testParamKey = "testParam";

    ServerResourceContext mockResourceContext = EasyMock.createMock(ServerResourceContext.class);

    List<Parameter<?>> parameters = new ArrayList<>();
    Parameter<ResourceContext> param1 = new Parameter<>(testParamKey, ResourceContext.class, null,
        false, null, Parameter.ParamType.RESOURCE_CONTEXT, false, AnnotationSet.EMPTY);
    Parameter<ResourceContext> param2 = new Parameter<>(testParamKey, ResourceContext.class, null,
        false, null, Parameter.ParamType.RESOURCE_CONTEXT_PARAM, false, AnnotationSet.EMPTY);
    parameters.add(param1);
    parameters.add(param2);

    EasyMock.expect(mockResourceContext.getRequestAttachmentReader()).andReturn(null);
    Object[] results = ArgumentBuilder.buildArgs(new Object[0], getMockResourceMethod(parameters), mockResourceContext, null, getMockResourceMethodConfig(false));
    Assert.assertEquals(results[0], mockResourceContext);
    Assert.assertEquals(results[1], mockResourceContext);
  }

  @Test
  public void testPostParameterType()
  {
    String testParamKey = "testParam";
    String expectedTestParamValue = "testParamValue";

    ServerResourceContext mockResourceContext = EasyMock.createMock(ServerResourceContext.class);
    DataMap entityBody = new DataMap();
    entityBody.put(testParamKey, expectedTestParamValue);
    DynamicRecordTemplate template = new DynamicRecordTemplate(entityBody, null);

    Parameter<String> param = new Parameter<>(testParamKey, String.class, DataSchemaConstants.STRING_DATA_SCHEMA,
        false, null, Parameter.ParamType.POST, false, AnnotationSet.EMPTY);
    List<Parameter<?>> parameters = Collections.singletonList(param);

    EasyMock.expect(mockResourceContext.getRequestAttachmentReader()).andReturn(null);
    Object[] results = ArgumentBuilder.buildArgs(new Object[0], getMockResourceMethod(parameters), mockResourceContext, template, getMockResourceMethodConfig(false));
    Assert.assertEquals(results[0], expectedTestParamValue);
  }

  @Test
  public void testQueryParameterType()
  {
    String testParamKey = "testParam";
    String expectedTestParamValue = "testParamValue";

    ServerResourceContext mockResourceContext = EasyMock.createMock(ServerResourceContext.class);
    EasyMock.expect(mockResourceContext.hasParameter(testParamKey)).andReturn(true).times(1);
    EasyMock.expect(mockResourceContext.getParameter(testParamKey)).andReturn(expectedTestParamValue).anyTimes();
    EasyMock.expect(mockResourceContext.getRequestAttachmentReader()).andReturn(null);
    EasyMock.replay(mockResourceContext);

    Parameter<String> param = new Parameter<>(testParamKey, String.class, DataSchemaConstants.STRING_DATA_SCHEMA,
        false, null, Parameter.ParamType.QUERY, false, AnnotationSet.EMPTY);
    List<Parameter<?>> parameters = Collections.singletonList(param);

    Object[] results = ArgumentBuilder.buildArgs(new Object[0], getMockResourceMethod(parameters), mockResourceContext, null, getMockResourceMethodConfig(false));
    Assert.assertEquals(results[0], expectedTestParamValue);
  }

  @DataProvider(name = "positionalParameterData")
  private Object[][] positionalParameterData()
  {
    return new Object[][]
        {
            {
                Set.class,
                Parameter.ParamType.BATCH
            },
            {
                String.class,
                Parameter.ParamType.RESOURCE_KEY
            }
        };
  }

  @Test(expectedExceptions = RoutingException.class, dataProvider = "positionalParameterData")
  public void testPositionalParameterType(Class<?> dataType, Parameter.ParamType paramType)
  {
    String paramKey = "testParam";

    ServerResourceContext mockResourceContext = EasyMock.createMock(ServerResourceContext.class);

    @SuppressWarnings({"unchecked","rawtypes"})
    Parameter<?> param = new Parameter(paramKey, dataType, null, false, null, paramType,
        false, AnnotationSet.EMPTY);
    List<Parameter<?>> parameters = Collections.singletonList(param);

    ArgumentBuilder.buildArgs(new Object[0], getMockResourceMethod(parameters), mockResourceContext, null, getMockResourceMethodConfig(false));
  }


  // utility method for reuse
  private Object[] testBuildOptionalArg(Parameter<?> param)
  {
    String paramKey = param.getName();
    Class<?> dataType = param.getType();
    Parameter.ParamType paramType = param.getParamType();

    // mock resource context
    ServerResourceContext mockResourceContext = EasyMock.createMock(ServerResourceContext.class);
    DynamicRecordTemplate template = null;
    if (paramType == Parameter.ParamType.POST)
    {
      template = new DynamicRecordTemplate(new DataMap(), null);
    }
    else
    {
      MutablePathKeys mockPathKeys = EasyMock.createMock(MutablePathKeys.class);
      EasyMock.expect(mockPathKeys.get(paramKey)).andReturn(null);
      EasyMock.expect(mockResourceContext.getPathKeys()).andReturn(mockPathKeys);
      EasyMock.expect(mockResourceContext.getRequestAttachmentReader()).andReturn(null);
      EasyMock.expect(mockResourceContext.hasParameter(paramKey)).andReturn(false);
      if (DataTemplate.class.isAssignableFrom(dataType))
      {
        EasyMock.expect(mockResourceContext.getStructuredParameter(paramKey)).andReturn(null);
      }
      else
      {
        EasyMock.expect(mockResourceContext.getParameter(paramKey)).andReturn(null);
      }
      EasyMock.replay(mockResourceContext);
    }

    // invoke buildArgs
    List<Parameter<?>> parameters = Collections.singletonList(param);
    return ArgumentBuilder.buildArgs(new Object[0], getMockResourceMethod(parameters), mockResourceContext, template, getMockResourceMethodConfig(false));
  }

  @DataProvider(name = "parameterDataWithDefault")
  @SuppressWarnings("deprecation")
  private Object[][] parameterDataWithDefault()
  {
    String intParamKey = "intParam";
    Integer intParamDefault = 123;

    String strParamKey = "strParam";
    String strParamDefault = "str";

    String arrParamKey = "arrParam";
    DataList arrParamDefault = new DataList(Arrays.asList("arrParam1", "arrParam2"));

    String recordParamKey = "recParam";
    TestRecord recordParamDefault = new TestRecord();
    recordParamDefault.setDoubleField(5.5);
    recordParamDefault.setFloatField(5F);
    recordParamDefault.setIntField(5);
    recordParamDefault.setLongField(5);

    return new Object[][]
        {
            // primitive integer parameter
            {
                intParamKey,
                Integer.TYPE,
                Parameter.ParamType.KEY,
                intParamDefault
            },
            {
                intParamKey,
                Integer.TYPE,
                Parameter.ParamType.ASSOC_KEY_PARAM,
                intParamDefault
            },
            {
                intParamKey,
                Integer.TYPE,
                Parameter.ParamType.POST,
                intParamDefault
            },
            {
                intParamKey,
                Integer.TYPE,
                Parameter.ParamType.QUERY,
                intParamDefault
            },
            // string type parameter
            {
                strParamKey,
                String.class,
                Parameter.ParamType.KEY,
                strParamDefault
            },
            {
                strParamKey,
                String.class,
                Parameter.ParamType.ASSOC_KEY_PARAM,
                strParamDefault
            },
            {
                strParamKey,
                String.class,
                Parameter.ParamType.POST,
                strParamDefault
            },
            {
                strParamKey,
                String.class,
                Parameter.ParamType.QUERY,
                strParamDefault
            },
            // array type parameter
            {
                arrParamKey,
                StringArray.class,
                Parameter.ParamType.KEY,
                arrParamDefault
            },
            {
                arrParamKey,
                StringArray.class,
                Parameter.ParamType.ASSOC_KEY_PARAM,
                arrParamDefault
            },
            {
                arrParamKey,
                StringArray.class,
                Parameter.ParamType.POST,
                arrParamDefault
            },
            {
                arrParamKey,
                StringArray.class,
                Parameter.ParamType.QUERY,
                arrParamDefault
            },
            // data template parameter
            {
                recordParamKey,
                TestRecord.class,
                Parameter.ParamType.KEY,
                recordParamDefault
            },
            {
                recordParamKey,
                TestRecord.class,
                Parameter.ParamType.ASSOC_KEY_PARAM,
                recordParamDefault
            },
            {
                recordParamKey,
                TestRecord.class,
                Parameter.ParamType.POST,
                recordParamDefault
            },
            {
                recordParamKey,
                TestRecord.class,
                Parameter.ParamType.QUERY,
                recordParamDefault
            }
        };
  }

  @Test(dataProvider = "parameterDataWithDefault")
  public void testBuildArgsOptionalWithDefault(String paramKey, Class<?> dataType, Parameter.ParamType paramType, Object defaultValue)
  {
    @SuppressWarnings({"unchecked","rawtypes"})
    Parameter<?> param = new Parameter(paramKey, dataType, DataTemplateUtil.getSchema(dataType),
        true, defaultValue, paramType, false, AnnotationSet.EMPTY);
    Object[] results = testBuildOptionalArg(param);
    Assert.assertEquals(results[0], defaultValue);
  }

  @Test(dataProvider = "parameterDataWithDefault")
  public void testBuildArgsOptionalNoDefault(String paramKey, Class<?> dataType, Parameter.ParamType paramType, Object defaultValue)
  {
    @SuppressWarnings({"unchecked","rawtypes"})
    Parameter<?> param = new Parameter(paramKey, dataType, DataTemplateUtil.getSchema(dataType),
        true, null, paramType, false, AnnotationSet.EMPTY);
    try
    {
      Object[] results = testBuildOptionalArg(param);
      if (!dataType.isPrimitive())
      {
        Assert.assertNull(results[0]);
      }
      else
      {
        Assert.fail("Expecting RoutingException for optional parameter of primitive type with no default!");
      }
    }
    catch (RoutingException ex)
    {
      if (!dataType.isPrimitive())
      {
        Assert.fail("Optional non-primitive parameter with no default should be resolved to null!");
      }
    }
  }

  @Test(expectedExceptions = RoutingException.class, dataProvider = "parameterDataWithDefault")
  public void testBuildArgsNonOptionalNoDefault(String paramKey, Class<?> dataType, Parameter.ParamType paramType, Object defaultValue)
  {
    @SuppressWarnings({"unchecked","rawtypes"})
    Parameter<?> param = new Parameter(paramKey, dataType, DataTemplateUtil.getSchema(dataType),
        false, null, paramType, false, AnnotationSet.EMPTY);
    testBuildOptionalArg(param);
  }

  @DataProvider(name = "validateQueryParameter")
  private Object[][] validateQueryParameter()
  {

    //missing required
    String recordParamKey = "recParam";
    DataMap recordParamValue = new DataMap();
    recordParamValue.put("intField", "5");
    recordParamValue.put("longField", "5");

    //field cannot be coerced
    DataMap recordParamValue2 = new DataMap();
    recordParamValue2.put("intField", "5");
    recordParamValue2.put("longField", "5");
    recordParamValue2.put("doubleField", "5.0");
    recordParamValue2.put("floatField", "invalidValue");


    //a valid example
    DataMap recordParamValue3 = new DataMap();
    recordParamValue3.put("intField", "5");
    recordParamValue3.put("longField", "5");
    recordParamValue3.put("doubleField", "5.0");
    recordParamValue3.put("floatField", "5.0");

    return new Object[][]
        {
            {
                recordParamKey,
                TestRecord.class,
                recordParamValue,
                false,
                "Input field validation failure, reason: ERROR :: /floatField :: field is required but not found and has no default value\n" +
                    "ERROR :: /doubleField :: field is required but not found and has no default value\n"

            },
            {
                recordParamKey,
                TestRecord.class,
                recordParamValue2,
                false,
                "Input field validation failure, reason: ERROR :: /floatField :: invalidValue cannot be coerced to Float\n"
            },
            {
                recordParamKey,
                TestRecord.class,
                recordParamValue3,
                true,
                null
            }
        };
  }

  @Test(dataProvider = "validateQueryParameter")
  public void testQueryParameterValidation(String paramKey, Class<?> dataType,
                                           Object paramValue,
                                           boolean isValid,
                                           String errorMessage)
  {
    Parameter<?> param = new Parameter<>(paramKey, dataType, DataTemplateUtil.getSchema(dataType),
        false, null, Parameter.ParamType.QUERY, false, AnnotationSet.EMPTY);

    ServerResourceContext mockResourceContext = EasyMock.createMock(ServerResourceContext.class);
    EasyMock.expect(mockResourceContext.getRequestAttachmentReader()).andReturn(null).anyTimes();
    EasyMock.expect(mockResourceContext.getStructuredParameter(paramKey)).andReturn(paramValue).anyTimes();
    EasyMock.replay(mockResourceContext);

    List<Parameter<?>> parameters = Collections.singletonList(param);

    try
    {
      ArgumentBuilder.buildArgs(new Object[0], getMockResourceMethod(parameters), mockResourceContext, null, getMockResourceMethodConfig(true));
      assert(isValid);
    } catch (Exception e) {
      assert(!isValid);
      assert(e.getMessage().equals(errorMessage));
    }
  }

}
