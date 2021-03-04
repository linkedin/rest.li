/*
   Copyright (c) 2021 LinkedIn Corp.

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

package com.linkedin.data.schema;

import com.linkedin.data.template.RecordTemplate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;


/**
 * Represents an immutable set of {@link PathSpec}s.
 *
 * <p>Path spec set is a convenient wrapper for a collection of {@link PathSpec}. A few advantages of using this class
 * over manually passing around {@code Set<PathSpec>} is:
 *
 * <ul>
 *   <li>guaranteed immutable representation</li>
 *   <li>
 *     explicitly supports "all inclusive" which translates into fetching everything (default for Rest.li resources),
 *     as well as "empty" which translates into fetching no fields
 *   </li>
 *   <li>mutable builder for incrementally assembling an immutable {@link PathSpecSet}</li>
 *   <li>built-in utility to mask a {@link RecordTemplate} with the stored path specs</li>
 * </ul>
 *
 * @author Joseph Florencio
 */
final public class PathSpecSet
{
  private final static PathSpecSet EMPTY = new PathSpecSet(Collections.emptySet(), false);
  private final static PathSpecSet ALL_INCLUSIVE = new PathSpecSet(Collections.emptySet(), true);

  private final Set<PathSpec> _pathSpecs;
  private final boolean _allInclusive;

  private PathSpecSet(Builder builder)
  {
    this(new HashSet<>(builder._pathSpecs), builder._allInclusive);
  }

  private PathSpecSet(Set<PathSpec> pathSpecs, boolean allInclusive)
  {
    _pathSpecs = Collections.unmodifiableSet(pathSpecs);
    _allInclusive = allInclusive;
  }

  /**
   * Creates a new PathSpecSet by copying the input {@code pathSpecs}.
   *
   * @param pathSpecs input path specs
   * @return immutable path spec set
   */
  public static PathSpecSet of(Collection<PathSpec> pathSpecs)
  {
    if (pathSpecs.isEmpty())
    {
      return empty();
    }
    else
    {
      return new PathSpecSet(new HashSet<>(pathSpecs), false);
    }
  }

  /**
   * Creates a new path spec set from the input {@code pathSpecs}.
   *
   * @param pathSpecs input path specs
   * @return immutable path spec set
   */
  public static PathSpecSet of(PathSpec... pathSpecs)
  {
    return of(Arrays.asList(pathSpecs));
  }

  /**
   * @return mutable builder to incrementally construct a {@link PathSpecSet}
   */
  public static Builder newBuilder()
  {
    return new Builder();
  }

  /**
   * @return immutable path spec set that represents an empty projection (no fields requested)
   */
  public static PathSpecSet empty()
  {
    return EMPTY;
  }

  /**
   * @return immutable path spec set that represents that the user wants all fields (all fields requested)
   */
  public static PathSpecSet allInclusive()
  {
    return ALL_INCLUSIVE;
  }

  /**
   * @return null if a {@link #allInclusive()}, elsewise a new {@link PathSpec} array for the projection. Intended to
   *         be used passed into Rest.li builder's {@code fields} method.
   */
  public PathSpec[] toArray()
  {
    if (_allInclusive)
    {
      return null;
    }
    return _pathSpecs.toArray(new PathSpec[0]);
  }

  /**
   * Creates a new mutable builder using this path spec set as a starting state
   *
   * @return a mutable builder
   */
  public Builder toBuilder()
  {
    return newBuilder().add(this);
  }

  /**
   * @return underlying {@link PathSpec}s represented by this path spec set. Note that if this is a
   *         {@link #allInclusive()} this will be an empty set even though all fields are desired.
   */
  public Set<PathSpec> getPathSpecs()
  {
    return _pathSpecs;
  }

  /**
   * @return if this is an {@link #empty()} path spec set (no fields requested)
   */
  public boolean isEmpty()
  {
    return _pathSpecs.isEmpty() && !_allInclusive;
  }

  /**
   * @return if this is a PathSpecSet representing the intent to retrieve all fields
   */
  public boolean isAllInclusive()
  {
    return _allInclusive;
  }

