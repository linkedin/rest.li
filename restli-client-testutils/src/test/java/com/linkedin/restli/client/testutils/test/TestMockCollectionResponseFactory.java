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

package com.linkedin.restli.client.testutils.test;


import com.linkedin.data.DataMap;
import com.linkedin.restli.client.testutils.MockCollectionResponseFactory;
import com.linkedin.restli.common.CollectionMetadata;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.examples.greetings.api.Greeting;
import java.util.Arrays;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * @author kparikh
 */
public class TestMockCollectionResponseFactory
{
  @Test
  public void testCreate()
  {
    Greeting g1 = new Greeting().setId(1L).setMessage("g1");
    Greeting g2 = new Greeting().setId(2L).setMessage("g2");

    List<Greeting> greetings = Arrays.asList(g1, g2);

    CollectionMetadata pagingMetadata = new CollectionMetadata().setCount(2).setStart(0).setTotal(2);

    DataMap customMetadata = new DataMap();
    customMetadata.put("foo", "bar");

    CollectionResponse<Greeting> collectionResponse =
        MockCollectionResponseFactory.create(Greeting.class, greetings, pagingMetadata, customMetadata);

    Assert.assertEquals(collectionResponse.getElements(), greetings);
    Assert.assertEquals(collectionResponse.getPaging(), pagingMetadata);
    Assert.assertEquals(collectionResponse.getMetadataRaw(), customMetadata);
  }
}
