package com.linkedin.restli.server;

import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.HttpStatus;

/**
 * Create a key-value response, enriching the createIdResponse with entity field.
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

  public K getId()
  {
    return (K)super.getId();
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
