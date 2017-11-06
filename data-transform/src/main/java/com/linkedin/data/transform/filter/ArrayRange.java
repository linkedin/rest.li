package com.linkedin.data.transform.filter;

/**
 * A helper POJO to hold the array range values (start and count).
 */
class ArrayRange
{
  static final Integer DEFAULT_START = 0;
  static final Integer DEFAULT_COUNT = Integer.MAX_VALUE;

  private final Integer _start;
  private final Integer _count;

  /**
   * Default constructor.
   */
  ArrayRange(Integer start, Integer count)
  {
    _start = start;
    _count = count;
  }

  /**
   * Returns the start value. If the start value is not present, returns null.
   */
  Integer getStart()
  {
    return _start;
  }

  /**
   * Returns the start value. If the start value is not present, returns the default value.
   */
  Integer getStartOrDefault()
  {
    return hasStart() ? _start : DEFAULT_START;
  }

  /**
   * Returns true if the count value is present and false otherwise.
   */
  boolean hasStart()
  {
    return _start != null;
  }

  /**
   * Returns the count value. If the start value is not present, returns null.
   */
  Integer getCount()
  {
    return _count;
  }

  /**
   * Returns the count value. If the start value is not present, returns the default value.
   */
  Integer getCountOrDefault()
  {
    return hasCount() ? _count : DEFAULT_COUNT;
  }

  /**
   * Returns true if the count value is present and false otherwise.
   */
  boolean hasCount()
  {
    return _count != null;
  }

  /**
   * Returns the end index (excluded) for this array range. The returned value is labelled excluded as it is one more
   * than the index of the last element included. If adding count to the start is more than the allowed maximum
   * ({@link #DEFAULT_COUNT}), the allowed maximum value will be returned.
   */
  Integer getEnd()
  {
    return getStartOrDefault() + Math.min(DEFAULT_COUNT - getStartOrDefault(), getCountOrDefault());
  }

  /**
   * Returns true if either of start and count is present. If neither of them is present the method returns false.
   */
  boolean hasAnyValue()
  {
    return hasStart() || hasCount();
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("[start: ").append(_start).append("][").append("count: ").append(_count).append("]");
    return sb.toString();
  }
}
