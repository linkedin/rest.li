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

package com.linkedin.data;

import java.util.Map;


/**
 * An {@link Instrumentable} data structure that is capable of tracking and reporting
 * about the usage of contained data.
 *
 * @author Eran Leshem
 */
public interface Instrumentable
{
  static final String VALUE = "value";
  static final String TIMES_ACCESSED = "timesAccessed";

  /**
   * Sets the state of this {@link Instrumentable} to start instrumenting data access.
   *
   * The types of accesses that are instrumented depends on the actual concrete classes
   * that implement this interface. The minimum expectation is that read access via
   * {@code Collection#get}, {@code Map#get}, and {@link Map#containsKey(Object)}
   * are instrumented as an access.
   */
  void startInstrumentingAccess();

  /**
   * Sets the state of this {@link Instrumentable} to stop instrumenting data access.
   */
  void stopInstrumentingAccess();

  /**
   * Clear tracked data.
   */
  void clearInstrumentedData();

  /**
   * Collect either all the data in this and contained {@link Instrumentable} or only the keys
   * or indices that have been marked as accessed depending on the {@code collectAllData} flag.
   * The data is collected into the {@code instrumentedData} supplied. Each entry's key in
   * the {@code instrumentedData} map is the fully qualified name of the accessed key or index.
   * The fully qualified name is constructed by pre-pending the provided {@code keyPrefix}
   * and the keys and indices of antecedent objects traversed to reach this particular
   * key or index. Each entry's value in the {@code instrumentedData} map is another map.
   * The minimum expectation is that the latter map has at least two entries,
   * the "value" entry holds the String representation object identified by the fully
   * qualified name, and the "timesAcessed" entry has the number of times the fully
   * qualified name has been accessed. The number of times accessed may be {@code null} in
   * case it is unknown how many times the key or index was accessed due to instrumentation limitations.
   *
   * @param keyPrefix is the prefix to prepend to the fully qualified name of accessed keys and indices.
   * @param instrumentedData provides the map to hold the output of the collected instrumented data, each
   *                         map entry's key is the fully qualified name of the key or index accessed, and the
   *                         map entry's value is another map that holds collected data.
   * @param collectAllData indicates whether to collect all data in this and contained {@link Instrumentable} or
   *                       only the keys or indices accessed.
   */
  void collectInstrumentedData(StringBuilder keyPrefix, Map<String, Map<String, Object>> instrumentedData, boolean collectAllData);
}
