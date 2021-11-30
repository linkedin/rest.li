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

package com.linkedin.restli.server.test;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import org.easymock.EasyMock;
import org.easymock.IArgumentMatcher;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

/**
 * @author dellamag
 */
public class EasyMockUtils
{
  /**
   * Static collection of custom "eq" methods, which are required when using custom
   * argument matchers in {@link EasyMock#expect(Object)} calls.
   *
   * @author dellamag
   * @see IArgumentMatcher
   */
  public static class Matchers
  {
    public static <F, T> Collection<F> eqCollection(CollectionArgumentMatcher<F, T> matcher)
    {
      EasyMock.reportMatcher(matcher);
      return null;
    }

    public static <F> Collection<F> eqCollectionOrdered(Collection<F> expected)
    {
      EasyMock.reportMatcher(CollectionArgumentMatcher.createOrderedExactMatcher(expected));
      return null;
    }

    public static <F> Collection<F> eqCollectionUnordered(Collection<F> expected)
    {
      EasyMock.reportMatcher(CollectionArgumentMatcher.createUnorderedExactMatcher(expected));
      return null;
    }

    public static <F> Collection<F> eqCollectionSize(Collection<F> expected)
    {
      EasyMock.reportMatcher(CollectionArgumentMatcher.createSizeMatcher(expected));
      return null;
    }
  }

  /**
   * Generic EasyMock {@link IArgumentMatcher} that performs a "deep" match on any
   * Collection i.e. verifies that the elements in the expected and actual Collections
   * are equal. This implementation supports the following features:
   *
   * <ul>
   * <li>Three match types: size => no deep checking, only list sizes are checked),
   *                        unordered => element equality checking, but collection order doesn't matter
   *                        ordered => element equality checking and elements must be iterable
   *                                   in the same order
   * <li>Optional transformation of elements to facilitate easier matching. For example,
   *     you may want to match two collections of ProfileInfo objects such that memberID is
   *     the only field considered in the equality check.
   * <li>Convenient factory methods to create "identity" matchers that bypass the
   *     transformation step
   * </ul>
   *
   * @author dellamag
   */
  public static class CollectionArgumentMatcher<F, T> implements IArgumentMatcher
  {
    public enum MatchType
    {
      size,
      unordered,
      ordered;
    }

    private final Collection<F> _collection;
    private final Function<F, T> _function;

    private Integer _actualSize;
    private T _expectedElement;
    private T _actualElement;
    private final MatchType _matchType;

    /**
     * Constructor.
     */
    public CollectionArgumentMatcher(Collection<F> collection,
                                     Function<F, T> function,
                                     MatchType matchType)
    {
      _collection = collection;
      _function = function;
      _matchType = matchType;
    }

    /**
     * @see org.easymock.IArgumentMatcher#appendTo(java.lang.StringBuffer)
     */
    @Override
    public void appendTo(StringBuffer sb)
    {
      if (_actualSize != null)
      {
        sb.append(" expected ").append(_collection.size()).append(", but actual size was " + _actualSize);
      }
      else if (_expectedElement != null && _actualElement != null)
      {
        sb.append(" expected ").append(_expectedElement).append(", but was " + _actualElement);
      }
      else if (_expectedElement != null)
      {
        sb.append(" expected ").append(_expectedElement).append(", is not in actual collection");
      }
      else
      {
        sb.append(" match was never called");
      }
    }

    /**
     * @see org.easymock.IArgumentMatcher#matches(java.lang.Object)
     */
    @Override
    @SuppressWarnings("unchecked")
    public boolean matches(Object obj)
    {
      if (obj instanceof Collection)
      {
        Collection<F> collection = (Collection<F>)obj;

        if (collection.size() != _collection.size())
        {
          _actualSize = collection.size();
          _expectedElement = null;
          _actualElement = null;
          return false;
        }

        Iterator<F> expectedItr;
        T expectedElement;
        switch (_matchType)
        {
          case size:
            // already checked above
            break;
          case ordered:
            expectedItr = _collection.iterator();
            Iterator<F> actualItr = collection.iterator();
            T actualElement;
            while (expectedItr.hasNext())
            {
              expectedElement = _function.apply(expectedItr.next());
              actualElement = _function.apply(actualItr.next());

              if (! expectedElement.equals(actualElement))
              {
                _expectedElement = expectedElement;
                _actualElement = actualElement;
                _actualSize = null;
                return false;
              }
            }
            break;
          case unordered:
            expectedItr = _collection.iterator();
            Set<T> actualSet = Sets.newHashSet(Iterables.transform(_collection, _function));

            while (expectedItr.hasNext())
            {
              expectedElement = _function.apply(expectedItr.next());

              if (! actualSet.contains(expectedElement))
              {
                _expectedElement = expectedElement;
                _actualElement = null;
                _actualSize = null;
                return false;
              }
            }
            break;
          default:
            throw new IllegalArgumentException("MatchType " + _matchType + "  not supported");
        }

        return true;
      }
      return false;
    }

    /**
     * Conveniently creates a matcher that just checks collection size
     */
    public static <F> CollectionArgumentMatcher<F, F> createSizeMatcher(Collection<F> expected)
    {
      return new CollectionArgumentMatcher<>(expected, Functions.<F>identity(), MatchType.size);
    }

    /**
     * Conveniently creates a matcher that matches elements without transformation
     */
    public static <F> CollectionArgumentMatcher<F, F> createUnorderedExactMatcher(Collection<F> expected)
    {
      return new CollectionArgumentMatcher<>(expected, Functions.<F>identity(), MatchType.unordered);
    }

    /**
     * Conveniently creates a matcher that matches elements without transformation
     */
    public static <F> CollectionArgumentMatcher<F, F> createOrderedExactMatcher(Collection<F> expected)
    {
      return new CollectionArgumentMatcher<>(expected, Functions.<F>identity(), MatchType.ordered);
    }
  }
}
