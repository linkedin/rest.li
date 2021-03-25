package com.linkedin.restli.internal.server.util;

import com.linkedin.restli.server.InvalidMimeTypeException;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

/**
 * Released Under the MIT license
 *
 * Copyright (c) 2009 Joe Gregorio, Tom Zellman
 *
 *   Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 *   and associated documentation files (the "Software"), to deal in the Software without
 *   restriction, including without limitation the rights to use, copy, modify, merge, publish,
 *   distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 *   Software is furnished to do so, subject to the following conditions. The above copyright
 *   notice and this permission notice shall be included in all copies or substantial portions of
 *   the Software.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 *   BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 *   NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 *   DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *   OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

/**
 * MIME-Type Parser
 *
 * This class provides basic functions for handling mime-types. It can handle
 * matching mime-types against a list of media-ranges. See section 14.1 of the
 * HTTP specification [RFC 2616] for a complete explanation.
 *
 * http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.1
 *
 * A port to Java of Joe Gregorio's MIME-Type Parser:
 *
 * http://code.google.com/p/mimeparse/
 *
 * Ported by Tom Zellman <tzellman@gmail.com>.
 *
 */
public final class MIMEParse
{

  private static final String QUALITY_PARAM = "q";

  /**
   * Parse results container
   */
  protected static class ParseResults
  {
    String type;

    String subType;

    // !a dictionary of all the parameters for the media range
    Map<String, String> params;

    @Override
    public String toString()
    {
      StringBuilder s = new StringBuilder("('").append(type).append("', '").append(subType).append("', {");
      params.forEach((k, v) -> s.append("'").append(k).append("':'").append(v).append("',"));
      return s.append("})").toString();
    }

    /**
     * Build the String for the content type header
     */
    String toContentType() {
      StringBuilder s = new StringBuilder(type).append("/").append(subType);
      params.forEach((k, v) ->
      {
        // Exclude accept type's "q" param from the content type
        if (!k.equals(QUALITY_PARAM))
        {
          s.append("; ").append(k).append("=").append(v);
        }
      });
      return s.toString();
    }
  }

  /**
   * Carves up a mime-type and returns a ParseResults object
   *
   * For example, the media range 'application/xhtml;q=0.5' would get parsed
   * into:
   *
   * ('application', 'xhtml', {'q', '0.5'})
   */
  protected static ParseResults parseMimeType(String mimeType)
  {
    String[] parts = StringUtils.split(mimeType, ";");
    ParseResults results = new ParseResults();
    results.params = new HashMap<String, String>();

    for (int i = 1; i < parts.length; ++i)
    {
      String p = parts[i];
      String[] subParts = StringUtils.split(p, '=');
      if (subParts.length == 2)
      {
        results.params.put(subParts[0].trim(), subParts[1].trim());
      }
    }
    String fullType = parts[0].trim();

    // Java URLConnection class sends an Accept header that includes a
    // single "*" - Turn it into a legal wildcard.
    if (fullType.equals("*"))
      fullType = "*/*";
    String[] types = StringUtils.split(fullType, "/");
    if (types.length != 2)
    {
      throw new InvalidMimeTypeException(mimeType);
    }
    results.type = types[0].trim();
    results.subType = types[1].trim();
    return results;
  }

  /**
   * Carves up a media range and returns a ParseResults.
   *
   * For example, the media range 'application/*;q=0.5' would get parsed into:
   *
   * ('application', '*', {'q', '0.5'})
   *
   * In addition this function also guarantees that there is a value for 'q'
   * in the params dictionary, filling it in with a proper default if
   * necessary.
   */
  protected static ParseResults parseMediaRange(String range)
  {
    ParseResults results = parseMimeType(range);
    String q = results.params.get(QUALITY_PARAM);
    float f = NumberUtils.toFloat(q, 1);
    if (StringUtils.isBlank(q) || f < 0 || f > 1)
      results.params.put(QUALITY_PARAM, "1");
    return results;
  }

  /**
   * Structure for holding a fitness/quality combo
   */
  protected static class FitnessAndQuality implements
          Comparable<FitnessAndQuality>
  {
    int fitness;

    float quality;

    String mimeType; // optionally used

    public FitnessAndQuality(int fitness, float quality)
    {
      this.fitness = fitness;
      this.quality = quality;
    }

    @Override
    public int compareTo(FitnessAndQuality o)
    {
      if (fitness == o.fitness)
      {
        if (quality == o.quality)
          return 0;
        else
          return quality < o.quality ? -1 : 1;
      }
      else
        return fitness < o.fitness ? -1 : 1;
    }
  }

