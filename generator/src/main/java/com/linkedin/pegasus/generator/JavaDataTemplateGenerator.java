/*
   Copyright (c) 2015 LinkedIn Corp.

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
import com.linkedin.data.DataMap;
import com.linkedin.data.DataMapBuilder;
import com.linkedin.data.collections.CheckedMap;
import com.linkedin.data.schema.ArrayDataSchema;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaConstants;
import com.linkedin.data.schema.EnumDataSchema;
import com.linkedin.data.schema.JsonBuilder;
import com.linkedin.data.schema.MapDataSchema;
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.SchemaFormatType;
import com.linkedin.data.schema.SchemaToJsonEncoder;
import com.linkedin.data.schema.SchemaToPdlEncoder;
import com.linkedin.data.template.BooleanArray;
import com.linkedin.data.template.BooleanMap;
import com.linkedin.data.template.BytesArray;
import com.linkedin.data.template.BytesMap;
import com.linkedin.data.template.DataTemplateUtil;
import com.linkedin.data.template.DirectArrayTemplate;
import com.linkedin.data.template.DirectMapTemplate;
import com.linkedin.data.template.DoubleArray;
import com.linkedin.data.template.DoubleMap;
import com.linkedin.data.template.FixedTemplate;
import com.linkedin.data.template.FloatArray;
import com.linkedin.data.template.FloatMap;
import com.linkedin.data.template.HasTyperefInfo;
import com.linkedin.data.template.IntegerArray;
import com.linkedin.data.template.IntegerMap;
import com.linkedin.data.template.LongArray;
import com.linkedin.data.template.LongMap;
import com.linkedin.data.template.RecordTemplate;
import com.linkedin.data.template.RequiredFieldNotPresentException;
import com.linkedin.data.template.StringArray;
import com.linkedin.data.template.StringMap;
import com.linkedin.data.template.TemplateOutputCastException;
import com.linkedin.data.template.TyperefInfo;
import com.linkedin.data.template.UnionTemplate;
import com.linkedin.data.template.WrappingArrayTemplate;
import com.linkedin.data.template.WrappingMapTemplate;
import com.linkedin.pegasus.generator.spec.ArrayTemplateSpec;
import com.linkedin.pegasus.generator.spec.ClassTemplateSpec;
import com.linkedin.pegasus.generator.spec.CustomInfoSpec;
import com.linkedin.pegasus.generator.spec.EnumTemplateSpec;
import com.linkedin.pegasus.generator.spec.FixedTemplateSpec;
import com.linkedin.pegasus.generator.spec.MapTemplateSpec;
import com.linkedin.pegasus.generator.spec.ModifierSpec;
import com.linkedin.pegasus.generator.spec.PrimitiveTemplateSpec;
import com.linkedin.pegasus.generator.spec.RecordTemplateSpec;
import com.linkedin.pegasus.generator.spec.TyperefTemplateSpec;
import com.linkedin.pegasus.generator.spec.UnionTemplateSpec;

import com.linkedin.util.ArgumentUtil;
import com.sun.codemodel.JCase;
import com.sun.codemodel.JConditional;
import com.sun.codemodel.JFieldRef;
import com.sun.codemodel.JOp;
import com.sun.codemodel.JSwitch;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.sun.codemodel.ClassType;
import com.sun.codemodel.JAnnotatable;
import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JClassContainer;
import com.sun.codemodel.JCommentPart;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JDocCommentable;
import com.sun.codemodel.JEnumConstant;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JVar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Generates CodeModel {@link JClass} of data templates from {@link ClassTemplateSpec}.
 *
 * @author Keren Jin
 */
public class JavaDataTemplateGenerator extends JavaCodeGeneratorBase
{
  /**
   * Rest.li pre-defines some commonly used Java data template classes such as {@link IntegerArray}.
   * This generator will directly use these classes instead of generate them anew.
   */
  public static final Map<DataSchema, Class<?>> PredefinedJavaClasses;
  static
  {
    final Class<?>[] predefinedClass = new Class<?>[] {
        BooleanArray.class,
        BooleanMap.class,
        BytesArray.class,
        BytesMap.class,
        DoubleArray.class,
        DoubleMap.class,
        FloatArray.class,
        FloatMap.class,
        IntegerArray.class,
        IntegerMap.class,
        LongArray.class,
        LongMap.class,
        StringArray.class,
        StringMap.class
    };

    PredefinedJavaClasses = new HashMap<DataSchema, Class<?>>();

    for (Class<?> clazz : predefinedClass)
    {
      final DataSchema schema = DataTemplateUtil.getSchema(clazz);
      PredefinedJavaClasses.put(schema, clazz);
    }
  }

  /*
   * When the original schema format type cannot be determined, encode the generated schema field in this format.
   * This is primarily to handle data templates generated from IDLs, which are not in a particular schema format.
   *
   * TODO: once the PDL migration is done, switch this to PDL
   */
  private static final SchemaFormatType DEFAULT_SCHEMA_FORMAT_TYPE = SchemaFormatType.PDSC;

  private static final int DEFAULT_DATAMAP_INITIAL_CAPACITY = 16; // From HashMap's default initial capacity
  private static final Logger _log = LoggerFactory.getLogger(JavaDataTemplateGenerator.class);
  //
  // Deprecated annotation utils
  //
  private static final String DEPRECATED_KEY = "deprecated";

  private final Map<ClassTemplateSpec, JDefinedClass> _definedClasses = new HashMap<ClassTemplateSpec, JDefinedClass>();
  private final Map<JDefinedClass, ClassTemplateSpec> _generatedClasses = new HashMap<JDefinedClass, ClassTemplateSpec>();

  private final JClass _recordBaseClass;
  private final JClass _unionBaseClass;
  private final JClass _wrappingArrayBaseClass;
  private final JClass _wrappingMapBaseClass;
  private final JClass _directArrayBaseClass;
  private final JClass _directMapBaseClass;
  private final JClass _schemaFormatTypeClass;

  private final boolean _recordFieldAccessorWithMode;
  private final boolean _recordFieldRemove;
  private final boolean _pathSpecMethods;
  private final boolean _copierMethods;
  private final String _rootPath;

  private JavaDataTemplateGenerator(String defaultPackage,
                                    boolean recordFieldAccessorWithMode,
                                    boolean recordFieldRemove,
                                    boolean pathSpecMethods,
                                    boolean copierMethods,
                                    String rootPath)
  {
    super(defaultPackage);

    _recordBaseClass = getCodeModel().ref(RecordTemplate.class);
    _unionBaseClass = getCodeModel().ref(UnionTemplate.class);
    _wrappingArrayBaseClass = getCodeModel().ref(WrappingArrayTemplate.class);
    _wrappingMapBaseClass = getCodeModel().ref(WrappingMapTemplate.class);
    _directArrayBaseClass = getCodeModel().ref(DirectArrayTemplate.class);
    _directMapBaseClass = getCodeModel().ref(DirectMapTemplate.class);
    _schemaFormatTypeClass = getCodeModel().ref(SchemaFormatType.class);

    _recordFieldAccessorWithMode = recordFieldAccessorWithMode;
    _recordFieldRemove = recordFieldRemove;
    _pathSpecMethods = pathSpecMethods;
    _copierMethods = copierMethods;
    _rootPath = rootPath;
  }

  public JavaDataTemplateGenerator(Config config)
  {
    this(config.getDefaultPackage(),
         config.getRecordFieldAccessorWithMode(),
         config.getRecordFieldRemove(),
         config.getPathSpecMethods(),
         config.getCopierMethods(),
         config.getRootPath());
  }

  /**
   * @param defaultPackage package to be used when a {@link NamedDataSchema} does not specify a namespace
   */
  public JavaDataTemplateGenerator(String defaultPackage)
  {
    this(defaultPackage,
         null);
  }

  /**
   * @param defaultPackage package to be used when a {@link NamedDataSchema} does not specify a namespace
   * @param rootPath root path to relativize the location
   */
  public JavaDataTemplateGenerator(String defaultPackage, String rootPath)
  {
    this(defaultPackage,
         true,
         true,
         true,
         true,
         rootPath);
  }

  public Map<JDefinedClass, ClassTemplateSpec> getGeneratedClasses()
  {
    return _generatedClasses;
  }

