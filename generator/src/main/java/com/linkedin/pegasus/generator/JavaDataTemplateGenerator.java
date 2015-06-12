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
import com.linkedin.data.schema.ArrayDataSchema;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.EnumDataSchema;
import com.linkedin.data.schema.JsonBuilder;
import com.linkedin.data.schema.MapDataSchema;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.SchemaToJsonEncoder;
import com.linkedin.data.template.BooleanArray;
import com.linkedin.data.template.BooleanMap;
import com.linkedin.data.template.BytesArray;
import com.linkedin.data.template.BytesMap;
import com.linkedin.data.template.DataTemplateUtil;
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
import com.linkedin.data.template.StringArray;
import com.linkedin.data.template.StringMap;
import com.linkedin.data.template.TyperefInfo;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.codemodel.ClassType;
import com.sun.codemodel.JAnnotatable;
import com.sun.codemodel.JBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JClassContainer;
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

  private static final int MAX_SCHEMA_FIELD_JSON_LENGTH = 32000;
  private static final Logger _log = LoggerFactory.getLogger(JavaDataTemplateGenerator.class);
  //
  // Deprecated annotation utils
  //
  private static final String DEPRECATED_KEY = "deprecated";
  private static final String DEPRECATED_SYMBOLS_KEY = "deprecatedSymbols";

  private final Map<ClassTemplateSpec, JDefinedClass> _definedClasses = new HashMap<ClassTemplateSpec, JDefinedClass>();
  private final Map<JDefinedClass, ClassTemplateSpec> _generatedClasses = new HashMap<JDefinedClass, ClassTemplateSpec>();

  private final boolean _recordFieldAccessorWithMode;

  private JavaDataTemplateGenerator(String defaultPackage,
                                    boolean recordFieldAccessorWithMode)
  {
    super(defaultPackage);

    _recordFieldAccessorWithMode = recordFieldAccessorWithMode;
  }

  public JavaDataTemplateGenerator(Config config)
  {
    this(config.getDefaultPackage(),
         config.getRecordFieldAccessorWithMode());
  }

  /**
   * @param defaultPackage package to be used when a {@link NamedDataSchema} does not specify a namespace
   */
  public JavaDataTemplateGenerator(String defaultPackage)
  {
    this(defaultPackage, true);
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

  private static void generateCopierMethods(JDefinedClass templateClass)
  {
    overrideCopierMethod(templateClass, "clone");
    overrideCopierMethod(templateClass, "copy");
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

  private static void generateConstructorWithNoArg(JDefinedClass cls, JVar schemaField, JClass newClass)
  {
    final JMethod noArgConstructor = cls.constructor(JMod.PUBLIC);
    noArgConstructor.body().invoke(SUPER).arg(JExpr._new(newClass)).arg(schemaField);
  }

  private static void generateConstructorWithNoArg(JDefinedClass cls, JClass newClass)
  {
    final JMethod noArgConstructor = cls.constructor(JMod.PUBLIC);
    noArgConstructor.body().invoke(THIS).arg(JExpr._new(newClass));
  }

  private static void generateConstructorWithObjectArg(JDefinedClass cls, JVar schemaField)
  {
    final JMethod argConstructor = cls.constructor(JMod.PUBLIC);
    final JVar param = argConstructor.param(Object.class, "data");
    argConstructor.body().invoke(SUPER).arg(param).arg(schemaField);
  }

  private static void generateConstructorWithArg(JDefinedClass cls, JVar schemaField, JClass paramClass)
  {
    final JMethod argConstructor = cls.constructor(JMod.PUBLIC);
    final JVar param = argConstructor.param(paramClass, "data");
    argConstructor.body().invoke(SUPER).arg(param).arg(schemaField);
  }

  private static void generateConstructorWithArg(JDefinedClass cls, JVar schemaField, JClass paramClass, JClass elementClass, JClass dataClass)
  {
    final JMethod argConstructor = cls.constructor(JMod.PUBLIC);
    final JVar param = argConstructor.param(paramClass, "data");
    final JInvocation inv = argConstructor.body().invoke(SUPER).arg(param).arg(schemaField).arg(JExpr.dotclass(elementClass));
    dataClassArg(inv, dataClass);
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

  private static void overrideCopierMethod(JDefinedClass templateClass, String methodName)
  {
    final JMethod copierMethod = templateClass.method(JMod.PUBLIC, templateClass, methodName);
    copierMethod.annotate(Override.class);
    copierMethod._throws(CloneNotSupportedException.class);
    copierMethod.body()._return(JExpr.cast(templateClass, JExpr._super().invoke(methodName)));
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
    final Object deprecatedSymbolsProp = enumSchema.getProperties().get(DEPRECATED_SYMBOLS_KEY);
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

  private static void addAccessorDoc(JMethod method, RecordDataSchema.Field field, String prefix)
  {
    method.javadoc().append(prefix + " for " + field.getName());
    method.javadoc().addXdoclet("see Fields#" + escapeReserved(field.getName()));
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
        container = getPackage(classTemplateSpec.getNamespace());
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

    if (CodeUtil.isDirectType(arrayDataTemplateSpec.getSchema().getItems()))
    {
      arrayClass._extends(_directArrayClass.narrow(itemJClass));
    }
    else
    {
      arrayClass._extends(_wrappingArrayClass.narrow(itemJClass));
    }

    /** see {@link #schemaForArrayItemsOrMapValues} */
    final DataSchema bareSchema = new ArrayDataSchema(schemaForArrayItemsOrMapValues(arrayDataTemplateSpec.getCustomInfo(), arrayDataTemplateSpec.getSchema().getItems()));
    final JVar schemaField = generateSchemaField(arrayClass, bareSchema);

    generateConstructorWithNoArg(arrayClass, _dataListClass);
    generateConstructorWithInitialCapacity(arrayClass, _dataListClass);
    generateConstructorWithCollection(arrayClass, itemJClass);
    generateConstructorWithArg(arrayClass, schemaField, _dataListClass, itemJClass, dataJClass);

    generatePathSpecMethodsForCollection(arrayClass, arrayDataTemplateSpec.getSchema(), itemJClass, "items");

    generateCustomClassInitialization(arrayClass, arrayDataTemplateSpec.getCustomInfo());

    generateCopierMethods(arrayClass);
  }

  protected void generateEnum(JDefinedClass enumClass, EnumTemplateSpec enumSpec)
  {
    enumClass.javadoc().append(enumSpec.getSchema().getDoc());

    setDeprecatedAnnotationAndJavadoc(enumSpec.getSchema(), enumClass);

    generateSchemaField(enumClass, enumSpec.getSchema());

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

    final JVar schemaField = generateSchemaField(fixedClass, fixedSpec.getSchema());

    final JMethod bytesConstructor = fixedClass.constructor(JMod.PUBLIC);
    final JVar param = bytesConstructor.param(ByteString.class, "value");
    bytesConstructor.body().invoke(SUPER).arg(param).arg(schemaField);

    generateConstructorWithObjectArg(fixedClass, schemaField);

    generateCopierMethods(fixedClass);
  }

  protected void generateMap(JDefinedClass mapClass, MapTemplateSpec mapSpec)
      throws JClassAlreadyExistsException
  {
    final JClass valueJClass = generate(mapSpec.getValueClass());
    final JClass dataJClass = generate(mapSpec.getValueDataClass());

    if (CodeUtil.isDirectType(mapSpec.getSchema().getValues()))
    {
      mapClass._extends(_directMapClass.narrow(valueJClass));
    }
    else
    {
      mapClass._extends(_wrappingMapClass.narrow(valueJClass));
    }

    final DataSchema bareSchema = new MapDataSchema(schemaForArrayItemsOrMapValues(mapSpec.getCustomInfo(), mapSpec.getSchema().getValues()));
    final JVar schemaField = generateSchemaField(mapClass, bareSchema);

    generateConstructorWithNoArg(mapClass, _dataMapClass);
    generateConstructorWithInitialCapacity(mapClass, _dataMapClass);
    generateConstructorWithInitialCapacityAndLoadFactor(mapClass);
    generateConstructorWithMap(mapClass, valueJClass);
    generateConstructorWithArg(mapClass, schemaField, _dataMapClass, valueJClass, dataJClass);

    generatePathSpecMethodsForCollection(mapClass, mapSpec.getSchema(), valueJClass, "values");

    generateCustomClassInitialization(mapClass, mapSpec.getCustomInfo());

    generateCopierMethods(mapClass);
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

    templateClass._extends(_recordClass);

    generatePathSpecMethodsForRecord(recordSpec.getFields(), templateClass);

    final JFieldVar schemaFieldVar = generateSchemaField(templateClass, recordSpec.getSchema());
    generateConstructorWithNoArg(templateClass, schemaFieldVar, _dataMapClass);
    generateConstructorWithArg(templateClass, schemaFieldVar, _dataMapClass);

    for (RecordTemplateSpec.Field field : recordSpec.getFields())
    {
      generateRecordFieldAccessors(templateClass, field, generate(field.getType()), schemaFieldVar);

      if (field.getCustomInfo() != null)
      {
        generateCustomClassInitialization(templateClass, field.getCustomInfo());
      }
    }

    generateCopierMethods(templateClass);
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
    }

    final JVar staticFields = templateClass.field(JMod.PRIVATE | JMod.STATIC | JMod.FINAL, fieldsNestedClass, "_fields").init(JExpr._new(fieldsNestedClass));
    final JMethod staticFieldsAccessor = templateClass.method(JMod.PUBLIC | JMod.STATIC, fieldsNestedClass, "fields");
    staticFieldsAccessor.body()._return(staticFields);
  }

  private void generateRecordFieldAccessors(JDefinedClass templateClass, RecordTemplateSpec.Field field, JClass type, JVar schemaFieldVar)
  {
    final RecordDataSchema.Field schemaField = field.getSchemaField();
    final DataSchema fieldSchema = schemaField.getType();
    final boolean isDirect = CodeUtil.isDirectType(fieldSchema);
    final String wrappedOrDirect;
    if (isDirect)
    {
      wrappedOrDirect = (field.getCustomInfo() == null ? "Direct" : "CustomType");
    }
    else
    {
      wrappedOrDirect = "Wrapped";
    }
    final String capitalizedName = CodeUtil.capitalize(schemaField.getName());

    final String fieldFieldName = "FIELD_" + capitalizedName;
    final JFieldVar fieldField = templateClass.field(JMod.PRIVATE | JMod.STATIC | JMod.FINAL, RecordDataSchema.Field.class, fieldFieldName);
    fieldField.init(schemaFieldVar.invoke("getField").arg(schemaField.getName()));

    // Generate has method.
    final JMethod has = templateClass.method(JMod.PUBLIC, getCodeModel().BOOLEAN, "has" + capitalizedName);
    addAccessorDoc(has, schemaField, "Existence checker");
    setDeprecatedAnnotationAndJavadoc(has, schemaField);
    final JBlock hasBody = has.body();
    JExpression res = JExpr.invoke("contains").arg(fieldField);
    hasBody._return(res);

    // Generate remove method.
    final String removeName = "remove" + capitalizedName;
    final JMethod remove = templateClass.method(JMod.PUBLIC, getCodeModel().VOID, removeName);
    addAccessorDoc(remove, schemaField, "Remover");
    setDeprecatedAnnotationAndJavadoc(remove, schemaField);
    final JBlock removeBody = remove.body();
    removeBody.invoke("remove").arg(fieldField);

    final String getterName = JavaCodeUtil.getGetterName(getCodeModel(), type, capitalizedName);

    if (_recordFieldAccessorWithMode)
    {
      // Getter method with mode.
      final JMethod getterWithMode = templateClass.method(JMod.PUBLIC, type, getterName);
      addAccessorDoc(getterWithMode, schemaField, "Getter");
      setDeprecatedAnnotationAndJavadoc(getterWithMode, schemaField);
      JVar modeParam = getterWithMode.param(_getModeClass, "mode");
      final JBlock getterWithModeBody = getterWithMode.body();
      res = JExpr.invoke("obtain" + wrappedOrDirect).arg(fieldField).arg(JExpr.dotclass(type)).arg(modeParam);
      getterWithModeBody._return(res);
    }

    // Getter method without mode.
    final JMethod getterWithoutMode = templateClass.method(JMod.PUBLIC, type, getterName);
    addAccessorDoc(getterWithoutMode, schemaField, "Getter");
    setDeprecatedAnnotationAndJavadoc(getterWithoutMode, schemaField);
    final JBlock getterWithoutModeBody = getterWithoutMode.body();
    res = JExpr.invoke("obtain" + wrappedOrDirect).arg(fieldField).arg(JExpr.dotclass(type)).arg(_strictGetMode);
    getterWithoutModeBody._return(res);

    // Determine dataClass
    final JClass dataClass = generate(field.getDataClass());
    final String setterName = "set" + capitalizedName;

    if (_recordFieldAccessorWithMode)
    {
      // Setter method with mode
      final JMethod setterWithMode = templateClass.method(JMod.PUBLIC, templateClass, setterName);
      addAccessorDoc(setterWithMode, schemaField, "Setter");
      setDeprecatedAnnotationAndJavadoc(setterWithMode, schemaField);
      JVar param = setterWithMode.param(type, "value");
      JVar modeParam = setterWithMode.param(_setModeClass, "mode");
      JInvocation inv = setterWithMode.body().invoke("put" + wrappedOrDirect).arg(fieldField).arg(JExpr.dotclass(type));
      dataClassArg(inv, dataClass).arg(param).arg(modeParam);
      setterWithMode.body()._return(JExpr._this());
    }

    // Setter method without mode
    final JMethod setter = templateClass.method(JMod.PUBLIC, templateClass, setterName);
    addAccessorDoc(setter, schemaField, "Setter");
    setDeprecatedAnnotationAndJavadoc(setter, schemaField);
    JVar param = setter.param(type, "value");
    JInvocation inv = setter.body().invoke("put" + wrappedOrDirect).arg(fieldField).arg(JExpr.dotclass(type));
    dataClassArg(inv, dataClass).arg(param).arg(_disallowNullSetMode);
    setter.body()._return(JExpr._this());

    // Setter method without mode for unboxified type
    if (!type.unboxify().equals(type))
    {
      final JMethod unboxifySetter = templateClass.method(JMod.PUBLIC, templateClass, setterName);
      addAccessorDoc(unboxifySetter, schemaField, "Setter");
      setDeprecatedAnnotationAndJavadoc(unboxifySetter, schemaField);
      param = unboxifySetter.param(type.unboxify(), "value");
      inv = unboxifySetter.body().invoke("put" + wrappedOrDirect).arg(fieldField).arg(JExpr.dotclass(type));
      dataClassArg(inv, dataClass).arg(param).arg(_disallowNullSetMode);
      unboxifySetter.body()._return(JExpr._this());
    }
  }

  protected void generateTyperef(JDefinedClass typerefClass, TyperefTemplateSpec typerefSpec)
  {
    typerefClass.javadoc().append(typerefSpec.getSchema().getDoc());

    setDeprecatedAnnotationAndJavadoc(typerefSpec.getSchema(), typerefClass);

    typerefClass._extends(TyperefInfo.class);

    final JVar schemaField = generateSchemaField(typerefClass, typerefSpec.getSchema());

    final JMethod constructor = typerefClass.constructor(JMod.PUBLIC);
    constructor.body().invoke(SUPER).arg(schemaField);
  }

  protected void generateUnion(JDefinedClass unionClass, UnionTemplateSpec unionSpec)
      throws JClassAlreadyExistsException
  {
    unionClass._extends(getUnionClass());

    final JVar schemaField = generateSchemaField(unionClass, unionSpec.getSchema());

    generateConstructorWithNoArg(unionClass, schemaField, _dataMapClass);
    generateConstructorWithObjectArg(unionClass, schemaField);

    for (UnionTemplateSpec.Member member : unionSpec.getMembers())
    {
      if (member.getClassTemplateSpec() != null)
      {
        generateUnionMemberAccessors(unionClass, member.getSchema(), generate(member.getClassTemplateSpec()), generate(member.getDataClass()), schemaField);
      }
    }

    generatePathSpecMethodsForUnion(unionSpec, unionClass);

    generateCopierMethods(unionClass);

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

  private void generateUnionMemberAccessors(JDefinedClass unionClass, DataSchema memberType, JClass memberClass, JClass dataClass, JVar schemaField)
  {
    final boolean isDirect = CodeUtil.isDirectType(memberType);
    final String wrappedOrDirect = isDirect ? "Direct" : "Wrapped";
    final String memberKey = memberType.getUnionMemberKey();
    final String capitalizedName = CodeUtil.getUnionMemberName(memberType);

    final String memberFieldName = "MEMBER_" + capitalizedName;
    final JFieldVar memberField = unionClass.field(JMod.PRIVATE | JMod.STATIC | JMod.FINAL, DataSchema.class, memberFieldName);
    memberField.init(schemaField.invoke("getType").arg(memberKey));
    final String setterName = "set" + capitalizedName;

    // Generate builder.

    final JMethod createMethod = unionClass.method(JMod.PUBLIC | JMod.STATIC, unionClass, "create");
    JVar param = createMethod.param(memberClass, "value");
    final JVar newUnionVar = createMethod.body().decl(unionClass, "newUnion", JExpr._new(unionClass));
    createMethod.body().invoke(newUnionVar, setterName).arg(param);
    createMethod.body()._return(newUnionVar);

    // Is method.

    final JMethod is = unionClass.method(JMod.PUBLIC, getCodeModel().BOOLEAN, "is" + capitalizedName);
    final JBlock isBody = is.body();
    JExpression res = JExpr.invoke("memberIs").arg(memberKey);
    isBody._return(res);

    // Getter method.

    final String getterName = "get" + capitalizedName;
    final JMethod getter = unionClass.method(JMod.PUBLIC, memberClass, getterName);
    final JBlock getterBody = getter.body();
    res = JExpr.invoke("obtain" + wrappedOrDirect).arg(memberField).arg(JExpr.dotclass(memberClass)).arg(memberKey);
    getterBody._return(res);

    // Setter method.

    final JMethod setter = unionClass.method(JMod.PUBLIC, Void.TYPE, setterName);
    param = setter.param(memberClass, "value");
    final JInvocation inv = setter.body().invoke("select" + wrappedOrDirect).arg(memberField).arg(JExpr.dotclass(memberClass));
    dataClassArg(inv, dataClass).arg(memberKey).arg(param);
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
      final JMethod accessorMethod = fieldsNestedClass.method(JMod.PUBLIC, fieldsRefType, CodeUtil.getUnionMemberName(member.getSchema()));
      accessorMethod.body()._return(JExpr._new(fieldsRefType).arg(JExpr.invoke("getPathComponents")).arg(member.getSchema().getUnionMemberKey()));
    }
  }

  private void populateClassContent(ClassTemplateSpec classTemplateSpec, JDefinedClass definedClass)
      throws JClassAlreadyExistsException
  {
    if (!_generatedClasses.containsKey(definedClass))
    {
      _generatedClasses.put(definedClass, classTemplateSpec);

      JavaCodeUtil.annotate(definedClass, "Data Template", classTemplateSpec.getLocation());

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

  private JFieldVar generateSchemaField(JDefinedClass templateClass, DataSchema schema)
  {
    final JFieldVar schemaField = templateClass.field(JMod.PRIVATE | JMod.STATIC | JMod.FINAL, schema.getClass(), DataTemplateUtil.SCHEMA_FIELD_NAME);
    final String schemaJson = SchemaToJsonEncoder.schemaToJson(schema, JsonBuilder.Pretty.COMPACT);
    final JInvocation parseSchemaInvocation;
    if (schemaJson.length() < MAX_SCHEMA_FIELD_JSON_LENGTH)
    {
      parseSchemaInvocation = _dataTemplateUtilClass.staticInvoke("parseSchema").arg(schemaJson);
    }
    else
    {
      JInvocation stringBuilderInvocation = JExpr._new(_stringBuilderClass);
      for (int index = 0; index < schemaJson.length(); index += MAX_SCHEMA_FIELD_JSON_LENGTH)
      {
        stringBuilderInvocation = stringBuilderInvocation.
            invoke("append").
            arg(schemaJson.substring(index, Math.min(schemaJson.length(), index + MAX_SCHEMA_FIELD_JSON_LENGTH)));
      }
      stringBuilderInvocation = stringBuilderInvocation.invoke("toString");
      parseSchemaInvocation = _dataTemplateUtilClass.staticInvoke("parseSchema").arg(stringBuilderInvocation);
    }
    schemaField.init(JExpr.cast(getCodeModel()._ref(schema.getClass()), parseSchemaInvocation));

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

  public static class Config
  {
    private String _defaultPackage;
    private boolean _recordFieldAccessorWithMode;

    public Config()
    {
      _defaultPackage = null;
      _recordFieldAccessorWithMode = true;
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
  }
}
