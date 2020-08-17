package com.linkedin.restli.server.config;

import com.linkedin.restli.common.ResourceMethod;
import com.linkedin.restli.server.config.ResourceMethodKeyParser.RestResourceContext;
import com.linkedin.restli.server.config.ResourceMethodKeyParser.OperationContext;

import java.util.HashSet;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.BiFunction;


class ResourceMethodConfigElement implements Comparable<ResourceMethodConfigElement>
{
  // config key string
  private final String _key;
  // config value
  private final Object _value;
  // config category, like timeoutMs, concurrencyLimit, etc
  private final RestLiMethodConfig.ConfigType _configType;
  // rest.li resource name
  private final Optional<String> _resourceName;
  // rest.li resource method type
  private final Optional<ResourceMethod> _opType;
  // action name or finder name or batch_finder name
  private final Optional<String> _opName;

  private final static Set<ResourceMethod> complexOpSet = EnumSet.of(ResourceMethod.FINDER,
          ResourceMethod.ACTION, ResourceMethod.BATCH_FINDER);



  private ResourceMethodConfigElement(String key, Object value, RestLiMethodConfig.ConfigType configType,
                                      Optional<String> resourceName,
                                      Optional<ResourceMethod> opType,
                                      Optional<String> opName)
  {
    _key = key;
    _value = value;
    _configType = configType;
    _resourceName = resourceName;
    _opType = opType;
    _opName = opName;
  }

  public String getKey()
  {
    return _key;
  }

  public Object getValue()
  {
    return _value;
  }

  public String getProperty()
  {
    return _configType.getConfigName();
  }

  public RestLiMethodConfig.ConfigType getConfigType()
  {
    return _configType;
  }

  public Optional<String> getResourceName()
  {
    return _resourceName;
  }

  public Optional<ResourceMethod> getOpType()
  {
    return _opType;
  }

  public Optional<String> getOpName()
  {
    return _opName;
  }

  private static Optional<String> handlingWildcard(RestResourceContext resourceContext)
  {
    if (resourceContext == null) {
      return Optional.empty();
    } else {
      return Optional.of(resourceContext.getText());
    }
  }

  private static Optional<String> handlingWildcard(TerminalNode input)
  {
    if (input == null) {
      return Optional.empty();
    } else {
      return Optional.of(input.getText());
    }
  }

  static ResourceMethodConfigElement parse(RestLiMethodConfig.ConfigType configType, String key, Object value)
          throws ResourceMethodConfigParsingException
  {
    ParsingErrorListener errorListener = new ParsingErrorListener();
    ANTLRInputStream input = new ANTLRInputStream(key);
    ResourceMethodKeyLexer lexer = new ResourceMethodKeyLexer(input);
    lexer.removeErrorListeners();
    lexer.addErrorListener(errorListener);
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    ResourceMethodKeyParser parser = new ResourceMethodKeyParser(tokens);
    parser.removeErrorListeners();
    parser.addErrorListener(errorListener);
    ResourceMethodKeyParser.KeyContext keyTree = parser.key();

    if (!errorListener.hasErrors())
    {
      Optional<String> resourceName = handlingWildcard(keyTree.restResource());
      Optional<ResourceMethod> opType = getOpType(keyTree.operation());
      Optional<String> opName = opType.flatMap(method -> getOpName(method, keyTree.operation()));
      return new ResourceMethodConfigElement(key, coerceValue(configType, value), configType, resourceName, opType, opName);
    }
    else
    {
      throw new ResourceMethodConfigParsingException(
              "Error" + ((errorListener.errorsSize() > 1) ? "s" : "") + " parsing key: " + key + "\n" + errorListener);
    }
  }

  private static Optional<String> getOpName(ResourceMethod method, OperationContext operation)
  {
    if (complexOpSet.contains(method))
    {
      return handlingWildcard(operation.complex().Name());
    }
    else
    {
      return Optional.empty();
    }
  }

  private static Optional<ResourceMethod> getOpType(OperationContext operation)
  {
    if (operation == null)
    {
      return Optional.empty();
    }
    else
    {
      if (operation.simpleOp() != null)
      {
        return Optional.of(ResourceMethod.fromString(operation.simpleOp().getText()));
      }
      else
      {
        return Optional.of(ResourceMethod.fromString(operation.complex().complexOp().getText()));
      }
    }
  }

  private static Object coerceValue(RestLiMethodConfig.ConfigType configType, Object value) throws ResourceMethodConfigParsingException
  {
    try
    {
      switch(configType)
      {
        case TIMEOUT:
          return ConfigValueCoercers.LONG.apply(value);
        case ALWAYS_PROJECTED_FIELDS:
          return new HashSet<>(ConfigValueCoercers.COMMA_SEPARATED_STRINGS.apply(value));
        default:
          throw new ResourceMethodConfigParsingException("Invalid method-level config property: " + configType.getConfigName());
      }
    } catch (Exception e)
    {
      throw new ResourceMethodConfigParsingException(e);
    }
  }

  // Compare two strings by taking care of precedence in case of wildcard presence. We use Optional to represent
  // "*" wildcard.
  private static Integer compare(Optional<String> e1, Optional<String> e2)
  {
    if (e1.isPresent() && !e2.isPresent())
    {
      return -1;
    }
    else if (!e1.isPresent() && e2.isPresent())
    {
      return 1;
    }
    else
    {
      return 0;
    }
  }

  // Helper routine to chain comparing different parts of ResourceMethodConfigElement together by
  // taking care of precedence order.
  private static BiFunction<ResourceMethodConfigElement, ResourceMethodConfigElement, Integer> chain(
          BiFunction<ResourceMethodConfigElement, ResourceMethodConfigElement, Integer> f1,
          BiFunction<ResourceMethodConfigElement, ResourceMethodConfigElement, Integer> f2)
  {
    return (e1, e2) -> {
      int f1Result = f1.apply(e1, e2);
      if (f1Result != 0)
      {
        return f1Result;
      }
      else
      {
        return f2.apply(e1, e2);
      }
    };
  }

  @Override
  public int compareTo(ResourceMethodConfigElement o) {
    return chain(
            chain(
                    (e1, e2) -> compare(e1._resourceName, e2._resourceName),
                    (e1, e2) -> compare(e1._opType.map(ResourceMethod::toString),
                            e2._opType.map(ResourceMethod::toString))),
            (e1, e2) -> compare(e1._opName, o._opName)).apply(this, o);
  }

  static class ParsingErrorListener extends BaseErrorListener
  {

    private final List<String> _errors = new ArrayList<>();

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine,
                            String msg, RecognitionException e) {
      _errors.add("line " + line + ":" + charPositionInLine + " " + msg + "\n");
    }

    public boolean hasErrors() {
      return !_errors.isEmpty();
    }

    public List<String> getErrors() {
      return Collections.unmodifiableList(_errors);
    }

    public int errorsSize() {
      return _errors.size();
    }

    @Override
    public String toString() {
      StringJoiner sj = new StringJoiner("");
      for (String error: _errors) {
        sj.add(error);
      }
      return sj.toString();
    }
  }
}
