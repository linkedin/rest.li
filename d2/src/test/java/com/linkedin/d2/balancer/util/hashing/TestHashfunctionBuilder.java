/*
   Copyright (c) 2018 LinkedIn Corp.

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
package com.linkedin.d2.balancer.util.hashing;

import com.linkedin.d2.balancer.properties.PropertyKeys;
import com.linkedin.jersey.api.uri.UriBuilder;
import com.linkedin.r2.message.Request;
import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestRequestBuilder;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.Test;


public class TestHashfunctionBuilder
{

  @Test
  public void testHashFunctionBuilderWithMethodNone()
  {
    String hashMethod = PropertyKeys.HASH_METHOD_NONE;
    Map<String, Object> hashConfig = new HashMap<>();
    hashConfig.put(PropertyKeys.HASH_SEED, "12345");

    HashFunction<Request> hashFunction = new HashFunctionBuilder().setHashMethod(hashMethod).setHashConfig(hashConfig).build();
    URI uri = UriBuilder.fromPath("http://www.linkedin.com/tests").build();
    RestRequest request = new RestRequestBuilder(uri).build();

    int try1 = hashFunction.hash(request);
    int try2 = hashFunction.hash(request);

    Assert.assertNotEquals(try1, try2);

    hashFunction = new HashFunctionBuilder().setHashMethod(hashMethod).setHashConfig(hashConfig).build();
    int try3 = hashFunction.hash(request);
    int try4 = hashFunction.hash(request);
    Assert.assertNotEquals(try3, try4);
    Assert.assertEquals(try1, try3);
    Assert.assertEquals(try2, try4);
  }

  @Test
  public void testHashFunctionBuilderWithMethodRegex()
  {
    String hashMethod = PropertyKeys.HASH_METHOD_URI_REGEX;
    Map<String, Object> hashConfig = new HashMap<>();
    List<String> regexes = Arrays.asList("testvalue(.+)", "tryvalue(.+)");
    hashConfig.put("regexes", regexes);

    HashFunction<Request> hashFunction = new HashFunctionBuilder().setHashMethod(hashMethod).setHashConfig(hashConfig).build();
    URI uri1 = UriBuilder.fromPath("http://www.linkedin.com/testvalue(1000)").build();
    RestRequest request1 = new RestRequestBuilder(uri1).build();

    int try1 = hashFunction.hash(request1);
    int try2 = hashFunction.hash(request1);

    Assert.assertEquals(try1, try2);

    URI uri2 = UriBuilder.fromPath("http://www.linkedin.com/tryvalue(1000)").build();
    RestRequest request2 = new RestRequestBuilder(uri2).build();

    int try3 = hashFunction.hash(request2);
    Assert.assertEquals(try1, try3);

    URI uri3 = UriBuilder.fromPath("http://www.linkedin.com/othervalues1000").build();
    RestRequest request3 = new RestRequestBuilder(uri3).build();
    int try4 = hashFunction.hash(request3);
    Assert.assertNotEquals(try1, try4);
  }
}
