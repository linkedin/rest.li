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
public class DegraderRingFactory<T> implements RingFactory<T>
{
  private static final Charset UTF8 = Charset.forName("UTF-8");
  private static final Logger _log = LoggerFactory.getLogger(DegraderRingFactory.class);

  private Map<T, List<Point<T>>> _points;
  private Map<T, byte[]> _hashCodes;  // saved hashing keys
  private final MessageDigest _md;
  // threshold to clean up old factory points. See clearPoints function
  private final double POINTS_CLEANUP_RATE = 0.2;

  public DegraderRingFactory()
  {
    _points = new HashMap<T, List<Point<T>>>();
    _hashCodes = new HashMap<T, byte[]>();

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

    _log.debug("Creating new hash ring with the following points" + newRingPoints);
    return new ConsistentHashRing<>(newRingPoints);
  }

  public Map<T, List<Point<T>>> getPointsMap()
  {
    return _points;
  }

  /**
   * Check and clean up the points maintained by the factory
   *
   * DegraderRingFactory keep a copy of all points. It is possible that some of the URIs are already
   * dead therefore save the points for those URIs are meaningless. However we do not want to immediately
   * follow individual URI changes as well because: 1. it is costly. 2. when a service is bounced, the URI
   * is gone and comes back again, so it makes sense if the URI can be kept around for some time.
   *
   * The following heuristic will be used to decide if the Points need to recycled all together:
   * 1. If entry number <= 15, 3 unused entries lead to a recycle.
   * 2. If entry number > 15, 20% or up to 20 of the entry difference trigger a recycle.
   *
   * @param size: the size of new URI list
   */
  private void clearPoints(int size)
  {
    boolean doClear = false;
    int unusedEntries = _points.size() - size;
    if (_points.size() <= 15)
    {
      if (unusedEntries >= 3)
      {
        doClear = true;
      }
    }
    else
    {
      if (unusedEntries > _points.size() * POINTS_CLEANUP_RATE || unusedEntries >= 20)
      {
        doClear = true;
      }
    }

    if (doClear)
    {
      _points.clear();
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
    List<Point<T>> pointList = _points.get(t);
    // extend the point number to the times of 4
    numDesiredPoints = ((numDesiredPoints + 3) / 4) * 4;

    if (pointList == null)
    {
      pointList = new ArrayList<>(numDesiredPoints);
      _points.put(t, pointList);
    }
    else if (numDesiredPoints <= pointList.size())
    {
      return pointList;
    }

    // Need to create new points, get the saved hash keys
    byte[] hashBytes = _hashCodes.get(t);
    if (hashBytes == null)
    {
      hashBytes = t.toString().getBytes(UTF8);
    }

    ByteBuffer buf = null;
    for (int i = pointList.size(); i < numDesiredPoints; ++i)
    {
      if (buf == null || buf.remaining() < 4)
      {
        // Generate new hash values and wrap it with Bytebuffer
        hashBytes = _md.digest(hashBytes);
        buf = ByteBuffer.wrap(hashBytes);
        buf.order(ByteOrder.LITTLE_ENDIAN);  // change order to little endian to match previous implementation
      }
      int hashInt = buf.getInt();

      pointList.add(new Point<T>(t, hashInt));
    }

    // save the code for next hash key
    _hashCodes.put(t, hashBytes);

    return pointList;
  }
}
