/*
   Copyright (c) 2012 LinkedIn Corp.

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

import java.util.List;

import com.linkedin.data.template.RecordTemplate;
import java.util.Objects;


public class CollectionResult<T extends RecordTemplate, MD extends RecordTemplate>
{
  private final List<T> _elements;
  private final MD      _metadata;
  private final Integer _total;
  private final PageIncrement _pageIncrement;

  public enum PageIncrement
  {
    /**
     * Paging is relative to the returned element count.
     *
     * I.e. If start=10, count=10 is requested, and 9 elements and total=100 is
     * returned, then next page is start=19, count=10.
     *
     * If total is null, once a page contains less than 'count' results, it
     * is considered to be the last page and the next page link will be excluded.
     */
    RELATIVE,

    /**
     * Paging is based on the 'total' elements.
     *
     * I.e. If start=10, count=10 is requested, and 9 elements and total=100 is
     * returned, then next page is start=20, count=10.
     *
     * 'total' must be non-null when using FIXED.
     */
    FIXED
  }

  /**
   * Constructor. Uses the default page increment mode of {@link PageIncrement#RELATIVE} and sets the total to null.
   *
   * @param elements provides the elements in current page of collection results.
   * @param metadata provides search result metadata, as defined by the application.
   */
  public CollectionResult(final List<T> elements, final MD metadata)
  {
    this(elements, null, metadata);
  }

  /**
   * Constructor. Total is set to null.
   *
   * @param elements provides the elements in current page of collection results.
   * @param metadata provides search result metadata, as defined by the application.
   * @param pageIncrement Provides the page increment mode.
   */
  public CollectionResult(final List<T> elements, final MD metadata, final PageIncrement pageIncrement)
  {
    this(elements, null, metadata, pageIncrement);
  }

  /**
   * Constructor. Uses the default page increment mode of {@link PageIncrement#RELATIVE}. Metadata is null and
   * the total is null. Next page links will only be displayed if elements size is equal to requested page count.
   *
   * @param elements provides the elements in current page of collection results.
   */
  public CollectionResult(final List<T> elements)
  {
    this(elements, null, (MD)null);
  }

  /**
   * Constructor.  Uses the default page increment mode of {@link PageIncrement#RELATIVE}.  Metadata is null.
   *
   * If total is null, next page links will only be displayed if elements size is equal to
   * requested page count.
   *
   * @param elements provides the elements in current page of collection results.
   * @param total provides the total elements, required if using {@link PageIncrement#FIXED}
   */
  public CollectionResult(final List<T> elements, final Integer total)
  {
    this(elements, total, null);
  }

  /**
   * Constructor.  Uses the default page increment mode of {@link PageIncrement#RELATIVE}.
   * @param elements provides the elements in current page of collection results.
   * @param total provides the total elements, required if using {@link PageIncrement#FIXED}
   * @param metadata provides search result metadata, as defined by the application.
   */
  public CollectionResult(final List<T> elements, final Integer total, final MD metadata)
  {
    this(elements, total, metadata, PageIncrement.RELATIVE);
  }

  /**
   * Constructor.
   *
   * @param elements provides the elements in current page of collection results.
   * @param total provides the total elements, required if using {@link PageIncrement#FIXED}
   * @param metadata provides search result metadata, as defined by the application.
   * @param pageIncrement Provides the page increment mode.
   */
  public CollectionResult(final List<T> elements, final Integer total, final MD metadata, final PageIncrement pageIncrement)
  {
    _elements = elements;
    _total = total;
    _metadata = metadata;
    _pageIncrement = pageIncrement;
    if(_pageIncrement == PageIncrement.FIXED && _total == null) throw new IllegalArgumentException("'total' must be non null if PageIncrement is FIXED.");
  }

  public List<T> getElements()
  {
    return _elements;
  }

  public PageIncrement getPageIncrement()
  {
    return _pageIncrement;
  }

  public boolean getHasTotal()
  {
    return _total != null;
  }

  public Integer getTotal()
  {
    return _total;
  }

  public MD getMetadata()
  {
    return _metadata;
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null || getClass() != object.getClass()) {
      return false;
    }
    CollectionResult<?, ?> that = (CollectionResult<?, ?>) object;
    return Objects.equals(_elements, that._elements)
        && Objects.equals(_metadata, that._metadata)
        && Objects.equals(_total, that._total)
        && _pageIncrement == that._pageIncrement;
  }

  @Override
  public int hashCode() {
    return Objects.hash(_elements, _metadata, _total, _pageIncrement);
  }
}
