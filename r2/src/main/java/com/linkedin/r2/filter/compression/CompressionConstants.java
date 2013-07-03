package com.linkedin.r2.filter.compression;

public class CompressionConstants
{
  protected static final int BUFFER_SIZE = 4*1024; //NOTE: works reasonably well in most cases.
  protected static final String DECODING_ERROR = "Cannot properly decode stream: ";
  protected static final String BAD_STREAM = "Bad input stream";

  protected static final String ENCODING_DELIMITER = ",";
  protected static final String QUALITY_DELIMITER = ";";
  protected static final String QUALITY_PREFIX = "q=";
}
