package com.linkedin.intellij.pegasusplugin.psi;

import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;

public class PdlTypeNameDeclarationStubImpl extends StubBase<PdlTypeNameDeclaration> implements PdlTypeNameDeclarationStub {
  private PdlTypeName _fullname;

  public PdlTypeNameDeclarationStubImpl(final StubElement parent, PdlTypeName fullname) {
    // TODO: remove cast, this should already be sorted out in Types.  Need to
    // fix the problem there.
    super(parent, (IStubElementType) Types.TYPE_NAME_DECLARATION);
    this._fullname = fullname;
  }

  @Override
  public PdlTypeName getFullname() {
    return _fullname;
  }
}
