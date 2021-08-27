package com.linkedin.restli.internal.server.methods;

import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.internal.server.methods.arguments.RestLiArgumentBuilder;
import com.linkedin.restli.internal.server.response.RestLiResponseBuilder;

public interface MethodAdapterRegistry {
  /**
   * Lookup {@link RestLiArgumentBuilder} for the given {@link ResourceMethod}.
   *
   * @param resourceMethod {@link ResourceMethod}
   * @return The {@link RestLiArgumentBuilder} for the provided {@link ResourceMethod}.
   */
  RestLiArgumentBuilder getArgumentBuilder(final ResourceMethod resourceMethod);

  /**
   * Get the {@link RestLiResponseBuilder} for the given {@link ResourceMethod}.
   *
   * @param resourceMethod {@link ResourceMethod}
   * @return The {@link RestLiResponseBuilder} for the provided {@link ResourceMethod}.
   */
  RestLiResponseBuilder<?> getResponseBuilder(final ResourceMethod resourceMethod);
}
