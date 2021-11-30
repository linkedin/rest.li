/*
   Copyright (c) 2014 LinkedIn Corp.

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

package com.linkedin.restli.client.testutils;


import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.restli.common.CollectionMetadata;
import com.linkedin.restli.common.CollectionResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * Factory for creating {@link CollectionResponse}s and {@link CollectionMetadata} that can be used for tests.
 *
 * @author jflorencio
 * @author kparikh
 */
public class MockCollectionResponseFactory
{
  private MockCollectionResponseFactory() { }

  /**
   * Creates a {@link com.linkedin.restli.common.CollectionResponse}
   *
   * @param entryClass the class of the objects being stored in the {@link com.linkedin.restli.common.CollectionResponse}
   * @param recordTemplates the objects that will be stored in the {@link com.linkedin.restli.common.CollectionResponse}
   * @param <T> the class of the objects being stored in the {@link com.linkedin.restli.common.CollectionResponse}
   * @return a {@link com.linkedin.restli.common.CollectionResponse} with the above properties
   */
  public static <T extends RecordTemplate> CollectionResponse<T> create(Class<T> entryClass,
                                                                        Collection<T> recordTemplates)
  {
    List<DataMap> dataMapsOfRecordTemplates = new ArrayList<>();
    for (T recordTemplate : recordTemplates)
    {
      dataMapsOfRecordTemplates.add(recordTemplate.data());
    }
    DataMap dataMapCollection = new DataMap();
    dataMapCollection.put(CollectionResponse.ELEMENTS,
                          new DataList(dataMapsOfRecordTemplates));
    return new CollectionResponse<>(dataMapCollection, entryClass);
  }

  /**
   * Creates a {@link CollectionResponse}
   * @param entryClass the class of the objects being stored in the {@link CollectionResponse}
   * @param recordTemplates the objects that will be stored in the {@link CollectionResponse}
   * @param metadata the {@link CollectionMetadata} for this {@link CollectionResponse}
   * @param <T> the class of the objects being stored in the {@link CollectionResponse}
   * @return a {@link CollectionResponse} with the above properties
   */
  public static <T extends RecordTemplate> CollectionResponse<T> create(Class<T> entryClass,
                                                                        Collection<T> recordTemplates,
                                                                        CollectionMetadata metadata)
  {
    CollectionResponse<T> response = create(entryClass, recordTemplates);
    response.setPaging(metadata);
    return response;
  }

  /**
   * Creates a {@link CollectionResponse}
   *
   * @param entryClass the class of the objects being stored in the {@link CollectionResponse}
   * @param recordTemplates the objects that will be stored in the {@link CollectionResponse}
   * @param metadata the {@link CollectionMetadata} for this {@link CollectionResponse}
   * @param customMetadata raw custom metadata for this {@link CollectionResponse}
   * @param <T> the class of the objects being stored in the {@link CollectionResponse}
   * @return a {@link CollectionResponse} with the above properties
   */
  public static <T extends RecordTemplate> CollectionResponse<T> create(Class<T> entryClass,
      Collection<T> recordTemplates,
      CollectionMetadata metadata,
      DataMap customMetadata)
  {
    CollectionResponse<T> response = create(entryClass, recordTemplates, metadata);
    response.setMetadataRaw(customMetadata);
    return response;
  }
}
