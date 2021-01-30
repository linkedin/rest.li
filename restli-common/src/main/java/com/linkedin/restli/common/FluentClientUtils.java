package com.linkedin.restli.common;

import com.linkedin.data.DataMap;
import com.linkedin.data.template.RecordTemplate;
import java.util.List;
import java.util.Map;


/**
 * Utility functions used by generated fluent client APIs.
 *
 * @author Karthik Balasubramanian
 */
public class FluentClientUtils {
  private FluentClientUtils()
  {}

  /**
   * Converts Key -> Value inputs for batch_* requests to a {@link CollectionRequest} as needed by the request classes.
   * @param inputs                Inputs to the batch_* methods.
   * @param keyValueRecordFactory Factory for converting a (key, value) tuple to {@link KeyValueRecord}
   * @param <K>                   Key type
   * @param <V>                   Value type
   */
  public static <K, V extends RecordTemplate> CollectionRequest<KeyValueRecord<K, V>> buildBatchKVInputs(
      Map<K, V> inputs, KeyValueRecordFactory<K, V> keyValueRecordFactory)
  {
    DataMap map = new DataMap();
    @SuppressWarnings({ "unchecked", "rawtypes" })
    CollectionRequest<KeyValueRecord<K, V>> input = new CollectionRequest(map, KeyValueRecord.class);

    for (Map.Entry<K, V> inputEntry : inputs.entrySet())
    {
      K key = inputEntry.getKey();
      V entity = inputEntry.getValue();
      KeyValueRecord<K, V> keyValueRecord = keyValueRecordFactory.create(key, entity);
      keyValueRecord.data().setReadOnly();
      input.getElements().add(keyValueRecord);
    }

    map.setReadOnly();
    return input;
  }

  /**
   * Converts list of entities for batch_* requests to a {@link CollectionRequest} as needed by the request classes.
   * @param entities   Inputs to the batch_* methods.
   * @param valueClass Entity's class.
   * @param <V>        Value type
   */
  public static <V extends RecordTemplate> CollectionRequest<V> buildBatchEntityInputs(List<V> entities, Class<V> valueClass)
  {
    DataMap map = new DataMap();
    CollectionRequest<V> input = new CollectionRequest<>(map, valueClass);
    for (V entity : entities)
    {
      input.getElements().add(entity);
    }
    return input;
  }
}
