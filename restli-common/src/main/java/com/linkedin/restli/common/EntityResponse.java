/*
   Copyright (c) 2014 LinkedIn Corp.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package com.linkedin.restli.common;


import com.linkedin.data.DataMap;
import com.linkedin.data.schema.Name;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.template.GetMode;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.data.template.SetMode;


/**
 * A manually crafted {@link RecordTemplate} that returns the entity in arbitrary class.
 * Requires the entity class literal.
 *
 * @author Keren Jin
 */
public class EntityResponse<E extends RecordTemplate> extends RecordTemplate
{
  public static final String ENTITY = "entity";
  public static final String STATUS = "status";
  public static final String ERROR = "error";

  private static final RecordDataSchema.Field _entityField;
  private static final RecordDataSchema.Field _statusField;
  private static final RecordDataSchema.Field _errorField;
  static
  {
    final StringBuilder errorBuilder = new StringBuilder();

    _entityField = new RecordDataSchema.Field(new RecordDataSchema(new Name(ENTITY), RecordDataSchema.RecordType.RECORD));
    _entityField.setName(ENTITY, errorBuilder);
    _entityField.setOptional(true);

    _statusField = new RecordDataSchema.Field(new RecordDataSchema(new Name(STATUS), RecordDataSchema.RecordType.RECORD));
    _statusField.setName(STATUS, errorBuilder);
    _statusField.setOptional(true);

    _errorField = new RecordDataSchema.Field(new RecordDataSchema(new Name(ERROR), RecordDataSchema.RecordType.RECORD));
    _errorField.setName(ERROR, errorBuilder);
    _errorField.setOptional(true);
  }

  private final Class<E> _entityClass;

  public EntityResponse(Class<E> entityClass)
  {
    this(new DataMap(), entityClass);
  }

  public EntityResponse(DataMap map, Class<E> entityClass)
  {
    super(map, null);
    _entityClass = entityClass;
  }

  public Class<E> getEntityClass()
  {
    return _entityClass;
  }

  public boolean hasEntry()
  {
    return contains(_entityField);
  }

  public E getEntity(GetMode mode)
  {
    return obtainWrapped(_entityField, _entityClass, mode);
  }

  public E getEntity()
  {
    return getEntity(GetMode.STRICT);
  }

  public boolean hasStatus()
  {
    return contains(_statusField);
  }

  public HttpStatus getStatus(GetMode mode)
  {
    final Integer statusNumber = obtainDirect(_statusField, Integer.class, mode);
    if (statusNumber == null)
    {
      return null;
    }
    else
    {
      return HttpStatus.fromCode(statusNumber);
    }
  }

  public HttpStatus getStatus()
  {
    return getStatus(GetMode.STRICT);
  }

  public boolean hasError()
  {
    return contains(_errorField);
  }

  public ErrorResponse getError(GetMode mode)
  {
    return obtainWrapped(_errorField, ErrorResponse.class, mode);
  }

  public ErrorResponse getError()
  {
    return getError(GetMode.STRICT);
  }

  public EntityResponse<E> setEntity(E entity, SetMode mode)
  {
    putWrapped(_entityField, _entityClass, entity, mode);
    return this;
  }

  public EntityResponse<E> setEntity(E entity)
  {
    return setEntity(entity, SetMode.DISALLOW_NULL);
  }

  public EntityResponse<E> setStatus(HttpStatus status, SetMode mode)
  {
    final Integer statusCode;
    if (status == null)
    {
      statusCode = null;
    }
    else
    {
      statusCode = status.getCode();
    }

    putDirect(_statusField, Integer.class, statusCode, mode);
    return this;
  }

  public EntityResponse<E> setStatus(HttpStatus status)
  {
    return setStatus(status, SetMode.DISALLOW_NULL);
  }

  public EntityResponse<E> setError(ErrorResponse error, SetMode mode)
  {
    putWrapped(_errorField, ErrorResponse.class, error, mode);
    return this;
  }

  public EntityResponse<E> setError(ErrorResponse error)
  {
    return setError(error, SetMode.DISALLOW_NULL);
  }
}
