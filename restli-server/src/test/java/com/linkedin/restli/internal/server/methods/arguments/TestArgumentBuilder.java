package com.linkedin.restli.internal.server.methods.arguments;


import com.linkedin.data.DataList;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.StringArray;
import com.linkedin.restli.internal.server.model.AnnotationSet;
import com.linkedin.restli.server.PathKeys;
import com.linkedin.restli.server.ResourceContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
                                          false, null, Parameter.ParamType.KEY, false, AnnotationSet.EMPTY);
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

    //test data template argument array with only one element
    String param4Key = "param4";
    @SuppressWarnings("unchecked")
    Parameter<StringArray> param4 = new Parameter<StringArray>(param4Key, StringArray.class, DataTemplateUtil.getSchema(StringArray.class),
                                          true, null, Parameter.ParamType.QUERY, true, AnnotationSet.EMPTY);
    String param4Value = "param4Value";

    List<Parameter<?>> parameters = new ArrayList<Parameter<?>>();
    parameters.add(param1);
    parameters.add(param2);
    parameters.add(param3);
    parameters.add(param4);
    Object[] positionalArguments = new Object[0];

    Capture<String> param1Capture = new Capture<String>();
    Capture<String> param2Capture = new Capture<String>();
    Capture<String> param3Capture = new Capture<String>();
    Capture<String> param4Capture = new Capture<String>();

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
    EasyMock.replay(mockResourceContext, mockPathKeys);

    Object[] results = ArgumentBuilder.buildArgs(positionalArguments, parameters, mockResourceContext);

    EasyMock.verify(mockPathKeys, mockResourceContext);
    Assert.assertEquals(param1Capture.getValue(), param1Key);
    Assert.assertEquals(param2Capture.getValue(), param2Key);
    Assert.assertEquals(param3Capture.getValue(), param3Key);
    Assert.assertEquals(param4Capture.getValue(), param4Key);

    Assert.assertEquals(results[0], param1Value);
    Assert.assertEquals(results[1], param2Value);
    StringArray param3Array = new StringArray(param3Value);
    Assert.assertEquals(results[2], param3Array);

    List<String> param4List = new ArrayList<String>();
    param4List.add(param4Value);
    StringArray param4Array = new StringArray(new DataList(param4List));
    Assert.assertEquals(results[3], param4Array);
  }
}
