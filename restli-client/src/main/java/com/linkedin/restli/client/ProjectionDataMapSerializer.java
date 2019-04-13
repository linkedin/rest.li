package com.linkedin.restli.client;

import com.linkedin.data.DataMap;
import com.linkedin.data.schema.PathSpec;
import java.util.Set;


/**
 * An interface to serialize projection parameters (ie. a set of {@link com.linkedin.data.schema.PathSpec} instances
 * to a masked data map.
 */
public interface ProjectionDataMapSerializer
{
  /**
   * Serialize the given set of specs to a data map. The serialized map must be a valid
   * {@link com.linkedin.data.transform.filter.request.MaskTree} representation.
   *
   * @param paramName The name of the projection query param to serialize.
   * @param pathSpecs The set of path specs to serialize.
   *
   * @return The serialized data map.
   */
  DataMap toDataMap(String paramName, Set<PathSpec> pathSpecs);
}