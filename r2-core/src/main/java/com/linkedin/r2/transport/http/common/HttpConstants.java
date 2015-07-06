package com.linkedin.r2.transport.http.common;

public interface HttpConstants
{
  public static final String ACCEPT_ENCODING = "Accept-Encoding";
  public static final String CONTENT_ENCODING = "Content-Encoding";
  public static final String CONTENT_LENGTH = "Content-Length";
  public static final String TRANSFER_ENCODING = "Transfer-Encoding";

  /**
   * HTTP Cookie header name. See RFC 2109.
   */
  public static final String REQUEST_COOKIE_HEADER_NAME = "Cookie";

  /**
   * HTTP Set-Cookie header name. See RFC 2109.
   */
  public static final String RESPONSE_COOKIE_HEADER_NAME = "Set-Cookie";

  public static final int NOT_ACCEPTABLE = 406;
  public static final int OK = 200;
  public static final int UNSUPPORTED_MEDIA_TYPE = 415;
  public static final int INTERNAL_SERVER_ERROR = 500;
  public static final String ACCEPT = "Accept";
}
