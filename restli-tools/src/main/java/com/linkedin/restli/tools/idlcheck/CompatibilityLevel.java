package com.linkedin.restli.tools.idlcheck;


/**
 * Enum of idl compatibility level requirement.
 * The order of the members are critical. Least requirement member comes first.
 * All members should be in UPPERCASE.
 *
 * @author Keren Jin
 */
public enum CompatibilityLevel
{
  OFF,
  IGNORE,
  BACKWARDS,
  EQUIVALENT
}