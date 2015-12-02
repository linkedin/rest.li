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

package com.linkedin.data.schema.resolver;


import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaLocation;
import com.linkedin.data.schema.DataSchemaParserFactory;
import com.linkedin.data.schema.DataSchemaResolver;
import com.linkedin.data.schema.Name;
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.schema.PegasusSchemaParser;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Abstract class to assist in implementing a {@link DataSchemaResolver}.
 * <p>
 *
 * This abstract class provides the common base functionality which is to
 * search for the {@link NamedDataSchema} in a possible list of locations, similar
 * to how POSIX shells search directories specified by the PATH environment
 * variable.
 * <p>
 *
 * The derived concrete class provides:
 * <ul>
 * <li> the locations to search for the named DataSchema
 *      (by implementing {@link #possibleLocations(String)},
 * <li> how transform the name and a search path into a location
 *      (by implementing {@link AbstractIterator#transform(String)}, and
 * <li> how to obtain an {@link InputStream} from a location
 *      (by implementing {@link #locationToInputStream(DataSchemaLocation, StringBuilder)}.
 * </ul>
 *
 * @author slim
 */
public abstract class AbstractDataSchemaResolver implements DataSchemaResolver
{
  /**
   * Provide an iterator to obtain locations to try to find the specified name.
   *
   * @param name to locate.
   * @return iterator that returns the locations to find the specified name.
   */
  protected abstract Iterator<DataSchemaLocation> possibleLocations(String name);

  /**
   * Obtain an {@link InputStream} from a location string.
   *
   * @param location to obtain an {@link InputStream} from.
   * @param errorMessageBuilder to append error messages to.
   * @return the InputStream if an {@link InputStream} can be obtained for the location,
   *         else return null.
   */
  protected abstract InputStream locationToInputStream(DataSchemaLocation location, StringBuilder errorMessageBuilder);

  /**
   * Abstract class to help implement iterator returned by {@link #possibleLocations(String)}.
   *
   * @author slim
   */
  public abstract static class AbstractIterator implements Iterator<DataSchemaLocation>
  {
    protected abstract DataSchemaLocation transform(String input);

    /**
     * Constructor.
     *
     * Obtain {@link Iterator} from the input {@link Iterable}.
     *
     * @param iterable to be iterated, usually it holds
     *                 an ordered list of search paths.
     */
    protected AbstractIterator(Iterable<String> iterable)
    {
      _it = iterable.iterator();
    }

    /**
     * Constructor.
     *
     * @param it is the underlying Iterator that usually iterates
     *           through an ordered list of search paths.
     */
    protected AbstractIterator(Iterator<String> it)
    {
      _it = it;
    }

    /**
     * Return whether there is another location to search.
     *
     * Default implementation delegates to underlying {@link Iterator}.
     *
     * @return true if there is another location to search.
     */
    @Override
    public boolean hasNext()
    {
      return _it.hasNext();
    }

    /**
     * Obtains the next element, invoke {@link #transform(String)}, and
     * return output of (@link #transform(String)).
     *
     * @return the next location to search.
     */
    @Override
    public DataSchemaLocation next()
    {
      return transform(_it.next());
    }

    /**
     * Not implemented.
     *
     * @throws UnsupportedOperationException always.
     */
    @Override
    public void remove()
    {
      throw new UnsupportedOperationException();
    }

    /**
     * The underlying {@link Iterator}.
     */
    private final Iterator<String> _it;
  }

  /**
   * Constructor.
   *
   * @param parserFactory that will be used by the resolver to parse schemas.
   */
  protected AbstractDataSchemaResolver(DataSchemaParserFactory parserFactory)
  {
    _parserFactory = parserFactory;
  }

  protected boolean isBadLocation(DataSchemaLocation location)
  {
    return _badLocations.contains(location);
  }

  protected boolean addBadLocation(DataSchemaLocation location)
  {
    return _badLocations.add(location);
  }

  @Override
  public Map<String, NamedDataSchema> bindings()
  {
    return Collections.unmodifiableMap(_nameToDataSchema);
  }

  @Override
  public Map<String, DataSchemaLocation> nameToDataSchemaLocations()
  {
    return Collections.unmodifiableMap(_nameToDataSchemaLocations);
  }

  @Override
  public NamedDataSchema findDataSchema(String name, StringBuilder errorMessageBuilder)
  {
    NamedDataSchema found = existingDataSchema(name);
    if (found == null)
    {
      found = locateDataSchema(name, errorMessageBuilder);
    }
    return found;
  }

  @Override
  public NamedDataSchema existingDataSchema(String name)
  {
    NamedDataSchema found = _nameToDataSchema.get(name);
    return found;
  }

  @Override
  public void bindNameToSchema(Name name, NamedDataSchema schema, DataSchemaLocation location)
  {
    String fullName = name.getFullName();
    NamedDataSchema replaced = _nameToDataSchema.put(fullName, schema);
    if (replaced != null)
      throw new IllegalStateException(fullName + " cannot be refined from " + replaced + " to " + schema);
    _nameToDataSchemaLocations.put(fullName, location);
    _resolvedLocations.add(location);
  }

  @Override
  public boolean locationResolved(DataSchemaLocation location)
  {
    return _resolvedLocations.contains(location);
  }

  /**
   * Locate a {@link NamedDataSchema} with the specified name.
   *
   * @param name of schema to locate.
   * @param errorMessageBuilder to append error messages to.
   * @return the NamedDataSchema if it can be located, else return null.
   */
  protected NamedDataSchema locateDataSchema(String name, StringBuilder errorMessageBuilder)
  {
    NamedDataSchema schema = null;
    Iterator<DataSchemaLocation> locations = possibleLocations(name);
    while (locations.hasNext())
    {
      DataSchemaLocation location = locations.next();
      if (location == null || isBadLocation(location))
      {
        continue;
      }

      //out.println("Location " + location);
      InputStream inputStream = null;
      try
      {
        inputStream = locationToInputStream(location, errorMessageBuilder);
        if (inputStream == null)
        {
          //out.println("Bad location " + location);
          addBadLocation(location);
        }
        else
        {
          schema = parse(inputStream, location, name, errorMessageBuilder);
          if (schema != null)
          {
            break;
          }
        }
      }
      finally
      {
        if (inputStream != null)
        {
          try
          {
            inputStream.close();
          }
          catch (IOException exc)
          {
          }
        }
      }
    }
    return schema;
  }

  /**
   * Read an {@link InputStream} and parse the {@link InputStream} looking for the
   * specified name.
   *
   * @param inputStream to parse.
   * @param location of the input source.
   * @param name to locate.
   * @param errorMessageBuilder to append error messages to.
   * @return the {@link NamedDataSchema} is found in the input stream, else return null.
   */
  protected NamedDataSchema parse(InputStream inputStream, final DataSchemaLocation location, String name, StringBuilder errorMessageBuilder)
  {
    NamedDataSchema schema = null;
    PegasusSchemaParser parser = _parserFactory.create(this);
    parser.setLocation(location);
    //out.println("start parsing " + location);

    parser.parse(new FilterInputStream(inputStream)
    {
      @Override
      public String toString()
      {
        return location.toString();
      }
    });

    if (parser.hasError())
    {
      //out.println(parser.errorMessageBuilder().toString());

      errorMessageBuilder.append("Error parsing ").append(location).append(" for \"").append(name).append("\".\n");
      errorMessageBuilder.append(parser.errorMessageBuilder());
      errorMessageBuilder.append("Done parsing ").append(location).append(".\n");

      _badLocations.add(location);
    }
    else
    {
      //out.println(parser.schemasToString());

      DataSchema found = _nameToDataSchema.get(name);
      if (found != null && found instanceof NamedDataSchema)
      {
        schema = (NamedDataSchema) found;
      }

      //out.println(name + " can" + (schema == null ? "not" : "") + " be found in " + location + ".");
    }

    //out.println("Done parsing " + location);
    return schema;
  }

  // private final PrintStream out = new PrintStream(new FileOutputStream(FileDescriptor.out));

  private final Map<String, NamedDataSchema> _nameToDataSchema = new HashMap<String, NamedDataSchema>();
  private final Map<String, DataSchemaLocation> _nameToDataSchemaLocations = new HashMap<String, DataSchemaLocation>();
  private final DataSchemaParserFactory _parserFactory;
  private final Set<DataSchemaLocation> _badLocations = new HashSet<DataSchemaLocation>();
  private final Set<DataSchemaLocation> _resolvedLocations = new HashSet<DataSchemaLocation>();

  protected static final PrintStream out = new PrintStream(new FileOutputStream(FileDescriptor.out));
}
