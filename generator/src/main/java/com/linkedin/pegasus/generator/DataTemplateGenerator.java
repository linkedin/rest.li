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

package com.linkedin.pegasus.generator;


import com.linkedin.data.ByteString;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.schema.ArrayDataSchema;
import com.linkedin.data.schema.ComplexDataSchema;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaLocation;
import com.linkedin.data.schema.EnumDataSchema;
import com.linkedin.data.schema.FixedDataSchema;
import com.linkedin.data.schema.JsonBuilder;
import com.linkedin.data.schema.MapDataSchema;
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.schema.PrimitiveDataSchema;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.SchemaToJsonEncoder;
import com.linkedin.data.schema.TyperefDataSchema;
import com.linkedin.data.schema.UnionDataSchema;
import com.linkedin.data.template.BooleanArray;
import com.linkedin.data.template.BooleanMap;
import com.linkedin.data.template.BytesArray;
import com.linkedin.data.template.BytesMap;
import com.linkedin.data.template.Custom;
import com.linkedin.data.template.DataTemplate;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.DirectArrayTemplate;
import com.linkedin.data.template.DirectMapTemplate;
import com.linkedin.data.template.DoubleArray;
import com.linkedin.data.template.DoubleMap;
import com.linkedin.data.template.FixedTemplate;
import com.linkedin.data.template.FloatArray;
import com.linkedin.data.template.FloatMap;
import com.linkedin.data.template.GetMode;
import com.linkedin.data.template.HasTyperefInfo;
import com.linkedin.data.template.IntegerArray;
import com.linkedin.data.template.IntegerMap;
import com.linkedin.data.template.LongArray;
import com.linkedin.data.template.LongMap;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.data.template.SetMode;
import com.linkedin.data.template.StringArray;
import com.linkedin.data.template.StringMap;
import com.linkedin.data.template.TyperefInfo;
import com.linkedin.data.template.UnionTemplate;
import com.linkedin.data.template.WrappingArrayTemplate;
import com.linkedin.data.template.WrappingMapTemplate;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.codemodel.ClassType;
import com.sun.codemodel.JAnnotatable;
import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JDocCommentable;
import com.sun.codemodel.JEnumConstant;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JFieldRef;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JPackage;
import com.sun.codemodel.JVar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates Java data templates.
 *
 * @author Eran Leshem
 */
public abstract class DataTemplateGenerator extends CodeGenerator
{
  /**
   * The system property that specifies whether to generate classes for externally resolved schemas
   */
  public static final String GENERATOR_GENERATE_IMPORTED = "generator.generate.imported";

  @SuppressWarnings("unused")
  private static final Logger log = LoggerFactory.getLogger(DataTemplateGenerator.class);
  protected static final String SUPER = "super";
  protected static final String THIS = "this";
  protected static final String JAVA_PROPERTY = "java";
  protected static final String CLASS_PROPERTY = "class";
  protected static final String COERCER_CLASS_PROPERTY = "coercerClass";

  private static final String ARRAY_SUFFIX = "Array";
  private static final String MAP_SUFFIX = "Map";
  private static final String[] SPECIAL_SUFFIXES = {
    ARRAY_SUFFIX,
    MAP_SUFFIX
  };

  private static final int MAX_SCHEMA_JSON_LENGTH = 32000;

  /**
   * Useful type references
   */
  protected final JClass _customClass = getCodeModel().ref(Custom.class);
  protected final JClass _dataMapClass = getCodeModel().ref(DataMap.class);
  protected final JClass _dataListClass = getCodeModel().ref(DataList.class);
  protected final JClass _stringClass = getCodeModel().ref(String.class);
  protected final JClass _collectionClass = getCodeModel().ref(Collection.class);
  private final JClass _mapClass = getCodeModel().ref(Map.class);
  private final JClass _dataTemplateUtilClass = getCodeModel().ref(DataTemplateUtil.class);
  private final JClass _getModeClass = getCodeModel().ref(GetMode.class);
  private final JClass _setModeClass = getCodeModel().ref(SetMode.class);
  protected final JClass _directArrayClass = getCodeModel().ref(DirectArrayTemplate.class);
  protected final JClass _wrappingArrayClass = getCodeModel().ref(WrappingArrayTemplate.class);
  protected final JClass _directMapClass = getCodeModel().ref(DirectMapTemplate.class);
  protected final JClass _wrappingMapClass = getCodeModel().ref(WrappingMapTemplate.class);
  private final JFieldRef _strictGetMode = getCodeModel().ref(GetMode.class).staticRef("STRICT");
  private final JFieldRef _disallowNullSetMode = getCodeModel().ref(SetMode.class).staticRef("DISALLOW_NULL");
  private final JClass _byteStringClass = getCodeModel().ref(ByteString.class);
  private final JClass _pathSpecClass = getCodeModel().ref(PathSpec.class);
  private final JClass _stringBuilderClass = getCodeModel().ref(StringBuilder.class);

  private final String _templatePackageName = DataTemplate.class.getPackage().getName();

  private final Deque<DataSchemaLocation> _locationStack = new ArrayDeque<DataSchemaLocation>();
  private final Set<File> _sourceFiles = new HashSet<File>();

  private static final Class<?> _nativeJavaClasses[] =
    {
      BooleanArray.class,
      IntegerArray.class,
      LongArray.class,
      FloatArray.class,
      DoubleArray.class,
      StringArray.class,
      BytesArray.class,
      BooleanMap.class,
      IntegerMap.class,
      LongMap.class,
      FloatMap.class,
      DoubleMap.class,
      StringMap.class,
      BytesMap.class,
    };
  
  /**
   * Map of {@link JDefinedClass} to {@link DataSchemaLocation}.
   */
  private final Map<JDefinedClass, DataSchemaLocation> _classToDataSchemaLocationMap = new HashMap<JDefinedClass, DataSchemaLocation>();

  /**
   * Map of Java class name to a {@link DataSchema}.
   */
  private final Map<String, DataSchema> _classNameToSchemaMap = new HashMap<String, DataSchema>(100);

  /**
   * Map of {@link DataSchema} to {@link JDefinedClass}.
   */
  private final IdentityHashMap<DataSchema, JClass> _schemaToClassMap = new IdentityHashMap<DataSchema, JClass>(100);

  /**
   * Map of {@link DataSchema} to the information about the first dereferenced {@link DataSchema}
   * with custom Java class binding.
   */
  private final Map<DataSchema, CustomInfo> _firstCustomMap = new IdentityHashMap<DataSchema, CustomInfo>();

  private static class CustomClasses
  {
    private JClass javaClass;
    private JClass javaCoercerClass;
  }

  private static class CustomInfo
  {
    private final NamedDataSchema sourceSchema;
    private final NamedDataSchema customSchema;
    private final JClass customClass;
    private final JClass coercerClass;

    private CustomInfo(NamedDataSchema sourceSchema, NamedDataSchema customSchema, CustomClasses customClasses)
    {
      this.sourceSchema = sourceSchema;
      this.customSchema = customSchema;
      this.customClass = customClasses.javaClass;
      this.coercerClass = customClasses.javaCoercerClass;
    }

    public String toString()
    {
      return "sourceSchema=" + sourceSchema.getFullName() + ", customSchema=" + customSchema.getFullName() +
             ", customClass=" + customClass + (coercerClass != null ? (", coercerClass=" + coercerClass) : "");
    }
  }

  protected static class Result implements GeneratorResult
  {
    private final Collection<File> _sourceFiles;
    private final Collection<File> _targetFiles;
    private final Collection<File> _modifiedFiles;

    public Result(Collection<File> sourceFiles, Collection<File> targetFiles, Collection<File> modifiedFiles)
    {
      _sourceFiles = Collections.unmodifiableCollection(sourceFiles);
      _targetFiles = Collections.unmodifiableCollection(targetFiles);
      _modifiedFiles = Collections.unmodifiableCollection(modifiedFiles);
    }

    @Override
    public Collection<File> getSourceFiles()
    {
      return _sourceFiles;
    }

    @Override
    public Collection<File> getTargetFiles()
    {
      return _targetFiles;
    }

