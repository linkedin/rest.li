package com.linkedin.r2.filter.compression;

public class CompressionConstants
{
  public static final int BUFFER_SIZE = 4*1024; //NOTE: works reasonably well in most cases.

  public static final String DECODING_ERROR = "Cannot properly decode stream: ";
  public static final String BAD_STREAM = "Bad input stream";

  public static final String ILLEGAL_FORMAT = "Illegal format in Accept-Encoding: ";
  public static final String NULL_COMPRESSOR_ERROR = "Request compression encoding must be valid non-null, use \"identity\"/EncodingType.IDENTITY for no compression.";
  protected static final String NULL_ENCODING_ERROR = "Cannot use null encoding as an accept encoding value.";
  protected static final String NULL_CONFIG_ERROR = "Compression config should not be null.";

  public static final String UNSUPPORTED_ENCODING = "Unsupported encoding referenced: ";
  public static final String SERVER_ENCODING_ERROR = "Server returned unrecognized content encoding: ";
  public static final String REQUEST_ANY_ERROR = "ANY may not be used as request encoding type: ";
  public static final String UNKNOWN_ENCODING = "Unknown encoding type: ";
  protected static final String INVALID_THRESHOLD = "Invalid compression threshold: ";


  public static final String ENCODING_DELIMITER = ",";
  public static final String QUALITY_DELIMITER = ";";
  public static final String QUALITY_PREFIX = "q=";
}
