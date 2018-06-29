/*
   Copyright (c) 2017 LinkedIn Corp.

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

package com.linkedin.restli.server;

import com.linkedin.data.template.RecordTemplate;
import java.util.HashMap;
import java.util.Map;


/**
 * @param <QK> The type of the batch finder criteria filter
 * @param <V> The type of the resource
 * @param <MD> The type of the meta data
 *
 * @author Maxime Lamure
 */
public class BatchFinderResult<QK extends RecordTemplate, V extends RecordTemplate, MD extends RecordTemplate>
{
  private final Map<QK, CollectionResult<V, MD>> _results;
  private final Map<QK, RestLiServiceException> _errors;

  /**
   * Constructs a default <tt>BatchFinderResult</tt>
   */
  public BatchFinderResult()
  {
    this(null, null);
  }

  /**
   * Constructs a <tt>BatchFinderResult</tt> with a map of CollectionResult and errors.
   * If the parameter is null, an empty map is created.
   * The criteria filter is used as a key for the map.
   *
   * @param resultList the list of {@link CollectionResult} mapped to the criteria filters
   * @param errors the list of {@link RestLiServiceException} mapped to the criteria filters
   */
  public BatchFinderResult(Map<QK, CollectionResult<V, MD>> resultList, Map<QK, RestLiServiceException> errors)
  {
    _results = resultList == null ? new HashMap<>() : resultList;
    _errors = errors == null ? new HashMap<QK, RestLiServiceException>() : errors;
  }

  /**
   * Associates the specified {@link CollectionResult} with the specified criteria key.
   *
   * @param key the criteria to which the specified value is mapped
   * @param elements the {@link CollectionResult} to be associated with the specified key
   */
  public void putResult(QK key, CollectionResult<V, MD> elements)
  {
    this._results.put(key, elements);
  }

  /**
   * Associates the specified {@link RestLiServiceException} with the specified criteria key.
   *
   * @param key the criteria with which the specified value is mapped
   * @param error the error to be associated with the specified key
   */
  public void putError(QK key, RestLiServiceException error)
  {
    this._errors.put(key, error);
  }

  /**
   * Returns the list of {@link CollectionResult} mapped to the criteria filters
   *
   * @return the list of {@link CollectionResult}
   */
  public Map<QK, CollectionResult<V, MD>> getResults()
  {
    return _results;
  }

  /**
   * Returns the {@link CollectionResult} to which the specified key is mapped,
   * or {@code null} if this map contains no mapping for the key.
   *
   * @param key the criteria to which the specified value is mapped
   * @return the {@link CollectionResult} mapped to the key
   */
  public CollectionResult<V, MD> getResult(QK key)
  {
    return _results.get(key);
  }

  /**
   * Returns the list of {@link RestLiServiceException} mapped to the criteria filters
   *
   * @return the list of {@link RestLiServiceException}
   */
  public Map<QK, RestLiServiceException> getErrors()
  {
    return _errors;
  }

  /**
   * Returns the {@link RestLiServiceException} mapped to the criteria filters
   *
   * @return the {@link RestLiServiceException}
   */
  public RestLiServiceException getError(QK key)
  {
    return _errors.get(key);
  }

  /**
   * Removes the result for the specified key if present
   *
   * @return the {@link CollectionResult} mapped to the {param key}
   */
  public CollectionResult<V, MD> removeResult(QK key)
  {
    return _results.remove(key);
  }

  /**
   * Removes the error for the specified key if present
   *
   * @return the {@link CollectionResult} mapped to the {param key}
   */
  public RestLiServiceException removeError(QK key)
  {
    return _errors.remove(key);
  }
}
