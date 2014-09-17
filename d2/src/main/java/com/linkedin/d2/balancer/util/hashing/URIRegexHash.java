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

/**
 * $Id: $
 */

package com.linkedin.d2.balancer.util.hashing;

import com.linkedin.common.util.MapUtil;
import com.linkedin.r2.message.Request;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A hash function that is matches specified regular expressions against the request URI.
 * The hash value is computed based on the contents of the first capture group of the first
 * expression that matches the request URI.
 * @author Steven Ihde
 * @version $Revision: $
 */

public class URIRegexHash implements HashFunction<Request>
{
  /** config value should be a <code>List&lt;String></code> where each string is a Regex
   * to match against the URI with one capture group. */
  public static final String KEY_REGEXES = "regexes";

  /** optional config value; if true, fail if no regex matches, otherwise fall back to random */
  public static final String KEY_FAIL_ON_NO_MATCH = "failOnNoMatch";

  /** optional config value; if false, don't warn on falling back to random if the uri doesn't match
   *  the regex.
   */
  public static final String KEY_WARN_ON_NO_MATCH = "warnOnNoMatch";

  private static final Logger LOG = LoggerFactory.getLogger(URIRegexHash.class);

  private final List<Pattern> _patterns;
  private final boolean _failOnNoMatch;
  private final boolean _warnOnNoMatch;
  private final Random _random = new Random();
  private final MD5Hash _md5 = new MD5Hash();

  /**
   * Initialize the hash from a JSON-style Map (whose values are primitives, Lists, or Maps).
   * @param config The config must contain the following keys:
   * {@link #KEY_REGEXES}.  The following are optional:
   * {@link #KEY_FAIL_ON_NO_MATCH}
   * {@link #KEY_WARN_ON_NO_MATCH}
   */
  @SuppressWarnings("unchecked")
  public URIRegexHash(Map<String,Object> config)
  {
    this((List<String>)config.get(KEY_REGEXES),
         MapUtil.getWithDefault(config, KEY_FAIL_ON_NO_MATCH, false),
         MapUtil.getWithDefault(config, KEY_WARN_ON_NO_MATCH, true));
  }

  public URIRegexHash(List<String> patterns, boolean failOnNoMatch)
  {
    this(patterns, failOnNoMatch, true);
  }

  public URIRegexHash(List<String> patterns, boolean failOnNoMatch, boolean warnOnNoMatch)
  {
    List<Pattern> compiledPatterns = new ArrayList<Pattern>(patterns.size());
    for (String p : patterns)
    {
      compiledPatterns.add(Pattern.compile(p));
    }
    _patterns = Collections.unmodifiableList(compiledPatterns);
    _failOnNoMatch = failOnNoMatch;
    _warnOnNoMatch = warnOnNoMatch;
  }

  @Override
  public int hash(Request request)
  {
    String uriString = request.getURI().toString();
    for (Pattern p : _patterns)
    {
      Matcher matcher = p.matcher(uriString);
      if (matcher.find())
      {
        int count = matcher.groupCount();
        if (count > 0)
        {
          if (LOG.isDebugEnabled())
          {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < count; i++)
            {
              builder.append(matcher.group(i + 1));
            }
            LOG.debug("URI {} matched pattern {} with result: {}",
                      new Object[]{ uriString, p.pattern(), builder.toString() });
          }
          String[] keyTokens = new String[count];
          for (int i = 0; i < count; i++)
          {
            keyTokens[i] = matcher.group(i + 1);
          }

          return _md5.hash(keyTokens);
        }
        LOG.warn("Ignoring pattern '{}' which matched but produced no capture groups for URI '{}'",
                 p, uriString);
      }
    }

    if (_failOnNoMatch)
    {
      // TODO better exception class
      throw new RuntimeException("No expression matched URI " + uriString);
    }

    if (_warnOnNoMatch)
    {
      LOG.warn("No expression matched URI {}, falling back to random", uriString);
    }
    else
    {
      LOG.debug("No expression matched URI {}, falling back to random", uriString);
    }
    return _random.nextInt();
  }

  @Override
  public long hashLong(Request request)
  {
    throw new UnsupportedOperationException("hashLong is not supported yet by URIRegexHash.");
  }
}
