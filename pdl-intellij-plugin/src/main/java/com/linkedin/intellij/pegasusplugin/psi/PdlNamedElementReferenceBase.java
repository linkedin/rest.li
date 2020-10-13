/*
 * Copyright 2015 Coursera Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.linkedin.intellij.pegasusplugin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.PsiTreeUtil;
import com.linkedin.intellij.pegasusplugin.PdlIdentifierResolver;
import com.linkedin.intellij.pegasusplugin.PdlTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class PdlNamedElementReferenceBase extends PdlNamedElementBase {
  public PdlNamedElementReferenceBase(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public PdlTypeName getFullname() {
    PdlFullyQualifiedName nameNode = PsiTreeUtil.findChildOfType(this, PdlFullyQualifiedName.class);
    if (nameNode != null) {
      return toFullname(getPdlFile(), nameNode.getText(), this);
    }
    PdlJsonNull nullNode = PsiTreeUtil.findChildOfType(this, PdlJsonNull.class);
    if (nullNode != null) {
      return toFullname(getPdlFile(), "null", this);
    }
    return null;
  }

  public static PdlTypeName toFullname(PdlPsiFile pdlPsiFile, String unescapedName, PsiElement location) {
    if (PdlTypeName.isPrimitive(unescapedName) || unescapedName.contains(".")) {
      return PdlTypeName.decode(unescapedName);
    } else {
      PdlTypeName importedName = pdlPsiFile.lookupImport(PdlTypeName.escape(unescapedName));
      if (importedName != null) {
        return importedName;
      } else {
        PdlNamespace namespace = PdlTreeUtil.getNamespaceAtElement(location);
        if (namespace != null) {
          return PdlTypeName.decode(namespace.getText(), unescapedName);
        } else {
          return PdlTypeName.decode(unescapedName);
        }
      }
    }
  }

  @Override
  public boolean isCrossNamespace() {
    PdlTypeName referenceFullname = getFullname();
    PdlNamespace localNamespace = PdlTreeUtil.getNamespaceAtElement(this);

    // If at least one of these is null, non-emptiness of the other namespace constitutes a "cross-namespace" reference
    if (referenceFullname == null || localNamespace == null) {
      return (referenceFullname != null && !referenceFullname.getNamespace().isEmpty())
          || (localNamespace != null && !localNamespace.getText().isEmpty());
    }

    return !referenceFullname.getNamespace().equals(localNamespace.getText());
  }

  @Nullable
  @Override
  public PsiElement getNameIdentifier() {
    if (getFullname().isPrimitive()) {
      return null;
    }
    ASTNode qualifiedName = getNode().findChildByType(Types.FULLY_QUALIFIED_NAME);
    if (qualifiedName != null) {
      return qualifiedName.getLastChildNode().getPsi();
    }
    return null;
  }

  @Override
  public PsiReference getReference() {
    PdlTypeName fullname = getFullname();
    if (fullname == null || fullname.isPrimitive()) {
      return null;
    }
    PsiElement element = PdlIdentifierResolver.findTypeDeclaration(getProject(), getPdlFile(), getFullname());
    if (element instanceof PdlTypeNameDeclaration) {
      PdlTypeNameDeclaration declaration = (PdlTypeNameDeclaration) element;
      // need the offsets of the declaration name within the text of this element
      String name = declaration.getName();
      int start = getText().lastIndexOf(name);
      int end = start + name.length();
      return new PdlReference(this, declaration, new TextRange(start, end));
    } else if (element instanceof PsiFile) {
      String name = getFullname().getName();
      int start = getText().lastIndexOf(name);
      int end = start + name.length();
      return new PdscReference(this, (PsiFile) element, new TextRange(start, end));
    }
    return null;
  }
}
