/*
   Copyright (c) 2023 LinkedIn Corp.

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

package com.linkedin.d2.xds;

import io.grpc.ManagedChannel;
import java.net.InetSocketAddress;
import javax.annotation.Nullable;
import java.net.Socket;
import java.net.SocketTimeoutException;
import com.google.common.net.HostAndPort;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validation utilities for XDS client pre-checks.
 * This class contains all the validation logic that should be performed before starting an XDS client.
 */
public class XdsClientValidator
{
  private static final Logger LOG = LoggerFactory.getLogger(XdsClientValidator.class);

  public static final String DEFAULT_MINIMUM_JAVA_VERSION = "1.8.0_282";
  public static final ActionOnPrecheckFailure DEFAULT_ACTION_ON_PRECHECK_FAILURE = ActionOnPrecheckFailure.ERROR;

  // Required class names for XDS functionality
  private static final String PROTOBUF_FILE_DESCRIPTOR_CLASS = "com.google.protobuf.Descriptors$FileDescriptor";
  private static final String ENVOY_AGGREGATED_DISCOVERY_SERVICE_CLASS = "io.envoyproxy.envoy.service.discovery.v3.AggregatedDiscoveryServiceGrpc";

  /**
   * Action to take when pre-check validation fails.
   */
  public enum ActionOnPrecheckFailure
  {
    ERROR,
    // Throw exception and stop client creation
    THROW
  }

  /**
   * Performs all pre-checks required for INDIS connection.
   * This includes Java version validation, class availability checks, network connectivity tests, etc.
   *
   * @param managedChannel     the gRPC managed channel
   * @param readyTimeoutMillis timeout for connection checks in milliseconds
   * @param minimumJavaVersion the minimum required Java version
   * @param actionOnFailure    action to take when validation fails
   */
  public static void preCheckForIndisConnection(@Nullable ManagedChannel managedChannel, long readyTimeoutMillis,
      String minimumJavaVersion, ActionOnPrecheckFailure actionOnFailure)
  {
    try
    {
      String errorMsg = processValidation(managedChannel, readyTimeoutMillis, minimumJavaVersion);
      if (errorMsg == null)
      {
        LOG.info(
            "[xds pre-check] All pre-checks for INDIS connection passed successfully, ready to start xDS RPC stream");
      }
      else
      {
        handlePrecheckFailure(errorMsg, actionOnFailure);
      }
    }
    catch (Exception e)
    {
      String errorMsg = "[xds pre-check] Unexpected exception during pre-checks: " + e.getMessage();
      handlePrecheckFailure(errorMsg, actionOnFailure);
    }
  }

  /**
   * This method focuses purely on validation and returns error messages on failure.
   *
   * @param managedChannel     the gRPC managed channel
   * @param readyTimeoutMillis timeout for connection checks in milliseconds
   * @param minimumJavaVersion the minimum required Java version
   * @return null if all pre-checks pass, error message if validation fails
   */
  @Nullable
  static String processValidation(@Nullable ManagedChannel managedChannel, long readyTimeoutMillis,
      String minimumJavaVersion)
  {
    // Check Java version
    String javaVersionError = validateJavaVersion(minimumJavaVersion);
    if (javaVersionError != null)
    {
      return javaVersionError;
    }

    // Check required classes
    String classAvailabilityError =
        validateRequiredClasses(PROTOBUF_FILE_DESCRIPTOR_CLASS, ENVOY_AGGREGATED_DISCOVERY_SERVICE_CLASS);
    if (classAvailabilityError != null)
    {
      return classAvailabilityError;
    }

    // Check channel and authority
    String authorityError = validateChannelAuthority(managedChannel);
    if (authorityError != null)
    {
      return authorityError;
    }

    // Check socket connection to rule out NACL issue
    String socketError = validateSocketConnection(managedChannel.authority(), readyTimeoutMillis, new Socket());
    if (socketError != null)
    {
      return socketError;
    }

    // Check io socket health check for channel to rule out cert/ssl issue
    String healthCheckError = validateHealthCheck(managedChannel, readyTimeoutMillis);
    if (healthCheckError != null)
    {
      return healthCheckError;
    }

    return null; // All validations passed
  }

