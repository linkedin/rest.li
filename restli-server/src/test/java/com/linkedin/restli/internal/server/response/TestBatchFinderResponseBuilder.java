package com.linkedin.restli.internal.server.response;

import com.google.common.collect.Lists;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.data.transform.filter.request.MaskTree;
import com.linkedin.jersey.api.uri.UriBuilder;
import com.linkedin.pegasus.generator.examples.Foo;
import com.linkedin.pegasus.generator.examples.Fruits;
import com.linkedin.r2.message.RequestContext;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import com.linkedin.restli.common.CollectionMetadata;
import com.linkedin.restli.common.HttpStatus;
import com.linkedin.restli.common.ProtocolVersion;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.restli.internal.common.AllProtocolVersions;
import com.linkedin.restli.internal.common.QueryParamsDataMap;
import com.linkedin.restli.internal.common.TestConstants;
import com.linkedin.restli.internal.server.PathKeysImpl;
import com.linkedin.restli.internal.server.ResourceContextImpl;
import com.linkedin.restli.internal.server.RoutingResult;
import com.linkedin.restli.internal.server.methods.AnyRecord;
import com.linkedin.restli.internal.server.model.AnnotationSet;
import com.linkedin.restli.internal.server.model.Parameter;
import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.internal.server.util.RestLiSyntaxException;
import com.linkedin.restli.server.BatchFinderResult;
import com.linkedin.restli.server.CollectionResult;
import com.linkedin.restli.server.PagingContext;
import com.linkedin.restli.server.ProjectionMode;
import com.linkedin.restli.server.RestLiResponseData;
import com.linkedin.restli.server.RestLiServiceException;
import com.linkedin.restli.server.annotations.BatchFinder;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.*;


public class TestBatchFinderResponseBuilder
{

  private static final class Criteria
  {
    private final Foo criteria;
    private final String nextHrefV2;
    private boolean onError;

    public Criteria(Foo criteria, String nextHrefV2)
    {
      this.criteria = criteria;
      this.nextHrefV2 = nextHrefV2;
    }

    public Foo getCriteria()
    {
      return this.criteria;
    }


    public boolean validateLink(String link, ProtocolVersion protocolVersion)
    {
      if (protocolVersion == AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion())
      {
        return !link.contains("batch_param[1]");
      }

      return link.contains(this.nextHrefV2);
    }

    public boolean getOnError()
    {
      return this.onError;
    }

    public void setOnError(boolean onError)
    {
      this.onError = onError;
    }
  }

  private static final String BATCH_PARAM = "batch_param";
  private static final int PAGE_COUNT = 1;
  private static final String BATCH_METHOD = "batch_finder";

  @DataProvider(name = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "testData")
  public Object[][] dataProvider()
  {
    List<Criteria> criteria = generateCriteria(5);
    BatchFinderResult<Foo, Foo, Foo> results = generateResults(criteria);
    BatchFinderResult<Foo, Foo, Foo> resultsWithErrors = generateResultsWithErrors(criteria);

    ProtocolVersion protocolVersion1 = AllProtocolVersions.RESTLI_PROTOCOL_1_0_0.getProtocolVersion();
    ProtocolVersion protocolVersion2 = AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion();

    List<Foo> crit = new ArrayList<>(criteria.size());
    for (Criteria c : criteria)
    {
      crit.add(c.criteria);
    }

    RestRequest requestV2 = new RestRequestBuilder(buildURI(crit, protocolVersion2)).build();
    RestRequest requestV1 = new RestRequestBuilder(buildURI(crit, protocolVersion1)).build();

    return new Object[][]{{criteria, requestV2, results, protocolVersion2, "Items ordered with protocol v2"},
                          {criteria, requestV2, resultsWithErrors, protocolVersion2, "Items ordered with errors"},
                          {criteria, requestV1, results, protocolVersion1, "Items ordered with protocol v1"},};
  }