  /**
   * Returns true if this {@link PathSpecSet} contains the input {@link PathSpec}.
   *
   * A {@link PathSpec} is always in a {@link PathSpecSet} if {@link PathSpecSet#isAllInclusive()}.
   *
   * A {@link PathSpec} is in a {@link PathSpecSet} if {@link PathSpecSet#getPathSpecs()} contains the {@link PathSpec} or
   * any parent {@link PathSpec}.
   *
   * <pre>
   * PathSpecSet.allInclusive().contains(/a); // true
   * PathSpecSet.of(/a).contains(/a); // true
   * PathSpecSet.of(/a).contains(/a/b); // true
   * </pre>
   *
   * @param pathSpec the input {@link PathSpec} to look for in the {@link PathSpecSet}
   * @return true if the input {@link PathSpec} is in this {@link PathSpecSet}
   */
  public boolean contains(PathSpec pathSpec)
  {
    if (_allInclusive)
    {
      return true;
    }

    return IntStream.range(0, pathSpec.getPathComponents().size() + 1)
        .mapToObj(i -> new PathSpec(pathSpec.getPathComponents().subList(0, i).toArray(new String[0])))
        .anyMatch(_pathSpecs::contains);
  }

  /**
   * Return a copy of this {@link PathSpecSet} where the contained {@link PathSpec}s are scoped to the input parent
   * {@link PathSpec}.
   *
   * For example, suppose you have these models:
   * <pre>
   * record Foo {
   *   bar: int
   *   baz: int
   * }
   *
   * record Zing {
   *   foo: Foo
   * }
   * </pre>
   *
   * <p>If you want to only fetch the "bar" field from a "Zing" record you might make a {@link PathSpecSet} like this:
   * {@code PathSpecSet.of(/foo/bar)}.</p>
   *
   * <p>However, suppose you already have a {@link PathSpecSet} from the perspective of "Foo" but need a
   * {@link PathSpecSet} for your "Zing" downstream.  This method make this easy:
   * <pre>
   * PathSpecSet fooPathSpecSet = PathSpecSet.of(/bar);
   * PathSpecSet zingPathSpecSet = fooPathSpecSet.copyWithScope(/foo);
   *
   * zingPathSpecSet.equals(PathSpecSet.of(/foo/bar); // true
   * </pre>
   * </p>
   * If you scope an empty {@link PathSpecSet} it remains empty.
   *
   * @param parent the parent {@link PathSpec} to use when scoping the contained {@link PathSpec}s
   * @return a new {@link PathSpecSet} that is scoped to the new parent
   */
  public PathSpecSet copyWithScope(PathSpec parent)
  {
    if (this.isAllInclusive())
    {
      return PathSpecSet.of(parent);
    }

    if (this.isEmpty())
    {
      return PathSpecSet.empty();
    }

    Builder builder = newBuilder();

    this.getPathSpecs().stream()
        .map(childPathSpec -> {
          List<String> parentPathComponents = parent.getPathComponents();
          List<String> childPathComponents = childPathSpec.getPathComponents();
          ArrayList<String> list = new ArrayList<>(parentPathComponents.size() + childPathComponents.size());
          list.addAll(parentPathComponents);
          list.addAll(childPathComponents);
          return list;
        })
        .map(PathSpec::new)
        .forEach(builder::add);

    return builder.build();
  }

