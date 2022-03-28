package com.linkedin.restli.examples;

import com.linkedin.data.ByteString;
import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.restli.client.Request;
import com.linkedin.restli.client.ResponseFuture;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.examples.greetings.api.Greeting;
import com.linkedin.restli.examples.greetings.client.ByteStringArrayBuilders;
import com.linkedin.restli.examples.greetings.client.ByteStringArrayRequestBuilders;
import com.linkedin.restli.test.util.RootBuilderWrapper;
import java.util.Arrays;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


public class TestByteStringArrayAsQueryParam extends RestLiIntegrationTest{
    @BeforeClass
    public void initClass() throws Exception
    {
      super.init();
    }

    @AfterClass
    public void shutDown() throws Exception
    {
      super.shutdown();
    }


  @Test(dataProvider = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "byteStringArrayQueryParamRequestBuilderDataProvider")
  public void testByteStringArrayAsQueryParam(RootBuilderWrapper<Long, Greeting> builders) throws
                                                                                           RemoteInvocationException
  {
    ByteString bs1 = ByteString.copyString("bytestring one", "ASCII");
    ByteString bs2 = ByteString.copyString("bytestring one", "ASCII");

    Request<CollectionResponse<Greeting>> request = builders.findBy("byteStringArrayFinder").setQueryParam("byteStrings",
        Arrays.asList(bs1, bs2)).build();
    ResponseFuture<CollectionResponse<Greeting>> future = getClient().sendRequest(request);
    CollectionResponse<Greeting> response = future.getResponse().getEntity();

    List<Greeting> result = response.getElements();

    Assert.assertTrue(result.isEmpty());
  }

  @DataProvider(name = com.linkedin.restli.internal.common.TestConstants.RESTLI_PROTOCOL_1_2_PREFIX + "byteStringArrayQueryParamRequestBuilderDataProvider")
  private static Object[][] byteStringArrayQueryParamRequestBuilderDataProvider()
  {
    return new Object[][] {
        { new RootBuilderWrapper<Long, Greeting>(new ByteStringArrayBuilders())},
        { new RootBuilderWrapper<Long, Greeting>(new ByteStringArrayRequestBuilders())}
    };
  }
}