  @Test(dataProvider = TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "testData")
  @SuppressWarnings({"unchecked", "deprecation"})
  public void testItemsOrder(List<Criteria> criteria, RestRequest request, Object results,
      ProtocolVersion protocolVersion, String label)
  {
    RoutingResult routingResult = getMockRoutingResult(criteria, protocolVersion);

    Map<String, String> headers = ResponseBuilderUtil.getHeaders();
    BatchFinderResponseBuilder responseBuilder = new BatchFinderResponseBuilder(new ErrorResponseBuilder());

    RestLiResponseData<BatchFinderResponseEnvelope> responseData =
        responseBuilder.buildRestLiResponseData(request, routingResult, results, headers, Collections.emptyList());
    RestLiResponse restResponse = responseBuilder.buildResponse(routingResult, responseData);

    Assert.assertNotNull(restResponse.getEntity());
    Assert.assertEquals(restResponse.getStatus(), HttpStatus.S_200_OK);

    List<BatchFinderResponseEnvelope.BatchFinderEntry> entries = responseData.getResponseEnvelope().getItems();
    assertEquals(entries.size(), criteria.size());

    // check the order is maintained
    for (int i = 0; i < criteria.size(); i++)
    {
      Foo currentCriteria = criteria.get(i).criteria;
      BatchFinderResponseEnvelope.BatchFinderEntry entry = entries.get(i);

      // If on error, the criteria i should have an exception with the serviceError i
      if (entry.getElements() == null)
      {
        Assert.assertNotNull(entry.getException());
        Assert.assertEquals(entry.getException().getServiceErrorCode(), currentCriteria.getIntField());
      }
      else // otherwise, at least the StringField of the first element should be equal to the criteria
      {
        Foo t = new Foo(entry.getElements().get(0).data());
        Assert.assertEquals(t.getStringField(), currentCriteria.getStringField());

        //Check paging
        CollectionMetadata paging = entry.getPaging();
        // If we have less or more elements that the number we asked for, we should not have links
        if (currentCriteria.getIntField() != PAGE_COUNT)
        {
          Assert.assertTrue(paging.getLinks().size() == 0);
        }
        else // Check the pagination format and contain only the current criteria
        {
          Assert.assertTrue(paging.getLinks().size() == 1);
          //Only 1 criteria and the criteria that match
          Assert.assertTrue(criteria.get(i).validateLink(paging.getLinks().get(0).getHref(),protocolVersion));

        }
      }
    }
  }

  private static List<Parameter<?>> getPagingContextParam()
  {
    return Lists.newArrayList(new Parameter<>("", PagingContext.class, null, false, new PagingContext(0, PAGE_COUNT),
        Parameter.ParamType.PAGING_CONTEXT_PARAM, false, new AnnotationSet(new Annotation[]{})));
  }

  private static URI buildURI(List<Foo> criteria, ProtocolVersion version)
  {
    UriBuilder builder = UriBuilder.fromPath("/");
    DataMap param = new DataMap();
    param.put("bq", BATCH_METHOD);
    QueryParamsDataMap.addSortedParams(builder, param);
    return builder.build();
  }

  private static RoutingResult getMockRoutingResult(List<Criteria> criteria, ProtocolVersion protocolVersion)
  {

    DataList param = new DataList();
    for (int i = 0; i < criteria.size(); i++)
    {
      param.add(criteria.get(i).criteria.data());
    }

    MaskTree mockMask = EasyMock.createMock(MaskTree.class);
    EasyMock.expect(mockMask.getDataMap()).andStubReturn(new DataMap());
    EasyMock.replay(mockMask);

    ResourceContextImpl mockContext = EasyMock.createMock(ResourceContextImpl.class);
    DataMap batch_method = new DataMap();
    batch_method.put("bq", BATCH_METHOD);

    EasyMock.expect(mockContext.getRestliProtocolVersion()).andStubReturn(protocolVersion);
    EasyMock.expect(mockContext.getStructuredParameter(BATCH_PARAM)).andStubReturn(param);
    EasyMock.expect(mockContext.getParameters()).andStubReturn(batch_method);
    EasyMock.expect(mockContext.getParameter("start")).andStubReturn("0");
    EasyMock.expect(mockContext.getParameter("count")).andStubReturn(Integer.toString(PAGE_COUNT));
    EasyMock.expect(mockContext.getRequestHeaders()).andStubReturn(new HashMap<>());
    EasyMock.expect(mockContext.getPagingProjectionMask()).andStubReturn(null);
    EasyMock.expect(mockContext.getProjectionMode()).andStubReturn(ProjectionMode.MANUAL);
    EasyMock.expect(mockContext.getProjectionMask()).andStubReturn(mockMask);
    EasyMock.expect(mockContext.getMetadataProjectionMask()).andStubReturn(mockMask);
    EasyMock.expect(mockContext.getMetadataProjectionMode()).andStubReturn(ProjectionMode.MANUAL);
    EasyMock.expect(mockContext.getRawRequestContext()).andStubReturn(new RequestContext());
    EasyMock.expect(mockContext.getAlwaysProjectedFields()).andReturn(Collections.emptySet()).anyTimes();
    EasyMock.expect(mockContext.isFillInDefaultsRequested()).andStubReturn(false);

    EasyMock.replay(mockContext);

    List<Parameter<?>> parameterList = new ArrayList<>();
    parameterList.add(new Parameter<>(BATCH_PARAM,
                                      String.class,
                                      null,
                                      false,
                                      null,
                                      Parameter.ParamType.QUERY,
                                      true,
                                      AnnotationSet.EMPTY));
    ResourceMethodDescriptor mockDescriptor = EasyMock.createMock(ResourceMethodDescriptor.class);
    EasyMock.expect(mockDescriptor.getAnnotation(BatchFinder.class))
        .andStubReturn(getInstanceOfAnnotation(BATCH_PARAM, BATCH_PARAM));
    EasyMock.expect(mockDescriptor.getParametersWithType(Parameter.ParamType.PAGING_CONTEXT_PARAM))
        .andStubReturn(getPagingContextParam());
    EasyMock.expect(mockDescriptor.getBatchFinderCriteriaParamIndex()).andStubReturn(0);
    EasyMock.expect(mockDescriptor.getParameters()).andStubReturn(parameterList);
    EasyMock.replay(mockDescriptor);

    RoutingResult mockRoutingResult = EasyMock.createMock(RoutingResult.class);
    EasyMock.expect(mockRoutingResult.getResourceMethod()).andStubReturn(mockDescriptor);
    EasyMock.expect(mockRoutingResult.getContext()).andStubReturn(mockContext);
    EasyMock.replay(mockRoutingResult);

    return mockRoutingResult;
  }

