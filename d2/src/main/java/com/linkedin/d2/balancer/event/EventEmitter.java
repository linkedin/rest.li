package com.linkedin.d2.balancer.event;

/**
 * {@link EventEmitter} defines the interface to emit D2Monitor event
 */

public interface EventEmitter
{
  void emitEvent(D2Monitor event);
}
