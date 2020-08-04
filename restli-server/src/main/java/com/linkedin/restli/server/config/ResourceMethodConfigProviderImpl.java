package com.linkedin.restli.server.config;

import com.linkedin.restli.internal.server.model.ResourceMethodDescriptor;
import com.linkedin.restli.server.RestLiConfig;
import java.util.Collections;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


/**
 * An implementation for rest.li resource method level configuration resolver based on configurations provided
 * in {@link RestLiConfig} by following certain precedence rules. This should be extensible if we have other
 * method-level configuration supported for rest.li server.
 *
 * @author mnchen
 */
class ResourceMethodConfigProviderImpl implements ResourceMethodConfigProvider
{
  private static final Logger LOGGER = LoggerFactory.getLogger(ResourceMethodConfigProviderImpl.class);

  static final long DEFAULT_TIMEOUT = 0L;

  static final RestLiMethodConfig DEFAULT_CONFIG = createDefaultConfig();

  private final ResourceMethodConfigTree<Long> _timeoutMs = new ResourceMethodConfigTree<>();
  private final ResourceMethodConfigTree<Set<String>> _alwaysProjectedFields = new ResourceMethodConfigTree<>();
  private final ConcurrentMap<ResourceMethodConfigCacheKey, ResourceMethodConfig> _cache = new ConcurrentHashMap<>();
  private boolean _shouldValidateQueryParams;
  private boolean _shouldValidateResourceKey;

  public ResourceMethodConfigProviderImpl(RestLiMethodConfig config)
      throws ResourceMethodConfigParsingException
  {
    initialize(config);
    _shouldValidateQueryParams = config.shouldValidateQueryParams();
    _shouldValidateResourceKey = config.shouldValidateResourceKey();
  }

  private void initialize(RestLiMethodConfig config) throws ResourceMethodConfigParsingException
  {
    boolean success = initializeProperty(config.getTimeoutMsConfig(), RestLiMethodConfig.ConfigType.TIMEOUT);
    success &= initializeProperty(config.getAlwaysProjectedFieldsConfig(), RestLiMethodConfig.ConfigType.ALWAYS_PROJECTED_FIELDS);
    if (!success)
    {
      throw new ResourceMethodConfigParsingException("Rest.li resource method level configuration parsing error!");
    }
  }

  private boolean initializeProperty(Map<String, ?> config, RestLiMethodConfig.ConfigType configType)
  {
    for (Map.Entry<String, ?> entry : config.entrySet())
    {
      try
      {
        ResourceMethodConfigElement element = ResourceMethodConfigElement.parse(configType, entry.getKey(), entry.getValue());
        processConfigElement(element);
      }
      catch (ResourceMethodConfigParsingException e)
      {
        LOGGER.error("Configuration parsing error", e);
        return false;
      }
    }

    // logging configuration items in priority orderCollections.sort(elements);
    List<ResourceMethodConfigElement> elements = Collections.emptyList();
    switch (configType)
    {
      case TIMEOUT:
        elements = _timeoutMs.getConfigItemsByPriority();
        break;
      case ALWAYS_PROJECTED_FIELDS:
        elements = _alwaysProjectedFields.getConfigItemsByPriority();
        break;
    }
    StringBuilder sb = new StringBuilder();
    sb.append("RestLi MethodLevel Configuration for property " + configType.getConfigName() + " sorted by priority - first match gets applied:\n");
    elements.forEach(el -> sb.append(el.getKey())
            .append(" = ")
            .append(el.getValue())
            .append("\n"));
    LOGGER.info(sb.toString());
    return true;
  }

  private void processConfigElement(ResourceMethodConfigElement element) throws ResourceMethodConfigParsingException
  {
    switch (element.getConfigType())
    {
      // switch case is for future extension to another method-level configuration category
      case TIMEOUT: _timeoutMs.add(element); break;
      case ALWAYS_PROJECTED_FIELDS:
        _alwaysProjectedFields.add(element);
        break;
      default: throw new ResourceMethodConfigParsingException("Unrecognized property: " + element.getProperty());
    }
  }

  @Override
  public ResourceMethodConfig apply(ResourceMethodDescriptor requestMethod)
  {
    ResourceMethodConfigCacheKey cacheKey = new ResourceMethodConfigCacheKey(requestMethod);
    return _cache.computeIfAbsent(cacheKey, this::resolve);
  }

  private ResourceMethodConfig resolve(ResourceMethodConfigCacheKey cacheKey)
  {
    return new ResourceMethodConfigImpl(_timeoutMs.resolve(cacheKey), _shouldValidateQueryParams,
        _shouldValidateResourceKey, _alwaysProjectedFields.resolve(cacheKey));
  }

  /**
   * Default configuration map must specify default values for all properties as last fallback in matching
   */
  private static RestLiMethodConfig createDefaultConfig()
  {
    RestLiMethodConfigBuilder builder = new RestLiMethodConfigBuilder();
    builder.addTimeoutMs("*.*", DEFAULT_TIMEOUT);
    return builder.build();
  }
}
