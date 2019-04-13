package com.linkedin.restli.client;

import com.linkedin.data.DataMap;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.transform.filter.request.MaskCreator;
import java.util.Set;


/**
 * Default implementation of {@link ProjectionDataMapSerializer} that uses {@link MaskCreator} to create a serialized
 * representation of a {@link com.linkedin.data.transform.filter.request.MaskTree} as a {@link DataMap}.
 */
public class RestLiProjectionDataMapSerializer implements ProjectionDataMapSerializer
{

  public static final RestLiProjectionDataMapSerializer DEFAULT_SERIALIZER = new RestLiProjectionDataMapSerializer();

  private RestLiProjectionDataMapSerializer()
  {
    // Prevent external instantiation.
  }

  public DataMap toDataMap(String paramName, Set<PathSpec> pathSpecs)
  {
    return MaskCreator.createPositiveMask(pathSpecs).getDataMap();
  }
}
