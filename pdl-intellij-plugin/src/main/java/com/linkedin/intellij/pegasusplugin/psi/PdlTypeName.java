package com.linkedin.intellij.pegasusplugin.psi;

import java.util.HashSet;
import java.util.Set;
import org.jetbrains.annotations.NotNull;


/**
 * A namespaced pegasus type name.
 */
public class PdlTypeName implements Comparable<PdlTypeName> {
  private final String _fullname;

  public static PdlTypeName decode(String unescapedFullname) {
    return new PdlTypeName(escape(unescapedFullname));
  }

  public static PdlTypeName decode(String unescapedNamespace, String unescapedName) {
    return new PdlTypeName(escape(unescapedNamespace), escape(unescapedName));
  }

  public static boolean isPrimitive(String name) {
    return (PdlTokenType.PRIMITIVE_TYPES.contains(escape(name)));
  }

  public static String escape(String name) {
    return name.replaceAll("`", "");
  }

  private static final Set<String> KEYWORDS;
  static {
    KEYWORDS = new HashSet<>();
    KEYWORDS.add("namespace");
    KEYWORDS.add("import");
    KEYWORDS.add("includes");
    KEYWORDS.add("record");
    KEYWORDS.add("enum");
    KEYWORDS.add("fixed");
    KEYWORDS.add("typeref");
    KEYWORDS.add("union");
    KEYWORDS.add("map");
    KEYWORDS.add("array");
    KEYWORDS.add("boolean");
    KEYWORDS.add("int");
    KEYWORDS.add("long");
    KEYWORDS.add("double");
    KEYWORDS.add("float");
    KEYWORDS.add("string");
    KEYWORDS.add("bytes");
    KEYWORDS.add("null");
    KEYWORDS.add("true");
    KEYWORDS.add("false");
  }

  public static String unescape(String name) {
    String[] parts = name.split("\\.");
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < parts.length; i++) {
      builder.append(unescapeIdentifier(parts[i]));
      if (i < parts.length - 1) {
        builder.append(".");
      }
    }
    return builder.toString();
  }

  private static String unescapeIdentifier(String name) {
    if (KEYWORDS.contains(name)) {
      return "`" + name + "`";
    } else {
      return name;
    }
  }

  public PdlTypeName(String fullname) {
    this._fullname = fullname;
  }

  public PdlTypeName(String namespace, String name) {
    if (namespace == null || namespace.isEmpty()) {
      this._fullname = name;
    } else {
      this._fullname = namespace + "." + name;
    }
  }

  public String getName() {
    int idx = _fullname.lastIndexOf('.');
    if (idx > 0 && idx < _fullname.length() - 2) {
      return _fullname.substring(idx + 1);
    } else {
      return _fullname;
    }
  }

  @NotNull
  public String getNamespace() {
    int idx = _fullname.lastIndexOf('.');
    if (idx > 1) {
      return _fullname.substring(0, idx);
    } else {
      return "";
    }
  }

  public boolean isPrimitive() {
    return (PdlTokenType.PRIMITIVE_TYPES.contains(_fullname));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    PdlTypeName typeName = (PdlTypeName) o;

    return !(_fullname != null ? !_fullname.equals(typeName._fullname) : typeName._fullname != null);
  }

  @Override
  public int hashCode() {
    return _fullname != null ? _fullname.hashCode() : 0;
  }

  @Override
  public String toString() {
    return _fullname;
  }

  public String unescape() {
    return unescape(_fullname);
  }

  @Override
  public int compareTo(PdlTypeName o) {
    if (o == null) {
      return 1;
    }
    return this._fullname.compareTo(o._fullname);
  }
}
