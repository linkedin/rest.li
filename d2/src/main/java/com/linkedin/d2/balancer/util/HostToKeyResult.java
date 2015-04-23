package com.linkedin.d2.balancer.util;


import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;


/**
 * Encapsulates the list of keys and host that the user can use to build multiple requests.
 * The reason why we have this class  is because in this class, a host can
 * appear multiple times in the result. Where as in MapKeyResult a host only appear once.
 *
 * @author Oby Sumampouw (osumampouw@linkedin.com)
 *
 */
public class HostToKeyResult<K>
{
  public static enum ErrorType
  {
    //this means the key cannot be mapped into a partition
    FAIL_TO_FIND_PARTITION,

    //this means even though a key maps to a partition, there is no host that belong to that partition
    NO_HOST_AVAILABLE_IN_PARTITION,

  }

  public static class UnmappedKey<K>
  {
    private final K _key;
    private final ErrorType _errorType;

    public UnmappedKey(K key, ErrorType errorType)
    {
      _key = key;
      _errorType = errorType;
    }

    public K getKey()
    {
      return _key;
    }

    public ErrorType getErrorType()
    {
      return _errorType;
    }

    @Override
    public int hashCode()
    {
      int hashCode = _key == null ? 1 : _key.hashCode() * 31;
      hashCode = 31 * hashCode * (_errorType == null ? 1 : _errorType.hashCode());
      return hashCode;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object o)
    {
      if (o == null || !(o instanceof UnmappedKey))
      {
        return false;
      }
      UnmappedKey<K> u = (UnmappedKey<K>) o;
      return this._errorType.equals(u._errorType) &&
          this._key.equals(u._key);
    }
  }

  private final Map<URI, Collection<K>> _mapResult;
  private final Collection<UnmappedKey<K>> _unmappedKeys;

  public HostToKeyResult(Map<URI, Collection<K>> mapResult, Collection<UnmappedKey<K>> unMappedKeys)
  {
    _mapResult = mapResult;
    _unmappedKeys = Collections.unmodifiableCollection(unMappedKeys);
  }

  public Map<URI, Collection<K>> getMapResult()
  {
    return _mapResult;
  }

  public Collection<UnmappedKey<K>> getUnmappedKeys()
  {
    return _unmappedKeys;
  }
}