    @Override
    public Collection<File> getModifiedFiles()
    {
      return _modifiedFiles;
    }
  }

  protected static class Config extends CodeGenerator.Config
  {
    public Config(String resolverPath, String defaultPackage, Boolean generateImported)
    {
      super(resolverPath, defaultPackage);
      _generateImported = generateImported;
    }

    /**
     * @return true if imported data templates will be regenerated, false otherwise.
     *         If null is assigned to this property, by default returns true.
     */
    public boolean getGenerateImported()
    {
      return _generateImported == null || _generateImported.booleanValue();
    }

    private final Boolean _generateImported;
  }

  protected DataTemplateGenerator()
  {
    for (Class<?> nativeClass : _nativeJavaClasses)
    {
      DataSchema schema = DataTemplateUtil.getSchema(nativeClass);
      _schemaToClassMap.put(schema, getCodeModel().ref(nativeClass));
      _classNameToSchemaMap.put(nativeClass.getName(), schema);
    }
  }

  @Override
  protected abstract Config getConfig();

  @Override
  protected void parseFile(File schemaSourceFile) throws IOException
  {
    _sourceFiles.add(schemaSourceFile);
    super.parseFile(schemaSourceFile);
  }

  private boolean isImported(JDefinedClass clazz)
  {
    DataSchemaLocation loc = _classToDataSchemaLocationMap.get(clazz);
    if (loc != null)
    {
      return _sourceFiles.contains(loc.getSourceFile()) == false;
    }
    //assume local
    return false;
  }

  @Override
  protected boolean hideClass(JDefinedClass clazz)
  {
    if (!getConfig().getGenerateImported() && isImported(clazz))
    {
      return true;
    }
    return super.hideClass(clazz);
  }

  @Override
  protected void handleSchema(DataSchema schema)
  {
    processSchema(schema, null, null);
  }

  /**
   * Emit message if the schema is a {@link NamedDataSchema} and
   * the class name ends with one of the special suffixes,
   * e.g. "Array", "Map".
   *
   * <p>
   * This may potentially conflict with class names for
   * Java binding for array or map of this type.
   *
   * @param className provides the class name.
   */
  private static void checkClassNameForSpecialSuffix(String className)
  {
    for (String suffix : SPECIAL_SUFFIXES)
    {
      if (className.endsWith(suffix))
      {
        log.warn("Class name for named type ends with a suffix that may conflict with derived class names for unnamed types" +
                 ", name: " + className +
                 ", suffix: " + suffix);
        break;
      }
    }
  }

  /**
   * Register a new generated class.
   *
   * Registration is necessary to associate the {@link JDefinedClass}
   * with the source file for which it was generated. This may be used
   * later to determine if generated class should be emitted based on
   * the location of the source file.
   *
   * Registration also associates the {@link DataSchema} to the generated
   * {@link JDefinedClass} and the generated class's full name to the the
   * {@link JDefinedClass}.
   *
   * @param schema provides the {@link DataSchema} of the generated class.
   * @param clazz provides the generated class.
   */
  private void registerGeneratedClass(DataSchema schema, JDefinedClass clazz)
  {
    annotate(clazz, "Data Template", currentLocation().toString());
    _schemaToClassMap.put(schema, clazz);
    _classNameToSchemaMap.put(clazz.fullName(), schema);
    _classToDataSchemaLocationMap.put(clazz, currentLocation());

    if (schema instanceof NamedDataSchema)
    {
      checkClassNameForSpecialSuffix(clazz.fullName());
    }
  }

  /**
   * Validates that all JDefinedClass instances in the code model have been properly
   * registered. See {@link #registerGeneratedClass}.
   */
  protected void validateDefinedClassRegistration()
  {
    JCodeModel model = getCodeModel();
    for(Iterator<JPackage> packageIterator = model.packages(); packageIterator.hasNext(); )
    {
      JPackage p = packageIterator.next();
      for(Iterator<JDefinedClass> classIterator = p.classes(); classIterator.hasNext(); )
      {
        JDefinedClass clazz = classIterator.next();
        if (_classToDataSchemaLocationMap.containsKey(clazz) == false)
        {
          throw new IllegalStateException("Attempting to generate unregistered class: '" + clazz.fullName() + "'");
        }
      }
    }
  }

  protected DataSchemaLocation currentLocation()
  {
    return _locationStack.getLast();
  }

  protected void pushCurrentLocation(DataSchemaLocation location)
  {
    _locationStack.addLast(location);
  }

  protected void popCurrentLocation()
  {
    _locationStack.removeLast();
  }

  protected JClass processSchema(DataSchema schema, JDefinedClass parentClass, String memberName)
  {
    JClass result = null;
    try
    {
      CustomInfo customInfo = firstCustomInfo(schema);
      while (schema.getType() == DataSchema.Type.TYPEREF)
      {
        TyperefDataSchema typerefSchema = (TyperefDataSchema) schema;
        JClass found = _schemaToClassMap.get(schema);
        if (found == null)
        {
          if (typerefSchema.getRef().getType() == DataSchema.Type.UNION)
          {
            result = generateUnion((UnionDataSchema) typerefSchema.getRef(), typerefSchema);
            break;
          }
          else
          {
            generateTyperef(typerefSchema);
          }
        }
        else if (typerefSchema.getRef().getType() == DataSchema.Type.UNION)
        {
          result = found;
          break;
        }
        schema = typerefSchema.getRef();
      }
      if (result == null)
      {
        assert schema == schema.getDereferencedDataSchema();
        if (schema instanceof ComplexDataSchema)
        {
          JClass found = _schemaToClassMap.get(schema);
          if (found == null)
          {
            if (schema instanceof NamedDataSchema)
            {
              result = generateNamedSchema((NamedDataSchema) schema);
            }
            else
            {
              result = generateUnnamedComplexSchema(schema, parentClass, memberName);
            }
          }
          else
          {
            result = found;
          }
          if (customInfo != null)
          {
            result = customInfo.customClass;
          }
        }
        else if (schema instanceof PrimitiveDataSchema)
        {
          result = (customInfo != null) ?
            customInfo.customClass :
            getPrimitiveRefForSchema((PrimitiveDataSchema) schema, parentClass, memberName);
        }
      }

    }
    catch (JClassAlreadyExistsException exc)
    {
      throw new IllegalStateException("Duplicate class definitions should not occur " + schema +
                                      parentClassAndMemberNameToString(parentClass, memberName), exc);
    }
    if (result == null)
    {
      throw unrecognizedSchemaType(parentClass, memberName, schema);
    }
    return result;
  }

  /**
   * Determine whether a custom class has been defined for the {@link DataSchema}.
   *
   * A custom class is defined through the "java" property of the schema.
   * Within this property, a custom class is specified if "java" is
   * a map that contains a "class" property whose value is a string.
   * This value specifies the Java class name of the custom class.
   *
   * The map may optionally include a "coercerClass" property to specify
   * a coercer class that should be initialized.
   *
   * @see Custom#initializeCoercerClass(Class)
   *
   * @param schema to look for custom class specification in.
   * @return null if no custom class is specified, otherwise return the custom class
   *         and the coercer class, the coercer class may be null if no coercer class
   *         is specified.
   */
  private CustomClasses getCustomClasses(DataSchema schema)
  {
    CustomClasses customClasses = null;
    Map<String, Object> properties = schema.getProperties();
    Object java = properties.get(JAVA_PROPERTY);
    if (java != null)
    {
      if (java.getClass() != DataMap.class)
      {
        throw new IllegalArgumentException(schema + " has \"java\" property that is not a DataMap");
      }
      DataMap map = (DataMap) java;
      Object custom = map.get(CLASS_PROPERTY);
      if (custom != null)
      {
        if (custom.getClass() != String.class)
        {
          throw new IllegalArgumentException(schema + " has \"java\" property with \"class\" that is not a string");
        }
        // a custom class specification has been found
        customClasses = new CustomClasses();
        customClasses.javaClass = getCodeModel().directClass((String) custom);
        if (allowCustomClass(schema) == false)
        {
          throw new IllegalArgumentException(schema + " cannot have custom class binding");
        }
      }
      // check for coercer class
      Object customClass = map.get(COERCER_CLASS_PROPERTY);
      if (customClass != null)
      {
        if (customClass.getClass() != String.class)
        {
          throw new IllegalArgumentException(schema + " has \"java\" property with \"coercerClass\" that is not a string");
        }
        if (customClasses == null)
        {
          throw new IllegalArgumentException(schema + " has \"java\" property with \"coercerClass\" but does not have \"class\" property");
        }
        // a custom class specification has been found
        customClasses.javaCoercerClass = getCodeModel().directClass((String) customClass);
      }
    }
    return customClasses;
  }