  /**
   * Find the best match for a given mimeType against a list of media_ranges
   * that have already been parsed by MimeParse.parseMediaRange(). Returns a
   * tuple of the fitness value and the value of the 'q' quality parameter of
   * the best match, or (-1, 0) if no match was found. Just as for
   * quality_parsed(), 'parsed_ranges' must be a list of parsed media ranges.
   */
  protected static FitnessAndQuality fitnessAndQualityParsed(String mimeType,
                                                             Collection<ParseResults> parsedRanges)
  {
    int bestFitness = -1;
    float bestFitQ = 0;
    Map<String, String> bestFitParams = Collections.emptyMap();
    ParseResults target = parseMediaRange(mimeType);

    for (ParseResults range : parsedRanges)
    {
      if ((target.type.equals(range.type) || range.type.equals("*") || target.type
              .equals("*"))
              && (target.subType.equals(range.subType)
              || range.subType.equals("*") || target.subType
              .equals("*")))
      {
        for (String k : target.params.keySet())
        {
          int paramMatches = 0;
          if (!k.equals(QUALITY_PARAM) && range.params.containsKey(k)
                  && target.params.get(k).equals(range.params.get(k)))
          {
            paramMatches++;
          }
          int fitness = (range.type.equals(target.type)) ? 100 : 0;
          fitness += (range.subType.equals(target.subType)) ? 10 : 0;
          fitness += paramMatches;
          if (fitness > bestFitness)
          {
            bestFitness = fitness;
            bestFitQ = NumberUtils
                    .toFloat(range.params.get(QUALITY_PARAM), 0);
            bestFitParams = range.params;
          }
        }
      }
    }
    FitnessAndQuality fitnessAndQuality = new FitnessAndQuality(bestFitness, bestFitQ);
    target.params = bestFitParams;
    fitnessAndQuality.mimeType = target.toContentType();

    return fitnessAndQuality;
  }

  /**
   * Find the best match for a given mime-type against a list of ranges that
   * have already been parsed by parseMediaRange(). Returns the 'q' quality
   * parameter of the best match, 0 if no match was found. This function
   * bahaves the same as quality() except that 'parsed_ranges' must be a list
   * of parsed media ranges.
   */
  protected static float qualityParsed(String mimeType,
                                       Collection<ParseResults> parsedRanges)
  {
    return fitnessAndQualityParsed(mimeType, parsedRanges).quality;
  }

  /**
   * Returns the quality 'q' of a mime-type when compared against the
   * mediaRanges in ranges. For example:
   *
   * @param mimeType
   * @param ranges
   */
  public static float quality(String mimeType, String ranges)
  {
    List<ParseResults> results = new LinkedList<ParseResults>();
    for (String r : StringUtils.split(ranges, ','))
      results.add(parseMediaRange(r));
    return qualityParsed(mimeType, results);
  }

  /**
   * Takes a list of supported mime-types and finds the best match for all the
   * media-ranges listed in header. The value of header must be a string that
   * conforms to the format of the HTTP Accept: header. The value of
   * 'supported' is a list of mime-types.
   *
   * MimeParse.bestMatch(Arrays.asList(new String[]{"application/xbel+xml",
   * "text/xml"}), "text/*;q=0.5,*; q=0.1") 'text/xml'
   *
   * @return content-type
   */
  public static String bestMatch(Collection<String> supported, String header)
  {
    List<ParseResults> parseResults = new LinkedList<ParseResults>();
    List<FitnessAndQuality> weightedMatches = new LinkedList<FitnessAndQuality>();
    for (String r : StringUtils.split(header, ','))
      parseResults.add(parseMediaRange(r));

    for (String s : supported)
    {
      FitnessAndQuality fitnessAndQuality = fitnessAndQualityParsed(s,
                                                                    parseResults);
      weightedMatches.add(fitnessAndQuality);
    }
    Collections.sort(weightedMatches);

    FitnessAndQuality lastOne = weightedMatches
            .get(weightedMatches.size() - 1);
    return NumberUtils.compare(lastOne.quality, 0) != 0 ? lastOne.mimeType
            : "";
  }

  /**
   * Returns a {@link List} of {@link String}s representing all possible accept types from the provided header.
   * The provided header should be the value of the 'Accept' header. This method simply returns the primary type
   * followed by the subtype, meaning 'primaryType/subType'. For example it will return 'application/json' or
   * 'multipart/related'. Therefore no quality factor information is preserved in the returned list of accept types.
   *
   * @param header the header to parse
   * @return a List of Strings representing all possible accept types
   */
  public static List<String> parseAcceptType(final String header)
  {
    List<String> acceptTypes = new LinkedList<String>();
    for (String acceptType : StringUtils.split(header, ','))
    {
      final ParseResults parseResults = parseMimeType(acceptType);
      acceptTypes.add(parseResults.type + "/" + parseResults.subType);
    }
    return acceptTypes;
  }

  //Disable instantiation
  private MIMEParse()
  {
  }
}
