package com.linkedin.intellij.pegasusplugin.psi;

import com.linkedin.intellij.pegasusplugin.PdlImportError;
import org.jetbrains.annotations.Nullable;


/**
 * Models an import.
 */
public interface PdlNamedElementImport extends PdlNamedElement {
  /**
   * Checks if the import is in use.
   * @return true if the import is used in the containing .pdl file.
   */
  boolean isUsed();

  /**
   * Returns a {@link PdlImportError} describing this import if this import is prohibited.
   * @return import error, or null if there is none
   */
  @Nullable
  PdlImportError getImportError();
}