  /**
   * Allow custom class to to bound to record or typeref of primitive types that are not enums.
   */
  protected static boolean allowCustomClass(DataSchema schema)
  {
    boolean result = false;
    DataSchema.Type type = schema.getType();
    if (type == DataSchema.Type.TYPEREF || type == DataSchema.Type.RECORD)
    {
      // allow custom class only if the dereferenced type is a record or a primitive types that are not enums
      DataSchema dereferencedSchema = schema.getDereferencedDataSchema();
      if (dereferencedSchema.getType() == DataSchema.Type.RECORD ||
          (isDirectType(dereferencedSchema) && dereferencedSchema.getType() != DataSchema.Type.ENUM))
      {
        result = true;
      }
    }
    return result;
  }

  protected static DataSchema dereferenceIfTyperef(DataSchema schema)
  {
    DataSchema.Type type = schema.getType();
    return type == DataSchema.Type.TYPEREF ? ((TyperefDataSchema) schema).getRef() : null;
  }

  protected CustomInfo firstCustomInfo(DataSchema schema)
  {
    if (_firstCustomMap.containsKey(schema))
    {
      return _firstCustomMap.get(schema);
    }

    CustomInfo first = null;
    for (DataSchema current = schema;
         current != null;
         current = dereferenceIfTyperef(current))
    {
      CustomClasses customClasses = getCustomClasses(current);
      if (customClasses != null)
      {
        first = new CustomInfo((NamedDataSchema) schema, (NamedDataSchema) current, customClasses);
        break;
      }
    }

    // first may be null
    _firstCustomMap.put(schema, first);
    return first;
  }

  protected JClass getClassRefForSchema(DataSchema schema, JDefinedClass parentClass)
  {
    if (schema instanceof NamedDataSchema)
    {
      String fullName = classNameForNamedSchema((NamedDataSchema)schema);
      return getCodeModel().ref(fullName);
    }
    else if (schema instanceof PrimitiveDataSchema)
    {
      return getPrimitiveRefForSchema((PrimitiveDataSchema) schema, parentClass, null);
    }
    else
    {
      ClassInfo classInfo = classNameForUnnamedTraverse(parentClass, null, schema);
      String fullName = classInfo.fullName();

      return getCodeModel().ref(fullName);
    }
  }

  protected JClass getPrimitiveRefForSchema(PrimitiveDataSchema schema,
                                            JDefinedClass parentClass,
                                            String memberName)
  {
    switch (schema.getType())
    {
      case INT:
        return getCodeModel().INT.boxify();

      case DOUBLE:
        return getCodeModel().DOUBLE.boxify();

      case BOOLEAN:
        return getCodeModel().BOOLEAN.boxify();

      case STRING:
        return _stringClass;

      case LONG:
        return getCodeModel().LONG.boxify();

      case FLOAT:
        return getCodeModel().FLOAT.boxify();

      case BYTES:
        return _byteStringClass;

      case NULL:
        throw nullTypeNotAllowed(parentClass, memberName);
    }
    throw unrecognizedSchemaType(parentClass, memberName, schema);
  }

  protected JDefinedClass generateNamedSchema(NamedDataSchema schema) throws JClassAlreadyExistsException
  {
    pushCurrentLocation(getSchemaResolver().nameToDataSchemaLocations().get(schema.getFullName()));

    String className = classNameForNamedSchema(schema);
    checkForClassNameConflict(className, schema);

    JDefinedClass templateClass;
    switch (schema.getType()) {
      case RECORD:
        templateClass = generateRecord((RecordDataSchema) schema);
        break;
      case ENUM:
        templateClass = generateEnum((EnumDataSchema) schema);
        break;
      case FIXED:
        templateClass = generateFixed((FixedDataSchema) schema);
        break;
      default:
        throw unrecognizedSchemaType(null, null, schema);
    }

    popCurrentLocation();

    return templateClass;
  }

  private JClass generateUnnamedComplexSchema(DataSchema schema, JDefinedClass parentClass, String memberName)
          throws JClassAlreadyExistsException
  {
    if (schema instanceof ArrayDataSchema)
    {
      return generateArray((ArrayDataSchema) schema, parentClass, memberName);
    }
    else if (schema instanceof MapDataSchema)
    {
      return generateMap((MapDataSchema) schema, parentClass, memberName);
    }
    else if (schema instanceof UnionDataSchema)
    {
      return generateUnion((UnionDataSchema) schema, parentClass, memberName);
    }
    else
    {
      throw unrecognizedSchemaType(parentClass, memberName, schema);
    }
  }

  private JClass determineDataClass(DataSchema schema, JDefinedClass parentClass, String memberName)
  {
    JClass result;
    DataSchema dereferencedSchema = schema.getDereferencedDataSchema();
    if (isDirectType(dereferencedSchema))
    {
      if (dereferencedSchema.getType() == DataSchema.Type.ENUM)
      {
        result = _stringClass;
      }
      else
      {
        result = getPrimitiveRefForSchema((PrimitiveDataSchema) dereferencedSchema, parentClass, memberName);
      }
    }
    else
    {
      result = null;
    }
    return result;
  }

  private static JInvocation dataClassArg(JInvocation inv, JClass dataClass)
  {
    if (dataClass != null)
    {
      inv.arg(JExpr.dotclass(dataClass));
    }
    return inv;
  }

  /**
   * Return the {@link DataSchema} for the array items or map values of the
   * generated class.
   *
   * <p>
   * When there is both an array of X and array of typeref XRef to X,
   * both of these cases maps to the same generated class name, i.e. XArray.
   * However, their schema is slightly different.
   * From a compile time binding perspective, the generated class is the same except
   * the SCHEMA field may have different schema strings.
   *
   * <p>
   * An option to retain the schema differences is to emit a different class
   * for each different schema. The generator could emit XArray and XRefArray.
   * However, this would lead to a proliferation of generated classes for different
   * array or maps of typerefs of the same type. This is not ideal as a
   * common pattern is to use typeref to differentiate different uses of string.
   *
   * <p>
   * To avoid a proliferation of classes and maintain backwards compatibility,
   * the generator will always emit map and array whose values and items types
   * are dereferenced to the native type or to the first typeref with a
   * custom Java class binding.
   *
   * @param customInfo provides the first {@link CustomInfo} of an array's items or
   *                   a map's values.
   * @param schema provides the {@link DataSchema} of an array's items or
   *               a map's values.
   * @return the {@link DataSchema} for the array items or map values of the
   *         generated class.
   */
  private static DataSchema schemaForArrayItemsOrMapValues(CustomInfo customInfo, DataSchema schema)
  {
    return customInfo != null ? customInfo.customSchema : schema.getDereferencedDataSchema();
  }

