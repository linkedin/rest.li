package com.linkedin.r2.transport.http.client;

public interface PoolStats {
  /**
   * Get the total number of pool objects created between
   * the starting of the Pool and the call to getStats().
   * Does not include create errors.
   * @return The total number of pool objects created
   */
  public int getTotalCreated();

  /**
   * Get the total number of pool objects destroyed between
   * the starting of the Pool and the call to getStats().
   * @return The total number of pool objects destroyed
   */
  public int getTotalDestroyed();


  /**
   * Get the total number of lifecycle create errors between
   * the starting of the Pool and the call to getStats().
   * @return The total number of create errors
   */
  public int getTotalCreateErrors();

  /**
   * Get the total number of lifecycle destroy errors between
   * the starting of the Pool and the call to getStats().
   * @return The total number of destroy errors
   */
  public int getTotalDestroyErrors();

  /**
   * Get the total number of pool objects destroyed (or failed to
   * to destroy because of an error) because of disposes or failed
   * lifecycle validations between the starting of the Pool
   * and the call to getStats().
   * @return The total number of destroyed "bad" objects
   */
  public int getTotalBadDestroyed();

  /**
   * Get the total number of timed out pool objects between the
   * starting of the Pool and the call to getStats().
   * @return The total number of timed out objects
   */
  public int getTotalTimedOut();

  /**
   * Get the number of pool objects checked out at the time of
   * the call to getStats().
   * @return The number of checked out pool objects
   */
  public int getCheckedOut();

  /**
   * Get the configured maximum pool size.
   * @return The maximum pool size
   */
  public int getMaxPoolSize();

  /**
   * Get the configured minimum pool size.
   * @return The minimum pool size
   */
  public int getMinPoolSize();

  /**
   * Get the pool size at the time of the call to getStats().
   * @return The pool size
   */
  public int getPoolSize();

  /**
   * Get the maximum number of checked out objects. Reset
   * after each call to getStats().
   * @return The maximum number of checked out objects
   */
  public int getSampleMaxCheckedOut();

  /**
   * Get the maximum pool size. Reset after each call to
   * getStats().
   * @return The maximum pool size
   */
  public int getSampleMaxPoolSize();

  public String toString();
}
