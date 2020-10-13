package com.linkedin.intellij.pegasusplugin;

import com.intellij.psi.stubs.PsiFileStubImpl;
import com.linkedin.intellij.pegasusplugin.psi.PdlPsiFile;


public class PsiFileStub extends PsiFileStubImpl<PdlPsiFile> {
  public PsiFileStub(PdlPsiFile pdlPsiFile) {
    super(pdlPsiFile);
  }
}