  public JClass generate(ClassTemplateSpec classTemplateSpec)
  {
    final JClass result;

    if (classTemplateSpec == null)
    {
      result = null;
    }
    else
    {
      if (classTemplateSpec.getSchema() == null)
      {
        // this is for custom class, package override is not applicable.
        result = getCodeModel().directClass(classTemplateSpec.getFullName());
      }
      else if (PredefinedJavaClasses.containsKey(classTemplateSpec.getSchema()))
      {
        final Class<?> nativeJavaClass = PredefinedJavaClasses.get(classTemplateSpec.getSchema());
        result = getCodeModel().ref(nativeJavaClass);
      }
      else if (classTemplateSpec.getSchema().isPrimitive())
      {
        result = generatePrimitive((PrimitiveTemplateSpec) classTemplateSpec);
      }
      else
      {
        try
        {
          final JDefinedClass definedClass = defineClass(classTemplateSpec);
          populateClassContent(classTemplateSpec, definedClass);
          result = definedClass;
        }
        catch (JClassAlreadyExistsException e)
        {
          throw new IllegalArgumentException(classTemplateSpec.getFullName());
        }
      }
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

  private static void generateCopierMethods(JDefinedClass templateClass, Map<String, JVar> fields, JClass changeListenerClass)
  {
    // Clone is a shallow copy and shouldn't reset fields, copy is a deep copy and should.
    overrideCopierMethod(templateClass, "clone", fields, false, changeListenerClass);
    overrideCopierMethod(templateClass, "copy", fields, true, changeListenerClass);
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

  private static boolean isArrayType(DataSchema schema)
  {
    return schema.getDereferencedType() == DataSchema.Type.ARRAY;
  }

  private static void generateConstructorWithNoArg(JDefinedClass cls, JClass newClass)
  {
    final JMethod noArgConstructor = cls.constructor(JMod.PUBLIC);
    noArgConstructor.body().invoke(THIS).arg(JExpr._new(newClass));
  }

  private static void generateConstructorWithObjectArg(JDefinedClass cls, JVar schemaField, JVar changeListenerVar)
  {
    final JMethod argConstructor = cls.constructor(JMod.PUBLIC);
    final JVar param = argConstructor.param(Object.class, "data");
    argConstructor.body().invoke(SUPER).arg(param).arg(schemaField);
    if (changeListenerVar != null)
    {
      addChangeListenerRegistration(argConstructor, changeListenerVar);
    }
  }

  private static void generateConstructorWithArg(JDefinedClass cls, JVar schemaField, JClass paramClass, JVar changeListenerVar)
  {
    final JMethod argConstructor = cls.constructor(JMod.PUBLIC);
    final JVar param = argConstructor.param(paramClass, "data");
    argConstructor.body().invoke(SUPER).arg(param).arg(schemaField);

    if (changeListenerVar != null)
    {
      addChangeListenerRegistration(argConstructor, changeListenerVar);
    }
  }

  private static void generateConstructorWithArg(JDefinedClass cls, JVar schemaField, JClass paramClass, JClass elementClass, JClass dataClass)
  {
    final JMethod argConstructor = cls.constructor(JMod.PUBLIC);
    final JVar param = argConstructor.param(paramClass, "data");
    final JInvocation inv = argConstructor.body().invoke(SUPER).arg(param).arg(schemaField).arg(JExpr.dotclass(elementClass));
    dataClassArg(inv, dataClass);
  }

  private static void addChangeListenerRegistration(JMethod constructor, JVar changeListenerVar)
  {
    constructor.body().invoke("addChangeListener").arg(changeListenerVar);
  }

  /**
   * Return the {@link DataSchema} for the array items or map values of the generated class.
   * <p/>
   * <p/>
   * When there is both an array of X and array of typeref XRef to X, both of these cases maps to the same generated class name, i.e. XArray. However, their schema is slightly different. From a
   * compile time binding perspective, the generated class is the same except the SCHEMA field may have different schema strings.
   * <p/>
   * <p/>
   * An option to retain the schema differences is to emit a different class for each different schema. The generator could emit XArray and XRefArray. However, this would lead to a proliferation of
   * generated classes for different array or maps of typerefs of the same type. This is not ideal as a common pattern is to use typeref to differentiate different uses of string.
   * <p/>
   * <p/>
   * To avoid a proliferation of classes and maintain backwards compatibility, the generator will always emit map and array whose values and items types are dereferenced to the native type or to the
   * first typeref with a custom Java class binding.
   *
   * @param customInfo provides the first {@link CustomInfoSpec} of an array's items or a map's values.
   * @param schema     provides the {@link DataSchema} of an array's items or a map's values.
   *
   * @return the {@link DataSchema} for the array items or map values of the generated class.
   */
  private static DataSchema schemaForArrayItemsOrMapValues(CustomInfoSpec customInfo, DataSchema schema)
  {
    return customInfo != null ? customInfo.getCustomSchema() : schema.getDereferencedDataSchema();
  }

  private static void overrideCopierMethod(JDefinedClass templateClass, String methodName, Map<String, JVar> fields,
      boolean resetFields,
      JClass changeListenerClass)
  {
    final JMethod copierMethod = templateClass.method(JMod.PUBLIC, templateClass, methodName);
    copierMethod.annotate(Override.class);
    copierMethod._throws(CloneNotSupportedException.class);
    JVar copyVar = copierMethod.body().decl(templateClass, "__" + methodName, JExpr.cast(templateClass, JExpr._super().invoke(methodName)));

    if (!fields.isEmpty())
    {
      if (resetFields)
      {
        fields.values().forEach(var -> {
          copierMethod.body().assign(copyVar.ref(var), JExpr._null());
        });
      }

      copierMethod.body().assign(copyVar.ref("__changeListener"), JExpr._new(changeListenerClass).arg(copyVar));
      copierMethod.body().add(copyVar.invoke("addChangeListener").arg(copyVar.ref("__changeListener")));
    }

    copierMethod.body()._return(copyVar);
  }

  private static void setDeprecatedAnnotationAndJavadoc(DataSchema schema, JDefinedClass schemaClass)
  {
    setDeprecatedAnnotationAndJavadoc(schema.getProperties().get(DEPRECATED_KEY), schemaClass, schemaClass);
  }

  private static void setDeprecatedAnnotationAndJavadoc(JMethod method, RecordDataSchema.Field field)
  {
    setDeprecatedAnnotationAndJavadoc(field.getProperties().get(DEPRECATED_KEY), method, method);
  }

  private static void setDeprecatedAnnotationAndJavadoc(EnumDataSchema enumSchema, String symbol, JEnumConstant constant)
  {
    final Object deprecatedSymbolsProp = enumSchema.getProperties().get(DataSchemaConstants.DEPRECATED_SYMBOLS_KEY);
    if (deprecatedSymbolsProp instanceof DataMap)
    {
      final DataMap deprecatedSymbols = (DataMap) deprecatedSymbolsProp;

      final Object deprecatedProp = deprecatedSymbols.get(symbol);
      setDeprecatedAnnotationAndJavadoc(deprecatedProp, constant, constant);
    }
  }

  private static void setDeprecatedAnnotationAndJavadoc(Object deprecatedProp, JAnnotatable annotatable, JDocCommentable commentable)
  {
    if (Boolean.TRUE.equals(deprecatedProp) && annotatable != null)
    {
      annotatable.annotate(Deprecated.class);
    }
    else if (deprecatedProp instanceof String)
    {
      if (commentable != null)
      {
        final String deprecatedReason = (String) deprecatedProp;
        commentable.javadoc().addDeprecated().append(deprecatedReason);
      }
      if (annotatable != null)
      {
        annotatable.annotate(Deprecated.class);
      }
    }
  }

  private static int getJModValue(Set<ModifierSpec> modifiers)
  {
    try
    {
      int value = 0;
      for (ModifierSpec mod : modifiers)
      {
        value |= JMod.class.getDeclaredField(mod.name()).getInt(null);
      }
      return value;
    }
    catch (NoSuchFieldException e)
    {
      throw new RuntimeException(e);
    }
    catch (IllegalAccessException e)
    {
      throw new RuntimeException(e);
    }
  }

  private static void addAccessorDoc(JClass clazz, JMethod method, RecordDataSchema.Field field, String prefix)
  {
    method.javadoc().append(prefix + " for " + field.getName());
    method.javadoc().addXdoclet("see " + clazz.name() + ".Fields#" + escapeReserved(field.getName()));
  }

  private JDefinedClass defineClass(ClassTemplateSpec classTemplateSpec)
      throws JClassAlreadyExistsException
  {
    JDefinedClass result = _definedClasses.get(classTemplateSpec);
    if (result == null)
    {
      final int jmodValue = getJModValue(classTemplateSpec.getModifiers());
      final JClassContainer container;
      if (classTemplateSpec.getEnclosingClass() == null)
      {
        container = getPackage(classTemplateSpec.getPackage());
      }
      else
      {
        container = defineClass(classTemplateSpec.getEnclosingClass());
      }

      if (classTemplateSpec instanceof ArrayTemplateSpec ||
          classTemplateSpec instanceof FixedTemplateSpec ||
          classTemplateSpec instanceof MapTemplateSpec ||
          classTemplateSpec instanceof RecordTemplateSpec ||
          classTemplateSpec instanceof TyperefTemplateSpec ||
          classTemplateSpec instanceof UnionTemplateSpec)
      {
        result = container._class(jmodValue, escapeReserved(classTemplateSpec.getClassName()));
      }
      else if (classTemplateSpec instanceof EnumTemplateSpec)
      {
        result = container._class(jmodValue, escapeReserved(classTemplateSpec.getClassName()), ClassType.ENUM);
      }
      else
      {
        throw new RuntimeException();
      }

      _definedClasses.put(classTemplateSpec, result);
    }

    return result;
  }

  protected void generateArray(JDefinedClass arrayClass, ArrayTemplateSpec arrayDataTemplateSpec)
      throws JClassAlreadyExistsException
  {
    final JClass itemJClass = generate(arrayDataTemplateSpec.getItemClass());
    final JClass dataJClass = generate(arrayDataTemplateSpec.getItemDataClass());

    final boolean isDirect = CodeUtil.isDirectType(arrayDataTemplateSpec.getSchema().getItems());
    if (isDirect)
    {
      arrayClass._extends(_directArrayBaseClass.narrow(itemJClass));
    }
    else
    {
      extendWrappingArrayBaseClass(itemJClass, arrayClass);
    }

    /** see {@link #schemaForArrayItemsOrMapValues} */
    final DataSchema bareSchema = new ArrayDataSchema(schemaForArrayItemsOrMapValues(arrayDataTemplateSpec.getCustomInfo(), arrayDataTemplateSpec.getSchema().getItems()));
    final JVar schemaField = generateSchemaField(arrayClass, bareSchema, arrayDataTemplateSpec.getSourceFileFormat());

    generateConstructorWithNoArg(arrayClass, _dataListClass);
    generateConstructorWithInitialCapacity(arrayClass, _dataListClass);
    generateConstructorWithCollection(arrayClass, itemJClass);
    generateConstructorWithArg(arrayClass, schemaField, _dataListClass, itemJClass, dataJClass);
    generateConstructorWithVarArgs(arrayClass, itemJClass);

    if (_pathSpecMethods)
    {
      generatePathSpecMethodsForCollection(arrayClass, arrayDataTemplateSpec.getSchema(), itemJClass, "items");
    }

    generateCustomClassInitialization(arrayClass, arrayDataTemplateSpec.getCustomInfo());

    if (_copierMethods)
    {
      generateCopierMethods(arrayClass, Collections.emptyMap(), null);
    }

    // Generate coercer overrides
    generateCoercerOverrides(arrayClass,
        arrayDataTemplateSpec.getItemClass(),
        arrayDataTemplateSpec.getSchema().getItems(),
        arrayDataTemplateSpec.getCustomInfo(),
        false);
  }

  protected void extendWrappingArrayBaseClass(JClass itemJClass, JDefinedClass arrayClass)
  {
    arrayClass._extends(_wrappingArrayBaseClass.narrow(itemJClass));
  }

  protected void generateEnum(JDefinedClass enumClass, EnumTemplateSpec enumSpec)
  {
    enumClass.javadoc().append(enumSpec.getSchema().getDoc());

    setDeprecatedAnnotationAndJavadoc(enumSpec.getSchema(), enumClass);

    generateSchemaField(enumClass, enumSpec.getSchema(), enumSpec.getSourceFileFormat());

    for (String value : enumSpec.getSchema().getSymbols())
    {
      if (isReserved(value))
      {
        throw new IllegalArgumentException("Enum contains Java reserved symbol: " + value + " schema: " + enumSpec.getSchema());
      }

      final JEnumConstant enumConstant = enumClass.enumConstant(value);

      final String enumConstantDoc = enumSpec.getSchema().getSymbolDocs().get(value);

      if (enumConstantDoc != null)
      {
        enumConstant.javadoc().append(enumConstantDoc);
      }

      setDeprecatedAnnotationAndJavadoc(enumSpec.getSchema(), value, enumConstant);
    }
    enumClass.enumConstant(DataTemplateUtil.UNKNOWN_ENUM);
  }

  protected void generateFixed(JDefinedClass fixedClass, FixedTemplateSpec fixedSpec)
  {
    fixedClass.javadoc().append(fixedSpec.getSchema().getDoc());

    setDeprecatedAnnotationAndJavadoc(fixedSpec.getSchema(), fixedClass);

    fixedClass._extends(FixedTemplate.class);

    final JVar schemaField = generateSchemaField(fixedClass, fixedSpec.getSchema(), fixedSpec.getSourceFileFormat());

    final JMethod bytesConstructor = fixedClass.constructor(JMod.PUBLIC);
    final JVar param = bytesConstructor.param(ByteString.class, "value");
    bytesConstructor.body().invoke(SUPER).arg(param).arg(schemaField);

    generateConstructorWithObjectArg(fixedClass, schemaField, null);

    if (_copierMethods)
    {
      generateCopierMethods(fixedClass, Collections.emptyMap(), null);
    }
  }

  protected void generateMap(JDefinedClass mapClass, MapTemplateSpec mapSpec)
      throws JClassAlreadyExistsException
  {
    final JClass valueJClass = generate(mapSpec.getValueClass());
    final JClass dataJClass = generate(mapSpec.getValueDataClass());

    final boolean isDirect = CodeUtil.isDirectType(mapSpec.getSchema().getValues());
    if (isDirect)
    {
      mapClass._extends(_directMapBaseClass.narrow(valueJClass));
    }
    else
    {
      extendWrappingMapBaseClass(valueJClass, mapClass);
    }

    final DataSchema bareSchema = new MapDataSchema(schemaForArrayItemsOrMapValues(mapSpec.getCustomInfo(), mapSpec.getSchema().getValues()));
    final JVar schemaField = generateSchemaField(mapClass, bareSchema, mapSpec.getSourceFileFormat());

    generateConstructorWithNoArg(mapClass, _dataMapClass);
    generateConstructorWithInitialCapacity(mapClass, _dataMapClass);
    generateConstructorWithInitialCapacityAndLoadFactor(mapClass);
    generateConstructorWithMap(mapClass, valueJClass);
    generateConstructorWithArg(mapClass, schemaField, _dataMapClass, valueJClass, dataJClass);

    if (_pathSpecMethods)
    {
      generatePathSpecMethodsForCollection(mapClass, mapSpec.getSchema(), valueJClass, "values");
    }

    generateCustomClassInitialization(mapClass, mapSpec.getCustomInfo());

    if (_copierMethods)
    {
      generateCopierMethods(mapClass, Collections.emptyMap(), null);
    }

    // Generate coercer overrides
    generateCoercerOverrides(mapClass,
        mapSpec.getValueClass(),
        mapSpec.getSchema().getValues(),
        mapSpec.getCustomInfo(),
        true);
  }

  protected void extendWrappingMapBaseClass(JClass valueJClass, JDefinedClass mapClass)
  {
    mapClass._extends(_wrappingMapBaseClass.narrow(valueJClass));
  }

  private JClass generatePrimitive(PrimitiveTemplateSpec primitiveSpec)
  {
    switch (primitiveSpec.getSchema().getType())
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

      default:
        throw new RuntimeException("Not supported primitive: " + primitiveSpec);
    }
  }

  protected void generateRecord(JDefinedClass templateClass, RecordTemplateSpec recordSpec)
      throws JClassAlreadyExistsException
  {
    templateClass.javadoc().append(recordSpec.getSchema().getDoc());

    setDeprecatedAnnotationAndJavadoc(recordSpec.getSchema(), templateClass);

    extendRecordBaseClass(templateClass);

    if (_pathSpecMethods)
    {
      generatePathSpecMethodsForRecord(recordSpec.getFields(), templateClass);
    }

    final JFieldVar schemaFieldVar = generateSchemaField(templateClass, recordSpec.getSchema(), recordSpec.getSourceFileFormat());

    // Generate instance vars
    Map<String, JVar> fieldVarMap = new HashMap<>();
    for (RecordTemplateSpec.Field field : recordSpec.getFields())
    {
      final String fieldName = field.getSchemaField().getName();
      final JVar fieldVar =
          templateClass.field(JMod.PRIVATE, generate(field.getType()), "_" + fieldName + "Field", JExpr._null());
      fieldVarMap.put(fieldName, fieldVar);
    }

    final JVar changeListenerVar;
    final JClass changeListenerClass;
    // Generate a change listener if there are any fields.
    if (!fieldVarMap.isEmpty())
    {
      changeListenerClass = generateChangeListener(templateClass, fieldVarMap);
      changeListenerVar = templateClass.field(JMod.PRIVATE, changeListenerClass, "__changeListener",
          JExpr._new(changeListenerClass).arg(JExpr._this()));
    }
    else
    {
      changeListenerClass = null;
      changeListenerVar = null;
    }
    generateDataMapConstructor(templateClass, schemaFieldVar, recordSpec.getFields().size(), recordSpec.getWrappedFields().size(), changeListenerVar);
    generateConstructorWithArg(templateClass, schemaFieldVar, _dataMapClass, changeListenerVar);

    recordSpec.getFields().stream()
        .map(RecordTemplateSpec.Field::getCustomInfo)
        .distinct()
        .forEach(customInfo -> generateCustomClassInitialization(templateClass, customInfo));

    // Generate accessors
    for (RecordTemplateSpec.Field field : recordSpec.getFields())
    {
      final String fieldName = field.getSchemaField().getName();
      generateRecordFieldAccessors(templateClass, field, generate(field.getType()), schemaFieldVar,
          fieldVarMap.get(fieldName));
    }

    if (_copierMethods)
    {
      generateCopierMethods(templateClass, fieldVarMap, changeListenerClass);
    }
  }

  /**
   * Generates a constructor with no arguments for a DataTemplate type. The constructor calls the super class
   * constructor that accepts a new instance of "DataMap" type (provided by _dataMapClass) and the SCHEMA.
   * @param cls DataTemplate class being constructed.
   * @param schemaField SCHEMA field to use for initialization.
   * @param initialDataMapSize Initial size for the DataMap, applied only if the capacity derived from this is smaller
   *                           than {@link #DEFAULT_DATAMAP_INITIAL_CAPACITY}.
   * @param initialCacheSize Initial size for the cache, applied only if capacity derived from this is smaller than
   *                         {@link #DEFAULT_DATAMAP_INITIAL_CAPACITY}
   * @param changeListenerVar The map change listener variable if any.
   */
  private void generateDataMapConstructor(JDefinedClass cls, JVar schemaField, int initialDataMapSize, int initialCacheSize,
      JVar changeListenerVar)
  {
    final JMethod noArgConstructor = cls.constructor(JMod.PUBLIC);
    JInvocation superConstructorArg = JExpr._new(_dataMapClass);
    // Compute the DataMap initial capacity based on the load factor of 0.75. Use lower capacity if possible.
    int initialDataMapCapacity = DataMapBuilder.getOptimumHashMapCapacityFromSize(initialDataMapSize);
    if (initialDataMapCapacity < DEFAULT_DATAMAP_INITIAL_CAPACITY)
    {
      superConstructorArg.arg(JExpr.lit(initialDataMapCapacity)); // Initial capacity
      superConstructorArg.arg(JExpr.lit(0.75f));  // Load factor.
    }

    // Compute the cache initial capacity based on the load factor of 0.75. Use lower capacity if possible.
    int initialCacheCapacity = DataMapBuilder.getOptimumHashMapCapacityFromSize(initialCacheSize);

    // If the cache size is positive and the capacity is less than the default data map initial capacity aka default
    // HashMap capacity, then explicitly pass in the cache capacity param. Else don't pass it in, so that the default
    // cache capacity gets used.
    if (initialCacheSize > 0 && initialCacheCapacity < DEFAULT_DATAMAP_INITIAL_CAPACITY)
    {
      noArgConstructor.body().invoke(SUPER).arg(superConstructorArg).arg(schemaField).arg(JExpr.lit(initialCacheCapacity));
    }
    else
    {
      noArgConstructor.body().invoke(SUPER).arg(superConstructorArg).arg(schemaField);
    }

    if (changeListenerVar != null)
    {
      addChangeListenerRegistration(noArgConstructor, changeListenerVar);
    }
  }

  protected void extendRecordBaseClass(JDefinedClass templateClass)
  {
    templateClass._extends(_recordBaseClass);
  }

  private void generatePathSpecMethodsForRecord(List<RecordTemplateSpec.Field> fieldSpecs, JDefinedClass templateClass)
      throws JClassAlreadyExistsException
  {
    final JDefinedClass fieldsNestedClass = generatePathSpecNestedClass(templateClass);

    for (RecordTemplateSpec.Field field : fieldSpecs)
    {
      JClass fieldsRefType = _pathSpecClass;
      if (hasNestedFields(field.getSchemaField().getType()))
      {
        final JClass fieldType = generate(field.getType());
        fieldsRefType = getCodeModel().ref(fieldType.fullName() + ".Fields");
      }

      final JMethod constantField = fieldsNestedClass.method(JMod.PUBLIC, fieldsRefType, escapeReserved(field.getSchemaField().getName()));
      constantField.body()._return(JExpr._new(fieldsRefType).arg(JExpr.invoke("getPathComponents")).arg(field.getSchemaField().getName()));
      if (!field.getSchemaField().getDoc().isEmpty())
      {
        constantField.javadoc().append(field.getSchemaField().getDoc());
      }
      setDeprecatedAnnotationAndJavadoc(constantField, field.getSchemaField());

      // For array types, add another method to get PathSpec with a range specified
      if (isArrayType(field.getSchemaField().getType()))
      {
        final JMethod pathSpecRangeMethod = fieldsNestedClass.method(JMod.PUBLIC, _pathSpecClass, escapeReserved(field.getSchemaField().getName()));
        final JVar arrayPathSpec = pathSpecRangeMethod.body()
            .decl(_pathSpecClass, "arrayPathSpec",
                JExpr._new(_pathSpecClass).arg(JExpr.invoke("getPathComponents")).arg(field.getSchemaField().getName()));
        JClass integerClass = generate(PrimitiveTemplateSpec.getInstance(DataSchema.Type.INT));
        JVar start = pathSpecRangeMethod.param(integerClass, "start");
        pathSpecRangeMethod.body()._if(start.ne(JExpr._null())).
            _then().invoke(arrayPathSpec, "setAttribute").arg(PathSpec.ATTR_ARRAY_START).arg(start);
        JVar count = pathSpecRangeMethod.param(integerClass, "count");
        pathSpecRangeMethod.body()._if(count.ne(JExpr._null()))
            ._then().invoke(arrayPathSpec, "setAttribute").arg(PathSpec.ATTR_ARRAY_COUNT).arg(count);
        pathSpecRangeMethod.body()._return(arrayPathSpec);

        if (!field.getSchemaField().getDoc().isEmpty())
        {
          pathSpecRangeMethod.javadoc().append(field.getSchemaField().getDoc());
        }
        setDeprecatedAnnotationAndJavadoc(pathSpecRangeMethod, field.getSchemaField());
      }
    }

    final JVar staticFields = templateClass.field(JMod.PRIVATE | JMod.STATIC | JMod.FINAL, fieldsNestedClass, "_fields").init(JExpr._new(fieldsNestedClass));
    final JMethod staticFieldsAccessor = templateClass.method(JMod.PUBLIC | JMod.STATIC, fieldsNestedClass, "fields");
    staticFieldsAccessor.body()._return(staticFields);
  }

  private void generateRecordFieldAccessors(JDefinedClass templateClass, RecordTemplateSpec.Field field, JClass type, JVar schemaFieldVar,
      JVar fieldVar)
  {
    final RecordDataSchema.Field schemaField = field.getSchemaField();
    final DataSchema fieldSchema = schemaField.getType();
    final String capitalizedName = CodeUtil.capitalize(schemaField.getName());

    final JExpression mapRef = JExpr._super().ref("_map");
    final JExpression fieldNameExpr = JExpr.lit(schemaField.getName());
    final String fieldFieldName = "FIELD_" + capitalizedName;
    final JFieldVar fieldField = templateClass.field(JMod.PRIVATE | JMod.STATIC | JMod.FINAL, RecordDataSchema.Field.class, fieldFieldName);
    fieldField.init(schemaFieldVar.invoke("getField").arg(schemaField.getName()));

    // Generate default field if applicable
    final String defaultFieldName = "DEFAULT_" + capitalizedName;
    final JFieldVar defaultField;
    if (field.getSchemaField().getDefault() != null)
    {
      defaultField = templateClass.field(JMod.PRIVATE | JMod.STATIC | JMod.FINAL, type, defaultFieldName);

      templateClass.init().assign(defaultField, getCoerceOutputExpression(
          fieldField.invoke("getDefault"), schemaField.getType(), type, field.getCustomInfo()));
    }
    else
    {
      defaultField = null;
    }

    // Generate has method.
    final JMethod has = templateClass.method(JMod.PUBLIC, getCodeModel().BOOLEAN, "has" + capitalizedName);
    addAccessorDoc(templateClass, has, schemaField, "Existence checker");
    setDeprecatedAnnotationAndJavadoc(has, schemaField);
    final JBlock hasBody = has.body();
    final JBlock hasInstanceVarBody = hasBody._if(fieldVar.ne(JExpr._null()))._then();
    hasInstanceVarBody._return(JExpr.lit(true));
    hasBody._return(mapRef.invoke("containsKey").arg(fieldNameExpr));

    if (_recordFieldRemove)
    {
      // Generate remove method.
      final String removeName = "remove" + capitalizedName;
      final JMethod remove = templateClass.method(JMod.PUBLIC, getCodeModel().VOID, removeName);
      addAccessorDoc(templateClass, remove, schemaField, "Remover");
      setDeprecatedAnnotationAndJavadoc(remove, schemaField);
      final JBlock removeBody = remove.body();
      removeBody.add(mapRef.invoke("remove").arg(fieldNameExpr));
    }

    final String getterName = JavaCodeUtil.getGetterName(getCodeModel(), type, capitalizedName);

    if (_recordFieldAccessorWithMode)
    {
      // Getter method with mode.
      final JMethod getterWithMode = templateClass.method(JMod.PUBLIC, type, getterName);
      addAccessorDoc(templateClass, getterWithMode, schemaField, "Getter");
      setDeprecatedAnnotationAndJavadoc(getterWithMode, schemaField);
      JVar modeParam = getterWithMode.param(_getModeClass, "mode");
      final JBlock getterWithModeBody = getterWithMode.body();

      // If it is an optional field with no default, just call out to the getter without mode.
      if (field.getSchemaField().getOptional() && defaultField == null)
      {
        getterWithModeBody._return(JExpr.invoke(getterName));
      }
      else
      {
        JSwitch modeSwitch = getterWithModeBody._switch(modeParam);
        JCase strictCase = modeSwitch._case(JExpr.ref("STRICT"));
        // If there is no default defined, call the getter without mode, else fall through to default.
        if (defaultField == null)
        {
          strictCase.body()._return(JExpr.invoke(getterName));
        }
        JCase defaultCase = modeSwitch._case(JExpr.ref("DEFAULT"));
        if (defaultField != null)
        {
          // If there is a default, then default is the same as strict, else we should fall through to null.
          defaultCase.body()._return(JExpr.invoke(getterName));
        }

        JCase nullCase = modeSwitch._case(JExpr.ref("NULL"));
        JConditional nullCaseConditional = nullCase.body()._if(fieldVar.ne(JExpr._null()));
        nullCaseConditional._then()._return(fieldVar);
        JBlock nullCaseConditionalElse = nullCaseConditional._else();
        JVar rawValueVar = nullCaseConditionalElse.decl(
            _objectClass, "__rawValue", mapRef.invoke("get").arg(fieldNameExpr));
        nullCaseConditionalElse.assign(fieldVar,
            getCoerceOutputExpression(rawValueVar, fieldSchema, type, field.getCustomInfo()));
        nullCaseConditionalElse._return(fieldVar);

        getterWithModeBody._throw(JExpr._new(getCodeModel().ref(IllegalStateException.class)).arg(JExpr.lit("Unknown mode ").plus(modeParam)));
      }
    }

    // Getter method without mode.
    final JMethod getterWithoutMode = templateClass.method(JMod.PUBLIC, type, getterName);
    addAccessorDoc(templateClass, getterWithoutMode, schemaField, "Getter");
    setDeprecatedAnnotationAndJavadoc(getterWithoutMode, schemaField);
    JCommentPart returnComment = getterWithoutMode.javadoc().addReturn();
    if (schemaField.getOptional())
    {
      getterWithoutMode.annotate(Nullable.class);
      returnComment.add("Optional field. Always check for null.");
    }
    else
    {
      getterWithoutMode.annotate(Nonnull.class);
      returnComment.add("Required field. Could be null for partial record.");
    }
    final JBlock getterWithoutModeBody = getterWithoutMode.body();
    JConditional getterWithoutModeBodyConditional = getterWithoutModeBody._if(fieldVar.ne(JExpr._null()));
    getterWithoutModeBodyConditional._then()._return(fieldVar);
    JBlock getterWithoutModeBodyConditionalElse = getterWithoutModeBodyConditional._else();
    JVar rawValueVar = getterWithoutModeBodyConditionalElse.decl(
        _objectClass, "__rawValue", mapRef.invoke("get").arg(fieldNameExpr));
    if (schemaField.getDefault() != null)
    {
      getterWithoutModeBodyConditionalElse._if(rawValueVar.eq(JExpr._null()))._then()._return(defaultField);
    }
    else if (!schemaField.getOptional())
    {
      getterWithoutModeBodyConditionalElse._if(rawValueVar.eq(JExpr._null()))._then()._throw(
          JExpr._new(getCodeModel().ref(RequiredFieldNotPresentException.class)).arg(fieldNameExpr));
    }
    getterWithoutModeBodyConditionalElse.assign(fieldVar,
        getCoerceOutputExpression(rawValueVar, fieldSchema, type, field.getCustomInfo()));
    getterWithoutModeBodyConditionalElse._return(fieldVar);

    final String setterName = "set" + capitalizedName;

    if (_recordFieldAccessorWithMode)
    {
      // Setter method with mode
      final JMethod setterWithMode = templateClass.method(JMod.PUBLIC, templateClass, setterName);
      addAccessorDoc(templateClass, setterWithMode, schemaField, "Setter");
      setDeprecatedAnnotationAndJavadoc(setterWithMode, schemaField);
      JVar param = setterWithMode.param(type, "value");
      JVar modeParam = setterWithMode.param(_setModeClass, "mode");
      JSwitch modeSwitch = setterWithMode.body()._switch(modeParam);
      JCase disallowNullCase = modeSwitch._case(JExpr.ref("DISALLOW_NULL"));
      disallowNullCase.body()._return(JExpr.invoke(setterName).arg(param));

      // Generate remove optional if null, only for required fields. Optional fields will fall through to
      // remove if null, which is the same behavior for them.
      JCase removeOptionalIfNullCase = modeSwitch._case(JExpr.ref("REMOVE_OPTIONAL_IF_NULL"));
      if (!schemaField.getOptional()) {
        JConditional paramIsNull = removeOptionalIfNullCase.body()._if(param.eq(JExpr._null()));
        paramIsNull._then()._throw(JExpr._new(getCodeModel().ref(IllegalArgumentException.class))
            .arg(JExpr.lit("Cannot remove mandatory field " + schemaField.getName() + " of " + templateClass.fullName())));
        paramIsNull._else()
            .add(_checkedUtilClass.staticInvoke("putWithoutChecking").arg(mapRef).arg(fieldNameExpr)
                .arg(getCoerceInputExpression(param, fieldSchema, field.getCustomInfo())));
        paramIsNull._else().assign(fieldVar, param);
        removeOptionalIfNullCase.body()._break();
      }

      JCase removeIfNullCase = modeSwitch._case(JExpr.ref("REMOVE_IF_NULL"));
      JConditional paramIsNull = removeIfNullCase.body()._if(param.eq(JExpr._null()));
      paramIsNull._then().invoke("remove" + capitalizedName);
      paramIsNull._else()
          .add(_checkedUtilClass.staticInvoke("putWithoutChecking").arg(mapRef).arg(fieldNameExpr)
              .arg(getCoerceInputExpression(param, fieldSchema, field.getCustomInfo())));
      paramIsNull._else().assign(fieldVar, param);
      removeIfNullCase.body()._break();

      JCase ignoreNullCase = modeSwitch._case(JExpr.ref("IGNORE_NULL"));
      JConditional paramIsNotNull = ignoreNullCase.body()._if(param.ne(JExpr._null()));
      paramIsNotNull._then()
          .add(_checkedUtilClass.staticInvoke("putWithoutChecking").arg(mapRef).arg(fieldNameExpr)
              .arg(getCoerceInputExpression(param, fieldSchema, field.getCustomInfo())));
      paramIsNotNull._then().assign(fieldVar, param);
      ignoreNullCase.body()._break();

      setterWithMode.body()._return(JExpr._this());
    }

    // Setter method without mode
    final JMethod setter = templateClass.method(JMod.PUBLIC, templateClass, setterName);
    addAccessorDoc(templateClass, setter, schemaField, "Setter");
    setDeprecatedAnnotationAndJavadoc(setter, schemaField);
    JVar param = setter.param(type, "value");
    param.annotate(Nonnull.class);
    JCommentPart paramDoc = setter.javadoc().addParam(param);
    paramDoc.add("Must not be null. For more control, use setters with mode instead.");
    JConditional paramIsNull = setter.body()._if(param.eq(JExpr._null()));
    paramIsNull._then()._throw(JExpr._new(getCodeModel().ref(NullPointerException.class))
        .arg(JExpr.lit("Cannot set field " + schemaField.getName() + " of " + templateClass.fullName() + " to null")));
    paramIsNull._else()
        .add(_checkedUtilClass.staticInvoke("putWithoutChecking").arg(mapRef).arg(fieldNameExpr)
            .arg(getCoerceInputExpression(param, fieldSchema, field.getCustomInfo())));
    paramIsNull._else().assign(fieldVar, param);
    setter.body()._return(JExpr._this());

    // Setter method without mode for unboxified type
    if (!type.unboxify().equals(type))
    {
      final JMethod unboxifySetter = templateClass.method(JMod.PUBLIC, templateClass, setterName);
      addAccessorDoc(templateClass, unboxifySetter, schemaField, "Setter");
      setDeprecatedAnnotationAndJavadoc(unboxifySetter, schemaField);
      param = unboxifySetter.param(type.unboxify(), "value");
      unboxifySetter.body().add(_checkedUtilClass.staticInvoke("putWithoutChecking").arg(mapRef).arg(fieldNameExpr)
              .arg(getCoerceInputExpression(param, fieldSchema, field.getCustomInfo())));
      unboxifySetter.body().assign(fieldVar, param);
      unboxifySetter.body()._return(JExpr._this());
    }
  }

  protected void generateTyperef(JDefinedClass typerefClass, TyperefTemplateSpec typerefSpec)
  {
    typerefClass.javadoc().append(typerefSpec.getSchema().getDoc());

    setDeprecatedAnnotationAndJavadoc(typerefSpec.getSchema(), typerefClass);

    typerefClass._extends(TyperefInfo.class);

    final JVar schemaField = generateSchemaField(typerefClass, typerefSpec.getSchema(), typerefSpec.getSourceFileFormat());

    generateCustomClassInitialization(typerefClass, typerefSpec.getCustomInfo());

    final JMethod constructor = typerefClass.constructor(JMod.PUBLIC);
    constructor.body().invoke(SUPER).arg(schemaField);
  }

  protected void generateUnion(JDefinedClass unionClass, UnionTemplateSpec unionSpec)
      throws JClassAlreadyExistsException
  {
    extendUnionBaseClass(unionClass);

    final JVar schemaField = generateSchemaField(unionClass, unionSpec.getSchema(), unionSpec.getSourceFileFormat());

    // Generate instance vars for members.
    Map<String, JVar> memberVarMap = new HashMap<>();
    for (UnionTemplateSpec.Member member : unionSpec.getMembers())
    {
      if (member.getClassTemplateSpec() != null)
      {
        final String memberName = CodeUtil.uncapitalize(CodeUtil.getUnionMemberName(member));
        final JVar memberVar =
            unionClass.field(JMod.PRIVATE, generate(member.getClassTemplateSpec()), "_" + memberName + "Member", JExpr._null());
        memberVarMap.put(member.getUnionMemberKey(), memberVar);
      }
    }

    final JClass changeListenerClass;
    final JVar changeListenerVar;

    // Generate change listener if there are any members.
    if (!memberVarMap.isEmpty())
    {
      changeListenerClass = generateChangeListener(unionClass, memberVarMap);
      changeListenerVar = unionClass.field(JMod.PRIVATE, changeListenerClass, "__changeListener",
          JExpr._new(changeListenerClass).arg(JExpr._this()));
    }
    else
    {
      changeListenerClass = null;
      changeListenerVar = null;
    }

    // Default union datamap size to 1 (last arg) as union can have at-most one element.
    // We don't need cache for unions, so pass in -1 for cache size to ignore size param.
    generateDataMapConstructor(unionClass, schemaField, 1, -1, changeListenerVar);
    generateConstructorWithObjectArg(unionClass, schemaField, changeListenerVar);

    for (UnionTemplateSpec.Member member : unionSpec.getMembers())
    {
      if (member.getClassTemplateSpec() != null)
      {
        generateUnionMemberAccessors(unionClass, member, generate(member.getClassTemplateSpec()),
            generate(member.getDataClass()), schemaField, memberVarMap.get(member.getUnionMemberKey()));
      }
    }

    unionSpec.getMembers().stream()
        .map(UnionTemplateSpec.Member::getCustomInfo)
        .distinct()
        .forEach(customInfo -> generateCustomClassInitialization(unionClass, customInfo));

    if (_pathSpecMethods)
    {
      generatePathSpecMethodsForUnion(unionSpec, unionClass);
    }

    if (_copierMethods)
    {
      generateCopierMethods(unionClass, memberVarMap, changeListenerClass);
    }

    if (unionSpec.getTyperefClass() != null)
    {
      final TyperefTemplateSpec typerefClassSpec = unionSpec.getTyperefClass();
      final JDefinedClass typerefInfoClass = unionClass._class(getJModValue(typerefClassSpec.getModifiers()), escapeReserved(typerefClassSpec.getClassName()));
      generateTyperef(typerefInfoClass, typerefClassSpec);

      final JFieldVar typerefInfoField = unionClass.field(JMod.PRIVATE | JMod.STATIC | JMod.FINAL, TyperefInfo.class, DataTemplateUtil.TYPEREFINFO_FIELD_NAME);
      typerefInfoField.init(JExpr._new(typerefInfoClass));

      unionClass._implements(HasTyperefInfo.class);
      final JMethod typerefInfoMethod = unionClass.method(JMod.PUBLIC, TyperefInfo.class, "typerefInfo");
      typerefInfoMethod.body()._return(typerefInfoField);
    }
  }

  protected void extendUnionBaseClass(JDefinedClass unionClass)
  {
    unionClass._extends(_unionBaseClass);
  }

  private void generateUnionMemberAccessors(JDefinedClass unionClass, UnionTemplateSpec.Member member,
      JClass memberClass, JClass dataClass, JVar schemaField, JVar memberVar)
  {
    final DataSchema memberType = member.getSchema();
    final String memberKey = member.getUnionMemberKey();
    final String capitalizedName = CodeUtil.getUnionMemberName(member);
    final JExpression mapRef = JExpr._super().ref("_map");

    final String memberFieldName = "MEMBER_" + capitalizedName;
    final JFieldVar memberField = unionClass.field(JMod.PRIVATE | JMod.STATIC | JMod.FINAL, DataSchema.class, memberFieldName);
    memberField.init(schemaField.invoke("getTypeByMemberKey").arg(memberKey));
    final String setterName = "set" + capitalizedName;

    // Generate builder.

    final String builderMethodName = (member.getAlias() != null) ? "createWith" + capitalizedName : "create";
    final JMethod createMethod = unionClass.method(JMod.PUBLIC | JMod.STATIC, unionClass, builderMethodName);
    JVar param = createMethod.param(memberClass, "value");
    final JVar newUnionVar = createMethod.body().decl(unionClass, "newUnion", JExpr._new(unionClass));
    createMethod.body().invoke(newUnionVar, setterName).arg(param);
    createMethod.body()._return(newUnionVar);

    // Is method.

    final JMethod is = unionClass.method(JMod.PUBLIC, getCodeModel().BOOLEAN, "is" + capitalizedName);
    final JBlock isBody = is.body();
    JExpression res = JExpr.invoke("memberIs").arg(JExpr.lit(memberKey));
    isBody._return(res);

    // Getter method.

    final String getterName = "get" + capitalizedName;
    final JMethod getter = unionClass.method(JMod.PUBLIC, memberClass, getterName);
    final JBlock getterBody = getter.body();
    getterBody.invoke("checkNotNull");
    JBlock memberVarNonNullBlock = getterBody._if(memberVar.ne(JExpr._null()))._then();
    memberVarNonNullBlock._return(memberVar);
    JVar rawValueVar = getterBody.decl(_objectClass, "__rawValue", mapRef.invoke("get").arg(JExpr.lit(memberKey)));
    getterBody.assign(memberVar, getCoerceOutputExpression(rawValueVar, memberType, memberClass, member.getCustomInfo()));
    getterBody._return(memberVar);

    // Setter method.

    final JMethod setter = unionClass.method(JMod.PUBLIC, Void.TYPE, setterName);
    param = setter.param(memberClass, "value");
    final JBlock setterBody = setter.body();
    setterBody.invoke("checkNotNull");
    setterBody.add(mapRef.invoke("clear"));
    setterBody.assign(memberVar, param);
    setterBody.add(_checkedUtilClass.staticInvoke("putWithoutChecking").arg(mapRef).arg(JExpr.lit(memberKey))
        .arg(getCoerceInputExpression(param, memberType, member.getCustomInfo())));
  }

  private void generatePathSpecMethodsForUnion(UnionTemplateSpec unionSpec, JDefinedClass unionClass)
      throws JClassAlreadyExistsException
  {
    final JDefinedClass fieldsNestedClass = generatePathSpecNestedClass(unionClass);

    for (UnionTemplateSpec.Member member : unionSpec.getMembers())
    {
      JClass fieldsRefType = _pathSpecClass;
      if (hasNestedFields(member.getSchema()))
      {
        final JClass unionMemberClass = generate(member.getClassTemplateSpec());
        fieldsRefType = getCodeModel().ref(unionMemberClass.fullName() + ".Fields");
      }

      String memberKey = member.getUnionMemberKey();
      String methodName = CodeUtil.getUnionMemberName(member);
      final JMethod accessorMethod = fieldsNestedClass.method(JMod.PUBLIC, fieldsRefType, methodName);
      accessorMethod.body()._return(JExpr._new(fieldsRefType).arg(JExpr.invoke("getPathComponents")).arg(memberKey));
    }
  }

  private void populateClassContent(ClassTemplateSpec classTemplateSpec, JDefinedClass definedClass)
      throws JClassAlreadyExistsException
  {
    if (!_generatedClasses.containsKey(definedClass))
    {
      _generatedClasses.put(definedClass, classTemplateSpec);

      JavaCodeUtil.annotate(definedClass, "Data Template", classTemplateSpec.getLocation(), _rootPath);

      if (classTemplateSpec instanceof ArrayTemplateSpec)
      {
        generateArray(definedClass, (ArrayTemplateSpec) classTemplateSpec);
      }
      else if (classTemplateSpec instanceof EnumTemplateSpec)
      {
        generateEnum(definedClass, (EnumTemplateSpec) classTemplateSpec);
      }
      else if (classTemplateSpec instanceof FixedTemplateSpec)
      {
        generateFixed(definedClass, (FixedTemplateSpec) classTemplateSpec);
      }
      else if (classTemplateSpec instanceof MapTemplateSpec)
      {
        generateMap(definedClass, (MapTemplateSpec) classTemplateSpec);
      }
      else if (classTemplateSpec instanceof RecordTemplateSpec)
      {
        generateRecord(definedClass, (RecordTemplateSpec) classTemplateSpec);
      }
      else if (classTemplateSpec instanceof TyperefTemplateSpec)
      {
        generateTyperef(definedClass, (TyperefTemplateSpec) classTemplateSpec);
      }
      else if (classTemplateSpec instanceof UnionTemplateSpec)
      {
        generateUnion(definedClass, (UnionTemplateSpec) classTemplateSpec);
      }
      else
      {
        throw new RuntimeException();
      }
    }
  }

  private JFieldVar generateSchemaField(JDefinedClass templateClass, DataSchema schema, SchemaFormatType sourceFormatType)
  {
    // If format is indeterminable (e.g. from IDL), then use default format
    final SchemaFormatType schemaFormatType = Optional.ofNullable(sourceFormatType).orElse(DEFAULT_SCHEMA_FORMAT_TYPE);

    final JFieldRef schemaFormatTypeRef = _schemaFormatTypeClass.staticRef(schemaFormatType.name());
    final JFieldVar schemaField = templateClass.field(JMod.PRIVATE | JMod.STATIC | JMod.FINAL, schema.getClass(), DataTemplateUtil.SCHEMA_FIELD_NAME);

    // Compactly encode the schema text
    String schemaText;
    switch (schemaFormatType)
    {
      case PDSC:
        schemaText = SchemaToJsonEncoder.schemaToJson(schema, JsonBuilder.Pretty.COMPACT);
        break;
      case PDL:
        schemaText = SchemaToPdlEncoder.schemaToPdl(schema, SchemaToPdlEncoder.EncodingStyle.COMPACT);
        break;
      default:
        // This should never happen if all enum values are handled
        throw new IllegalStateException(String.format("Unrecognized schema format type '%s'", schemaFormatType));
    }

    // Generate the method invocation to parse the schema text
    final JInvocation parseSchemaInvocation = _dataTemplateUtilClass.staticInvoke("parseSchema")
        .arg(getSizeBoundStringLiteral(schemaText));

    // TODO: Eventually use new interface for all formats, postponing adoption for PDSC to avoid build failures.
    if (schemaFormatType != SchemaFormatType.PDSC)
    {
      parseSchemaInvocation.arg(schemaFormatTypeRef);
    }

    // Generate the schema field initialization
    schemaField.init(JExpr.cast(getCodeModel()._ref(schema.getClass()), parseSchemaInvocation));

    // Using "dataSchema" as method name since "schema" conflicts with RecordTemplate::schema and "getSchema" conflicts
    // with TyperefInfo::getSchema
    final JMethod staticFieldsAccessor = templateClass.method(JMod.PUBLIC | JMod.STATIC, schema.getClass(), "dataSchema");
    staticFieldsAccessor.body()._return(schemaField);

    return schemaField;
  }

  private void generatePathSpecMethodsForCollection(JDefinedClass templateClass, DataSchema schema, JClass childClass, String wildcardMethodName)
      throws JClassAlreadyExistsException
  {
    if (hasNestedFields(schema))
    {
      final JDefinedClass fieldsNestedClass = generatePathSpecNestedClass(templateClass);

      final JClass itemsFieldType = getCodeModel().ref(childClass.fullName() + ".Fields");

      final JMethod constantField = fieldsNestedClass.method(JMod.PUBLIC, itemsFieldType, wildcardMethodName);
      constantField.body()._return(JExpr._new(itemsFieldType).arg(JExpr.invoke("getPathComponents")).arg(_pathSpecClass.staticRef("WILDCARD")));
    }
  }

  private JDefinedClass generatePathSpecNestedClass(JDefinedClass templateClass)
      throws JClassAlreadyExistsException
  {
    final JDefinedClass fieldsNestedClass = templateClass._class(JMod.PUBLIC | JMod.STATIC, "Fields");
    fieldsNestedClass._extends(_pathSpecClass);

    final JMethod constructor = fieldsNestedClass.constructor(JMod.PUBLIC);
    final JClass listString = getCodeModel().ref(List.class).narrow(String.class);
    final JVar namespace = constructor.param(listString, "path");
    final JVar name = constructor.param(String.class, "name");
    constructor.body().invoke(SUPER).arg(namespace).arg(name);

    fieldsNestedClass.constructor(JMod.PUBLIC).body().invoke(SUPER);
    return fieldsNestedClass;
  }

  /**
   * @see com.linkedin.data.template.Custom#initializeCustomClass(Class)
   * @see com.linkedin.data.template.Custom#initializeCoercerClass(Class)
   */
  private void generateCustomClassInitialization(JDefinedClass templateClass, CustomInfoSpec customInfo)
  {
    if (customInfo != null)
    {
      // initialize custom class
      final String customClassFullName = customInfo.getCustomClass().getNamespace() + "." + customInfo.getCustomClass().getClassName();
      templateClass.init().add(_customClass.staticInvoke("initializeCustomClass").arg(getCodeModel().ref(customClassFullName).dotclass()));

      // initialize explicitly specified coercer class
      if (customInfo.getCoercerClass() != null)
      {
        final String coercerClassFullName = customInfo.getCoercerClass().getNamespace() + "." + customInfo.getCoercerClass().getClassName();
        templateClass.init().add(_customClass.staticInvoke("initializeCoercerClass").arg(getCodeModel().ref(coercerClassFullName).dotclass()));
      }
    }
  }

  protected void generateCoercerOverrides(JDefinedClass wrapperClass,
      ClassTemplateSpec itemSpec,
      DataSchema itemSchema,
      CustomInfoSpec customInfoSpec,
      boolean tolerateNullForCoerceOutput)
  {
    JClass valueType = generate(itemSpec);

    // Generate coerce input only for direct types. Wrapped types will just call data().
    if (CodeUtil.isDirectType(itemSchema))
    {
      JMethod coerceInput = wrapperClass.method(JMod.PROTECTED, _objectClass, "coerceInput");
      JVar inputParam = coerceInput.param(valueType, "object");
      coerceInput._throws(ClassCastException.class);
      coerceInput.annotate(Override.class);
      coerceInput.body().add(
          getCodeModel().directClass(ArgumentUtil.class.getCanonicalName()).staticInvoke("notNull").arg(inputParam).arg("object"));
      coerceInput.body()._return(getCoerceInputExpression(inputParam, itemSchema, customInfoSpec));
    }

    JMethod coerceOutput = wrapperClass.method(JMod.PROTECTED, valueType, "coerceOutput");
    JVar outputParam = coerceOutput.param(_objectClass, "object");
    coerceOutput._throws(TemplateOutputCastException.class);
    coerceOutput.annotate(Override.class);
    if (tolerateNullForCoerceOutput)
    {
      coerceOutput.body()._if(outputParam.eq(JExpr._null()))._then()._return(JExpr._null());
    }
    else
    {
      coerceOutput.body().directStatement("assert(object != null);");
    }
    coerceOutput.body()._return(getCoerceOutputExpression(outputParam, itemSchema, valueType, customInfoSpec));
  }

  private void generateConstructorWithInitialCapacity(JDefinedClass cls, JClass elementClass)
  {
    final JMethod argConstructor = cls.constructor(JMod.PUBLIC);
    final JVar initialCapacity = argConstructor.param(getCodeModel().INT, "initialCapacity");
    argConstructor.body().invoke(THIS).arg(JExpr._new(elementClass).arg(initialCapacity));
  }

  private void generateConstructorWithCollection(JDefinedClass cls, JClass elementClass)
  {
    final JMethod argConstructor = cls.constructor(JMod.PUBLIC);
    final JVar c = argConstructor.param(_collectionClass.narrow(elementClass), "c");
    argConstructor.body().invoke(THIS).arg(JExpr._new(_dataListClass).arg(c.invoke("size")));
    argConstructor.body().invoke("addAll").arg(c);
  }

  private void generateConstructorWithVarArgs(JDefinedClass cls, JClass elementClass)
  {
    final JMethod argConstructor = cls.constructor(JMod.PUBLIC);
    final JVar first = argConstructor.param(elementClass, "first");
    final JVar rest = argConstructor.varParam(elementClass, "rest");
    argConstructor.body().invoke(THIS).arg(JExpr._new(_dataListClass)
        .arg(rest.ref("length").plus(JExpr.lit(1))));
    argConstructor.body().invoke("add").arg(first);
    argConstructor.body().invoke("addAll").arg(_arraysClass.staticInvoke("asList").arg(rest));
  }

  private void generateConstructorWithInitialCapacityAndLoadFactor(JDefinedClass cls)
  {
    final JMethod argConstructor = cls.constructor(JMod.PUBLIC);
    final JVar initialCapacity = argConstructor.param(getCodeModel().INT, "initialCapacity");
    final JVar loadFactor = argConstructor.param(getCodeModel().FLOAT, "loadFactor");
    argConstructor.body().invoke(THIS).arg(JExpr._new(_dataMapClass).arg(initialCapacity).arg(loadFactor));
  }

  private void generateConstructorWithMap(JDefinedClass cls, JClass valueClass)
  {
    final JMethod argConstructor = cls.constructor(JMod.PUBLIC);
    final JVar m = argConstructor.param(_mapClass.narrow(_stringClass, valueClass), "m");
    argConstructor.body().invoke(THIS).arg(JExpr.invoke("newDataMapOfSize").arg(m.invoke("size")));
    argConstructor.body().invoke("putAll").arg(m);
  }

  private JClass generateChangeListener(JDefinedClass cls, Map<String, JVar> fieldMap) throws JClassAlreadyExistsException
  {
    final JClass changeListenerInterface = getCodeModel().ref(CheckedMap.ChangeListener.class);
    final JDefinedClass changeListenerClass = cls._class(JMod.PRIVATE | JMod.STATIC, "ChangeListener");
    changeListenerClass._implements(changeListenerInterface.narrow(String.class, Object.class));

    final JFieldVar objectRefVar = changeListenerClass.field(JMod.PRIVATE | JMod.FINAL, cls, "__objectRef");

    final JMethod constructor = changeListenerClass.constructor(JMod.PRIVATE);
    JVar refParam = constructor.param(cls, "reference");
    constructor.body().assign(objectRefVar, refParam);

    final JMethod method = changeListenerClass.method(JMod.PUBLIC, void.class, "onUnderlyingMapChanged");
    method.annotate(Override.class);
    final JVar keyParam = method.param(String.class, "key");
    method.param(_objectClass, "value");
    JSwitch keySwitch = method.body()._switch(keyParam);
    fieldMap.forEach((key, field) -> {
      JCase keyCase = keySwitch._case(JExpr.lit(key));
      keyCase.body().assign(objectRefVar.ref(field.name()), JExpr._null());
      keyCase.body()._break();
    });

    return changeListenerClass;
  }

  private JExpression getCoerceOutputExpression(JExpression rawExpr, DataSchema schema, JClass typeClass,
      CustomInfoSpec customInfoSpec)
  {
    if (CodeUtil.isDirectType(schema))
    {
      if (customInfoSpec == null)
      {
        switch (schema.getDereferencedType())
        {
          case INT:
            return _dataTemplateUtilClass.staticInvoke("coerceIntOutput").arg(rawExpr);
          case FLOAT:
            return _dataTemplateUtilClass.staticInvoke("coerceFloatOutput").arg(rawExpr);
          case LONG:
            return _dataTemplateUtilClass.staticInvoke("coerceLongOutput").arg(rawExpr);
          case DOUBLE:
            return _dataTemplateUtilClass.staticInvoke("coerceDoubleOutput").arg(rawExpr);
          case BYTES:
            return _dataTemplateUtilClass.staticInvoke("coerceBytesOutput").arg(rawExpr);
          case BOOLEAN:
            return _dataTemplateUtilClass.staticInvoke("coerceBooleanOutput").arg(rawExpr);
          case STRING:
            return _dataTemplateUtilClass.staticInvoke("coerceStringOutput").arg(rawExpr);
          case ENUM:
            return _dataTemplateUtilClass.staticInvoke("coerceEnumOutput")
                .arg(rawExpr)
                .arg(typeClass.dotclass())
                .arg(typeClass.staticRef(DataTemplateUtil.UNKNOWN_ENUM));
        }
      }

      JClass customClass = generate(customInfoSpec.getCustomClass());
      return _dataTemplateUtilClass.staticInvoke("coerceCustomOutput").arg(rawExpr).arg(customClass.dotclass());
    }
    else
    {
      switch (schema.getDereferencedType())
      {
        case MAP:
        case RECORD:
          return JOp.cond(rawExpr.eq(JExpr._null()), JExpr._null(), JExpr._new(typeClass)
              .arg(_dataTemplateUtilClass.staticInvoke("castOrThrow").arg(rawExpr).arg(_dataMapClass.dotclass())));
        case ARRAY:
          return JOp.cond(rawExpr.eq(JExpr._null()), JExpr._null(), JExpr._new(typeClass)
              .arg(_dataTemplateUtilClass.staticInvoke("castOrThrow").arg(rawExpr).arg(_dataListClass.dotclass())));
        case FIXED:
        case UNION:
          return JOp.cond(rawExpr.eq(JExpr._null()), JExpr._null(), JExpr._new(typeClass).arg(rawExpr));
        default:
          throw new TemplateOutputCastException(
              "Cannot handle wrapped schema of type " + schema.getDereferencedType());
      }
    }
  }

  private JExpression getCoerceInputExpression(JExpression objectExpr, DataSchema schema, CustomInfoSpec customInfoSpec)
  {
    if (CodeUtil.isDirectType(schema))
    {
      if (customInfoSpec == null)
      {
        switch (schema.getDereferencedType())
        {
          case INT:
            return _dataTemplateUtilClass.staticInvoke("coerceIntInput").arg(objectExpr);
          case FLOAT:
            return _dataTemplateUtilClass.staticInvoke("coerceFloatInput").arg(objectExpr);
          case LONG:
            return _dataTemplateUtilClass.staticInvoke("coerceLongInput").arg(objectExpr);
          case DOUBLE:
            return _dataTemplateUtilClass.staticInvoke("coerceDoubleInput").arg(objectExpr);
          case BYTES:
          case BOOLEAN:
          case STRING:
            return objectExpr;
          case ENUM:
            return objectExpr.invoke("name");
        }
      }

      JClass customClass = generate(customInfoSpec.getCustomClass());
      return _dataTemplateUtilClass.staticInvoke("coerceCustomInput").arg(objectExpr).arg(customClass.dotclass());
    }
    else
    {
      return objectExpr.invoke("data");
    }
  }

  public static class Config
  {
    private String _defaultPackage;
    private boolean _recordFieldAccessorWithMode;
    private boolean _recordFieldRemove;
    private boolean _pathSpecMethods;
    private boolean _copierMethods;
    private String _rootPath;

    public Config()
    {
      _defaultPackage = null;
      _recordFieldAccessorWithMode = true;
      _recordFieldRemove = true;
      _pathSpecMethods = true;
      _copierMethods = true;
      _rootPath = null;
    }

    public void setDefaultPackage(String defaultPackage)
    {
      _defaultPackage = defaultPackage;
    }

    public String getDefaultPackage()
    {
      return _defaultPackage;
    }

    public void setRecordFieldAccessorWithMode(boolean recordFieldAccessorWithMode)
    {
      _recordFieldAccessorWithMode = recordFieldAccessorWithMode;
    }

    public boolean getRecordFieldAccessorWithMode()
    {
      return _recordFieldAccessorWithMode;
    }

    public void setRecordFieldRemove(boolean recordFieldRemove)
    {
      _recordFieldRemove = recordFieldRemove;
    }

    public boolean getRecordFieldRemove()
    {
      return _recordFieldRemove;
    }

    public void setPathSpecMethods(boolean pathSpecMethods)
    {
      _pathSpecMethods = pathSpecMethods;
    }

    public boolean getPathSpecMethods()
    {
      return _pathSpecMethods;
    }

    public void setCopierMethods(boolean copierMethods)
    {
      _copierMethods = copierMethods;
    }

    public boolean getCopierMethods()
    {
      return _copierMethods;
    }

    public void setRootPath(String rootPath)
    {
      _rootPath = rootPath;
    }

    public String getRootPath()
    {
      return _rootPath;
    }
  }
}
