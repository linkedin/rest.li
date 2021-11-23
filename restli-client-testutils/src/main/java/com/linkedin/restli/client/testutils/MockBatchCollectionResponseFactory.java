package com.linkedin.restli.client.testutils;

import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.DataMapBuilder;
import com.linkedin.data.collections.CheckedUtil;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.BatchCollectionResponse;
import com.linkedin.restli.common.CollectionMetadata;
import com.linkedin.restli.common.CollectionResponse;
import com.linkedin.restli.internal.common.BatchFinderCriteriaResultDecoder;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Factory for creating mock {@link BatchCollectionResponse}s that can be used on tests.
 */
public class MockBatchCollectionResponseFactory {
  private MockBatchCollectionResponseFactory() { }

  /**
   * Creates a {@link BatchCollectionResponse} with the specified mock data.
   *
   * @param entryClass  the class of elements to be stored in {@link BatchCollectionResponse}
   * @param elementsList A list of list containing the instances of type `entryClass`
   * @param <T> the type of elements to be stored in {@link BatchCollectionResponse}
   * @return An instance of {@link BatchCollectionResponse} created with the specified mock data
   */
  public static <T extends RecordTemplate> BatchCollectionResponse<T> create(
      Class<T> entryClass, List<List<T>> elementsList) {
    return create(entryClass, elementsList, Collections.emptyList(), Collections.emptyList());
  }

  /**
   * Creates a {@link BatchCollectionResponse} with the specified mock data. Make sure the size of the specified lists
   * are the same as the entries at the same index will be used for generating the instances of
   * {@link com.linkedin.restli.common.BatchFinderCriteriaResult} that goes into the final {@link BatchCollectionResponse}.
   * The specified paging and metadata list can contain null entries if the corresponding criteria result instance must
   * not have the paging and/or metadata set.
   *
   * @param entryClass  the class of elements to be stored in {@link BatchCollectionResponse}
   * @param elementsList A list of list containing the instances of type `entryClass`
   * @param pagingList A list of {@link CollectionMetadata} for paging
   * @param metadataList A list of {@link DataMap} for custom metadata
   * @param <T> the type of elements to be stored in {@link BatchCollectionResponse}
   * @return An instance of {@link BatchCollectionResponse} created with the specified mock data
   */
  public static <T extends RecordTemplate> BatchCollectionResponse<T> create(
      Class<T> entryClass, List<List<T>> elementsList,
      List<CollectionMetadata> pagingList, List<DataMap> metadataList) {

    DataList batchedCollectionResponse = new DataList(DataMapBuilder.getOptimumHashMapCapacityFromSize(elementsList.size()));
    for (int i = 0; i < elementsList.size(); i++) {
      Collection<T> recordElements = elementsList.get(i);

      DataList elements = recordElements.stream().map(RecordTemplate::data).collect(Collectors.toCollection(DataList::new));

      DataMap collectionResponse = new DataMap();
      collectionResponse.put(CollectionResponse.ELEMENTS, elements);

      if (!pagingList.isEmpty()) {
        CollectionMetadata paging = pagingList.get(i);
        if (paging != null) {
          collectionResponse.put(CollectionResponse.PAGING, paging.data());
        }
      }

      if (!metadataList.isEmpty()) {
        DataMap metadata = metadataList.get(i);
        if (metadata != null) {
          collectionResponse.put(CollectionResponse.METADATA, metadata);
        }
      }

      batchedCollectionResponse.add(collectionResponse);
    }

    DataMap batchResponse = new DataMap(DataMapBuilder.getOptimumHashMapCapacityFromSize(1));
    CheckedUtil.putWithoutCheckingOrChangeNotification(batchResponse, CollectionResponse.ELEMENTS, batchedCollectionResponse);

    return new BatchCollectionResponse<>(batchResponse, new BatchFinderCriteriaResultDecoder<>(entryClass));
  }
}
