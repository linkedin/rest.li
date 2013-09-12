package com.linkedin.restli.server;


/**
 * Setting for how projections are handled.
 */
public enum ProjectionMode
{
  /**
   * The rest.li framework will automatically apply projections to response bodies.
   */
  AUTOMATIC,

  /**
   * The rest.li framework will NOT apply projections to response bodies, the application code is expected
   * to apply the projection.
   */
  MANUAL;

  public static ProjectionMode getDefault()
  {
    return AUTOMATIC;
  }
}
