package com.linkedin.intellij.pegasusplugin.completion;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.patterns.PlatformPatterns;
import com.linkedin.intellij.pegasusplugin.PdlLanguage;
import com.linkedin.intellij.pegasusplugin.psi.PdlFullyQualifiedName;
import com.linkedin.intellij.pegasusplugin.psi.PdlImportDeclaration;
import com.linkedin.intellij.pegasusplugin.psi.PdlTypeReference;
import com.linkedin.intellij.pegasusplugin.psi.Types;


/**
 * Provides autocomplete for PDL types and PDSC types, including auto-import support.
 */
public class PdlCompletionContributor extends CompletionContributor {
  public PdlCompletionContributor() {
    extend(CompletionType.BASIC,
        PlatformPatterns.psiElement(Types.IDENTIFIER)
            .withParent(PdlFullyQualifiedName.class)
            .withSuperParent(2, PdlTypeReference.class)
            .withLanguage(PdlLanguage.INSTANCE),
        new TypeReferenceCompletionProvider());

    extend(CompletionType.BASIC,
        PlatformPatterns.psiElement(Types.IDENTIFIER)
            .withParent(PdlFullyQualifiedName.class)
            .withSuperParent(2, PdlImportDeclaration.class)
            .withLanguage(PdlLanguage.INSTANCE),
        new ImportDeclarationCompletionProvider());
  }
}
