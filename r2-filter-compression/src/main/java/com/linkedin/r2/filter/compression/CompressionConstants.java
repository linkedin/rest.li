package com.linkedin.r2.filter.compression;

public class CompressionConstants
{
  protected static final int BUFFER_SIZE = 4*1024; //NOTE: works reasonably well in most cases.

  protected static final String DECODING_ERROR = "Cannot properly decode stream: ";
  protected static final String BAD_STREAM = "Bad input stream";

  protected static final String ILLEGAL_FORMAT = "Illegal format in Accept-Encoding: ";
  protected static final String NULL_COMPRESSOR_ERROR = "Request content encoding must be valid non-null, use \"identity\"/EncodingType.IDENTITY for no compression.";
  protected static final String NULL_ENCODING_ERROR = "Cannot use null encoding as an accept encoding value.";
  protected static final String NULL_CONFIG_ERROR = "Compression config should not be null.";

  protected static final String UNSUPPORTED_ENCODING = "Unsupported encoding referenced: ";
  protected static final String SERVER_ENCODING_ERROR = "Server returned unrecognized content encoding: ";
  protected static final String REQUEST_ANY_ERROR = "ANY may not be used as request encoding type: ";
  protected static final String INVALID_THRESHOLD = "Invalid compression threshold: ";

  protected static final String ENCODING_DELIMITER = ",";
  protected static final String QUALITY_DELIMITER = ";";
  protected static final String QUALITY_PREFIX = "q=";
}
