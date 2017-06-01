package com.linkedin.d2.balancer.event;

/**
 * Empty EventEmitter with no operation.
 */

public class NoopEventEmitter implements EventEmitter
{
  @Override
  public void emitEvent(D2Monitor event)
  {
    // Nothing to do
  }
}
