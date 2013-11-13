package com.linkedin.data.collections;


/**
 * @author Keren Jin
 */
public class CheckedUtil
{
  /**
   * Add to {@link CheckedList} that does not check the added element being valid or allowed. Use with caution.
   *
   * This method checks value in assertion
   *
   * @param element provides the element to be added to the list.
   * @return true.
   * @throws UnsupportedOperationException if the list is read-only.
   */
  public static <E> boolean addWithoutChecking(CheckedList<E> list, E element)
  {
    return list.addWithAssertChecking(element);
  }

  /**
   * Set {@link CheckedList} that does not check the added element being valid or allowed. Use with caution.
   *
   * This method checks value in assertion.
   *
   * @param index of the element to replace.
   * @param element to be stored at the specified position.
   * @return the element previously at the specified position.
   */
  public static <E> E setWithoutChecking(CheckedList<E> list, int index, E element)
  {
    return list.setWithAssertChecking(index, element);
  }

  /**
   * Put to {@link CheckedMap} that does not check the added element being valid or allowed. Use with caution.
   *
   * This method checks value in assertion.
   *
   * @param key key with which the specified value is to be associated.
   * @param value to be associated with the specified key.
   * @return the previous value associated with key, or null if there was no mapping for key.
   *         A null return can also indicate that the map previously associated null with key.
   * @throws UnsupportedOperationException if the map is read-only.
   */
  public static <K, V> V putWithoutChecking(CheckedMap<K, V> map, K key, V value)
  {
    return map.putWithAssertedChecking(key, value);
  }
}
