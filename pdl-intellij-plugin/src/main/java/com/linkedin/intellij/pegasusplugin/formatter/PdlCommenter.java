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

package com.linkedin.intellij.pegasusplugin.formatter;

import com.intellij.lang.ASTNode;
import com.intellij.lang.CodeDocumentationAwareCommenterEx;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.linkedin.intellij.pegasusplugin.psi.PdlElementType;
import com.linkedin.intellij.pegasusplugin.psi.Types;
import com.linkedin.intellij.pegasusplugin.schemadoc.psi.PsiSchemadocElement;
import com.linkedin.intellij.pegasusplugin.schemadoc.psi.SchemadocTypes;
import org.jetbrains.annotations.Nullable;


/**
 * Register all comment styles supported by PDL: single line, multiline and documentation comments.
 */
public class PdlCommenter implements CodeDocumentationAwareCommenterEx {
  public PdlCommenter() { }

  @Nullable
  @Override
  public String getLineCommentPrefix() {
    return "//";
  }

  @Nullable
  @Override
  public String getBlockCommentPrefix() {
    return "/*";
  }

  @Nullable
  @Override
  public String getBlockCommentSuffix() {
    return "*/";
  }

  @Nullable
  @Override
  public String getCommentedBlockCommentPrefix() {
    return null;
  }

  @Nullable
  @Override
  public String getCommentedBlockCommentSuffix() {
    return null;
  }

  @Nullable
  @Override
  public IElementType getLineCommentTokenType() {
    return Types.LINE_COMMENT;
  }

  @Nullable
  @Override
  public IElementType getBlockCommentTokenType() {
    return Types.BLOCK_COMMENT;
  }

  @Nullable
  @Override
  public IElementType getDocumentationCommentTokenType() {
    return PdlElementType.DOC_COMMENT;
  }

  @Nullable
  @Override
  public String getDocumentationCommentPrefix() {
    return "/**";
  }

  @Nullable
  @Override
  public String getDocumentationCommentLinePrefix() {
    return "*";
  }

  @Nullable
  @Override
  public String getDocumentationCommentSuffix() {
    return "*/";
  }

  @Override
  public boolean isDocumentationComment(PsiComment comment) {
    return (comment instanceof PsiSchemadocElement);
  }

  @Override
  public boolean isDocumentationCommentText(PsiElement element) {
    if (element == null) {
      return false;
    }
    final ASTNode node = element.getNode();
    return node != null && node.getElementType() == SchemadocTypes.DOC_COMMENT_CONTENT;
  }
}