  private JClass generateArray(ArrayDataSchema schema, JDefinedClass parentClass, String memberName)
          throws JClassAlreadyExistsException
  {
    DataSchema itemSchema = schema.getItems();
    CustomInfo customInfo = firstCustomInfo(itemSchema);

    ClassInfo classInfo = classInfoForUnnamed(parentClass, memberName, schema);
    if (classInfo.existingClass != null)
    {
      /* When type refs are used as item types inside some unnamed complex schemas like map and array,
      * the type refs are de-referenced and the underlying real type is used in the generated class.
      * In those cases the type refs are not processed by the class generation logic, an explicit
      * schema processing is necessary in order to generate the data template classes for those type
      * refs.*/
      processSchema(itemSchema, parentClass, memberName);

      return classInfo.existingClass;
    }

    JDefinedClass arrayClass = classInfo.definedClass;
    registerGeneratedClass(schema, arrayClass);

    JClass itemClass = processSchema(itemSchema, parentClass, memberName);
    JClass dataClass = determineDataClass(itemSchema, parentClass, memberName);
    if (isDirectType(schema.getItems()))
    {
      arrayClass._extends(_directArrayClass.narrow(itemClass));
    }
    else
    {
      arrayClass._extends(_wrappingArrayClass.narrow(itemClass));
    }

    // see schemaForArrayItemsOrMapValues
    ArrayDataSchema bareSchema = new ArrayDataSchema(schemaForArrayItemsOrMapValues(customInfo, itemSchema));
    JVar schemaField = generateSchemaField(arrayClass, bareSchema);

    generateConstructorWithNoArg(arrayClass, _dataListClass);
    generateConstructorWithInitialCapacity(arrayClass, _dataListClass);
    generateConstructorWithCollection(arrayClass, itemClass);
    generateConstructorWithArg(arrayClass, schemaField, _dataListClass, itemClass, dataClass);

    generatePathSpecMethodsForCollection(arrayClass, schema, "items");

    generateCustomClassInitialization(arrayClass, customInfo);

    generateCopierMethods(arrayClass);

    return arrayClass;
  }

  private JClass generateMap(MapDataSchema schema, JDefinedClass parentClass, String memberName)
          throws JClassAlreadyExistsException
  {
    DataSchema valueSchema = schema.getValues();
    CustomInfo customInfo = firstCustomInfo(valueSchema);

    ClassInfo classInfo = classInfoForUnnamed(parentClass, memberName, schema);
    if (classInfo.existingClass != null)
    {
      /* When type refs are used as item types inside some unnamed complex schemas like map and array,
      * the type refs are de-referenced and the underlying real type is used in the generated class.
      * In those cases the type refs are not processed by the class generation logic, an explicit
      * schema processing is necessary in order to generate the data template classes for those type
      * refs.*/
      processSchema(valueSchema, parentClass, memberName);

      return classInfo.existingClass;
    }

    JDefinedClass mapClass = classInfo.definedClass;
    registerGeneratedClass(schema, mapClass);

    JClass valueClass = processSchema(valueSchema, parentClass, memberName);
    JClass dataClass = determineDataClass(valueSchema, parentClass, memberName);

    if (isDirectType(schema.getValues()))
    {
      mapClass._extends(_directMapClass.narrow(valueClass));
    }
    else
    {
      mapClass._extends(_wrappingMapClass.narrow(valueClass));
    }

    // see schemaForArrayItemsOrMapValues
    MapDataSchema bareSchema = new MapDataSchema(schemaForArrayItemsOrMapValues(customInfo, valueSchema));
    JVar schemaField = generateSchemaField(mapClass, bareSchema);

    generateConstructorWithNoArg(mapClass, _dataMapClass);
    generateConstructorWithInitialCapacity(mapClass, _dataMapClass);
    generateConstructorWithInitialCapacityAndLoadFactor(mapClass);
    generateConstructorWithMap(mapClass, valueClass);
    generateConstructorWithArg(mapClass, schemaField, _dataMapClass, valueClass, dataClass);

    generatePathSpecMethodsForCollection(mapClass, schema, "values");

    generateCustomClassInitialization(mapClass, customInfo);

    generateCopierMethods(mapClass);

    return mapClass;
  }

  private JClass generateUnion(UnionDataSchema schema, JDefinedClass parentClass, String memberName)
          throws JClassAlreadyExistsException
  {
    if (parentClass == null || memberName == null)
    {
      throw new IllegalArgumentException("Cannot generate template for top level union: " + schema);
    }
    ClassInfo classInfo = classInfoForUnnamed(parentClass, memberName, schema);
    if (classInfo.existingClass != null)
    {
      return classInfo.existingClass;
    }
    JDefinedClass unionClass = classInfo.definedClass;
    registerGeneratedClass(schema, unionClass);
    return generateUnion(schema, unionClass);
  }

  private JClass generateUnion(UnionDataSchema schema, TyperefDataSchema typerefDataSchema)
          throws JClassAlreadyExistsException
  {
    assert typerefDataSchema.getRef() == schema;

    pushCurrentLocation(getSchemaResolver().nameToDataSchemaLocations().get(typerefDataSchema.getFullName()));

    JDefinedClass unionClass = getPackage(typerefDataSchema.getNamespace())._class(JMod.PUBLIC, escapeReserved(typerefDataSchema.getName()));
    registerGeneratedClass(typerefDataSchema, unionClass);

    JDefinedClass typerefInfoClass = unionClass._class(JMod.PRIVATE | JMod.STATIC | JMod.FINAL, "UnionTyperefInfo");
    generateTyperef(typerefDataSchema, typerefInfoClass);

    JFieldVar typerefInfoField = unionClass.field(JMod.PRIVATE | JMod.STATIC | JMod.FINAL,
                                                  TyperefInfo.class,
                                                  DataTemplateUtil.TYPEREFINFO_FIELD_NAME);
    typerefInfoField.init(JExpr._new(typerefInfoClass));

    unionClass._implements(HasTyperefInfo.class);
    JMethod typerefInfoMethod = unionClass.method(JMod.PUBLIC, TyperefInfo.class, "typerefInfo");
    typerefInfoMethod.body()._return(typerefInfoField);

    final JClass result = generateUnion(schema, unionClass);
    popCurrentLocation();
    return result;
  }

  private JClass generateUnion(UnionDataSchema schema, JDefinedClass unionClass)
          throws JClassAlreadyExistsException
  {
    unionClass._extends(UnionTemplate.class);

    JVar schemaField = generateSchemaField(unionClass, schema);

    generateConstructorWithNoArg(unionClass, schemaField, _dataMapClass);
    generateConstructorWithObjectArg(unionClass, schemaField);

    for (DataSchema memberType: schema.getTypes())
    {
      if (memberType.getDereferencedType() == DataSchema.Type.NULL)
      {
        continue;
      }

      generateUnionMemberAccessors(unionClass,
                                   memberType,
                                   processSchema(memberType,
                                                 unionClass,
                                                 memberType.getUnionMemberKey()),
                                   schemaField);
    }
    generatePathSpecMethodsForUnion(unionClass, schema);

    generateCopierMethods(unionClass);

    return unionClass;
  }

  private void generateUnionMemberAccessors(JDefinedClass unionClass,
                                            DataSchema memberType,
                                            JClass memberClass,
                                            JVar schemaField)
  {
    boolean isDirect = isDirectType(memberType);
    String wrappedOrDirect = isDirect? "Direct" : "Wrapped";
    String memberKey = memberType.getUnionMemberKey();
    String capitalizedName = memberName(memberType);

    String memberFieldName = "MEMBER_" + capitalizedName;
    JFieldVar memberField = unionClass.field(JMod.PRIVATE | JMod.STATIC | JMod.FINAL, DataSchema.class, memberFieldName);
    memberField.init(schemaField.invoke("getType").arg(memberKey));
    String setterName = "set" + capitalizedName;

    // Generate builder.

    JMethod createMethod = unionClass.method(JMod.PUBLIC | JMod.STATIC, unionClass, "create");
    JVar param = createMethod.param(memberClass, "value");
    JVar newUnionVar = createMethod.body().decl(unionClass, "newUnion", JExpr._new(unionClass));
    createMethod.body().invoke(newUnionVar, setterName).arg(param);
    createMethod.body()._return(newUnionVar);

    // Is method.

    JMethod is = unionClass.method(JMod.PUBLIC, getCodeModel().BOOLEAN, "is" + capitalizedName);
    JBlock isBody = is.body();
    JExpression res = JExpr.invoke("memberIs").arg(memberKey);
    isBody._return(res);

    // Getter method.

    String getterName = "get" + capitalizedName;
    JMethod getter = unionClass.method(JMod.PUBLIC, memberClass, getterName);
    JBlock getterBody = getter.body();
    res = JExpr.invoke("obtain" + wrappedOrDirect).arg(memberField).arg(JExpr.dotclass(memberClass)).arg(memberKey);
    getterBody._return(res);

    // Setter method.

    JMethod setter = unionClass.method(JMod.PUBLIC, Void.TYPE, setterName);
    param = setter.param(memberClass, "value");
    JClass dataClass = determineDataClass(memberType, unionClass, memberType.getUnionMemberKey());
    JInvocation inv = setter.body().invoke("select" + wrappedOrDirect).arg(memberField).arg(JExpr.dotclass(memberClass));
    dataClassArg(inv, dataClass).arg(memberKey).arg(param);
  }

