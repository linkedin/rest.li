package com.linkedin.intellij.pegasusplugin.psi;

import com.intellij.psi.stubs.StubElement;


/**
 * Create a IntelliJ managed "stub index" of identifiers by their fully qualified name.
 *
 * Primarily used to lookup the declaration of a type by it's full name.
 */
public interface PdlTypeNameDeclarationStub extends StubElement<PdlTypeNameDeclaration> {
  PdlTypeName getFullname();
}
