/*
   Copyright (c) 2022 LinkedIn Corp.

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
package com.linkedin.d2.balancer.simple;

import com.linkedin.d2.balancer.util.CustomAffinityRoutingURIProvider;
import com.linkedin.util.RateLimitedLogger;
import java.net.URI;
import java.util.Optional;
import javax.annotation.Nullable;
import org.slf4j.LoggerFactory;

/**
 * A decorator for {@link CustomAffinityRoutingURIProvider} that safely handles exceptions
 * and logs errors without crashing the application.
 * It provides a fallback mechanism to ensure that if the delegate is null or throws an exception,
 * the application can continue to function without disruption.
 */
final class SafeCustomAffinityRoutingURIProviderDecorator implements CustomAffinityRoutingURIProvider {
  private static final RateLimitedLogger RATE_LIMITED_LOGGER =
      new RateLimitedLogger(LoggerFactory.getLogger(SafeCustomAffinityRoutingURIProviderDecorator.class),
          1000, // 1-second rate limit
          System::currentTimeMillis);

  @Nullable
  private final CustomAffinityRoutingURIProvider _delegate;

  public SafeCustomAffinityRoutingURIProviderDecorator(@Nullable CustomAffinityRoutingURIProvider delegate) {
    _delegate = delegate;
  }

  @Override
  public boolean isEnabled() {
    if (_delegate == null) {
      return false;
    }
    try {
      // Check if the delegate is enabled
      return _delegate.isEnabled();
    } catch (RuntimeException ex) {
      RATE_LIMITED_LOGGER.error("Error checking if CustomAffinityRoutingURIProvider is enabled", ex);
      return false;
    }
  }

  @Override
  public Optional<URI> getTargetHostURI(String clusterName) {
    if (_delegate == null) {
      return Optional.empty();
    }
    try {
      // Attempt to get the target host URI from the delegate
      return _delegate.getTargetHostURI(clusterName);
    } catch (RuntimeException ex) {
      RATE_LIMITED_LOGGER.error("Error getting target host URI for cluster: " + clusterName, ex);
      return Optional.empty();
    }
  }

  @Override
  public void setTargetHostURI(String clusterName, URI targetHostURI) {
    if (_delegate == null) {
      return;
    }
    try {
      // Attempt to set the target host URI in the delegate
      _delegate.setTargetHostURI(clusterName, targetHostURI);
    } catch (RuntimeException ex) {
      RATE_LIMITED_LOGGER.error("Error setting target host URI for cluster: " + clusterName + " to " + targetHostURI, ex);
    }
  }
}