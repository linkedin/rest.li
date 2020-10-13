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

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * Provides IntelliJ reference support for all by-name references in PDL.
 */
public class PdscReference extends PsiReferenceBase<PsiElement> {

  private final PsiFile _target;

  public PdscReference(@NotNull PsiElement element, PsiFile target, TextRange textRange) {
    super(element, textRange);
    this._target = target;
  }

  @Nullable
  @Override
  public PsiElement resolve() {
    return _target;
  }

  @Override
  public boolean isReferenceTo(PsiElement element) {
    if (!(element instanceof PsiFile)) {
      return false;
    }
    PsiElement resolved = resolve();
    if (resolved != null) {
      return resolved.equals(element);
    }
    return false;
  }

  @Override
  public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
    throw new IncorrectOperationException("Unable to rename " + newElementName + " type declared in a .pdsc file");
  }

  @NotNull
  @Override
  public Object[] getVariants() {

    return new Object[] {};
  }
}
