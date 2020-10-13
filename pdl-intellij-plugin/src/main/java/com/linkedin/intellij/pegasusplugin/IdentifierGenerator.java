package com.linkedin.intellij.pegasusplugin;

import java.util.HashSet;
import java.util.Set;


/**
 * Useful class for generating unique identifiers.
 */
public class IdentifierGenerator {
  private final String _baseName;
  private final Set<String> _usedIdentifiers;
  private int _counter;

  /**
   * Constructs a generator that will produce unique identifiers with a given prefix that don't conflict with a given
   * blacklist.
   * @param baseName identifier prefix
   * @param blacklist identifier blacklist
   */
  public IdentifierGenerator(String baseName, Set<String> blacklist) {
    _baseName = baseName;
    _usedIdentifiers = new HashSet<>(blacklist);
    _counter = 0;
  }

  /**
   * Returns the next unique identifier.
   * @return a unique identifier
   */
  public String next() {
    String next;
    do {
      next = _baseName + _counter++;
    } while (_usedIdentifiers.contains(next));

    _usedIdentifiers.add(next);

    return next;
  }
}