  private static String memberName(DataSchema memberType)
  {
    String name;
    if (memberType.getType() == DataSchema.Type.TYPEREF)
    {
      name = ((TyperefDataSchema) memberType).getName();
    }
    else
    {
      name = memberType.getUnionMemberKey();
      int lastIndex = name.lastIndexOf('.');
      if (lastIndex >= 0)
      {
        name = name.substring(lastIndex + 1);
      }
    }
    return capitalize(name);
  }

  private JDefinedClass generateEnum(EnumDataSchema schema) throws JClassAlreadyExistsException
  {
    JDefinedClass enumClass = getPackage(schema.getNamespace())._class(JMod.PUBLIC, escapeReserved(schema.getName()), ClassType.ENUM);

    enumClass.javadoc().append(schema.getDoc());
    setDeprecatedAnnotationAndJavadoc(schema, enumClass);

    registerGeneratedClass(schema, enumClass);
    generateSchemaField(enumClass, schema);

    for (String value: schema.getSymbols())
    {
      if (isReserved(value))
      {
        throw new IllegalArgumentException("Enum contains Java reserved symbol: " + value + " schema: " + schema);
      }

      JEnumConstant enumConstant = enumClass.enumConstant(value);

      String enumConstantDoc = schema.getSymbolDocs().get(value);

      if (enumConstantDoc != null)
      {
        enumConstant.javadoc().append(enumConstantDoc);
      }
      setDeprecatedAnnotationAndJavadoc(schema, value, enumConstant);
    }
    enumClass.enumConstant(DataTemplateUtil.UNKNOWN_ENUM);

    return enumClass;
  }

  private JDefinedClass generateFixed(FixedDataSchema schema) throws JClassAlreadyExistsException
  {
    JDefinedClass fixedClass = getPackage(schema.getNamespace())._class(escapeReserved(schema.getName()));

    fixedClass.javadoc().append(schema.getDoc());
    setDeprecatedAnnotationAndJavadoc(schema, fixedClass);

    registerGeneratedClass(schema, fixedClass);

    fixedClass._extends(FixedTemplate.class);

    JVar schemaField = generateSchemaField(fixedClass, schema);

    JMethod bytesConstructor = fixedClass.constructor(JMod.PUBLIC);
    JVar param = bytesConstructor.param(ByteString.class, "value");
    bytesConstructor.body().invoke(SUPER).arg(param).arg(schemaField);

    generateConstructorWithObjectArg(fixedClass, schemaField);

    generateCopierMethods(fixedClass);

    return fixedClass;
  }

  private JDefinedClass generateTyperef(TyperefDataSchema schema) throws JClassAlreadyExistsException
  {
    JDefinedClass typerefClass = getPackage(schema.getNamespace())._class(escapeReserved(schema.getName()));
    registerGeneratedClass(schema, typerefClass);
    return generateTyperef(schema, typerefClass);
  }

  private JDefinedClass generateTyperef(TyperefDataSchema schema, JDefinedClass typerefClass)
  {
    typerefClass.javadoc().append(schema.getDoc());
    setDeprecatedAnnotationAndJavadoc(schema, typerefClass);

    typerefClass._extends(TyperefInfo.class);

    JVar schemaField = generateSchemaField(typerefClass, schema);

    JMethod constructor = typerefClass.constructor(JMod.PUBLIC);
    constructor.body().invoke(SUPER).arg(schemaField);

    return typerefClass;
  }

  private JDefinedClass generateRecord(RecordDataSchema schema) throws JClassAlreadyExistsException
  {
    JDefinedClass templateClass = getPackage(schema.getNamespace())._class(escapeReserved(schema.getName()));

    templateClass.javadoc().append(schema.getDoc());
    setDeprecatedAnnotationAndJavadoc(schema, templateClass);

    registerGeneratedClass(schema, templateClass);

    templateClass._extends(RecordTemplate.class);

    generatePathSpecMethodsForRecord(templateClass, schema);

    JFieldVar schemaField = generateSchemaField(templateClass, schema);
    generateConstructorWithNoArg(templateClass, schemaField, _dataMapClass);
    generateConstructorWithArg(templateClass, schemaField, _dataMapClass);

    Map<CustomInfo, Object> customInfoMap = new IdentityHashMap<CustomInfo, Object>(schema.getFields().size() * 2);

    for (RecordDataSchema.Field field: schema.getFields())
    {
      generateRecordFieldAccessors(templateClass,
                                   field.getName(),
                                   processSchema(field.getType(), templateClass, field.getName()),
                                   schemaField,
                                   field);
      CustomInfo customInfo = firstCustomInfo(field.getType());
      if (customInfo != null && customInfoMap.containsKey(customInfo) == false)
      {
        customInfoMap.put(customInfo, null);
        generateCustomClassInitialization(templateClass, customInfo);
      }
    }

    List<NamedDataSchema> includes = schema.getInclude();
    for (NamedDataSchema includedSchema : includes)
    {
      handleSchema(includedSchema);
    }

    generateCopierMethods(templateClass);

    return templateClass;
  }

  private static void generateCopierMethods(JDefinedClass templateClass)
  {
    overrideCopierMethod(templateClass, "clone");
    overrideCopierMethod(templateClass, "copy");
  }

  private static void overrideCopierMethod(JDefinedClass templateClass, String methodName)
  {
    JMethod copierMethod = templateClass.method(JMod.PUBLIC, templateClass, methodName);
    copierMethod.annotate(Override.class);
    copierMethod._throws(CloneNotSupportedException.class);
    copierMethod.body()._return(JExpr.cast(templateClass, JExpr._super().invoke(methodName)));
  }

  /**
   * @see Custom#initializeCustomClass(Class)
   * @see Custom#initializeCoercerClass(Class)
   */
  private void generateCustomClassInitialization(JDefinedClass templateClass, CustomInfo customInfo)
  {
    if (customInfo != null)
    {
      // initialize custom class
      templateClass.init().add(_customClass.staticInvoke("initializeCustomClass").arg(customInfo.customClass.dotclass()));

      // initialize explicitly specified coercer class
      if (customInfo.coercerClass != null)
      {
        templateClass.init().add(_customClass.staticInvoke("initializeCoercerClass").arg(customInfo.coercerClass.dotclass()));
      }
    }
  }

  private static boolean hasNestedFields(DataSchema schema)
  {
    while (true)
    {
      switch (schema.getDereferencedType())
      {
      case RECORD:
        return true;
      case UNION:
        return true;
      case ARRAY:
        schema = ((ArrayDataSchema) schema.getDereferencedDataSchema()).getItems();
        continue;
      case MAP:
        schema = ((MapDataSchema) schema.getDereferencedDataSchema()).getValues();
        continue;
      default:
        return false;
      }
    }
  }

  private static DataSchema getCollectionChildSchema(DataSchema schema)
  {
    switch (schema.getDereferencedType())
    {
      case ARRAY:
        return ((ArrayDataSchema) schema.getDereferencedDataSchema()).getItems();
      case MAP:
        return ((MapDataSchema) schema.getDereferencedDataSchema()).getValues();
      default:
        return null;
    }
  }

