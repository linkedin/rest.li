package com.linkedin.d2.xds.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * Configurations for setting SSLContext are added here. Allows configuring TLS Cipher suites, TLS Version.
 * Adds preference to use TLS 1.3 which is faster and has better cipher suits.
 *
 */
public final class SslConfig {

  private SslConfig() {
  }

  public static final String DEFAULT_ALGORITHM = "SunX509";
  public static final String JKS_STORE_TYPE_NAME = "JKS";
  public static final List<String> DEFAULT_CIPHER_SUITES = Collections.unmodifiableList(Arrays.asList(
      // The following list is from https://github.com/netty/netty/blob/4.1/codec-http2/src/main/java/io/netty/handler/codec/http2/Http2SecurityUtil.java#L50
      "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",

      /* REQUIRED BY HTTP/2 SPEC */
      "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
      /* REQUIRED BY HTTP/2 SPEC */

      "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
      "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
      "TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256",
      "TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256",

      /* TLS 1.3 ciphers */
      "TLS_AES_128_GCM_SHA256",
      "TLS_AES_256_GCM_SHA384",
      "TLS_CHACHA20_POLY1305_SHA256"
  ));
  public static final List<String> DEFAULT_SSL_PROTOCOLS = Collections.unmodifiableList(
      Arrays.asList("TLSv1.2"));

}