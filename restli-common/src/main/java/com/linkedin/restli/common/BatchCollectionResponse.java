package com.linkedin.restli.common;

import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.schema.ArrayDataSchema;
import com.linkedin.data.schema.Name;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.template.RecordTemplate;
import java.util.Arrays;


/**
 * A Collection of records. Used for returning an ordered, variable-length, navigable collection of resources for Batch.
 * Instead of using the existing CollectionResponse, this class will provide more flexibility for future feature
 * enhancement specific for BATCH_FINDER.
 */
public class BatchCollectionResponse<T extends RecordTemplate> extends CollectionResponse<T> {

  /**
   * Initialize a BatchCollectionResponse based on the type of elements it returns.
   *
   * @param elementClass the class of the elements returned
   */
  public BatchCollectionResponse(Class<T> elementClass)
  {
    super(elementClass);
  }

  /**
   * Initialize a BatchCollectionResponse based on the given dataMap and the
   * elements it returns.
   *
   * @param data a DataMap
   * @param elementClass the class of the elements returned
   */
  public BatchCollectionResponse(DataMap data, Class<T> elementClass)
  {
    super(data, elementClass);
  }

}
