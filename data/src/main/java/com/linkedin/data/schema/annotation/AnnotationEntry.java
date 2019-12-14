package com.linkedin.data.schema.annotation;

import com.linkedin.data.schema.EnumDataSchema;
import com.linkedin.data.schema.FixedDataSchema;
import com.linkedin.data.schema.PathSpec;
import com.linkedin.data.schema.RecordDataSchema;
import com.linkedin.data.schema.TyperefDataSchema;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashSet;


/**
 * AnnotationEntry is an object that generated from
 * (1) an annotation override entry with PathSpec string as key and override properties as the value.
 * or
 * (2) an annotation namespace's properties directly (i.e. inline annotation)
 *
 * The {@link RecordDataSchema.Field} or {@link com.linkedin.data.schema.DataSchema} where the annotation entry was
 * annotated at is called "annotatedTarget".
 *
 * Take a schema example:
 *
 * <pre>{@code
 * record outerRcd{
 *    @customAnnotation= {"/f1/f2" : "2nd layer" }
 *    f: record rcd {
 *           @customAnnotation= {"/f2" : "1st layer" }
 *           f1: record rcd2 {
 *               @customAnnotation = "OriginalValue"
 *               f2: string
 *           }
 *       }
 *    }
 * }
 * </pre>
 *
 * The AnnotationEntry could be generated from
 * (1) {@code
 *        @customAnnotation= {"/f1/f2" : "2nd layer" }
 *     }
 *     This is an annotation override entry
 *
 *     _overridePathSpecStr is "/f1/f2"
 *     _annotationValue is "2nd layer"
 *     _pathToAnnotatedTarget is ["f"] (since this AnnotationEntry is constructed from field "/f" counting from "outerRcd")
 *     _annotatedTarget is the field named "f" inside the RecordDataSchema named "outerRcd"
 *     _annotationType is OVERRIDE_RECORD_FIELD
 *     _matchedPaths is [], i.e. empty
 *     _remainingPaths is ["f1", "f2"] (because there are two segments need to be matched)
 *
 * (2) or <pre>@customAnnotation = "OriginalValue"</pre>
 *     This is an inline annotation entry
 *
 *     _overridePathSpecStr is "" (no pathSpec specified)
 *     _annotationValue is "OriginalValue"
 *     _pathToAnnotatedTarget is ["f", "f1", "f2] (since this AnnotationEntry is constructed from field "/f/f1/f2" counting from "outerRcd")
 *     _annotatedTarget is the RecordDataSchema "rcd2"'s field named f2.
 *     _annotationType is NON_OVERRIDE_RECORD_FIELD
 *     _matchedPaths is []
 *     _remainingPaths is [] (because there are NO segments need to be matched)
 *
 * In order to construct the {@link AnnotationEntry}, a valid PathSpec string is assumed to be passed as an argument.
 * The constructor will parse the string into path components, which are string segments separated by {@link PathSpec#SEPARATOR}
 * _remainingPaths and _matchedPaths could be changed dynamically when {@link PathSpecBasedSchemaAnnotationVisitor} is visiting the schema.
 */
public class AnnotationEntry
{
  // the actual property value that this AnnotationEntry stored
  private Object _annotationValue;
  // The traverser path of the annotatedTarget(field, or DataSchema) that this AnnotationEntry is constructed from, relative to the schema root
  private final ArrayDeque<String> _pathToAnnotatedTarget;
  // The annotatedTarget(field, dataSchema, etc) that this entry was annotated at.
  private final Object _annotatedTarget;

  // pathSpec path components that have been matched
  private ArrayDeque<String> _matchedPaths = new ArrayDeque<>();
  // pathSpec path components that have not been matched, this value was initialized from {@link #_overridePathSpecStr}
  private ArrayDeque<String> _remainingPaths = new ArrayDeque<>();
  // the original PathSpec string
  private final String _overridePathSpecStr;
  /**
   * a type to specify what this {@link AnnotationEntry} is, also @see {@link AnnotationType}
   */
  private final AnnotationType _annotationType;
  // This field is used for Cyclic overriding detection. Need to record the start Schema that this AnnotationEntry
  // is generated, and when a next path segment to match has the same name as this startSchemaName, we detect the
  // cyclic referencing
  private String _startSchemaName = "";

