package com.linkedin.intellij.pegasusplugin.psi;

/**
 * Models a type reference.
 */
public interface PdlNamedElementTypeReference extends PdlNamedElement {
  /**
   * @return true if the type reference is a simple reference.
   */
  boolean isSimpleReference();
}
