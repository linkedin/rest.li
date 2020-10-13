package com.linkedin.intellij.pegasusplugin.completion;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.PlatformIcons;
import com.intellij.util.ProcessingContext;
import com.linkedin.intellij.pegasusplugin.PdlIcons;
import com.linkedin.intellij.pegasusplugin.PdlTreeUtil;
import com.linkedin.intellij.pegasusplugin.pdsc.PdscReferenceUtils;
import com.linkedin.intellij.pegasusplugin.psi.PdlFullnameStubIndex;
import com.linkedin.intellij.pegasusplugin.psi.PdlImportDeclaration;
import com.linkedin.intellij.pegasusplugin.psi.PdlNamedElementImportBase;
import com.linkedin.intellij.pegasusplugin.psi.PdlNamedElementTypeReference;
import com.linkedin.intellij.pegasusplugin.psi.PdlPsiFile;
import com.linkedin.intellij.pegasusplugin.psi.PdlTypeName;
import com.linkedin.intellij.pegasusplugin.psi.PdlTypeReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;


/**
 * Completion provider for type references that supports fully qualified names as well as simple names. The provider is
 * "package aware" in that typing a package name will suggest both subpackages and types within that package.
 *
 * This provider also support "package-less" completions, which are lookup elements to be suggested if no package is
 * specified. By default, all types accessible from this file are suggested during "package-less" completion using the
 * insert handler {@link SimpleTypeNameInsertHandler}.
 *
 * Subclasses of this provider have the ability to add custom "package-less" lookup elements and to apply custom logic
 * to filter out which types are included in the completion set.
 *
 * TODO(evwillia): The logic here attempts to duplicate the Java-like FQN completion, though it's a bit complicated.
 *                 Given more time, I'd figure out a more proper way to do this.
 * TODO(evwillia): Figure out how to alphabetize lookup elements.
 */
public abstract class FullyQualifiedNameCompletionProvider extends CompletionProvider<CompletionParameters> {
  public void addCompletions(@NotNull CompletionParameters parameters,
      @NotNull ProcessingContext context,
      @NotNull CompletionResultSet resultSet) {
    final PsiElement psiElement = parameters.getPosition();
    if (!(psiElement.getContainingFile() instanceof PdlPsiFile)) {
      return;
    }
    final PdlPsiFile pdlFile = (PdlPsiFile) psiElement.getContainingFile();
    final Project project = parameters.getEditor().getProject();
    if (project == null) {
      return;
    }

    // Get the "base package" (e.g. "com.x." for "com.x.foo<caret>")
    PsiElement thisSegment = parameters.getPosition();
    PsiElement node = thisSegment.getParent().getFirstChild();
    StringBuilder sb = new StringBuilder();
    while (node != null && node != thisSegment) {
      sb.append(node.getText());
      node = node.getNextSibling();
    }
    String basePackage = sb.toString();

    Collection<String> allFullnames = new ArrayList<>();

    // Get all known PDL full names
    PdlFullnameStubIndex fullnameIndex = PdlFullnameStubIndex.getInstance();
    fullnameIndex.getAllKeys(project)
        .stream()
        .filter(Objects::nonNull)
        .forEach(allFullnames::add);

    // Get all known PDSC full names, identified by filename and relative path to source root
    GlobalSearchScope scope = GlobalSearchScope.allScope(project);
    Set<String> pdscSet = new HashSet<>();
    PdscReferenceUtils.findPdscFiles(project, scope)
        .stream()
        .filter(Objects::nonNull)
        .map(PdlTypeName::toString)
        .filter(Objects::nonNull)
        .forEach(pdscSet::add);
    allFullnames.addAll(pdscSet);

    // Do filtering of master PDL/PDSC fullname list
    allFullnames = allFullnames.stream()
        // Filter using custom subclass filtering logic
        .filter(fullname -> filterFullname(fullname, pdlFile))
        // Allow only types that are accessible from this file (PDSC types exempt).
        .filter(fullname -> pdscSet.contains(fullname) || fullnameIndex.get(fullname, project, scope)
                .stream()
                .anyMatch(pdlTypeNameDeclaration -> pdlTypeNameDeclaration.isAccessibleFrom(pdlFile)))
        .collect(Collectors.toList());

    // Compile all the lookup elements based on the existing base package
    allFullnames.stream()
        // Filter out root (i.e. namespace-less) types, otherwise the completion set would contain duplicates
        // because the FQN and the simple name would be the same.
        .filter(fullname -> fullname.contains("."))
        // Allow only types containing the base package as a prefix
        .filter(fullname -> fullname.indexOf(basePackage) == 0)
        // Trim off the base package prefix
        .map(fullname -> fullname.substring(basePackage.length()))
        // Trim off all subsequent segments but keep the subsequent dot
        .map(fullname -> {
          int dotIndex = fullname.indexOf(".");
          if (dotIndex == -1) {
            return fullname;
          }
          return fullname.substring(0, dotIndex + 1);
        })
        // For each remaining segment, add a package/type lookup
        .forEach(text -> {
          LookupElementBuilder builder = LookupElementBuilder.create(text);
          if (text.contains(".")) {
            // Add package segment lookup
            resultSet.addElement(packageLookupElement(builder.withInsertHandler(new PackageSegmentInsertHandler())));
          } else {
            // Add type lookup
            if (basePackage.contains(".")) {
              builder = builder.withTailText(" (" + basePackage.substring(0, basePackage.lastIndexOf(".")) + ")", true);
            }
            resultSet.addElement(fileLookupElement(builder));
          }
        });

    // If no base package, add "package-less" completions
    if (basePackage.isEmpty()) {
      // Add lookup elements for all PDL and PDSC types
      for (String fullname : allFullnames) {
        LookupElementBuilder builder = createTypeLookupBuilder(new PdlTypeName(fullname))
            .withInsertHandler(new SimpleTypeNameInsertHandler(psiElement));
        resultSet.addElement(fileLookupElement(builder));
      }

      // Check extension point for custom "package-less" completions
      addPackagelessCompletions(parameters, context, resultSet);
    }
  }

