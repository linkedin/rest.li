/*
   Copyright (c) 2013 LinkedIn Corp.

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

package com.linkedin.restli.common;


/**
 * Represents a Rest.li protocol version.
 * Internally uses a minimal semantic version ((http://semver.org/).
 * @author kparikh
 */
public class ProtocolVersion implements Comparable<ProtocolVersion>
{
  private final int _major;
  private final int _minor;
  private final int _patch;

  private static final String VERSION_NUMBER_SEPARATOR = "\\.";
  private static final String PRE_RELEASE_VERSION_SEPARATOR = "\\-";

  /**
   * @param protcolVersion a String representing a protocol version
   *
   * @throws IllegalArgumentException if the input String does not represent a protocol version
   */
  public ProtocolVersion(String protcolVersion)
  {
    String[] parts = protcolVersion.split(VERSION_NUMBER_SEPARATOR);
    if (parts.length == 3)
    {
      try
      {
        _major = Integer.parseInt(parts[0]);
        _minor = Integer.parseInt(parts[1]);
        _patch = Integer.parseInt(parts[2].split(PRE_RELEASE_VERSION_SEPARATOR)[0]);
      }
      catch (NumberFormatException e)
      {
        throw new IllegalArgumentException("Illegal protocol version " + protcolVersion + " specified.");
      }
    }
    else
    {
      throw new IllegalArgumentException("Illegal protocol version " + protcolVersion + " specified.");
    }
  }

  /**
   * @param major the major version
   * @param minor the minor version
   * @param patch the patch version
   */
  public ProtocolVersion(int major, int minor, int patch)
  {
    _major = major;
    _minor = minor;
    _patch = patch;
  }

  public int getMajor()
  {
    return _major;
  }

  public int getMinor()
  {
    return _minor;
  }

  public int getPatch()
  {
    return _patch;
  }

  @Override
  public int hashCode()
  {
    int result = super.hashCode();
    result = 31 * result + new Integer(_major).hashCode();
    result = 31 * result + new Integer(_minor).hashCode();
    result = 31 * result + new Integer(_patch).hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj)
  {
    if (obj == this)
    {
      return true;
    }
    if (obj == null)
    {
      return false;
    }
    if (!(obj instanceof ProtocolVersion))
    {
      return false;
    }
    ProtocolVersion other = (ProtocolVersion) obj;
    return _major == other._major && _minor == other._minor && _patch == other._patch;
  }

  @Override
  public String toString()
  {
    return _major + "." + _minor + "." + _patch;
  }

  @Override
  public int compareTo(ProtocolVersion o)
  {
    if (_major == o._major)
    {
      if (_minor == o._minor)
      {
        if (_patch == o._patch)
        {
          return 0;
        }
        return _patch > o._patch ? 1 : -1;
      }
      return _minor > o._minor ? 1 : -1;
    }
    return _major > o._major ? 1 : -1;
  }
}

