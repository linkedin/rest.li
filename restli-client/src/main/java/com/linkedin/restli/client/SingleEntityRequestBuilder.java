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

package com.linkedin.restli.client;


import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.ResourceSpec;


/**
 * The request builder base class for all request types that operate on a single entity.
 * @param <K> The key type
 * @param <V> The value type
 * @param <R> The request type
 */
public abstract class SingleEntityRequestBuilder<K, V extends RecordTemplate, R extends Request<?>>
    extends RestfulRequestBuilder<K, V, R>
{
  private V _input;
  private K _id;
  private Class<V> _valueClass;

  public SingleEntityRequestBuilder(String baseUriTemplate,
                                    Class<V> valueClass,

                                    ResourceSpec resourceSpec,
                                    RestliRequestOptions requestOptions)
  {
    super(baseUriTemplate, resourceSpec, requestOptions);
    _valueClass = valueClass;
  }

  /**
   * Sets the id that represents the entity that this request builder operates on.
   * @param id The id value to be set.
   * @return the request builder itself.
   */
  protected SingleEntityRequestBuilder<K, V, R> id(K id)
  {
    _id = id;
    return this;
  }

  /**
   * Sets the entity that this request builder operates on.
   * @param entity The entity to be set.
   * @return the request builder itself.
   */
  protected SingleEntityRequestBuilder<K, V, R> input(V entity)
  {
    _input = entity;
    return this;
  }

  /**
   * Gets the id that represents the entity that this request builder operates on.
   * @return the value of the id.
   */
  protected K getId()
  {
    return _id;
  }

  /**
   * Creates a new read-only id value which is decoupled from the id value set on this request builder.
   * This method should be used to get the id value while creating the request so that this request
   * builder will be totally isolated from the request that it built.
   * @return An identical read-only id value.
   */
  protected K buildReadOnlyId()
  {
    try
    {
      return getReadOnlyOrCopyKey(_id);
    }
    catch (CloneNotSupportedException cloneException)
    {
      throw new IllegalArgumentException("Key cannot be copied.", cloneException);
    }
  }

  /**
   * Gets the entity that this request builder operates on.
   * @return the value of the entity.
   */
  protected V getInput()
  {
    return _input;
  }

  /**
   * Gets the class object for the value class.
   * @return the class object for the value class.
   */
  protected Class<V> getValueClass()
  {
    return _valueClass;
  }

  /**
   * Creates a new read-only entity value which is decoupled from the entity value set on this request builder.
   * This method should be used to get the entity value while creating the request so that this request
   * builder will be totally isolated from the request that it built.
   * @return An identical read-only entity value.
   */
  protected V buildReadOnlyInput()
  {
    try
    {
      return getReadOnlyOrCopyDataTemplate(_input);
    }
    catch (CloneNotSupportedException cloneException)
    {
      throw new IllegalArgumentException("Data template cannot be cloned", cloneException);
    }
  }
}