  /**
   * This is the attribute to tell whether this {@link AnnotationEntry}'s override path has been validated.
   */
  private OverridePathValidStatus _overridePathValidStatus = OverridePathValidStatus.UNCHECKED;

  /**
   * Use a enum to represent of a override path validation has been done against this {@link AnnotationEntry}
   * UNCHECKED: This {@link AnnotationEntry} is not validated. This could happen in the case where the entry is either not checked yet,
   *            or doesn't need to be checked(non-overrides).
   * VALID: This {@link AnnotationEntry} has been checked and is valid.
   * INVALID: This {@link AnnotationEntry} has been checked and is invalid.
   */
  enum OverridePathValidStatus
  {
    UNCHECKED,
    VALID,
    INVALID,
  }

  OverridePathValidStatus getOverridePathValidStatus()
  {
    return _overridePathValidStatus;
  }

  void setOverridePathValidStatus(OverridePathValidStatus overridePathValidStatus)
  {
    _overridePathValidStatus = overridePathValidStatus;
  }

  /**
   * As for {@link #_annotationType}, in detail, the {@link AnnotationEntry} can be generated from two ways:
   * (1) For overrides:
   *  (1.1). From {@link RecordDataSchema.Field}
   *  (1.2). From {@link TyperefDataSchema}
   *  (1.3). From {@link RecordDataSchema}: RecordDataSchemas' properties can have overrides for "included" RecordDataSchema.
   *
   * (2) For non-override:
   *  (2.1). From named schema: {@link TyperefDataSchema}
   *  (2.2). From named schema: {@link EnumDataSchema}
   *  (2.3). From named schema: {@link FixedDataSchema}
   *  (2.4). From {@link RecordDataSchema.Field}
   *
   */
  enum AnnotationType
  {
    OVERRIDE_RECORD_FIELD,
    OVERRIDE_RECORD_INCLUDE,
    OVERRIDE_TYPE_REF_OVERRIDE,
    NON_OVERRIDE_TYPE_REF,
    NON_OVERRIDE_ENUM,
    NON_OVERRIDE_FIXED,
    NON_OVERRIDE_RECORD_FIELD
  }

  AnnotationEntry(String pathSpecStr,
                  Object annotationValue,
                  AnnotationType annotationType,
                  ArrayDeque<String> pathToAnnotatedTarget,
                  Object annotatedTarget)
  {
    _remainingPaths = new ArrayDeque<>(Arrays.asList(pathSpecStr.split(Character.toString(PathSpec.SEPARATOR))));
    _remainingPaths.remove("");
    _annotationValue = annotationValue;
    _overridePathSpecStr = pathSpecStr;
    _annotationType = annotationType;
    _pathToAnnotatedTarget = new ArrayDeque<>(pathToAnnotatedTarget);
    _annotatedTarget = annotatedTarget;
  }

  boolean isOverride()
  {
    return new HashSet<>(Arrays.asList(AnnotationType.OVERRIDE_RECORD_FIELD,
                                       AnnotationType.OVERRIDE_RECORD_INCLUDE,
                                       AnnotationType.OVERRIDE_TYPE_REF_OVERRIDE)).contains(_annotationType);
  }

  ArrayDeque<String> getMatchedPaths()
  {
    return _matchedPaths;
  }

  ArrayDeque<String> getRemainingPaths()
  {
    return _remainingPaths;
  }

  Object getAnnotationValue()
  {
    return _annotationValue;
  }

  void setMatchedPaths(ArrayDeque<String> matchedPaths)
  {
    this._matchedPaths = matchedPaths;
  }

  void setRemainingPaths(ArrayDeque<String> remainingPaths)
  {
    this._remainingPaths = remainingPaths;
  }

  void setAnnotationValue(Object annotationValue)
  {
    this._annotationValue = annotationValue;
  }

  String getStartSchemaName()
  {
    return _startSchemaName;
  }

  void setStartSchemaName(String startSchemaName)
  {
    this._startSchemaName = startSchemaName;
  }

  String getOverridePathSpecStr()
  {
    return _overridePathSpecStr;
  }

  public AnnotationType getAnnotationType()
  {
    return _annotationType;
  }


  ArrayDeque<String> getPathToAnnotatedTarget()
  {
    return _pathToAnnotatedTarget;
  }

  public Object getAnnotatedTarget()
  {
    return _annotatedTarget;
  }
}