  /**
   * Validates Java version meets minimum requirements.
   *
   * @param minimumJavaVersion the minimum required Java version
   * @return error message if validation fails, null if passes
   */
  static String validateJavaVersion(String minimumJavaVersion)
  {
    String javaVersion = System.getProperty("java.version");
    String requiredVersion = minimumJavaVersion != null ? minimumJavaVersion : DEFAULT_MINIMUM_JAVA_VERSION;
    LOG.info("Current Java version: {}, minimum required: {}", javaVersion, requiredVersion);
    if (!meetsMinimumVersion(javaVersion, requiredVersion))
    {
      return "The current Java version " + javaVersion + " is too low, please upgrade to at least " + requiredVersion + ", " +
          "otherwise the service couldn't create grpc connection between service and INDIS, " +
          "check go/onboardindis Guidelines #2 for more details";
    }
    return null;
  }

  /**
   * Validates that the provided classes are available on the classpath.
   *
   * @param classNames fully-qualified class names to validate
   * @return error message if validation fails, null if all classes are present
   */
  @Nullable
  static String validateRequiredClasses(String... classNames)
  {
    try
    {
      if (classNames == null)
      {
        return null;
      }

      for (String className : classNames)
      {
        Class.forName(className);
        LOG.info("[xds pre-check] Required class available: {}", className);
      }
      return null;
    }
    catch (ClassNotFoundException | NoClassDefFoundError e)
    {
      return "[xds pre-check] Required classes not found, check go/onboardindis Guidelines #3 and #4: " + e.getMessage();
    }
    catch (Exception e)
    {
      return "[xds pre-check] Unexpected exception during class availability check: " + e.getMessage();
    }
  }

  /**
   * Validates channel and authority.
   *
   * @param managedChannel the gRPC managed channel
   * @return error message if validation fails, null if passes
   */
  @Nullable
  static String validateChannelAuthority(@Nullable ManagedChannel managedChannel)
  {
    if (managedChannel == null)
    {
      return "[xds pre-check] Managed channel is null, cannot establish XDS connection to INDIS server";
    }

    String serverAuthority = managedChannel.authority();
    if (serverAuthority == null || serverAuthority.isEmpty())
    {
      return "[xds pre-check] Cannot determine INDIS xDS server authority from managed channel, connection check " +
          "failed";
    }
    LOG.info("[xds pre-check] INDIS xDS server authority: {}", serverAuthority);
    return null;
  }

  /**
   * Validates socket connection to server.
   *
   * @param serverAuthority    the server authority string
   * @param readyTimeoutMillis timeout for connection checks in milliseconds
   * @return error message if validation fails, null if passes
   */
  @Nullable
  static String validateSocketConnection(String serverAuthority, long readyTimeoutMillis, Socket socket)
  {
    try
    {
      HostAndPort hostAndPort = HostAndPort.fromString(serverAuthority);
      String host = hostAndPort.getHost();
      int port = hostAndPort.getPort();

      LOG.info("[xds pre-check] Testing socket connection to INDIS xDS server: {}:{}", host, port);
      // Check if we can connect to the server using a socket
      socket.connect(new InetSocketAddress(host, port), (int) readyTimeoutMillis);
      LOG.info("[xds pre-check] Successfully pinged to INDIS xDS server at {}:{}", host, port);
      return null;

    }
    catch (IllegalArgumentException e)
    {
      return "[xds pre-check] Invalid server authority format: " + serverAuthority + ", expected format: host:port or" +
          " [host]:port for IPv6";
    }
    catch (SocketTimeoutException e)
    {
      LOG.warn("[xds pre-check] Connection timeout to INDIS xDS server at authority " + serverAuthority +
          " within " + readyTimeoutMillis + "ms. This may be transient - if the issue persists, " +
          "check go/onboardindis Guidelines #5 and #7 for more details" + e.getMessage());
      return null;
    }
    catch (Exception e)
    {
      return "[xds pre-check] Failed to connect to INDIS xDS server at authority " + serverAuthority +
          ", check go/onboardindis Guidelines #5 and #7 for more details: " + e.getMessage();
    }
  }