  /**
   * Extension point for adding "package-less" completions, which are lookup elements to be suggested should no package
   * be specified.
   * @param parameters completion parameters
   * @param context processing context
   * @param resultSet result set to which the lookup elements should be added
   */
  protected abstract void addPackagelessCompletions(@NotNull CompletionParameters parameters,
      @NotNull ProcessingContext context,
      @NotNull CompletionResultSet resultSet);

  /**
   * Extension point for filtering full names to be used in this completion.
   * @param fullname fullname to be filtered
   * @param pdlFile current file
   * @return True if this fullname should be included in this completion.
   */
  protected abstract boolean filterFullname(@NotNull String fullname, @NotNull PdlPsiFile pdlFile);

  /**
   * Helper method for generating a package lookup element from an existing lookup element builder.
   * Package lookup elements are rendered at the top of the completion list and have a special icon.
   * @param builder existing {@link LookupElementBuilder}
   * @return package lookup element
   */
  protected static LookupElement packageLookupElement(LookupElementBuilder builder) {
    return PrioritizedLookupElement.withPriority(builder.withIcon(PlatformIcons.PACKAGE_ICON), 3);
  }

  /**
   * Helper method for generating a file lookup element from an existing lookup element builder.
   * File lookup elements are rendered in the middle of the completion list, have a special icon, and are bolded.
   * @param builder existing {@link LookupElementBuilder}
   * @return file lookup element
   */
  protected LookupElement fileLookupElement(LookupElementBuilder builder) {
    return PrioritizedLookupElement.withPriority(builder.withIcon(PdlIcons.FILE).withBoldness(true), 2);
  }

  /**
   * Helper method for generating a primitive lookup element from an existing lookup element builder.
   * Primitive lookup elements are rendered at the bottom of the completion list.
   * @param builder existing {@link LookupElementBuilder}
   * @return primitive lookup element
   */
  protected LookupElement primitiveLookupElement(LookupElementBuilder builder) {
    return PrioritizedLookupElement.withPriority(builder, 1);
  }

  /**
   * Creates a lookup element builder for the given type name.
   * @param typeName type name
   * @return lookup element builder
   */
  private static LookupElementBuilder createTypeLookupBuilder(PdlTypeName typeName) {
    String simpleName = typeName.getName();
    LookupElementBuilder builder = LookupElementBuilder.create(typeName, simpleName);
    return builder.withTailText(" (" + typeName.getNamespace() + ")", true);
  }

