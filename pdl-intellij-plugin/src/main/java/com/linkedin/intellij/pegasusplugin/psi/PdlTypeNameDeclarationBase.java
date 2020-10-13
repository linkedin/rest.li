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

import com.intellij.extapi.psi.StubBasedPsiElementBase;
import com.intellij.lang.ASTNode;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.NavigationItem;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import com.linkedin.intellij.pegasusplugin.PdlFileType;
import com.linkedin.intellij.pegasusplugin.PdlIcons;
import com.linkedin.intellij.pegasusplugin.PdlTreeUtil;
import javax.swing.*;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class PdlTypeNameDeclarationBase extends StubBasedPsiElementBase<PdlTypeNameDeclarationStub>
    implements PdlTypeNameDeclarationInterface, PdlTypeNameDeclaration, NavigationItem {
  public PdlTypeNameDeclarationBase(@NotNull ASTNode node) {
    super(node);
  }

  public PdlTypeNameDeclarationBase(PdlTypeNameDeclarationStub stub, IStubElementType nodeType) {
    super(stub, nodeType);
  }

  @Override
  public String getName() {
    return getFullname().getName();
  }

  public PdlPsiFile getPdlFile() {
    return (PdlPsiFile) getContainingFile();
  }

  public PdlTypeName getFullname() {
    PdlSimpleName simpleName = getSimpleName();
    String unescapedName = simpleName.getText();
    if (PdlTypeName.isPrimitive(unescapedName)) {
      return PdlTypeName.decode(unescapedName);
    } else {
      PdlNamespace namespace = PdlTreeUtil.getNamespaceAtElement(this);
      if (namespace != null) {
        return PdlTypeName.decode(namespace.getText(), unescapedName);
      } else {
        return PdlTypeName.decode(unescapedName);
      }
    }
  }

  @Override
  public PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException {
    PdlTypeNameDeclaration replacement = PdlElementFactory.createTypeNameDeclaration(this.getProject(), name);
    getSimpleName().replace(replacement.getSimpleName());
    getPdlFile().setName(PdlFileType.toFilename(replacement.getName()));
    return this;
  }

  @Nullable
  @Override
  public PsiElement getNameIdentifier() {
    ASTNode name = getNode().findChildByType(Types.FULLY_QUALIFIED_NAME);
    if (name != null) {
      return name.getFirstChildNode().getPsi();
    }
    return null;
  }

  public String getSchemaTypeName() {
    IElementType element = getNode().getTreeParent().getElementType();
    if (element == Types.RECORD_DECLARATION) {
      return "record";
    } else if (element == Types.TYPEREF_DECLARATION) {
      return "typeref";
    } else if (element == Types.ENUM_DECLARATION) {
      return "enum";
    } else if (element == Types.FIXED_DECLARATION) {
      return "fixed";
    } else {
      return "UNKNOWN";
    }
  }

  public ItemPresentation getPresentation() {
    return new ItemPresentation() {
      @Nullable
      @Override
      public String getPresentableText() {
        return getName();
      }

      @Nullable
      @Override
      public String getLocationString() {
        String namespace = getFullname().getNamespace();
        if (namespace != null) {
          return "(" + namespace + ")";
        } else {
          return "";
        }
      }

      @Nullable
      @Override
      public Icon getIcon(boolean unused) {
        return PdlIcons.FILE;
      }
    };
  }

  @Nullable
  public PdlNamedTypeDeclaration getNamedTypeDeclaration() {
    PsiElement surroundingType =
        PsiTreeUtil.getParentOfType(this, PdlNamedTypeDeclaration.class, PdlAnonymousTypeDeclaration.class);
    if (surroundingType instanceof PdlNamedTypeDeclaration) {
      return (PdlNamedTypeDeclaration) surroundingType;
    } else {
      return null;
    }
  }

  @Override
  public boolean isDeprecated() {
    PdlNamedTypeDeclaration decl = getNamedTypeDeclaration();
    if (decl != null) {
      return decl.isDeprecated();
    } else {
      return false;
    }
  }

  @Override
  public boolean isAccessibleFrom(@Nullable PdlPsiFile pdlFile) {
    final PdlPsiFile currentFile = getPdlFile();
    return pdlFile == null || pdlFile.isSameFile(currentFile) || currentFile.getTopLevelTypeDeclaration() == this;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "(" + getName() + ")";
  }
}
