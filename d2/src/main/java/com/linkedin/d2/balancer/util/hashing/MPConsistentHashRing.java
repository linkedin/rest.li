/*
   Copyright (c) 2016 LinkedIn Corp.

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

package com.linkedin.d2.balancer.util.hashing;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import javax.annotation.Nonnull;
import net.openhft.hashing.LongHashFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A weighted multi-probe consistent hash ring based on the following two papers:
 * <a href="https://arxiv.org/pdf/1505.00062.pdf">Multi-probe consistent hashing</a>
 * <a href="http://www2.cs.uni-paderborn.de/cs/ag-madh/WWW/schindel/pubs/WDHT.pdf">Weighted Distributed Hash Tables</a>
 *
 * The differences between this implementation and point-based consistent hash ring are:
 * <ul>
 *   <li>The ring is more balanced in general and much more balanced in low points situation
 *   <li>Memory complexity is O(# of buckets) instead of O(# of points)
 *   <li>Retrieval time of each key is O(# of buckets * # of probes) instead of O(ln(# of points))
 * </ul>
 *
 * @author Ang Xu
 */
public class MPConsistentHashRing<T> implements Ring<T>
{
  public static final int DEFAULT_NUM_PROBES = 21;
  public static final int DEFAULT_POINTS_PER_HOST = 1;

  private static final Logger LOG = LoggerFactory.getLogger(ConsistentHashRing.class);
  private static final LongHashFunction HASH_FUNCTION_0 = LongHashFunction.xx_r39(0xDEADBEEF);
  private static final Charset UTF8 = Charset.forName("UTF-8");
  /* we will only use the lower 32 bit of the hash code to avoid overflow */
  private static final long MASK = 0x00000000FFFFFFFFL;

  private final List<Bucket> _buckets;
  private final List<T> _hosts;
  private final LongHashFunction[] _hashFunctions;
  private final int _numProbes;

  /**
   * Creates a multi-probe consistent hash ring with DEFAULT_NUM_PROBES (21).
   */
  public MPConsistentHashRing(Map<T, Integer> pointsMap)
  {
    this(pointsMap, DEFAULT_NUM_PROBES, DEFAULT_POINTS_PER_HOST);
  }

  /**
   * Creates a multi-probe consistent hash ring with given points map and number of probes.
   *
   * @param pointsMap A map between object to store in the ring and its points. The more points
   *                  one has, the higher its weight is.
   * @param numProbes Number of probes need to perform. The higher the number is, the more balanced
   *                  the hash ring is.
   */
  public MPConsistentHashRing(Map<T, Integer> pointsMap, int numProbes, int pointsPerHost)
  {
    _buckets = new ArrayList<>(pointsMap.size());
    _hosts = new ArrayList<>(pointsMap.size());
    for (Map.Entry<T, Integer> entry : pointsMap.entrySet())
    {
      // ignore items whose point is equal to zero
      if (entry.getValue() > 0)
      {
        byte[] bytesToHash = entry.getKey().toString().getBytes(UTF8);
        long hash = HASH_FUNCTION_0.hashBytes(bytesToHash) & MASK;
        _buckets.add(new Bucket(entry.getKey(), hash, entry.getValue()));
        _hosts.add(entry.getKey());

        long hashOfHash = hash;
        int duplicate = pointsPerHost - 1;
        while (duplicate-- > 0) {
          hashOfHash = HASH_FUNCTION_0.hashLong(hashOfHash) & MASK;
          _buckets.add(new Bucket(entry.getKey(), hashOfHash, entry.getValue()));
        }
      }
    }
    _numProbes = numProbes;
    _hashFunctions = new LongHashFunction[_numProbes];
    for (int i = 0; i < _numProbes; i++)
    {
      _hashFunctions[i] = LongHashFunction.xx_r39(i);
    }
  }

  @Override
  public T get(int key)
  {
    if (_buckets.isEmpty())
    {
      LOG.debug("get called on a hash ring with nothing in it");
      return null;
    }

    int index = getIndex(key);
    return _buckets.get(index).getT();
  }

  @Nonnull
  @Override
  public Iterator<T> getIterator(int key)
  {
    return new QuasiMPConsistentHashRingIterator(key, _hosts);
  }


  public Iterator<T> getOrderedIterator(int key)
  {
    //Return an iterator that will return the hosts in ranked order based on their points.
    return new Iterator<T>()
    {
      private final Set<T> _iterated = new HashSet<>();

      @Override
      public boolean hasNext()
      {
        return _iterated.size() < _hosts.size();
      }

      @Override
      public T next()
      {
        if (!hasNext())
        {
          throw new NoSuchElementException();
        }

        int index = getIndex(key, _iterated);
        T item = _buckets.get(index).getT();
        _iterated.add(item);
        return item;
      }
    };
  }

  private int getIndex(int key)
  {
    return getIndex(key, Collections.emptySet());
  }

  private int getIndex(int key, Set<T> excludes)
  {
    float minDistance = Float.MAX_VALUE;
    int index = 0;
    for (int i = 0; i < _numProbes; i++)
    {
      long hash = _hashFunctions[i].hashInt(key) & MASK;
      for (int j = 0; j < _buckets.size(); j++)
      {
        Bucket bucket = _buckets.get(j);
        if (!excludes.contains(bucket.getT()))
        {
          float distance = Math.abs(bucket.getHash() - hash) / (float) bucket.getPoints();
          if (distance < minDistance)
          {
            minDistance = distance;
            index = j;
          }
        }
      }
    }
    return index;
  }

  @Override
  public String toString()
  {
    return "MPConsistentHashRing [" + _buckets + "]";
  }

  @Override
  public boolean isStickyRoutingCapable() {
    return true;
  }

  @Override
  public boolean isEmpty()
  {
    return _hosts.isEmpty();
  }

  private class Bucket
  {
    private final T _t;
    private final long _hash;
    private final int _points;

    public Bucket(T t, long hash, int points)
    {
      _t = t;
      _hash = hash;
      _points = points;
    }

    public T getT()
    {
      return _t;
    }

    public long getHash()
    {
      return _hash;
    }

    public int getPoints()
    {
      return _points;
    }

    @Override
    public String toString()
    {
      return "Bucket [_hash=" + _hash + ", _t=" + _t + ", _points=" + _points + "]";
    }
  }

  /**
   * Other than returning the most wanted host when called for the FIRST time,
   * this iterator DOES NOT follow the ranking based on the points of the host.
   * This is a performance optimization based on use cases.
   */
  private class QuasiMPConsistentHashRingIterator implements Iterator<T> {

    private final List<T> _rankedList;
    private final Iterator<T> _rankedListIter;
    public QuasiMPConsistentHashRingIterator(int startKey, List<T> hosts) {
      _rankedList = new LinkedList<>(hosts);
      Collections.shuffle(_rankedList,
          new Random(startKey));// DOES not guarantee the ranking order of hosts after the first one.
      if (!hosts.isEmpty()) {
        T mostWantedHost = get(startKey);
        _rankedList.remove(mostWantedHost);
        _rankedList.add(0, mostWantedHost);
      }
      _rankedListIter = _rankedList.listIterator();
    }

    @Override
    public boolean hasNext() {
      return _rankedListIter.hasNext();
    }

    @Override
    public T next() {
      return _rankedListIter.next();
    }
  }
}
