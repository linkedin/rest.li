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

package com.linkedin.d2.balancer.strategies.degrader;

import com.linkedin.d2.balancer.util.hashing.ConsistentHashRing;
import com.linkedin.d2.balancer.util.hashing.ConsistentHashRing.Point;
import com.linkedin.d2.balancer.util.hashing.Ring;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DegraderRingFactory: implementation of the RingFactory interface with reused points
 *
 * The factory tries to keep around all the points for each URI and reuse them whenever possible.
 * There are two performance advantages with this approach:
 * 1. It is more GC friendly because no Point object will be thrown away and no generation of new
 *    points for each update unless more Points are needed.
 * 2. Avoid re-invoking MD5 (or other expensive hashing mechanisms) for the point generation.
 *
 * Note: DegraderRingFactory is not thread safe. It is currently protected by partition lock
 *       from the caller. Make sure to have proper protection if it is used in other environment.
 */
public class PointBasedConsistentHashRingFactory<T> implements RingFactory<T>
{
  private static final Charset UTF8 = Charset.forName("UTF-8");
  private static final Logger _log = LoggerFactory.getLogger(PointBasedConsistentHashRingFactory.class);

  final private Map<T, List<Point<T>>> _ringPoints; // map from object t --> list of points for this object
  private final MessageDigest _md;
  // threshold to clean up old factory points. See clearPoints function
  private final DegraderLoadBalancerStrategyConfig _config;
  private final int POINTS_CLEANUP_MIN_UNUSED_ENTRY = 3;
  // the partition number of each hash value
  private final int HASH_PARTITION_NUM = 4;
  private final int POINT_SIZE_IN_BYTE = 4;

  public PointBasedConsistentHashRingFactory(final DegraderLoadBalancerStrategyConfig config)
  {
    _ringPoints = new HashMap<T, List<Point<T>>>();
    _config = config;

    try {
      _md = MessageDigest.getInstance("MD5");
    }
    catch (NoSuchAlgorithmException e)
    {
      _log.error("unable to get md5 hash function");

      throw new RuntimeException(e);
    }
  }

  @Override
  public Ring<T> createRing(Map<T, Integer> points)
  {
    List<Point<T>> newRingPoints = new ArrayList<>();
    clearPoints(points.size());
    for (Map.Entry<T, Integer> entry : points.entrySet())
    {
      T t = entry.getKey();
      int numDesiredPoints = entry.getValue();
      List<Point<T>> tPoints = getPointList(t, numDesiredPoints);

      // Only copy the number of desired points
      newRingPoints.addAll(tPoints.subList(0, numDesiredPoints));
    }

    _log.debug("Creating new hash ring with the following points {}", newRingPoints);
    return new ConsistentHashRing<>(newRingPoints);
  }

  public Map<T, List<Point<T>>> getPointsMap()
  {
    return _ringPoints;
  }

  /**
   * Check and clean up the points maintained by the factory
   *
   * DegraderRingFactory keep a copy of all points. It is possible that some of the URIs are already
   * dead therefore save the points for those URIs are meaningless. However we do not want to immediately
   * follow individual URI changes as well because: 1. it is costly. 2. when a service is bounced, the URI
   * is gone and comes back again, so it makes sense if the URI can be kept around for some time.
   *
   * We decided to use the number of unused entries as the criteria -- the more unused entries, the higher
   * probability some of them are dead. When the unused entry number reaches up to the given threshold,
   * it's time to do the cleanup. For simplicity, all points will be purged and re-generated.
   *
   * HTTP_LB_HASHRING_POINT_CLEANUP_RATE defines the ratio of unused entries against the total entries. It
   * is configurable from cfg2. Also POINTS_CLEANUP_MIN_UNUSED_ENTRY is used to make sure we do not waste
   * time on clean up when the total host number is small.
   *
   * @param size: the size of new URI list
   */
  private void clearPoints(int size)
  {
    int unusedEntries = _ringPoints.size() - size;
    int unusedEntryThreshold = (int)(_ringPoints.size() * _config.getHashRingPointCleanUpRate());
    if (unusedEntries > Math.max(unusedEntryThreshold, POINTS_CLEANUP_MIN_UNUSED_ENTRY))
    {
      _ringPoints.clear();
    }
  }

  /**
   * Get a list of points for the given object t. Expand to create more points when needed.
   * @param t
   * @param numDesiredPoints
   * @return new point list for the given object
   */
  private List<Point<T>> getPointList(T t, int numDesiredPoints)
  {
    List<Point<T>> pointList = _ringPoints.get(t);
    // Round the point number up to the times of HASH_PARTITION_NUM so that all hash values
    // generated by MD5 can be consumed
    numDesiredPoints = ((numDesiredPoints + HASH_PARTITION_NUM - 1) / HASH_PARTITION_NUM) * HASH_PARTITION_NUM;

    if (pointList == null)
    {
      pointList = new ArrayList<>(numDesiredPoints);
      _ringPoints.put(t, pointList);
    }
    else if (numDesiredPoints <= pointList.size())
    {
      return pointList;
    }

    // Need to create new points
    byte[] hashBytes;
    if (pointList.size() < HASH_PARTITION_NUM)
    {
      // generate the first hashkey from object t
      hashBytes = t.toString().getBytes(UTF8);
    }
    else
    {
      // reconstruct the hashkey from the previous points
      // We know we can use the previous 4 points to reconstruct the hashkey because we made sure
      // when constructing the pointList to make the number of points a multiple of 4.
      // And the next hashKey is generated from the hash of the previous 4 points.

      ByteBuffer hashKey = ByteBuffer.allocate(HASH_PARTITION_NUM * POINT_SIZE_IN_BYTE);
      hashKey.order(ByteOrder.LITTLE_ENDIAN);
      for (int i = pointList.size() - HASH_PARTITION_NUM; i < pointList.size(); i++)
      {
        // grab the hash values of last HASH_PARTITION_NUM points
        hashKey.putInt(pointList.get(i).getHash());
      }
      hashBytes = hashKey.array();
    }


    ByteBuffer buf = null;
    for (int i = pointList.size(); i < numDesiredPoints; ++i)
    {
      if (buf == null || buf.remaining() < HASH_PARTITION_NUM)
      {
        // Generate new hash values and wrap it with Bytebuffer
        hashBytes = _md.digest(hashBytes);
        buf = ByteBuffer.wrap(hashBytes);
        buf.order(ByteOrder.LITTLE_ENDIAN);  // change order to little endian to match previous implementation
      }
      int hashInt = buf.getInt();

      pointList.add(new Point<T>(t, hashInt));
    }

    return pointList;
  }
}
