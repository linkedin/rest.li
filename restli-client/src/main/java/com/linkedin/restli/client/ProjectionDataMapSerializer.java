package com.linkedin.restli.client;

import com.linkedin.data.DataMap;
import com.linkedin.data.schema.PathSpec;
import java.util.Set;


/**
 * An interface to serialize projection parameters to either a String or a DataMap.
 */
public interface ProjectionDataMapSerializer
{
  /**
   * Serialize the given {@code String} projection value.
   * 
   * @param paramName The name of the projection query param to serialize.
   * @param projection The projection to serialize.
   * @return The serialized projection. If this returns null, this param is skipped when constructing. A valid return type could either be String or DataMap.
   */
  default Object serialize(String paramName, String projection) {
    return projection;
  }

  /**
   * Serialize the given {@link DataMap} projection value.
   *
   * @param paramName The name of the projection query param to serialize.
   * @param projection The projection to serialize.
   * @return The serialized projection. If this returns null, this param is skipped when constructing. A valid return type could either be String or DataMap.
   */
  default Object serialize(String paramName, DataMap projection) {
    return projection;
  }

  /**
   * Serialize the given {@code Set<PathSpec>} projection value.
   *
   * @param paramName The name of the projection query param to serialize.
   * @param projection The projection to serialize.
   * @return The serialized projection. If this returns null, this param is skipped when constructing. A valid return type could either be String or DataMap.
   */
  default Object serialize(String paramName, Set<PathSpec> projection) {
    return toDataMap(paramName, projection);
  }

  /**
   * Serialize the given set of specs to a data map. The serialized map must be a valid
   * {@link com.linkedin.data.transform.filter.request.MaskTree} representation.
   * This method will not be called if the projection is a {@code String} or {@link DataMap},
   * as well as if {@link #serialize(String, Set<PathSpec>)} is implemented.
   *
   * @param paramName The name of the projection query param to serialize.
   * @param pathSpecs The set of path specs to serialize.
   *
   * @return The serialized data map. If this returns null, this param is skipped when constructing
   * the R2 request.
   */
  DataMap toDataMap(String paramName, Set<PathSpec> pathSpecs);
}