  /**
   * Insert handler which automatically re-opens the completion pop-up on insert.
   */
  private static class PackageSegmentInsertHandler implements InsertHandler<LookupElement> {
    @Override
    public void handleInsert(InsertionContext context, @NotNull LookupElement declaration) {
      AutoPopupController.getInstance(context.getProject()).scheduleAutoPopup(context.getEditor());
    }
  }

  /**
   * Insert handler for simple type names suggested during "package-less" completion. This handler has logic to handle
   * both PDL and PDSC type references, and it also can determine when simple names, simple names with imports, and
   * fully qualified names are appropriate to use.
   */
  private static class SimpleTypeNameInsertHandler implements  InsertHandler<LookupElement> {
    private PsiElement _location;

    /**
     * @param location where this type reference exists in the syntax tree
     */
    SimpleTypeNameInsertHandler(PsiElement location) {
      _location = location;
    }

    @Override
    public void handleInsert(@NotNull InsertionContext context, @NotNull LookupElement lookupElement) {
      if (!(context.getFile() instanceof PdlPsiFile)) {
        return;
      }
      PdlPsiFile pdlFile = (PdlPsiFile) context.getFile();
      if (!(lookupElement.getObject() instanceof PdlTypeName)) {
        return;
      }
      PdlTypeName typeName = (PdlTypeName) lookupElement.getObject();

      // If this is for an import declaration, always replace the inserted simple name with the corresponding fullname
      if (PsiTreeUtil.getParentOfType(_location, PdlImportDeclaration.class) != null) {
        insertFullname(typeName, context);
        return;
      }

      // Check if an existing import already exists under this simple name
      PdlTypeName existingImportTypeName = pdlFile.lookupImport(typeName.getName());

      // No existing import matching the simple name of the type to insert.
      if (existingImportTypeName == null) {
        // Since there is no conflicting import, check if simple name without an import is sufficient
        if (isNotCrossNamespace(typeName, _location)) {
          return;
        }
        // Add an import for this type if not prohibited, otherwise just write the fullname
        // As imports take precedence, we should not add the import if there is any simple reference with the same
        // name as the import.
        if (!checkConflictWithSimpleReference(typeName)
            && PdlNamedElementImportBase.getImportError(pdlFile, typeName) == null) {
          pdlFile.addImport(typeName);
        } else {
          insertFullname(typeName, context);
        }
      } else if (!existingImportTypeName.equals(typeName)) {
        // If existing import is for a different type, write the fullname to avoid referring to an incorrect import
        insertFullname(typeName, context);
      }
      // Fall through case is to do nothing as an existing import matches the type to insert.
    }

    /**
     * Check if adding this import can cause conflict with any simple type references in the file.
     *
     * @param typeNameToImport Type name that is considered for import.
     * @return true if adding an import for typeNameToImport would conflict with a simple reference already in the
     * file.
     */
    private boolean checkConflictWithSimpleReference(PdlTypeName typeNameToImport) {
      return ((PdlPsiFile) _location.getContainingFile()).getTypeReferences().stream()
          .filter(PdlNamedElementTypeReference::isSimpleReference)
          .map(PdlTypeReference::getName)
          .anyMatch(typeNameToImport.getName()::equals);
    }

    /**
     * Return true if this type name references the namespace in which it resides.
     * @param typeName type name of the reference
     * @param location location of the type reference
     * @return whether this type name is not cross-namespace
     */
    private boolean isNotCrossNamespace(PdlTypeName typeName, PsiElement location) {
      String referenceNamespace = typeName.getNamespace();
      String localNamespace = PdlTreeUtil.getNamespaceAtElement(location).getText();

      return referenceNamespace.equals(localNamespace);
    }

    /**
     * Replaces the inserted simple name text with the corresponding full name text.
     * @param typeName full type name to be inserted
     * @param context insertion context
     */
    private void insertFullname(PdlTypeName typeName, InsertionContext context) {
      int start = context.getStartOffset();
      int end = context.getTailOffset();
      Document document = context.getDocument();
      document.replaceString(start, end, typeName.toString());
    }
  }
}
