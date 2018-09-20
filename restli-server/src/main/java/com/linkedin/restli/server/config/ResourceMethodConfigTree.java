package com.linkedin.restli.server.config;

import com.linkedin.restli.common.ConfigValue;
import com.linkedin.restli.common.ResourceMethod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * A tree-like structure to represent priority order of multiple resource method configurations. For a resource method
 * config key of the form <restResource>.<opType>-<opName>, the precedence order is as follows:
 * 1. restResource name (support sub-resource as well)
 * 2. operation type
 * 3. operation name
 * @param <T> config value type
 *
 * @author mnchen
 */
class ResourceMethodConfigTree<T>
{

  private final Map<Optional<String>, Map<Optional<ResourceMethod>, Map<Optional<String>, ConfigValue<T>>>> _tree =
          new HashMap<>();
  private final List<ResourceMethodConfigElement> _elements = new ArrayList<>();

  @SuppressWarnings("unchecked")
  void add(ResourceMethodConfigElement element)
  {
    _elements.add(element);
    _tree.computeIfAbsent(element.getResourceName(), k -> new HashMap<>())
            .computeIfAbsent(element.getOpType(), k -> new HashMap<>())
            .putIfAbsent(element.getOpName(), new ConfigValue<>((T)element.getValue(), element.getKey()));
  }

  ConfigValue<T> resolve(ResourceMethodConfigCacheKey cacheKey)
  {
    return resolveResourceName(cacheKey).get();
  }

  Optional<ConfigValue<T>> resolveResourceName(ResourceMethodConfigCacheKey cacheKeyd)
  {
    return resolveNameRecursively(Optional.of(cacheKeyd.getResourceName()), x -> resolveOpType(cacheKeyd, _tree.get(x)));
  }

  /**
   * This method recursively uses given resolver to resolve a config by given name taking into account
   * syntax of sub-resource names. For example, for given name: Optional.of("foo:bar:baz") it will make
   * the following resolver calls:
   * - resolver(Optional.of("foo:bar:baz"))
   * - resolver(Optional.of("foo:bar"))
   * - resolver(Optional.of("foo"))
   * - resolver(Optional.empty())
   */
  Optional<ConfigValue<T>> resolveNameRecursively(Optional<String> name, Function<Optional<String>, Optional<ConfigValue<T>>> resolver)
  {
    Optional<ConfigValue<T>> value = resolver.apply(name);
    if (value.isPresent())
    {
      return value;
    }
    else
    {
      if (name.isPresent())
      {
        return resolveNameRecursively(name.filter(s -> s.lastIndexOf(':') > 0).map(s -> s.substring(0, s.lastIndexOf(':'))), resolver);
      }
      else
      {
        return Optional.empty();
      }
    }
  }

  Optional<ConfigValue<T>> resolveOpType(ResourceMethodConfigCacheKey cacheKeyd,
                                         Map<Optional<ResourceMethod>, Map<Optional<String>, ConfigValue<T>>> map)
  {
    if (map != null)
    {
      Optional<ResourceMethod> opType = Optional.of(cacheKeyd.getOperationType());
      if (opType.isPresent())
      {
        Optional<ConfigValue<T>> value = resolveOpName(cacheKeyd, map.get(opType));
        if (value.isPresent())
        {
          return value;
        }
      }
      return resolveOpName(cacheKeyd, map.get(Optional.empty()));
    }
    else
    {
      return Optional.empty();
    }
  }

  Optional<ConfigValue<T>> resolveOpName(ResourceMethodConfigCacheKey cacheKeyd,
                                         Map<Optional<String>, ConfigValue<T>> map)
  {
    if (map != null)
    {
      Optional<String> inboundOpName = cacheKeyd.getOperationName();
      if (inboundOpName.isPresent())
      {
        ConfigValue<T> value = map.get(inboundOpName);
        if (value != null)
        {
          return Optional.of(value);
        }
      }
      return Optional.ofNullable(map.get(Optional.empty()));
    }
    else
    {
      return Optional.empty();
    }
  }

  // sort the resource method level configuration items by priority
  List<ResourceMethodConfigElement> getConfigItemsByPriority()
  {
    Collections.sort(_elements);
    return _elements;
  }
}