  /**
   * Validates health check for managed channel.
   *
   * @param managedChannel     the gRPC managed channel
   * @param readyTimeoutMillis timeout for health checks in milliseconds
   * @return error message if validation fails, null if passes
   */
  @Nullable
  static String validateHealthCheck(ManagedChannel managedChannel, long readyTimeoutMillis)
  {
    try
    {
      io.grpc.health.v1.HealthGrpc.newBlockingStub(managedChannel)
          .withDeadlineAfter(readyTimeoutMillis, java.util.concurrent.TimeUnit.MILLISECONDS)
          .check(io.grpc.health.v1.HealthCheckRequest.newBuilder().setService("").build());
      LOG.info("[xds pre-check] Health check for managed channel passed - channel is SERVING");
      return null;
    }
    catch (Exception e)
    {
      if (e instanceof StatusRuntimeException && ((StatusRuntimeException) e).getStatus().getCode() == io.grpc.Status.Code.DEADLINE_EXCEEDED)
      {
        LOG.warn("[xds pre-check] Health check timeout for managed channel within " + readyTimeoutMillis + "ms. " +
                     "This may be transient - if the issue persists, check go/onboardindis Guidelines #2 and #9 for " +
                     "more details" + e.getMessage());
        return null;
      }
      else
      {
        Throwable c = e;
        while (c.getCause() != null) c = c.getCause();
        return "[xds pre-check] Health check failed for managed channel: " + c.getClass().getName() + " | " + c.getMessage() +
            ", check go/onboardindis Guidelines #2 and #9 for more details, " +
            "if there is any other sslContext issue, please check with #pki team";
      }
    }
  }

  /**
   * Checks if the current Java version meets the minimum required version.
   * Supports all Java versions including 8, 9, 10, 11, 17, 21, etc.
   *
   * @param currentVersion the current Java version string (e.g., "1.8.0_172", "11.0.1", "17.0.2")
   * @param minVersion     the minimum required version string (e.g., "1.8.0_282", "11.0.1", "17.0.0")
   * @return true if the current version meets or exceeds the minimum requirement, false otherwise
   */
  public static boolean meetsMinimumVersion(String currentVersion, String minVersion)
  {
    if (currentVersion == null || currentVersion.trim().isEmpty() ||
        minVersion == null || minVersion.trim().isEmpty())
    {
      LOG.warn("Invalid Java version string: current='{}', required='{}'", currentVersion, minVersion);
      return false;
    }

    try
    {
      // Check if either version is clearly invalid (contains non-numeric characters after normalization)
      String normalizedCurrent = normalizeJavaVersion(currentVersion);
      String normalizedMin = normalizeJavaVersion(minVersion);

      if (isInvalidVersionString(normalizedCurrent) || isInvalidVersionString(normalizedMin))
      {
        LOG.warn("Invalid Java version format: current='{}', required='{}'", currentVersion, minVersion);
        return false;
      }

      return compareJavaVersions(normalizedCurrent, normalizedMin) >= 0;
    }
    catch (Exception e)
    {
      LOG.warn("Failed to parse Java versions. Current: {}, Min: {}", currentVersion, minVersion, e);
      return false;
    }
  }

