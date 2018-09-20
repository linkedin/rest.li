package com.linkedin.restli.server.config;

import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.internal.server.model.ResourceModel;
import com.linkedin.restli.server.PagingContext;
import com.linkedin.restli.server.test.RestLiTestHelper;
import com.linkedin.restli.server.twitter.StatusCollectionResource;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.util.Collections;

import static org.testng.Assert.assertEquals;
import static com.linkedin.restli.server.config.ResourceMethodConfigProviderImpl.DEFAULT_TIMEOUT;

public class TestResourceMethodConfigProvider
{

  @DataProvider
  public Object[][] methodConfigs()
  {
    return new Object[][]
        {
          {
            new RestLiMethodConfigBuilder(),
                  DEFAULT_TIMEOUT
          }, // empty map
          {
            new RestLiMethodConfigBuilder().addTimeoutMs("*.*", 1000L),
                  1000L
          }, // override default
          {
            new RestLiMethodConfigBuilder().addTimeoutMs("statuses.*", 1100L).addTimeoutMs("greetings.*", 2000L),
                  1100L
          }, // resource name
          {
            new RestLiMethodConfigBuilder().addTimeoutMs("*.FINDER-*", 1200L).addTimeoutMs("*.FINDER-public_timeline", 2000L),
                  2000L
          }, // operation name
          {
            new RestLiMethodConfigBuilder().addTimeoutMs("statuses.FINDER-*", 1200L).addTimeoutMs("statuses.DELETE", 2000L),
                  1200L
          }, // operation type
          {
            new RestLiMethodConfigBuilder()
                    .addTimeoutMs("*.*", 500L)
                    .addTimeoutMs("*.FINDER-*", 1000L)
                    .addTimeoutMs("statuses.*", 2000L)
                    .addTimeoutMs("statuses.GET", 2500L)
                    .addTimeoutMs("statuses.FINDER-*", 3000L)
                    .addTimeoutMs("statuses.FINDER-public_timeline", 4000L),
                  4000L
          } // multiple configuration precedence
        };
  }

  @Test(dataProvider = "methodConfigs")
  public void testMethodConfigPriority(RestLiMethodConfigBuilder configBuilder, Long timeout) throws NoSuchMethodException {
    ResourceMethodConfigProvider provider =
            ResourceMethodConfigProvider.build(configBuilder.build());
    Method method = StatusCollectionResource.class.getMethod("getPublicTimeline", PagingContext.class);
    ResourceModel model = RestLiTestHelper.buildResourceModel(StatusCollectionResource.class);
    ResourceMethodDescriptor methodDescriptor = ResourceMethodDescriptor.createForFinder(
            method,
            Collections.emptyList(),
            "public_timeline",
            null,
            ResourceMethodDescriptor.InterfaceType.SYNC,
            null);
    model.addResourceMethodDescriptor(methodDescriptor);
    ResourceMethodConfig rmc = provider.apply(methodDescriptor);
    assertEquals(rmc.getTimeoutMs().getValue(), timeout);
  }
}
