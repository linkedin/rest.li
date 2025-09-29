package com.linkedin.d2.xds;

import io.grpc.CallOptions;
import io.grpc.ClientCall;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.ConnectException;
import java.net.InetSocketAddress;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class TestXdsClientValidator
{
  @Mock
  private ManagedChannel mockChannel;


  @BeforeMethod
  public void setUp()
  {
    MockitoAnnotations.initMocks(this);
  }

  // ---------------- Java version comparison ----------------
  @DataProvider(name = "javaVersionComparisonTestData")
  public Object[][] javaVersionComparisonTestData()
  {
    return new Object[][] {
        // Test case name, current version, minimum version, expected result
        {"exactMatch", "1.8.0_282", "1.8.0_282", true},
        {"exactMatch", "1.8.0_282", "1.8.0_282-msft", true},
        {"higherPatch", "1.8.0_312", "1.8.0_282", true},
        {"higherPatch", "11.0.2", "1.8.0_282-msft", true},
        {"lowerPatch", "1.8.0_121", "1.8.0_282", false},
        {"lowerPatch", "1.8.0_172", "1.8.0_282-msft", false},
        {"modernVersion", "11.0.2", "1.8.0_282", true},
        {"modernToModern", "17.0.1", "11.0.0", true},
        {"vendorPrefix", "jdk-17.0.3+7-LTS", "11.0.2", true},
        {"openjdkLegacy", "openjdk-8u352-b08", "1.8.0_282", true},
        {"invalidVersion", "abc", "11", false}
    };
  }

  @Test(dataProvider = "javaVersionComparisonTestData")
  public void testMeetsMinimumVersion(String testCaseName, String currentVersion, String minVersion, boolean expectedResult)
  {
    boolean result = XdsClientValidator.meetsMinimumVersion(currentVersion, minVersion);
    Assert.assertEquals(result, expectedResult,
        "Version comparison failed for test case: " + testCaseName +
        " (current: " + currentVersion + ", min: " + minVersion + ")");
  }

  // ---------------- Required classes ----------------
  @DataProvider(name = "requiredClassesTestData")
  public Object[][] requiredClassesTestData()
  {
    return new Object[][] {
        {"xdsSpecificClasses", new String[]{
            "com.google.protobuf.Descriptors$FileDescriptor",
            "io.envoyproxy.envoy.service.discovery.v3.AggregatedDiscoveryServiceGrpc"
        }, null, null},
        {"nonExistentClass", new String[]{"com.example.DoesNotExistClass"}, "Required classes not found", null},
        {"emptyClasses", new String[]{}, null, null},
        {"nullClasses", null, null, null}
    };
  }

  @Test(dataProvider = "requiredClassesTestData")
  public void testValidateRequiredClasses(String testCaseName, String[] classes, String expectedErrorContains, String additionalCheck)
  {
    String error = XdsClientValidator.validateRequiredClasses(classes);

    if (expectedErrorContains == null)
    {
      Assert.assertNull(error, "Expected no error for test case: " + testCaseName);
    }
    else
    {
      Assert.assertNotNull(error, "Expected error for test case: " + testCaseName);
      Assert.assertTrue(error.contains(expectedErrorContains),
          "Expected error to contain '" + expectedErrorContains + "' but got: " + error);
    }
  }

  // ---------------- Channel authority ----------------
  @DataProvider(name = "channelAuthorityTestData")
  public Object[][] channelAuthorityTestData()
  {
    return new Object[][] {
        // Test case name, authority string, expected error contains (null if success)
        {"nullChannel", null, "Managed channel is null"},
        {"emptyAuthority", "", "Cannot determine INDIS xDS server authority"},
        {"validAuthority", "localhost:80", null},
    };
  }

  @Test(dataProvider = "channelAuthorityTestData")
  public void testValidateChannelAuthority(String testCaseName, String authority, String expectedErrorContains)
  {
    ManagedChannel channel = null;
    if (authority != null)
    {
      channel = mockChannel;
      when(mockChannel.authority()).thenReturn(authority);
    }

    String error = XdsClientValidator.validateChannelAuthority(channel);

    if (expectedErrorContains == null)
    {
      Assert.assertNull(error, "Expected no error for test case: " + testCaseName);
    }
    else
    {
      Assert.assertNotNull(error, "Expected error for test case: " + testCaseName);
      Assert.assertTrue(error.contains(expectedErrorContains),
          "Expected error to contain '" + expectedErrorContains + "' but got: " + error);
    }
  }

  // ---------------- Socket connectivity ----------------
  @DataProvider(name = "socketConnectionTestData")
  public Object[][] socketConnectionTestData()
  {
    return new Object[][] {
        // Test case name, server authority, timeout, connectionBehavior, expected error contains (null if success)
        {"successfulConnection", "localhost:8080", 1000L, "SUCCESS", null},
        {"successfulConnectionIPv4", "127.0.0.1:8080", 1000L, "SUCCESS", null},
        {"successfulConnectionIPv6", "[::1]:8080", 1000L, "SUCCESS", null},
        {"connectionTimeout", "localhost:8080", 1000L, "TIMEOUT", null}, // Timeout is treated as success
        {"connectionRefused", "localhost:8080", 1000L, "REFUSED", "Failed to connect to INDIS xDS server"},
        {"validFormatMultipleColons", "bad:authority:with:colons", 1000L, "REFUSED", "Failed to connect to INDIS xDS server"},
        {"invalidFormatBadPort", "127.0.0.1:abc", 1000L, "INVALID_FORMAT", "Invalid server authority format"},
        {"validFormatNull", null, 1000L, "REFUSED", "Failed to connect to INDIS xDS server"},
        {"validFormatInvalidChars", "host with spaces:8080", 1000L, "REFUSED", "Failed to connect to INDIS xDS server"},
        {"validFormatEmpty", "", 1000L, "REFUSED", "Failed to connect to INDIS xDS server"}, // Empty string might be treated as valid but will fail to connect
        {"validFormatNoPort", "127.0.0.1", 1000L, "REFUSED", "Failed to connect to INDIS xDS server"}, // This might be valid format but missing port
        {"validFormatButBadHost", "badAuthority", 1000L, "REFUSED", "Failed to connect to INDIS xDS server"}
    };
  }

  @Test(dataProvider = "socketConnectionTestData")
  public void testValidateSocketConnection(String testCaseName, String serverAuthority, long timeout, String connectionBehavior, String expectedErrorContains)
  {
    Socket socket = spy(new Socket());

    // Mock the socket connection behavior based on the test case
    try {
      switch (connectionBehavior) {
        case "SUCCESS":
          doNothing().when(socket).connect(any(InetSocketAddress.class), anyInt());
          break;
        case "TIMEOUT":
          doThrow(new SocketTimeoutException("Connection timed out")).when(socket).connect(any(InetSocketAddress.class), anyInt());
          break;
        case "REFUSED":
          doThrow(new ConnectException("Connection refused")).when(socket).connect(any(InetSocketAddress.class), anyInt());
          break;
        case "INVALID_FORMAT":
          // For invalid formats, we don't need to mock socket.connect since HostAndPort.fromString() will throw IllegalArgumentException first
          break;
        default:
          doNothing().when(socket).connect(any(InetSocketAddress.class), anyInt());
      }
    } catch (Exception e) {
      // This won't happen in the mock setup
    }

    String error = XdsClientValidator.validateSocketConnection(serverAuthority, timeout, socket);

    if (expectedErrorContains == null)
    {
      Assert.assertNull(error, "Expected no error for test case: " + testCaseName);
    }
    else
    {
      Assert.assertNotNull(error, "Expected error for test case: " + testCaseName);
      Assert.assertTrue(error.contains(expectedErrorContains),
          "Expected error to contain '" + expectedErrorContains + "' but got: " + error);
    }
  }

  // ---------------- Health check ----------------
  @DataProvider(name = "healthCheckTestData")
  public Object[][] healthCheckTestData()
  {
    return new Object[][] {
        // Test case name, status to throw, timeout, expected error contains (null if success)
        {"deadlineExceeded", Status.DEADLINE_EXCEEDED, 50L, null},
        {"internalError", Status.INTERNAL.withDescription("boom"), 50L, "Health check failed for managed channel"}
    };
  }

  @Test(dataProvider = "healthCheckTestData")
  @SuppressWarnings("unchecked")
  public void testValidateHealthCheck(String testCaseName, Status statusToThrow, long timeout, String expectedErrorContains)
  {
    // Mock the health check to throw the specified status
    when(mockChannel.newCall(any(MethodDescriptor.class), any(CallOptions.class)))
        .thenAnswer(invocation -> {
          ClientCall<?, ?> mockCall = org.mockito.Mockito.mock(ClientCall.class);
          org.mockito.Mockito.doAnswer(invocation1 -> {
            ClientCall.Listener<?> listener = (ClientCall.Listener<?>) invocation1.getArguments()[0];
            listener.onClose(statusToThrow, new Metadata());
            return null;
          }).when(mockCall).start(any(ClientCall.Listener.class), any(Metadata.class));
          return mockCall;
        });

    String error = XdsClientValidator.validateHealthCheck(mockChannel, timeout);

    if (expectedErrorContains == null)
    {
      Assert.assertNull(error, "Expected no error for test case: " + testCaseName);
    }
    else
    {
      Assert.assertNotNull(error, "Expected error for test case: " + testCaseName);
      Assert.assertTrue(error.contains(expectedErrorContains),
          "Expected error to contain '" + expectedErrorContains + "' but got: " + error);
    }
  }

}