  /**
   * Checks if a normalized version string is invalid (contains non-numeric characters).
   *
   * @param normalizedVersion the normalized version string
   * @return true if the version string is invalid, false otherwise
   */
  private static boolean isInvalidVersionString(String normalizedVersion)
  {
    if (normalizedVersion == null || normalizedVersion.isEmpty() || normalizedVersion.equals("0"))
    {
      return true;
    }

    // Check if the normalized version contains only digits and dots
    return !normalizedVersion.matches("^[0-9.]+$");
  }

  /**
   * Compares two Java version strings.
   * Returns: positive if a > b, zero if equal, negative if a < b
   */
  private static int compareJavaVersions(String a, String b)
  {
    if (a.equals(b))
    {
      return 0;
    }

    int[] partsA = parseVersionParts(a);
    int[] partsB = parseVersionParts(b);

    int maxLen = Math.max(partsA.length, partsB.length);
    for (int i = 0; i < maxLen; i++)
    {
      int partA = i < partsA.length ? partsA[i] : 0;
      int partB = i < partsB.length ? partsB[i] : 0;

      if (partA != partB)
      {
        return Integer.compare(partA, partB);
      }
    }
    return 0;
  }

  /**
   * Normalizes Java version string for comparison.
   * Handles common Java version formats: 1.8.0_282, 11.0.1, 17.0.2, etc.
   */
  private static String normalizeJavaVersion(String version)
  {
    if (version == null || version.trim().isEmpty())
    {
      return "0";
    }

    String s = version.trim().toLowerCase();

    // Remove vendor prefixes
    if (s.startsWith("jdk-") || s.startsWith("jre-"))
    {
      s = s.substring(4);
    }
    else if (s.startsWith("openjdk-") || s.startsWith("temurin-") ||
        s.startsWith("adoptopenjdk-") || s.startsWith("oracle-"))
    {
      s = s.substring(s.indexOf('-') + 1);
    }

    // Handle legacy "1.x" format
    if (s.startsWith("1."))
    {
      s = s.substring(2);
    }

    // Convert separators to dots
    s = s.replace('_', '.').replace('+', '.');

    // Remove prerelease tags (everything after first dash)
    int dashIndex = s.indexOf('-');
    if (dashIndex >= 0)
    {
      s = s.substring(0, dashIndex);
    }

    // Keep only digits and dots, remove other characters
    s = s.replaceAll("[^0-9.]", "").replaceAll("\\.+", ".");

    return s.isEmpty() ? "0" : s;
  }

  /**
   * Parses version string into integer array.
   * Handles version strings like "1.8.0.282" or "11.0.1".
   */
  private static int[] parseVersionParts(String version)
  {
    if (version == null || version.isEmpty())
    {
      return new int[0];
    }

    String[] parts = version.split("\\.");
    int[] result = new int[parts.length];

    for (int i = 0; i < parts.length; i++)
    {
      try
      {
        // Remove leading zeros and parse
        String part = parts[i].replaceFirst("^0+(?!$)", "");
        if (part.isEmpty()) part = "0";

        long value = Long.parseLong(part);
        result[i] = (int) Math.min(value, Integer.MAX_VALUE);
      }
      catch (NumberFormatException e)
      {
        result[i] = 0; // Invalid number, treat as 0
      }
    }

    return result;
  }


  /**
   * Helper function to handle pre-check failure based on the configured action.
   *
   * @param errorMessage    the error message to log or include in exception
   * @param actionOnFailure the action to take on failure
   * @throws IllegalStateException if actionOnFailure is THROW
   */
  private static void handlePrecheckFailure(String errorMessage, ActionOnPrecheckFailure actionOnFailure)
  {
    switch (actionOnFailure)
    {
      case ERROR:
        LOG.error(errorMessage);
        break;
      case THROW:
        throw new IllegalStateException(errorMessage);
      default:
        throw new IllegalArgumentException("Unknown action on precheck failure: " + actionOnFailure);
    }
  }

  private XdsClientValidator()
  {
    // Utility class, no instantiation
  }
}
