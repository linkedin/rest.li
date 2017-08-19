package com.linkedin.restli.server;

import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.HttpStatus;

/**
 * Create a key-value response, enriching the createIdResponse with entity field. This response can be used if the
 * resource wants to return the created entity in the response of create.
 *
 * @param <K> - the key type of the resource. When using {@link com.linkedin.restli.common.ComplexResourceKey}, K should
 *           be the entire {@code ComplexResourceKey} and not just the Key part of the complex key.
 * @param <V> - the value type of the resource.
 *
 * @author Boyang Chen
 */
public class CreateKVResponse<K, V extends RecordTemplate> extends CreateResponse
{
  private final V _entity;

  public CreateKVResponse(final K id, final V entity)
  {
    super(id);
    _entity = entity;
  }

  public CreateKVResponse(final K id, final V entity, HttpStatus status)
  {
    super(id, status);
    _entity = entity;
  }

  public CreateKVResponse(RestLiServiceException error)
  {
    super(error);
    _entity = null;
  }

  @SuppressWarnings("unchecked")
  public K getId()
  {
    return (K) super.getId();
  }

  public boolean hasEntity()
  {
    return _entity != null;
  }

  public V getEntity()
  {
    return _entity;
  }
}