  private static BatchFinder getInstanceOfAnnotation(final String param, final String val)
  {
    BatchFinder annotation = new BatchFinder()
    {
      @Override
      public String batchParam()
      {
        return param;
      }

      @Override
      public String value()
      {
        return val;
      }

      @Override
      public Class<? extends Annotation> annotationType()
      {
        return BatchFinder.class;
      }
    };

    return annotation;
  }

  private static List<Criteria> generateCriteria(int nb)
  {
    List<Criteria> criteria = new ArrayList<>(nb);
    for (int i = 1; i <= nb; i++)
    {
      Foo item = new Foo().setStringField("criteria_" + i)
          .setBooleanField(true)
          .setDoubleField(3.2)
          .setFruitsField(Fruits.ORANGE)
          .setIntField(i);

      String hrefV2 = "batch_param=List((booleanField:true,doubleField:3.2,fruitsField:ORANGE,intField:" + i
          + ",stringField:criteria_" + i + "))";
      criteria.add(new Criteria(item, hrefV2));
    }

    return criteria;
  }

  private static List<Foo> generateTestList(Foo criteria)
  {
    List<Foo> items = new ArrayList<>(criteria.getIntField());
    for (int i = 0; i < criteria.getIntField(); i++)
    {
      items.add(new Foo().setStringField(criteria.getStringField()).setIntField(i));
    }
    return items;
  }

  private static BatchFinderResult<Foo, Foo, Foo> generateResults(List<Criteria> criteria)
  {
    BatchFinderResult<Foo, Foo, Foo> results = new BatchFinderResult<>();
    for (int i = 0; i < criteria.size(); i++)
    {
      List<Foo> items = generateTestList(criteria.get(i).criteria);
      results.putResult(criteria.get(i).getCriteria(), new CollectionResult<>(items));
    }

    return results;
  }

  @SuppressWarnings("deprecation")
  private static BatchFinderResult<Foo, Foo, Foo> generateResultsWithErrors(List<Criteria> criteria)
  {
    BatchFinderResult<Foo, Foo, Foo> results = new BatchFinderResult<>();
    for (int i = 0; i < criteria.size(); i++)
    {
      if (i % 2 == 0)
      {
        criteria.get(i).onError = true;
        RestLiServiceException ex = new RestLiServiceException(HttpStatus.S_500_INTERNAL_SERVER_ERROR);
        ex.setServiceErrorCode(criteria.get(i).criteria.getIntField());
        results.putError(criteria.get(i).criteria, ex);
      }
      else
      {
        List<Foo> items = generateTestList(criteria.get(i).criteria);
        results.putResult(criteria.get(i).criteria, new CollectionResult<>(items));
      }
    }

    return results;
  }

  private static Foo generateMetaData(Boolean onError)
  {
    DataMap map = new DataMap();
    map.put("OnError", onError);
    Foo foo = new Foo(map);
    return foo;
  }

