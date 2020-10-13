package com.linkedin.intellij.pegasusplugin;

import com.linkedin.intellij.pegasusplugin.psi.PdlTypeNameDeclaration;
import java.util.Collection;
import java.util.Collections;
import org.jetbrains.annotations.NotNull;


/**
 * Describes a prohibited import in PDL.
 */
public class PdlImportError {
  private Type _type;
  private Collection<PdlTypeNameDeclaration> _conflictingTypes;

  public PdlImportError(Type type) {
    this(type, Collections.emptyList());
  }

  public PdlImportError(Type type, @NotNull Collection<PdlTypeNameDeclaration> conflictingTypes) {
    _type = type;
    _conflictingTypes = Collections.unmodifiableCollection(conflictingTypes);
  }

  /**
   * Returns the classification this prohibited import.
   * @return import error type
   */
  public Type getType() {
    return _type;
  }

  /**
   * If applicable, returns a collection of type name declarations that conflict with this import.
   * @return collection of conflicting type name declarations
   */
  @NotNull
  public Collection<PdlTypeNameDeclaration> getConflictingTypes() {
    return _conflictingTypes;
  }

  @Override
  public String toString() {
    return _type.toString() + (_type == Type.CONFLICTS_WITH_LOCAL_TYPE ? _conflictingTypes : "");
  }

  public enum Type {
    REFERENCES_ROOT_NAMESPACE,
    REFERENCES_LOCAL_TYPE,
    CONFLICTS_WITH_LOCAL_TYPE
  }
}
