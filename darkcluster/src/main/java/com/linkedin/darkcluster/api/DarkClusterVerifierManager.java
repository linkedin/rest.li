package com.linkedin.darkcluster.api;

import com.linkedin.r2.message.rest.RestRequest;
import com.linkedin.r2.message.rest.RestResponse;

/**
 * DarkClusterVerifierManager handles checking if the verifier is enabled and doing the verification on a
 * separate thread.
 */
public interface DarkClusterVerifierManager
{
  /**
   * verify the dark response if enabled
   * @param originalRequest original rest request
   * @param result dark response
   * @param darkClusterName dark cluster name
   */
  void onDarkResponse(RestRequest originalRequest, RestResponse result, String darkClusterName);

  /**
   * verify the dark error if enabled
   * @param originalRequest original rest request
   * @param e throwable
   * @param darkClusterName dark cluster name
   */
  void onDarkError(RestRequest originalRequest, Throwable e, String darkClusterName);

  /**
   * method to call when the original response comes back
   */
  void onResponse(RestRequest originalRequest, RestResponse result);

  /**
   * method to call when the original request throws
   */
  void onError(RestRequest originalRequest, Throwable e);
}
