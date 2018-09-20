package com.linkedin.restli.server.config;

import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.server.RestLiConfig;

import java.util.function.Function;

/**
 * A functional interface to resolve method level configuration value for a given {@link ResourceMethodDescriptor}.
 *
 * @author mnchen
 */
@FunctionalInterface
public interface ResourceMethodConfigProvider extends Function<ResourceMethodDescriptor, ResourceMethodConfig>
{
  public static ResourceMethodConfigProvider build(RestLiMethodConfig config) {
    try {
      RestLiMethodConfigBuilder builder = new RestLiMethodConfigBuilder();
      builder.addConfig(ResourceMethodConfigProviderImpl.DEFAULT_CONFIG);
      builder.addConfig(config);
      return new ResourceMethodConfigProviderImpl(builder.build());
    }
    catch (ResourceMethodConfigParsingException e)
    {
      throw new RuntimeException(e);
    }
  }
}