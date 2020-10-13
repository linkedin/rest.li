package com.linkedin.intellij.pegasusplugin.psi;

import com.intellij.psi.tree.IElementType;
import com.intellij.util.containers.HashSet;
import com.linkedin.intellij.pegasusplugin.PdlLanguage;
import java.util.Set;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class PdlTokenType extends IElementType {
  public PdlTokenType(@NotNull @NonNls String debugName) {
    super(debugName, PdlLanguage.INSTANCE);
  }

  @Override
  public String toString() {
    return "PdlTokenType." + super.toString();
  }

  public static final String NULL = "null";
  public static final Set<String> PRIMITIVE_TYPES = new HashSet<>();
  static {
    PRIMITIVE_TYPES.add("string");
    PRIMITIVE_TYPES.add("boolean");
    PRIMITIVE_TYPES.add("int");
    PRIMITIVE_TYPES.add("long");
    PRIMITIVE_TYPES.add("float");
    PRIMITIVE_TYPES.add("double");
    PRIMITIVE_TYPES.add("bytes");
    PRIMITIVE_TYPES.add(NULL);
  }
}
