package com.linkedin.restli.server;

import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.CursorPagination;
import java.util.List;


/**
 * Convenience extension to {@link CollectionResult} for use with cursor based pagination.
 */
public class CursorCollectionResult<T extends RecordTemplate> extends CollectionResult<T, CursorPagination>
{
  /**
   * Constructor
   *
   * @param elements       List of elements in the current page.
   * @param pagination     The cursor pagination metadata.
   */
  public CursorCollectionResult(final List<T> elements, CursorPagination pagination)
  {
    super(elements, null, pagination);
  }
}
