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
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.server.model.AnnotationSet;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.server.PagingContext;
import com.linkedin.restli.server.PathKeys;
import com.linkedin.restli.server.ResourceContext;
import com.linkedin.restli.server.RoutingException;
import com.linkedin.restli.server.annotations.HeaderParam;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import com.linkedin.restli.server.TestRecord;
import com.linkedin.restli.server.TestRecordArray;
import java.util.Map;
import java.util.Set;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.linkedin.restli.internal.server.model.Parameter;


/**
 * Unit tests for {@See ArgumentBuilder}
 *
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

  @Test
  public void testBuildArgsHappyPath()
  {
    //test integer association key integer
    String param1Key = "param1";
    Parameter<Integer> param1 = new Parameter<Integer>(param1Key, Integer.class, DataTemplateUtil.getSchema(Integer.class),
                                          false, null, Parameter.ParamType.ASSOC_KEY_PARAM, false, AnnotationSet.EMPTY);
    Integer param1Value = 123;

    //test regular string argument
    String param2Key = "param2";
    Parameter<String> param2 = new Parameter<String>(param2Key, String.class, DataTemplateUtil.getSchema(String.class),
                                          true, null, Parameter.ParamType.QUERY, true, AnnotationSet.EMPTY);
    String param2Value = "param2Value";

    //test data template argument array with more than element
    String param3Key = "param3";
    Parameter<StringArray> param3 = new Parameter<StringArray>(param3Key, StringArray.class, DataTemplateUtil.getSchema(StringArray.class),
                                      true, null, Parameter.ParamType.QUERY, true, AnnotationSet.EMPTY);

    DataList param3Value = new DataList(Arrays.asList("param3a", "param3b"));
    StringArray param3Final = new StringArray(param3Value);

    //test data template argument array with only one element
    String param4Key = "param4";
    Parameter<StringArray> param4 = new Parameter<StringArray>(param4Key, StringArray.class, DataTemplateUtil.getSchema(StringArray.class),
                                          true, null, Parameter.ParamType.QUERY, true, AnnotationSet.EMPTY);
    String param4Value = "param4Value";
    StringArray param4Final = new StringArray(new DataList(Collections.singletonList(param4Value)));

    // test record template
    String param5Key = "param5";
    Parameter<TestRecord> param5 = new Parameter<TestRecord>(param5Key, TestRecord.class, DataTemplateUtil.getSchema(TestRecord.class),
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
    Parameter<TestRecordArray> param6 = new Parameter<TestRecordArray>(param6Key, TestRecordArray.class, DataTemplateUtil.getSchema(TestRecordArray.class),
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


    List<Parameter<?>> parameters = new ArrayList<Parameter<?>>();
    parameters.add(param1);
    parameters.add(param2);
    parameters.add(param3);
    parameters.add(param4);
    parameters.add(param5);
    parameters.add(param6);
    Object[] positionalArguments = new Object[0];

    Capture<String> param1Capture = new Capture<String>();
    Capture<String> param2Capture = new Capture<String>();
    Capture<String> param3Capture = new Capture<String>();
    Capture<String> param4Capture = new Capture<String>();
    Capture<String> param5Capture = new Capture<String>();
    Capture<String> param6Capture = new Capture<String>();

    ResourceContext mockResourceContext = EasyMock.createMock(ResourceContext.class);
    PathKeys mockPathKeys = EasyMock.createMock(PathKeys.class);
    ResourceMethodDescriptor mockResourceMethodDescriptor = getMockResourceMethod(parameters);

    //easy mock for processing param1
    EasyMock.expect(mockPathKeys.get(EasyMock.capture(param1Capture))).andReturn(param1Value);
    EasyMock.expect(mockResourceContext.getPathKeys()).andReturn(mockPathKeys);
    //easy mock for processing param2
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

    Object[] results = ArgumentBuilder.buildArgs(positionalArguments, mockResourceMethodDescriptor, mockResourceContext, null);

    EasyMock.verify(mockPathKeys, mockResourceContext, mockResourceMethodDescriptor);
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

    ResourceContext mockResourceContext = EasyMock.createMock(ResourceContext.class);
    HeaderParam annotation = EasyMock.createMock(HeaderParam.class);
    EasyMock.expect(annotation.value()).andReturn(testParamKey);
    AnnotationSet annotationSet = EasyMock.createMock(AnnotationSet.class);
    EasyMock.expect(annotationSet.getAll()).andReturn(new Annotation[]{});
    EasyMock.expect(annotationSet.get(HeaderParam.class)).andReturn(annotation);
    Map<String, String> headers = new HashMap<String, String>();
    headers.put(testParamKey, expectedTestParamValue);
    EasyMock.expect(mockResourceContext.getRequestHeaders()).andReturn(headers);
    EasyMock.replay(mockResourceContext, annotation, annotationSet);

    Parameter<String> param = new Parameter<String>(testParamKey, String.class, DataSchemaConstants.STRING_DATA_SCHEMA,
        false, null, Parameter.ParamType.HEADER, false, annotationSet);
    List<Parameter<?>> parameters = Collections.<Parameter<?>>singletonList(param);

    Object[] results = ArgumentBuilder.buildArgs(new Object[0], getMockResourceMethod(parameters), mockResourceContext, null);
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

    ResourceContext mockResourceContext = EasyMock.createMock(ResourceContext.class);

    @SuppressWarnings({"unchecked","rawtypes"})
    Parameter<?> param = new Parameter(paramKey, dataType, null, false, null, paramType,
        false, AnnotationSet.EMPTY);
    List<Parameter<?>> parameters = Collections.<Parameter<?>>singletonList(param);

    Object[] results = ArgumentBuilder.buildArgs(new Object[0], getMockResourceMethod(parameters), mockResourceContext, null);
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

    ResourceContext mockResourceContext = EasyMock.createMock(ResourceContext.class);
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
    EasyMock.replay(mockResourceContext);

    Parameter<MaskTree> param = new Parameter<MaskTree>(testParamKey, MaskTree.class, null, false, null, paramType,
        false, AnnotationSet.EMPTY);
    List<Parameter<?>> parameters = Collections.<Parameter<?>>singletonList(param);

    Object[] results = ArgumentBuilder.buildArgs(new Object[0], getMockResourceMethod(parameters), mockResourceContext, null);
    Assert.assertEquals(results[0], mockMask);
  }

  @Test
  @SuppressWarnings("deprecation")
  public void testPagingContextParamType()
  {
    String testParamKey = "testParam";

    ResourceContext mockResourceContext = EasyMock.createMock(ResourceContext.class);
    PagingContext pagingContext = new PagingContext(RestConstants.DEFAULT_START, RestConstants.DEFAULT_COUNT, false, false);
    EasyMock.expect(mockResourceContext.getParameter(RestConstants.START_PARAM)).andReturn(null).anyTimes();
    EasyMock.expect(mockResourceContext.getParameter(RestConstants.COUNT_PARAM)).andReturn(null).anyTimes();
    EasyMock.replay(mockResourceContext);

    List<Parameter<?>> parameters = new ArrayList<Parameter<?>>();
    Parameter<PagingContext> param1 = new Parameter<PagingContext>(testParamKey, PagingContext.class, null,
        false, null, Parameter.ParamType.PAGING_CONTEXT_PARAM, false, AnnotationSet.EMPTY);
    Parameter<PagingContext> param2 = new Parameter<PagingContext>(testParamKey, PagingContext.class, null,
        false, null, Parameter.ParamType.CONTEXT, false, AnnotationSet.EMPTY);
    parameters.add(param1);
    parameters.add(param2);

    Object[] results = ArgumentBuilder.buildArgs(new Object[0], getMockResourceMethod(parameters), mockResourceContext, null);
    Assert.assertEquals(results[0], pagingContext);
    Assert.assertEquals(results[1], pagingContext);
  }

  @Test
  @SuppressWarnings("deprecation")
  public void testPathKeyParameterType()
  {
    String testParamKey = "testParam";
    String expectedTestParamValue = "testParamValue";

    // mock setup
    ResourceContext mockResourceContext = EasyMock.createMock(ResourceContext.class);
    PathKeys mockPathKeys = EasyMock.createMock(PathKeys.class);
    EasyMock.expect(mockPathKeys.get(testParamKey)).andReturn(expectedTestParamValue).anyTimes();
    EasyMock.expect(mockResourceContext.getPathKeys()).andReturn(mockPathKeys).anyTimes();
    EasyMock.replay(mockResourceContext, mockPathKeys);

    List<Parameter<?>> parameters = new ArrayList<Parameter<?>>();
    Parameter<String> param1 = new Parameter<String>(testParamKey, String.class, null,
      false, null, Parameter.ParamType.KEY, false, AnnotationSet.EMPTY);
    Parameter<String> param2 = new Parameter<String>(testParamKey, String.class, null,
        false, null, Parameter.ParamType.ASSOC_KEY_PARAM, false, AnnotationSet.EMPTY);
    Parameter<PathKeys> param3 = new Parameter<PathKeys>(testParamKey, PathKeys.class, null,
        false, null, Parameter.ParamType.PATH_KEYS, false, AnnotationSet.EMPTY);
    Parameter<PathKeys> param4 = new Parameter<PathKeys>(testParamKey, PathKeys.class, null,
        false, null, Parameter.ParamType.PATH_KEYS_PARAM, false, AnnotationSet.EMPTY);
    parameters.add(param1);
    parameters.add(param2);
    parameters.add(param3);
    parameters.add(param4);

    Object[] results = ArgumentBuilder.buildArgs(new Object[0], getMockResourceMethod(parameters), mockResourceContext, null);
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

    ResourceContext mockResourceContext = EasyMock.createMock(ResourceContext.class);

    List<Parameter<?>> parameters = new ArrayList<Parameter<?>>();
    Parameter<ResourceContext> param1 = new Parameter<ResourceContext>(testParamKey, ResourceContext.class, null,
        false, null, Parameter.ParamType.RESOURCE_CONTEXT, false, AnnotationSet.EMPTY);
    Parameter<ResourceContext> param2 = new Parameter<ResourceContext>(testParamKey, ResourceContext.class, null,
        false, null, Parameter.ParamType.RESOURCE_CONTEXT_PARAM, false, AnnotationSet.EMPTY);
    parameters.add(param1);
    parameters.add(param2);

    Object[] results = ArgumentBuilder.buildArgs(new Object[0], getMockResourceMethod(parameters), mockResourceContext, null);
    Assert.assertEquals(results[0], mockResourceContext);
    Assert.assertEquals(results[1], mockResourceContext);
  }

  @Test
  public void testPostParameterType()
  {
    String testParamKey = "testParam";
    String expectedTestParamValue = "testParamValue";

    ResourceContext mockResourceContext = EasyMock.createMock(ResourceContext.class);
    DataMap entityBody = new DataMap();
    entityBody.put(testParamKey, expectedTestParamValue);
    DynamicRecordTemplate template = new DynamicRecordTemplate(entityBody, null);

    Parameter<String> param = new Parameter<String>(testParamKey, String.class, DataSchemaConstants.STRING_DATA_SCHEMA,
        false, null, Parameter.ParamType.POST, false, AnnotationSet.EMPTY);
    List<Parameter<?>> parameters = Collections.<Parameter<?>>singletonList(param);

    Object[] results = ArgumentBuilder.buildArgs(new Object[0], getMockResourceMethod(parameters), mockResourceContext, template);
    Assert.assertEquals(results[0], expectedTestParamValue);
  }

  @Test
  public void testQueryParameterType()
  {
    String testParamKey = "testParam";
    String expectedTestParamValue = "testParamValue";

    ResourceContext mockResourceContext = EasyMock.createMock(ResourceContext.class);
    EasyMock.expect(mockResourceContext.getParameter(testParamKey)).andReturn(expectedTestParamValue).anyTimes();
    EasyMock.replay(mockResourceContext);

    Parameter<String> param = new Parameter<String>(testParamKey, String.class, DataSchemaConstants.STRING_DATA_SCHEMA,
        false, null, Parameter.ParamType.QUERY, false, AnnotationSet.EMPTY);
    List<Parameter<?>> parameters = Collections.<Parameter<?>>singletonList(param);

    Object[] results = ArgumentBuilder.buildArgs(new Object[0], getMockResourceMethod(parameters), mockResourceContext, null);
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

    ResourceContext mockResourceContext = EasyMock.createMock(ResourceContext.class);

    @SuppressWarnings({"unchecked","rawtypes"})
    Parameter<?> param = new Parameter(paramKey, dataType, null, false, null, paramType,
        false, AnnotationSet.EMPTY);
    List<Parameter<?>> parameters = Collections.<Parameter<?>>singletonList(param);

    ArgumentBuilder.buildArgs(new Object[0], getMockResourceMethod(parameters), mockResourceContext, null);
  }


  // utility method for reuse
  private Object[] testBuildOptionalArg(Parameter<?> param)
  {
    String paramKey = param.getName();
    Class<?> dataType = param.getType();
    Parameter.ParamType paramType = param.getParamType();

    // mock resource context
    ResourceContext mockResourceContext = EasyMock.createMock(ResourceContext.class);
    DynamicRecordTemplate template = null;
    if (paramType == Parameter.ParamType.POST)
    {
      template = new DynamicRecordTemplate(new DataMap(), null);
    }
    else
    {
      PathKeys mockPathKeys = EasyMock.createMock(PathKeys.class);
      EasyMock.expect(mockPathKeys.get(paramKey)).andReturn(null);
      EasyMock.expect(mockResourceContext.getPathKeys()).andReturn(mockPathKeys);
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
    List<Parameter<?>> parameters = Collections.<Parameter<?>>singletonList(param);
    return ArgumentBuilder.buildArgs(new Object[0], getMockResourceMethod(parameters), mockResourceContext, template);
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
}
