package com.linkedin.darkcluster;

import com.linkedin.darkcluster.api.DarkRequestHeaderGenerator;
import com.linkedin.darkcluster.api.DispatcherResponseValidationMetricsHolder;
import com.linkedin.darkcluster.api.ResponseValidationMetricsHeader;
import com.linkedin.darkcluster.impl.DarkClusterConstants;
import com.linkedin.darkcluster.impl.DarkResponseMetricsHeaderGenerator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;


public class TestDarkResponseMetricsHeaderGenerator {
  @Mock
  private DispatcherResponseValidationMetricsHolder _metricsHolder;
  private Supplier<String> _sourceSupplier;
  private DarkResponseMetricsHeaderGenerator _headerGenerator;

  private static final String HOST_NAME = "host1234";

  @BeforeMethod
  public void setup() {
    MockitoAnnotations.initMocks(this);
    _sourceSupplier = () -> HOST_NAME;
    _headerGenerator = new DarkResponseMetricsHeaderGenerator(_metricsHolder, _sourceSupplier);
  }

  @Test
  public void testGetHeader() {
    Map<String, Long> metricsMap = new HashMap<>();
    metricsMap.put("SUCCESS_COUNT", 10L);
    ResponseValidationMetricsHeader.ResponseValidationMetrics metrics =
        new ResponseValidationMetricsHeader.ResponseValidationMetrics(metricsMap, 1L);
    Mockito.when(_metricsHolder.get("dark")).thenReturn(metrics);
    Optional<DarkRequestHeaderGenerator.HeaderNameValuePair> maybeNameValuePair = _headerGenerator.get("dark");
    Assert.assertTrue(maybeNameValuePair.isPresent());
    DarkRequestHeaderGenerator.HeaderNameValuePair nameValuePair = maybeNameValuePair.get();
    Assert.assertEquals(nameValuePair.getName(), DarkClusterConstants.RESPONSE_VALIDATION_METRICS_HEADER_NAME);
    Assert.assertTrue(nameValuePair.getValue().contains("SUCCESS_COUNT"));
    Assert.assertTrue(nameValuePair.getValue().contains("10"));
    Assert.assertTrue(nameValuePair.getValue().contains(HOST_NAME));
  }

  @Test
  public void testGetHeaderWithNoDarkResponseMetrics() {
    Mockito.when(_metricsHolder.get("dark")).thenReturn(null);
    Optional<DarkRequestHeaderGenerator.HeaderNameValuePair> maybeNameValuePair = _headerGenerator.get("dark");
    Assert.assertFalse(maybeNameValuePair.isPresent());
  }
}