  private void generatePathSpecMethodsForUnion(JDefinedClass unionClass, UnionDataSchema schema)
            throws JClassAlreadyExistsException
  {
    JDefinedClass fieldsNestedClass = generatePathSpecNestedClass(unionClass);

    for (DataSchema unionMemberType : schema.getTypes())
    {
      JClass fieldsRefType = _pathSpecClass;
      if (hasNestedFields(unionMemberType))
      {
        JClass unionMemberClass = processSchema(unionMemberType, unionClass, unionMemberType.getUnionMemberKey());
        fieldsRefType = getCodeModel().ref(unionMemberClass.fullName() + ".Fields");
      }
      JMethod accessorMethod = fieldsNestedClass.method(JMod.PUBLIC, fieldsRefType, memberName(unionMemberType));
      accessorMethod.body()._return(JExpr._new(fieldsRefType).arg(JExpr.invoke("getPathComponents")).arg(unionMemberType.getUnionMemberKey()));
    }
  }

  private void generatePathSpecMethodsForRecord(JDefinedClass templateClass,
                                                RecordDataSchema schema)
          throws JClassAlreadyExistsException
  {
    JDefinedClass fieldsNestedClass = generatePathSpecNestedClass(templateClass);

    for (RecordDataSchema.Field field : schema.getFields())
    {
      JClass fieldsRefType = _pathSpecClass;
      if (hasNestedFields(field.getType()))
      {
        JClass fieldType = processSchema(field.getType(), templateClass, field.getName());
        fieldsRefType = getCodeModel().ref(fieldType.fullName() + ".Fields");
      }

      JMethod constantField = fieldsNestedClass.method(JMod.PUBLIC, fieldsRefType, escapeReserved(field.getName()));
      constantField.body()._return(JExpr._new(fieldsRefType).arg(JExpr.invoke("getPathComponents")).arg(
              field.getName()));
      if (! field.getDoc().isEmpty()) {
        constantField.javadoc().append(field.getDoc());
      }
      setDeprecatedAnnotationAndJavadoc(constantField, field);
    }

    JVar staticFields = templateClass.field(JMod.PRIVATE | JMod.STATIC | JMod.FINAL, fieldsNestedClass, "_fields").init(JExpr._new(fieldsNestedClass));
    JMethod staticFieldsAccessor = templateClass.method(JMod.PUBLIC | JMod.STATIC, fieldsNestedClass, "fields");
    staticFieldsAccessor.body()._return(staticFields);
  }

  private void generatePathSpecMethodsForCollection(JDefinedClass templateClass, DataSchema schema, String wildcardMethodName)
          throws JClassAlreadyExistsException
  {
    if (hasNestedFields(schema))
    {
      JDefinedClass fieldsNestedClass = generatePathSpecNestedClass(templateClass);

      DataSchema nestedSchema = getCollectionChildSchema(schema);
      JClass itemsType = processSchema(nestedSchema, templateClass, wildcardMethodName);
      JClass itemsFieldType = getCodeModel().ref(itemsType.fullName() + ".Fields");

      JMethod constantField = fieldsNestedClass.method(JMod.PUBLIC, itemsFieldType, wildcardMethodName);
      constantField.body()._return(JExpr._new(itemsFieldType).arg(JExpr.invoke("getPathComponents")).arg(_pathSpecClass.staticRef("WILDCARD")));
    }
  }

  private JDefinedClass generatePathSpecNestedClass(JDefinedClass templateClass)
          throws JClassAlreadyExistsException
  {
    JDefinedClass fieldsNestedClass = templateClass._class(JMod.PUBLIC | JMod.STATIC, "Fields");
    fieldsNestedClass._extends(_pathSpecClass);

    JMethod constructor = fieldsNestedClass.constructor(JMod.PUBLIC);
    JClass listString = getCodeModel().ref(List.class).narrow(String.class);
    JVar namespace = constructor.param(listString, "path");
    JVar name = constructor.param(String.class, "name");
    constructor.body().invoke(SUPER).arg(namespace).arg(name);

    fieldsNestedClass.constructor(JMod.PUBLIC).body().invoke(SUPER);
    return fieldsNestedClass;
  }

  private JFieldVar generateSchemaField(JDefinedClass templateClass, DataSchema schema)
  {
    JFieldVar schemaField = templateClass.field(JMod.PRIVATE | JMod.STATIC | JMod.FINAL,
                                                schema.getClass(),
                                                DataTemplateUtil.SCHEMA_FIELD_NAME);
    String schemaJson = SchemaToJsonEncoder.schemaToJson(schema, JsonBuilder.Pretty.COMPACT);
    JInvocation parseSchemaInvocation;
    if (schemaJson.length() < MAX_SCHEMA_JSON_LENGTH)
    {
      parseSchemaInvocation = _dataTemplateUtilClass.staticInvoke("parseSchema").arg(schemaJson);
    }
    else
    {
      JInvocation stringBuilderInvocation = JExpr._new(_stringBuilderClass);
      for (int index = 0; index < schemaJson.length(); index += MAX_SCHEMA_JSON_LENGTH)
      {
        stringBuilderInvocation = stringBuilderInvocation.
          invoke("append").
          arg(schemaJson.substring(index, Math.min(schemaJson.length(), index + MAX_SCHEMA_JSON_LENGTH)));
      }
      stringBuilderInvocation = stringBuilderInvocation.invoke("toString");
      parseSchemaInvocation = _dataTemplateUtilClass.staticInvoke("parseSchema").arg(stringBuilderInvocation);
    }
    schemaField.init(JExpr.cast(getCodeModel()._ref(schema.getClass()),
                                parseSchemaInvocation));

    return schemaField;
  }

  private static void generateConstructorWithNoArg(JDefinedClass cls, JVar schemaField, JClass newClass)
  {
    JMethod noArgConstructor = cls.constructor(JMod.PUBLIC);
    noArgConstructor.body().invoke(SUPER).arg(JExpr._new(newClass)).arg(schemaField);
  }

  private static void generateConstructorWithNoArg(JDefinedClass cls, JClass newClass)
  {
    JMethod noArgConstructor = cls.constructor(JMod.PUBLIC);
    noArgConstructor.body().invoke(THIS).arg(JExpr._new(newClass));
  }

  private static void generateConstructorWithObjectArg(JDefinedClass cls, JVar schemaField)
  {
    JMethod argConstructor = cls.constructor(JMod.PUBLIC);
    JVar param = argConstructor.param(Object.class, "data");
    argConstructor.body().invoke(SUPER).arg(param).arg(schemaField);
  }

  private static void generateConstructorWithArg(JDefinedClass cls, JVar schemaField, JClass paramClass)
  {
    JMethod argConstructor = cls.constructor(JMod.PUBLIC);
    JVar param = argConstructor.param(paramClass, "data");
    argConstructor.body().invoke(SUPER).arg(param).arg(schemaField);
  }

  private static void generateConstructorWithArg(JDefinedClass cls, JVar schemaField, JClass paramClass, JClass elementClass, JClass dataClass)
  {
    JMethod argConstructor = cls.constructor(JMod.PUBLIC);
    JVar param = argConstructor.param(paramClass, "data");
    JInvocation inv = argConstructor.body().invoke(SUPER).arg(param).arg(schemaField).arg(JExpr.dotclass(elementClass));
    dataClassArg(inv, dataClass);
  }

  private void generateConstructorWithInitialCapacity(JDefinedClass cls, JClass elementClass)
  {
    JMethod argConstructor = cls.constructor(JMod.PUBLIC);
    JVar initialCapacity = argConstructor.param(getCodeModel().INT, "initialCapacity");
    argConstructor.body().invoke(THIS).arg(JExpr._new(elementClass).arg(initialCapacity));
  }

  private void generateConstructorWithCollection(JDefinedClass cls, JClass elementClass)
  {
    JMethod argConstructor = cls.constructor(JMod.PUBLIC);
    JVar c = argConstructor.param(_collectionClass.narrow(elementClass), "c");
    argConstructor.body().invoke(THIS).arg(JExpr._new(_dataListClass).arg(c.invoke("size")));
    argConstructor.body().invoke("addAll").arg(c);
  }

  private void generateConstructorWithInitialCapacityAndLoadFactor(JDefinedClass cls)
  {
    JMethod argConstructor = cls.constructor(JMod.PUBLIC);
    JVar initialCapacity = argConstructor.param(getCodeModel().INT , "initialCapacity");
    JVar loadFactor = argConstructor.param(getCodeModel().FLOAT, "loadFactor");
    argConstructor.body().invoke(THIS).arg(JExpr._new(_dataMapClass).arg(initialCapacity).arg(loadFactor));
  }

