package com.linkedin.data.schema.compatibility;


import java.util.Collection;


/**
 * Provides the compatibility check result.
 */
public interface CompatibilityResult
{
  /**
   * Return the compatibility check result messages.
   *
   * @return the compatibility check result messages.
   */
  Collection<CompatibilityMessage> getMessages();

  /**
   * Return whether the messages contains any error messages.
   *
   * @return whether the messages contains any error messages.
   */
  boolean isError();
}
