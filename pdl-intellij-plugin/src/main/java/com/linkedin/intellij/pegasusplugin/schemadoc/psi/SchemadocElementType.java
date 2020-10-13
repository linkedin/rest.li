package com.linkedin.intellij.pegasusplugin.schemadoc.psi;

import com.intellij.psi.tree.IElementType;
import com.linkedin.intellij.pegasusplugin.schemadoc.SchemadocLanguage;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class SchemadocElementType extends IElementType {
  public SchemadocElementType(@NotNull @NonNls String debugName) {
    super(debugName, SchemadocLanguage.INSTANCE);
  }
}
