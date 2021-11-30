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


import com.linkedin.restli.client.testutils.MockBatchResponseFactory;
import com.linkedin.restli.common.BatchResponse;
import com.linkedin.restli.examples.greetings.api.Greeting;
import java.util.HashMap;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.Test;


/**
 * @author kparikh
 */
public class TestMockBatchResponseFactory
{
  @Test
  public void testCreate()
  {
    Greeting g1 = new Greeting().setId(1L).setMessage("g1");
    Greeting g2 = new Greeting().setId(2L).setMessage("g2");

    Map<String, Greeting> recordTemplates = new HashMap<>();
    recordTemplates.put("1", g1);
    recordTemplates.put("2", g2);

    BatchResponse<Greeting> response = MockBatchResponseFactory.create(Greeting.class, recordTemplates);
    Assert.assertEquals(response.getResults(), recordTemplates);
  }
}