  @DataProvider(name = "build_uri_test_cases")
  public Object[][] testBuildCriteriaURIDataProvider() throws RestLiSyntaxException
  {
    String[] requestURIs =
    {
        "/greetings?bq=searchGreetings&criteria=List((id:1,tone:SINCERE))&message=hello",
        "/talent/api/talentHiringProjectCandidates?bq=candidates&candidates=List((candidate:urn%3Ali%3Ats_hire_identity%3A88156577))",
        "/talent/api/talentHiringProjectCandidates?bq=candidates&candidates=List((candidate:urn%3Ali%3Ats_hire_identity%3A88156577))&fields=candidate",
        "/talent/api/talentHiringProjectCandidates?bq=candidates&candidates=List((candidate:urn%3Ali%3Ats_hire_identity%3A88156577),(candidate:urn%3Baba%3Ats_hire_identity%3A88156588))",
        "/talent/api/talentHiringProjectCandidates?bq=candidates&candidates=List((candidate:urn%3Ali%3Ats_hire_identity%3A88156577),(candidate:urn%3Baba%3Ats_hire_identity%3A88156588))&fields=candidate",
        "/test/api/restli?bq=findSomething&fields=field1&search_criteria=List((field1:val1,field2:val2))",
        "/test/api/restli?bq=findSomething&fields=field1&search_criteria=List((field1:val1,field2:val1),(field1:val2,field2:val2))",
        "/test/api/restli?bq=findSomething&fields=person:(firstname,lastname)&search_criteria=List((field1:val1,field2:val2))",
        "/groups?fields=state,locale&metadataFields=city,age&pagingFields=start,count&q=emailDomain&search=List((field1:value1))",
    };

    String[] batchCriteriaParameterNames =
    {
        "criteria",
        "candidates",
        "candidates",
        "candidates",
        "candidates",
        "search_criteria",
        "search_criteria",
        "search_criteria",
        "search",
    };

    String[] expectedURIs =
    {
        "/greetings?bq=searchGreetings&criteria=List((id:1,tone:SINCERE))&message=hello",
        "/talent/api/talentHiringProjectCandidates?bq=candidates&candidates=List((candidate:urn%3Ali%3Ats_hire_identity%3A88156577))",
        "/talent/api/talentHiringProjectCandidates?bq=candidates&candidates=List((candidate:urn%3Ali%3Ats_hire_identity%3A88156577))&fields=candidate",
        "/talent/api/talentHiringProjectCandidates?bq=candidates&candidates=List((candidate:urn%3Ali%3Ats_hire_identity%3A88156577))",
        "/talent/api/talentHiringProjectCandidates?bq=candidates&candidates=List((candidate:urn;aba%3Ats_hire_identity%3A88156588))",
        "/talent/api/talentHiringProjectCandidates?bq=candidates&candidates=List((candidate:urn%3Ali%3Ats_hire_identity%3A88156577))&fields=candidate",
        "/talent/api/talentHiringProjectCandidates?bq=candidates&candidates=List((candidate:urn;aba%3Ats_hire_identity%3A88156588))&fields=candidate",
        "/test/api/restli?bq=findSomething&fields=field1&search_criteria=List((field1:val1,field2:val2))",
        "/test/api/restli?bq=findSomething&fields=field1&search_criteria=List((field1:val1,field2:val1))",
        "/test/api/restli?bq=findSomething&fields=field1&search_criteria=List((field1:val2,field2:val2))",
        "/test/api/restli?bq=findSomething&fields=person:(firstname,lastname)&search_criteria=List((field1:val1,field2:val2))",
        "/groups?fields=state,locale&metadataFields=city,age&pagingFields=start,count&q=emailDomain&search=List((field1:value1))",
    };

    int totalCriteriaCases = expectedURIs.length;
    Object[][] testData = new Object[totalCriteriaCases][5];
    int cases = 0;
    int uriIndex = 0;
    while (cases < totalCriteriaCases)
    {
      Object[] singleCase = new Object[5];
      URI uri = URI.create(requestURIs[uriIndex]);
      RequestContext requestContext = new RequestContext();
      requestContext.putLocalAttr("timingsDisabled", true);
      ResourceContextImpl resourceContext = new ResourceContextImpl(new PathKeysImpl(),
          new RestRequestBuilder(uri)
              .setHeader(RestConstants.HEADER_RESTLI_PROTOCOL_VERSION, AllProtocolVersions.RESTLI_PROTOCOL_2_0_0.getProtocolVersion().toString())
              .build(),
          requestContext);

      DataList criteriaParameters = (DataList) resourceContext.getStructuredParameter(batchCriteriaParameterNames[uriIndex]);
      for (int i = 0; i < criteriaParameters.size(); i++)
      {
        RecordTemplate criteria = new AnyRecord((DataMap) criteriaParameters.get(i));
        singleCase[0] = resourceContext;
        singleCase[1] = criteria;
        singleCase[2] = batchCriteriaParameterNames[uriIndex];
        singleCase[3] = uri;
        singleCase[4] = URI.create(expectedURIs[cases]);
        testData[cases] = singleCase;
        cases ++;
      }
      uriIndex ++;
    }

    return testData;
  }

  @Test(dataProvider = "build_uri_test_cases")
  public void testBuildCriteriaURI(ResourceContextImpl resourceContext,
                                   RecordTemplate criteria,
                                   String batchParameterName,
                                   URI uri,
                                   URI expectedURI)
  {
    URI generatedURI = BatchFinderResponseBuilder.buildCriteriaURI(resourceContext, criteria, batchParameterName, uri);
    Assert.assertEquals(generatedURI.toString(), expectedURI.toString());
  }
}