  private void generateConstructorWithMap(JDefinedClass cls, JClass valueClass)
  {
    JMethod argConstructor = cls.constructor(JMod.PUBLIC);
    JVar m = argConstructor.param(_mapClass.narrow(_stringClass, valueClass) , "m");
    argConstructor.body().invoke(THIS).arg(JExpr.invoke("newDataMapOfSize").arg(m.invoke("size")));
    argConstructor.body().invoke("putAll").arg(m);
  }

  private void generateRecordFieldAccessors(JDefinedClass templateClass,
                                            String fieldName,
                                            JClass type,
                                            JVar schemaField,
                                            RecordDataSchema.Field field)
  {
    final DataSchema fieldSchema = field.getType();
    boolean isDirect = isDirectType(fieldSchema);
    String wrappedOrDirect;
    if (isDirect)
    {
      wrappedOrDirect = (firstCustomInfo(fieldSchema) == null ? "Direct" : "CustomType");
    }
    else
    {
      wrappedOrDirect = "Wrapped";
    }
    String capitalizedName = capitalize(fieldName);

    String fieldFieldName = "FIELD_" + capitalizedName;
    JFieldVar fieldField = templateClass.field(JMod.PRIVATE | JMod.STATIC | JMod.FINAL, RecordDataSchema.Field.class, fieldFieldName);
    fieldField.init(schemaField.invoke("getField").arg(field.getName()));

    // Generate has method.
    JMethod has = templateClass.method(JMod.PUBLIC, getCodeModel().BOOLEAN, "has" + capitalizedName);
    addAccessorDoc(has, field, "Existence checker");
    setDeprecatedAnnotationAndJavadoc(has, field);
    JBlock hasBody = has.body();
    JExpression res = JExpr.invoke("contains").arg(fieldField);
    hasBody._return(res);

    // Generate remove method.
    String removeName = "remove" + capitalizedName;
    JMethod remove = templateClass.method(JMod.PUBLIC, getCodeModel().VOID, removeName);
    addAccessorDoc(remove, field, "Remover");
    setDeprecatedAnnotationAndJavadoc(remove, field);
    JBlock removeBody = remove.body();
    removeBody.invoke("remove").arg(fieldField);

    // Getter method with mode.
    String getterName = getGetterName(type, capitalizedName);
    JMethod getterWithMode = templateClass.method(JMod.PUBLIC, type, getterName);
    addAccessorDoc(getterWithMode, field, "Getter");
    setDeprecatedAnnotationAndJavadoc(getterWithMode, field);
    JVar modeParam = getterWithMode.param(_getModeClass, "mode");
    JBlock getterWithModeBody = getterWithMode.body();
    res = JExpr.invoke("obtain" + wrappedOrDirect).arg(fieldField).arg(JExpr.dotclass(type)).arg(modeParam);
    getterWithModeBody._return(res);

    // Getter method without mode.
    JMethod getterWithoutMode = templateClass.method(JMod.PUBLIC, type, getterName);
    addAccessorDoc(getterWithoutMode, field, "Getter");
    setDeprecatedAnnotationAndJavadoc(getterWithoutMode, field);
    res = JExpr.invoke(getterName).arg(_strictGetMode);
    getterWithoutMode.body()._return(res);

    // Determine dataClass
    JClass dataClass = determineDataClass(field.getType(), templateClass, fieldName);

    // Setter method with mode
    String setterName = "set" + capitalizedName;
    JMethod setterWithMode =  templateClass.method(JMod.PUBLIC, templateClass, setterName);
    addAccessorDoc(setterWithMode, field, "Setter");
    setDeprecatedAnnotationAndJavadoc(setterWithMode, field);
    JVar param = setterWithMode.param(type, "value");
    modeParam = setterWithMode.param(_setModeClass, "mode");
    JInvocation inv = setterWithMode.body().invoke("put" + wrappedOrDirect).arg(fieldField).arg(JExpr.dotclass(type));
    dataClassArg(inv, dataClass).arg(param).arg(modeParam);
    setterWithMode.body()._return(JExpr._this());

    // Setter method without mode
    JMethod setter =  templateClass.method(JMod.PUBLIC, templateClass, setterName);
    addAccessorDoc(setter, field, "Setter");
    setDeprecatedAnnotationAndJavadoc(setter, field);
    param = setter.param(type, "value");
    inv = setter.body().invoke("put" + wrappedOrDirect).arg(fieldField).arg(JExpr.dotclass(type));
    dataClassArg(inv, dataClass).arg(param).arg(_disallowNullSetMode);
    setter.body()._return(JExpr._this());

    // Setter method without mode for unboxified type
    if (type.unboxify().equals(type) == false)
    {
      JMethod unboxifySetter =  templateClass.method(JMod.PUBLIC, templateClass, setterName);
      addAccessorDoc(unboxifySetter, field, "Setter");
      setDeprecatedAnnotationAndJavadoc(unboxifySetter, field);
      param = unboxifySetter.param(type.unboxify(), "value");
      inv = unboxifySetter.body().invoke("put" + wrappedOrDirect).arg(fieldField).arg(JExpr.dotclass(type));
      dataClassArg(inv, dataClass).arg(param).arg(_disallowNullSetMode);
      unboxifySetter.body()._return(JExpr._this());
    }
  }

  private void addAccessorDoc(JMethod method, RecordDataSchema.Field field, String prefix)
  {
    method.javadoc().append(prefix + " for " + field.getName());
    method.javadoc().addXdoclet("see Fields#" + escapeReserved(field.getName()));
  }

  /**
   * Return Java class name for a {@link NamedDataSchema}.
   *
   * @param schema provides the {@link NamedDataSchema}.
   * @return the fully qualified Java class name for the provided {@link NamedDataSchema}.
   */
  private String classNameForNamedSchema(NamedDataSchema schema)
  {
    StringBuilder sb = new StringBuilder();
    String namespace = schema.getNamespace();
    if (namespace.isEmpty() == false)
    {
      sb.append(namespace);
      sb.append('.');
    }
    sb.append(escapeReserved(schema.getName()));
    return sb.toString();
  }

  /*
   * Determine name and class for unnamed types.
   */
  private ClassInfo classInfoForUnnamed(JDefinedClass parentClass,
                                        String name,
                                        DataSchema schema)
  {
    assert schema instanceof NamedDataSchema == false;
    assert schema instanceof PrimitiveDataSchema == false;

    ClassInfo classInfo = classNameForUnnamedTraverse(parentClass, name, schema);
    String className = classInfo.fullName();

    DataSchema schemaFromClassName = _classNameToSchemaMap.get(className);
    if (schemaFromClassName != null)
    {
      checkForClassNameConflict(className, schema);
      classInfo.existingClass = _schemaToClassMap.get(schemaFromClassName);
    }
    else
    {
      try
      {
        classInfo.definedClass =
            parentClass != null && classInfo.namespace.equals(parentClass.fullName()) ?
                parentClass._class(JMod.PUBLIC | JMod.STATIC | JMod.FINAL, classInfo.name) :
                getPackage(classInfo.namespace)._class(JMod.PUBLIC, classInfo.name);
      }
      catch (JClassAlreadyExistsException exc)
      {
        throw new IllegalStateException("Duplicate class definition should never occur", exc);
      }
    }

    return classInfo;
  }

  private class ClassInfo
  {
    private ClassInfo(String namespace, String name)
    {
      this.namespace = namespace;
      this.name = name;
    }

    private String namespace;
    private String name;
    private JClass existingClass;
    private JDefinedClass definedClass;

    private String fullName()
    {
      String actualNamespace = getPackage(namespace).name();
      return actualNamespace.isEmpty() ? name : actualNamespace + '.' + name;
    }
  }

