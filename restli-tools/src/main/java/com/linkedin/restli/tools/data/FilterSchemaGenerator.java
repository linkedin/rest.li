package com.linkedin.restli.tools.data;


import com.linkedin.data.codec.JacksonDataCodec;
import com.linkedin.data.it.Predicate;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.schema.SchemaParser;
import com.linkedin.data.schema.SchemaParserFactory;
import com.linkedin.data.schema.util.Filters;
import com.linkedin.data.schema.validation.ValidationOptions;
import com.linkedin.restli.common.RestConstants;
import com.linkedin.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;


/**
 * Provides main function for filtering {@link com.linkedin.data.schema.DataSchema}s in a directory
 * by removing unwanted fields or custom properties of the schema according to given {@link com.linkedin.data.it.Predicate}.
 *
 * @author Keren Jin
 */
public class FilterSchemaGenerator
{
  public static void main(String[] args)
  {
    if (args.length < 3)
    {
      _log.error("Usage: FilterSchemaGenerator predicateClassName sourceDirectoryPath outputDirectoryPath [-a|--avroMode]");
      System.exit(1);
    }

    final File sourceDirectory = new File(args[1]);

    if (!sourceDirectory.exists())
    {
      _log.error(sourceDirectory.getPath() + " does not exist");
      System.exit(1);
    }
    if (!sourceDirectory.isDirectory())
    {
      _log.error(sourceDirectory.getPath() + " is not a directory");
      System.exit(1);
    }

    final File outputDirectory = new File(args[2]);
    if (outputDirectory.exists() && !sourceDirectory.isDirectory())
    {
      _log.error(outputDirectory.getPath() + " is not a directory");
      System.exit(1);
    }

    final URI sourceDirectoryURI = sourceDirectory.toURI();

    final boolean isAvroMode;
    if (args.length == 4)
    {
      isAvroMode = "-a".equals(args[3]) || "--avroMode".equals(args[3]);
    }
    else
    {
      isAvroMode = false;
    }

    final String predicateClassName = args[0];
    final Class<?> predicateClass;
    try
    {
      predicateClass = Class.forName(predicateClassName);
    }
    catch (ClassNotFoundException e)
    {
      reportException(e);
      return;
    }

    if (!Predicate.class.isAssignableFrom(predicateClass))
    {
      _log.error(predicateClassName + " must be the name of a subclass of com.linkedin.data.it.Predicate");
      System.exit(1);
    }

    final Predicate predicate;
    try
    {
      predicate = predicateClass.asSubclass(Predicate.class).newInstance();
    }
    catch (InstantiationException e)
    {
      reportException(e);
      return;
    }
    catch (IllegalAccessException e)
    {
      reportException(e);
      return;
    }

    final Collection<File> sourceFiles = FileUtil.listFiles(sourceDirectory, null);
    int exitCode = 0;
    for (File sourceFile : sourceFiles)
    {
      try
      {
        final ValidationOptions val = new ValidationOptions();
        val.setAvroUnionMode(isAvroMode);

        final SchemaParser schemaParser = new SchemaParser();
        schemaParser.setValidationOptions(val);

        schemaParser.parse(new FileInputStream(sourceFile));
        if (schemaParser.hasError())
        {
          _log.error("Error parsing " + sourceFile.getPath() + ": " + schemaParser.errorMessageBuilder().toString());
          exitCode = 1;
          continue;
        }

        final DataSchema originalSchema = schemaParser.topLevelDataSchemas().get(0);
        if (!(originalSchema instanceof NamedDataSchema))
        {
          _log.error(sourceFile.getPath() + " does not contain valid NamedDataSchema");
          exitCode = 1;
          continue;
        }

        final SchemaParser filterParser = new SchemaParser();
        filterParser.setValidationOptions(val);

        final NamedDataSchema filteredSchema = Filters.removeByPredicate((NamedDataSchema) originalSchema, predicate, filterParser);
        if (filterParser.hasError())
        {
          _log.error("Error applying predicate: " + filterParser.errorMessageBuilder().toString());
          exitCode = 1;
          continue;
        }

        final String relativePath = sourceDirectoryURI.relativize(sourceFile.toURI()).getPath();
        final String outputFilePath = outputDirectory.getPath() + File.separator + relativePath;
        final File outputFile = new File(outputFilePath);
        final File outputFileParent = outputFile.getParentFile();
        outputFileParent.mkdirs();
        if (!outputFileParent.exists())
        {
          _log.error("Unable to write filtered schema to " + outputFileParent.getPath());
          exitCode = 1;
          continue;
        }

        new FileOutputStream(outputFile).write(filteredSchema.toString().getBytes(RestConstants.DEFAULT_CHARSET));
      }
      catch (IOException e)
      {
        _log.error(e.getMessage());
        exitCode = 1;
      }
    }

    System.exit(exitCode);
  }

  private static void reportException(Exception e)
  {
    _log.error(e.getClass().getName() + ": " + e.getMessage());
    System.exit(1);
  }

  private static final Logger _log = LoggerFactory.getLogger(FilterSchemaGenerator.class);
}
