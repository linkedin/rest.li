package com.linkedin.intellij.pegasusplugin.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.util.ProcessingContext;
import com.linkedin.intellij.pegasusplugin.psi.PdlPsiFile;
import com.linkedin.intellij.pegasusplugin.psi.PdlTokenType;
import org.jetbrains.annotations.NotNull;


/**
 * Completion provider for type references. Utilizes the functionality of {@link FullyQualifiedNameCompletionProvider}
 * to support typing FQNs and simple names, but also extends this functionality by suggesting primitive types.
 */
public class TypeReferenceCompletionProvider extends FullyQualifiedNameCompletionProvider {
  @Override
  public void addPackagelessCompletions(@NotNull CompletionParameters parameters,
      @NotNull ProcessingContext context,
      @NotNull CompletionResultSet resultSet) {

    // Primitive types
    for (String primitiveType : PdlTokenType.PRIMITIVE_TYPES) {
      resultSet.addElement(primitiveLookupElement(LookupElementBuilder.create(primitiveType)));
    }
  }

  @Override
  protected boolean filterFullname(@NotNull String fullname, @NotNull PdlPsiFile pdlFile) {
    return true;
  }
}
