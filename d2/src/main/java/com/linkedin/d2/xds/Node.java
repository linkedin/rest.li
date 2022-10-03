package com.linkedin.d2.xds;

import com.google.protobuf.ListValue;
import com.google.protobuf.NullValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;


/**
 * See corresponding Envoy proto message {@link io.envoyproxy.envoy.config.core.v3.Node}.
 */
public final class Node
{
  public static final Node DEFAULT_NODE = new Node("sidecar~127.0.0.1~.default~default.svc.cluster.local",
      "svc.cluster.local", null);

  private final String _id;
  private final String _cluster;
  @Nullable
  private final Map<String, ?> _metadata;

  Node(String id, String cluster, @Nullable Map<String, ?> metadata)
  {
    _id = id;
    _cluster = cluster;
    _metadata = metadata;
  }

  /**
   * Converts Java representation of the given JSON value to protobuf's {@link
   * com.google.protobuf.Value} representation.
   *
   * <p>The given {@code rawObject} must be a valid JSON value in Java representation, which is
   * either a {@code Map<String, ?>}, {@code List<?>}, {@code String}, {@code Double}, {@code
   * Boolean}, or {@code null}.
   */
  private static Value convertToValue(Object rawObject)
  {
    Value.Builder valueBuilder = Value.newBuilder();
    if (rawObject == null)
    {
      valueBuilder.setNullValue(NullValue.NULL_VALUE);
    } else if (rawObject instanceof Double)
    {
      valueBuilder.setNumberValue((Double) rawObject);
    } else if (rawObject instanceof String)
    {
      valueBuilder.setStringValue((String) rawObject);
    } else if (rawObject instanceof Boolean)
    {
      valueBuilder.setBoolValue((Boolean) rawObject);
    } else if (rawObject instanceof Map)
    {
      Struct.Builder structBuilder = Struct.newBuilder();
      @SuppressWarnings("unchecked")
      Map<String, ?> map = (Map<String, ?>) rawObject;
      for (Map.Entry<String, ?> entry : map.entrySet())
      {
        structBuilder.putFields(entry.getKey(), convertToValue(entry.getValue()));
      }
      valueBuilder.setStructValue(structBuilder);
    } else if (rawObject instanceof List)
    {
      ListValue.Builder listBuilder = ListValue.newBuilder();
      List<?> list = (List<?>) rawObject;
      for (Object obj : list)
      {
        listBuilder.addValues(convertToValue(obj));
      }
      valueBuilder.setListValue(listBuilder);
    }
    return valueBuilder.build();
  }

  @Override
  public String toString()
  {
    return "Node{" + "_id='" + _id + '\'' + ", _cluster='" + _cluster + '\'' + ", _metadata=" + _metadata + '}';
  }

  @Override
  public boolean equals(Object o)
  {
    if (this == o)
    {
      return true;
    }
    if (o == null || getClass() != o.getClass())
    {
      return false;
    }
    Node node = (Node) o;
    return Objects.equals(_id, node._id) && Objects.equals(_cluster, node._cluster) && Objects.equals(_metadata,
        node._metadata);
  }

  @Override
  public int hashCode()
  {
    return Objects.hash(_id, _cluster, _metadata);
  }

  String getId()
  {
    return _id;
  }

  String getCluster()
  {
    return _cluster;
  }

  @Nullable
  Map<String, ?> getMetadata()
  {
    return _metadata;
  }

  public io.envoyproxy.envoy.config.core.v3.Node toEnvoyProtoNode()
  {
    io.envoyproxy.envoy.config.core.v3.Node.Builder builder = io.envoyproxy.envoy.config.core.v3.Node.newBuilder();
    builder.setId(_id);
    builder.setCluster(_cluster);
    if (_metadata != null)
    {
      Struct.Builder structBuilder = Struct.newBuilder();
      for (Map.Entry<String, ?> entry : _metadata.entrySet())
      {
        structBuilder.putFields(entry.getKey(), convertToValue(entry.getValue()));
      }
      builder.setMetadata(structBuilder);
    }
    return builder.build();
  }
}