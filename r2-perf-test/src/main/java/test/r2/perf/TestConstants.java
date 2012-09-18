package test.r2.perf;

import java.net.URI;

public interface TestConstants
{
  // Properties names
  static final String HEADER_PROP_NAME = "perf.header";
  static final String SERVER_HOST_PROP_NAME = "perf.host";
  static final String SERVER_PORT_PROP_NAME = "perf.port";
  static final String SERVER_RELATIVE_URI_PROP_NAME = "perf.uri";
  static final String DATA_FILE_PROP_NAME = "perf.datafile";
  static final String DELAY_PROP_NAME = "perf.delay";
  static final String RANGE_PROP_NAME = "perf.range";
  static final String REQUEST_LENGTH_PROP_NAME = "perf.request.length";
  static final String REQUEST_LENGTH_RANDOM_PROP_NAME = "perf.request.length.randomize";
  static final String THREADS_NUMBER_PROP_NAME = "perf.threads";
  static final String RUNS_NUMBER_PROP_NAME = "perf.runs";
  static final String RAMPUP_PROP_NAME = "perf.rampup";

  // Default values
  static final String DEFAULT_HOST = "localhost";
  static final String DEFAULT_PORT = "8083";
  static final String DEFAULT_RELATIVE_URI = "/echo";
  static final String DEFAULT_DATAFILE = "";
  static final String DEFAULT_LABEL = "SimpleRestClient";
  static final String DEFAULT_HEADER = "X-LinkedIn-Auth-Member";
  static final String DEFAULT_DELAY = "0.0";
  static final String DEFAULT_RANGE = "0.0";
  static final String DEFAULT_REQUEST_LENGTH = "-1";
  static final String DEFAULT_REQUEST_LENGTH_RANDOMIZE = "false";
  static final String DEFAULT_RESPONSE_CODE = "";
  static final String DEFAULT_RESPONSE_MESSAGE = "";
  static final String DEFAULT_THREADS = "1";
  static final String DEFAULT_RUNS = "1";
  static final String DEFAULT_RAMPUP = "0";

}
