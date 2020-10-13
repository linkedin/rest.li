package com.linkedin.intellij.pegasusplugin.schemadoc.psi;

import com.intellij.psi.PsiDocCommentBase;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LazyParseablePsiElement;
import com.intellij.psi.tree.IElementType;
import com.linkedin.intellij.pegasusplugin.psi.PdlElementType;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Nullable;

public class PsiSchemadocElement extends LazyParseablePsiElement implements PsiDocCommentBase {
  public PsiSchemadocElement(CharSequence buffer) {
    super(PdlElementType.DOC_COMMENT, buffer);
  }

  @Nullable
  @Override
  public PsiElement getOwner() {
    return null;
  }

  @Override
  public IElementType getTokenType() {
    return getElementType();
  }

  public String getComment() {
    PsiElement[] commentParts =
        this.getChildrenAsPsiElements(SchemadocTypes.DOC_COMMENT_CONTENT, count -> new PsiElement[count]);
    return Arrays.stream(commentParts).map(PsiElement::getText).collect(Collectors.joining("\n"));
  }
}
