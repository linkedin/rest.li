package com.linkedin.restli.client.testutils.test;

import com.linkedin.data.DataMap;
import com.linkedin.restli.client.testutils.MockBatchCollectionResponseFactory;
import com.linkedin.restli.common.BatchCollectionResponse;
import com.linkedin.restli.common.BatchFinderCriteriaResult;
import com.linkedin.restli.common.CollectionMetadata;
import com.linkedin.restli.examples.greetings.api.Greeting;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Test;


public class TestMockBatchCollectionResponseFactory
{
  @Test
  public void testCreate()
  {
    Greeting g1 = new Greeting().setId(1L).setMessage("g1");
    Greeting g2 = new Greeting().setId(2L).setMessage("g2");

    List<Greeting> greetings1 = Collections.singletonList(g1);
    List<Greeting> greetings2 = Collections.singletonList(g2);

    List<List<Greeting>> greetingsList = new ArrayList<>();
    greetingsList.add(greetings1);
    greetingsList.add(greetings2);

    BatchCollectionResponse<Greeting> batchCollectionResponse = MockBatchCollectionResponseFactory.create(
        Greeting.class, greetingsList, Collections.emptyList(), Collections.emptyList());

    List<BatchFinderCriteriaResult<Greeting>> elements = batchCollectionResponse.getResults();
    Assert.assertEquals(elements.size(), 2);

    BatchFinderCriteriaResult<Greeting> criteriaResult1 = elements.get(0);
    Assert.assertEquals(criteriaResult1.getElements(), greetings1);
    Assert.assertNull(criteriaResult1.getPaging());
    Assert.assertNull(criteriaResult1.getMetadataRaw());

    BatchFinderCriteriaResult<Greeting> criteriaResult2 = elements.get(1);
    Assert.assertEquals(criteriaResult2.getElements(), greetings2);
    Assert.assertNull(criteriaResult2.getPaging());
    Assert.assertNull(criteriaResult2.getMetadataRaw());
  }

  @Test
  public void testCreateWithPagingAndMetadata()
  {
    List<List<Greeting>> greetingsList = new ArrayList<>();

    Greeting g1 = new Greeting().setId(1L).setMessage("g1");
    List<Greeting> greetings1 = Collections.singletonList(g1);
    greetingsList.add(greetings1);

    Greeting g2 = new Greeting().setId(2L).setMessage("g2");
    List<Greeting> greetings2 = Collections.singletonList(g2);
    greetingsList.add(greetings2);

    List<CollectionMetadata> pagingList = new ArrayList<>();

    CollectionMetadata paging1 = new CollectionMetadata().setCount(2).setStart(0).setTotal(2);
    pagingList.add(paging1);

    pagingList.add(null);

    List<DataMap> metadataList = new ArrayList<>();

    metadataList.add(null);

    DataMap customMetadata2 = new DataMap();
    customMetadata2.put("foo", "bar");
    metadataList.add(customMetadata2);

    BatchCollectionResponse<Greeting> batchCollectionResponse = MockBatchCollectionResponseFactory.create(
        Greeting.class, greetingsList, pagingList, metadataList);

    List<BatchFinderCriteriaResult<Greeting>> elements = batchCollectionResponse.getResults();
    Assert.assertEquals(elements.size(), 2);

    BatchFinderCriteriaResult<Greeting> criteriaResult1 = elements.get(0);
    Assert.assertEquals(criteriaResult1.getElements(), greetings1);
    Assert.assertEquals(criteriaResult1.getPaging(), paging1);
    Assert.assertNull(criteriaResult1.getMetadataRaw());

    BatchFinderCriteriaResult<Greeting> criteriaResult2 = elements.get(1);
    Assert.assertEquals(criteriaResult2.getElements(), greetings2);
    Assert.assertNull(criteriaResult2.getPaging());
    Assert.assertEquals(criteriaResult2.getMetadataRaw(), customMetadata2);
  }
}