  private ClassInfo classNameForUnnamedTraverse(JDefinedClass parentClass, String memberName, DataSchema schema)
  {
    DataSchema dereferencedDataSchema = schema.getDereferencedDataSchema();
    switch (dereferencedDataSchema.getType())
    {
      case ARRAY:
        ArrayDataSchema arraySchema = (ArrayDataSchema) dereferencedDataSchema;
        CustomInfo customInfo = firstCustomInfo(arraySchema.getItems());
        if (customInfo != null)
        {
          return new ClassInfo(customInfo.customSchema.getNamespace(), customInfo.customSchema.getName() + ARRAY_SUFFIX);
        }
        else
        {
          ClassInfo classInfo = classNameForUnnamedTraverse(parentClass, memberName, arraySchema.getItems());
          classInfo.name += ARRAY_SUFFIX;
          return classInfo;
        }
      case MAP:
        MapDataSchema mapSchema = (MapDataSchema) dereferencedDataSchema;
        customInfo = firstCustomInfo(mapSchema.getValues());
        if (customInfo != null)
        {
          return new ClassInfo(customInfo.customSchema.getNamespace(), customInfo.customSchema.getName() + MAP_SUFFIX);
        }
        else
        {
          ClassInfo classInfo = classNameForUnnamedTraverse(parentClass, memberName, mapSchema.getValues());
          classInfo.name += MAP_SUFFIX;
          return classInfo;
        }

      case UNION:
        if (schema.getType() == DataSchema.Type.TYPEREF)
        {
          DataSchema referencedDataSchema;
          TyperefDataSchema typerefDataSchema = (TyperefDataSchema) schema;
          while ((referencedDataSchema = typerefDataSchema.getDereferencedDataSchema()) != dereferencedDataSchema)
          {
            typerefDataSchema = (TyperefDataSchema) referencedDataSchema;
          }
          return new ClassInfo(typerefDataSchema.getNamespace(), capitalize(typerefDataSchema.getName()));
        }
        else
        {
          return new ClassInfo(parentClass.fullName(), capitalize(memberName));
        }

      case FIXED:
      case RECORD:
      case ENUM:
        NamedDataSchema namedSchema = (NamedDataSchema) dereferencedDataSchema;
        return new ClassInfo(namedSchema.getNamespace(), capitalize(namedSchema.getName()));

      case BOOLEAN:
        return new ClassInfo(_templatePackageName, "Boolean");

      case INT:
        return new ClassInfo(_templatePackageName, "Integer");

      case LONG:
        return new ClassInfo(_templatePackageName, "Long");

      case FLOAT:
        return new ClassInfo(_templatePackageName, "Float");

      case DOUBLE:
        return new ClassInfo(_templatePackageName, "Double");

      case STRING:
        return new ClassInfo(_templatePackageName, "String");

      case BYTES:
        return new ClassInfo(_templatePackageName, "ByteString");

      case NULL:
        throw nullTypeNotAllowed(parentClass, memberName);

      default:
        throw unrecognizedSchemaType(parentClass, memberName, dereferencedDataSchema);
    }
  }

  /**
   * Checks if a class name conflict occurs, if it occurs throws {@link IllegalArgumentException}.
   *
   * @param className provides the Java class name.
   * @param schema provides the {@link DataSchema} that would be bound if there is no conflict.
   * @throws IllegalArgumentException
   */
  private void checkForClassNameConflict(String className, DataSchema schema)
    throws IllegalArgumentException
  {
    DataSchema schemaFromClassName = _classNameToSchemaMap.get(className);
    boolean conflict = false;
    if (schemaFromClassName != null && schemaFromClassName != schema)
    {
      DataSchema.Type schemaType = schema.getType();
      if (schemaFromClassName.getType() != schemaType)
      {
        conflict = true;
      }
      else if (schema instanceof NamedDataSchema)
      {
        conflict = true;
      }
      else if (schemaFromClassName.equals(schema) == false)
      {
        assert schemaType == DataSchema.Type.ARRAY || schemaType == DataSchema.Type.MAP;
        //
        // see schemaForArrayItemsOrMapValues
        //
        // When the schema bound to the specified class name is different
        // from the specified schema, then emit a log message when this occurs.
        //
        log.info("Class name: " + className +
                 ", bound to schema:" + schemaFromClassName +
                 ", instead of schema: " + schema);
      }
    }
    if (conflict)
    {
      throw new IllegalArgumentException("Java class name conflict detected, class name: " + className +
                                         ", class already bound to schema: " + schemaFromClassName +
                                         ", attempting to rebind to schema: " + schema);
    }
  }

  /*
   * Return exception for trying to use null type outside of a union.
   */
  private static IllegalArgumentException nullTypeNotAllowed(JDefinedClass parentClass, String memberName)
  {
    return new IllegalArgumentException("The null type can only be used in unions, null found" +
                                        parentClassAndMemberNameToString(parentClass, memberName));
  }

  /*
   * Return exception for unrecognized schema type.
   */
  private static IllegalStateException unrecognizedSchemaType(JDefinedClass parentClass,
                                                              String memberName,
                                                              DataSchema schema)
  {
    return new IllegalStateException("Unrecognized schema: " + schema +
                                     parentClassAndMemberNameToString(parentClass, memberName));
  }

  /*
   * Generate human consumable representation of parent class and field name.
   */
  private static String parentClassAndMemberNameToString(JDefinedClass parentClass, String memberName)
  {
    StringBuilder sb = new StringBuilder();
    if (memberName != null)
    {
      sb.append(" in ");
      sb.append(memberName);
    }
    if (parentClass != null)
    {
      sb.append(" in ");
      sb.append(parentClass.fullName());
    }
    return sb.toString();
  }

  /**
   * Used by isDirectType to determine which types are direct vs wrapped.
   */
  private static final Set<DataSchema.Type> _directTypes = EnumSet.of(
                  DataSchema.Type.BOOLEAN,
                  DataSchema.Type.INT,
                  DataSchema.Type.LONG,
                  DataSchema.Type.FLOAT,
                  DataSchema.Type.DOUBLE,
                  DataSchema.Type.STRING,
                  DataSchema.Type.BYTES,
                  DataSchema.Type.ENUM
  );

  protected static boolean isDirectType(DataSchema schema)
  {
    return _directTypes.contains(schema.getDereferencedType());
  }

  //
  // Deprecated annotation utils
  //
  private static final String DEPRECATED_KEY = "deprecated";
  private static final String DEPRECATED_SYMBOLS_KEY = "deprecatedSymbols";

  private void setDeprecatedAnnotationAndJavadoc(DataSchema schema, JDefinedClass schemaClass)
  {
    setDeprecatedAnnotationAndJavadoc(schema.getProperties().get(DEPRECATED_KEY), schemaClass, schemaClass);
  }

  private void setDeprecatedAnnotationAndJavadoc(JMethod method, RecordDataSchema.Field field)
  {
    setDeprecatedAnnotationAndJavadoc(field.getProperties().get(DEPRECATED_KEY), method, method);
  }

  private void setDeprecatedAnnotationAndJavadoc(EnumDataSchema enumSchema, String symbol, JEnumConstant constant)
  {
    Object deprecatedSymbolsProp = enumSchema.getProperties().get(DEPRECATED_SYMBOLS_KEY);
    if (deprecatedSymbolsProp instanceof DataMap)
    {
      DataMap deprecatedSymbols = (DataMap)deprecatedSymbolsProp;

      Object deprecatedProp = deprecatedSymbols.get(symbol);
      setDeprecatedAnnotationAndJavadoc(deprecatedProp, constant, constant);
    }
  }

  private void setDeprecatedAnnotationAndJavadoc(Object deprecatedProp,
                                                 JAnnotatable annotatable,
                                                 JDocCommentable commentable)
  {
    if (Boolean.TRUE.equals(deprecatedProp) && annotatable != null)
    {
      annotatable.annotate(Deprecated.class);
    }
    else if (deprecatedProp instanceof String)
    {
      if (commentable != null)
      {
        String deprecatedReason = (String)deprecatedProp;
        commentable.javadoc().addDeprecated().append(deprecatedReason);
      }
      if (annotatable != null)
      {
        annotatable.annotate(Deprecated.class);
      }
    }
  }
}
