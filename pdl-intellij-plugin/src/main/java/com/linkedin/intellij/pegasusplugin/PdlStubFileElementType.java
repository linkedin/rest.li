package com.linkedin.intellij.pegasusplugin;

import com.intellij.lang.Language;
import com.intellij.psi.tree.IStubFileElementType;

public class PdlStubFileElementType extends IStubFileElementType<PsiFileStub> {

  public PdlStubFileElementType(Language language) {
    super(language);
  }

  @Override
  public int getStubVersion() {
    return 4;
  }

  public String getExternalId() {
    return "pdl.FILE";
  }
}