  /**
   * Return a copy of this {@link PathSpecSet} where only {@link PathSpec}s that are prefixed with the input
   * {@link PathSpec} are retained.
   *
   * Additionally, the prefix is removed for the retained {@link PathSpec}s.
   *
   * Here are some examples showing the functionality:
   *
   * <pre>
   * // This PathSpecSet is empty because no PathSpecs originally contained start with "abc"
   * PathSpecSet emptyPathSpecSet = PathSpecSet.of(/bar/baz).copyAndRemovePrefix(/abc);
   *
   * // This PathSpecSet is allInclusive because it contains the entire prefix PathSpec
   * PathSpecSet allInclusivePathSpecSet = PathSpecSet.of(/bar).copyAndRemovePrefix(/bar)
   *
   * // The following "equals" evaluates to true
   * PathSpecSet prefixRemovedPathSpecSet = PathSpecSet.of(/bar/baz, /bar/abc).copyAndRemovePrefix(/bar);
   * prefixRemovedPathSpecSet.equals(PathSpecSet.of(/baz, /abc));
   * </pre>
   *
   * @param prefix the {@link PathSpec} prefix to use when retaining {@link PathSpec}s.
   * @return a {@link PathSpecSet} with elements starting with the input {@link PathSpec} prefix
   */
  public PathSpecSet copyAndRemovePrefix(PathSpec prefix)
  {
    if (isAllInclusive() || isEmpty())
    {
      // allInclusive or empty projections stay the same
      return this;
    }

    // if we contain the exact prefix or any sub prefix, it should be an all inclusive set
    PathSpec partialPrefix = prefix;
    do
    {
      if (getPathSpecs().contains(partialPrefix))
      {
        return allInclusive();
      }
      partialPrefix = partialPrefix.getParent();
    } while (!partialPrefix.isEmptyPath());

    List<String> prefixPathComponents = prefix.getPathComponents();
    int prefixPathLength = prefixPathComponents.size();

    return PathSpecSet.of(
        getPathSpecs().stream()
            .filter(pathSpec -> {
              List<String> pathComponents = pathSpec.getPathComponents();
              return pathComponents.size() > prefixPathLength && prefixPathComponents.equals(pathComponents.subList(0, prefixPathLength));
            })
            .map(pathSpec -> new PathSpec(pathSpec.getPathComponents().subList(prefixPathLength, pathSpec.getPathComponents().size()).toArray(new String[0])))
            .collect(Collectors.toSet()));
  }

  /**
   * Mutable builder for {@link PathSpecSet}.
   */
  public static final class Builder
  {
    private final Set<PathSpec> _pathSpecs = new HashSet<>();
    private boolean _allInclusive;

    /**
     * Add all of the fields stored inside of {@code ps} to this builder.
     *
     * <p>Note that if {@code ps} {@link #isAllInclusive()}, this builder converts into "allInclusive" mode and
     * all subsequent add operations are ignored.
     *
     * @param ps path specs to add
     * @return this builder
     */
    public Builder add(PathSpecSet ps)
    {
      if (ps._allInclusive || _allInclusive)
      {
        _pathSpecs.clear();
        _allInclusive = true;
        return this;
      }
      _pathSpecs.addAll(ps._pathSpecs);
      return this;
    }

    /**
     * Add all {@code pathSpecs} to this builder.
     *
     * @param pathSpecs path specs to add
     * @return this builder
     */
    public Builder add(PathSpec... pathSpecs)
    {
      if (_allInclusive)
      {
        return this;
      }
      Collections.addAll(_pathSpecs, pathSpecs);
      return this;
    }

    /**
     * Add a single {@link PathSpec} specified by the components in {@code paths}.
     *
     * @param paths path components to form into a single {@link PathSpec} and add
     * @return this builder
     */
    public Builder add(Collection<String> paths)
    {
      return add(new PathSpec(paths.toArray(new String[paths.size()])));
    }

    /**
     * Add all {@link PathSpec} in another Builder to this Builder.
     *
     * @param builder add all the instances of {@link PathSpec} in this builder to the current builder
     * @return this builder
     */
    public Builder addAll(Builder builder)
    {
      return add(builder.build());
    }

    /**
     * @return if this builder will build into an empty {@link PathSpecSet}.
     */
    public boolean isEmpty()
    {
      return !_allInclusive && _pathSpecs.isEmpty();
    }

    /**
     * @return immutable path spec set
     */
    public PathSpecSet build()
    {
      if (_allInclusive)
      {
        return PathSpecSet.allInclusive();
      }
      else
      {
        return new PathSpecSet(this);
      }
    }
  }

  @Override
  public boolean equals(Object o)
  {
    return EqualsBuilder.reflectionEquals(this, o);
  }

  @Override
  public int hashCode()
  {
    return HashCodeBuilder.reflectionHashCode(this);
  }

  @Override
  public String toString()
  {
    return "PathSpecSet{" + (_allInclusive ? "all inclusive" : StringUtils.join(_pathSpecs, ',')) + "}";
  }
}
