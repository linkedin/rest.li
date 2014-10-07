package com.linkedin.restli.internal.server.methods.arguments;


import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.StringArray;
import com.linkedin.restli.internal.server.model.AnnotationSet;
import com.linkedin.restli.server.PathKeys;
import com.linkedin.restli.server.ResourceContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.linkedin.restli.server.TestRecord;
import com.linkedin.restli.server.TestRecordArray;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.linkedin.restli.internal.server.model.Parameter;

/**
 * Unit tests for {@See ArgumentBuilder}
 *
 * @author Oby Sumampouw
 */
public class TestArgumentBuilder
{
  @Test
  public void testBuildArgsHappyPath()
  {
    //test normal key integer
    String param1Key = "param1";
    @SuppressWarnings("unchecked")
    Parameter<Integer> param1 = new Parameter<Integer>(param1Key, Integer.class, DataTemplateUtil.getSchema(Integer.class),
                                          false, null, Parameter.ParamType.ASSOC_KEY_PARAM, false, AnnotationSet.EMPTY);
    Integer param1Value = 123;

    //test regular string argument
    String param2Key = "param2";
    @SuppressWarnings("unchecked")
    Parameter<String> param2 = new Parameter<String>(param2Key, String.class, DataTemplateUtil.getSchema(String.class),
                                          true, null, Parameter.ParamType.POST, true, AnnotationSet.EMPTY);
    String param2Value = "param2Value";

    //test data template argument array with more than element
    String param3Key = "param3";
    @SuppressWarnings("unchecked")
    Parameter<StringArray> param3 = new Parameter<StringArray>(param3Key, StringArray.class, DataTemplateUtil.getSchema(StringArray.class),
                                      true, null, Parameter.ParamType.QUERY, true, AnnotationSet.EMPTY);

    DataList param3Value = new DataList(Arrays.asList("param3a", "param3b"));
    StringArray param3Final = new StringArray(param3Value);

    //test data template argument array with only one element
    String param4Key = "param4";
    @SuppressWarnings("unchecked")
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

    // test record template array

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

    Object[] results = ArgumentBuilder.buildArgs(positionalArguments, parameters, mockResourceContext);

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
}
