// Copyright 2016 Google Inc. All Rights Reserved.
package com.google.copybara;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.copybara.config.ConfigValidationException;
import com.google.copybara.doc.annotations.DocElement;
import com.google.copybara.doc.annotations.DocField;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents the authors mapping between an origin and a destination.
 *
 * <p>For a given author in the origin, always provides an author in the destination.
 */
final class Authoring {

  private final String defaultAuthor;
  private final AuthoringMappingMode mode;
  private final ImmutableSet<String> whitelist;

  Authoring(
      String defaultAuthor, AuthoringMappingMode mode, ImmutableSet<String> whitelist) {
    this.defaultAuthor = Preconditions.checkNotNull(defaultAuthor);
    this.mode = Preconditions.checkNotNull(mode);
    this.whitelist = Preconditions.checkNotNull(whitelist);
  }

  /**
   * Returns the default author for squash workflows where there is more than one author.
   */
  public String getDefaultAuthor() {
    return defaultAuthor;
  }

  String getDestinationAuthor(String originAuthor) {
    switch (mode) {
      case PASS_THRU:
        return originAuthor;
      case USE_DEFAULT:
        return defaultAuthor;
      case WHITELIST:
        return getWhitelistedAuthor(originAuthor);
      default:
        throw new IllegalStateException(String.format("Mode '%s' not implemented.", mode));
    }
  }

  private String getWhitelistedAuthor(String originAuthor) {
    if (!whitelist.contains(originAuthor)) {
      return defaultAuthor;
    }
    return originAuthor;
  }

  /**
   * Config builder used by YAML.
   */
  @DocElement(yamlName = "!Authoring",
      description = "Defines the authoring mapping between the origin and destination of the "
          + "workflow.",
      elementKind = Authoring.class)
  public static final class Yaml {

    private String defaultAuthor;
    private ImmutableSet<String> whitelist = ImmutableSet.of();
    private AuthoringMappingMode mode = AuthoringMappingMode.USE_DEFAULT;


    /**
     * Sets the default author for commits in the destination.
     *
     * <p>This field cannot be empty, so there is always an author that can be used in the
     * destination in case there is no mapping for an individual.
     */
    @DocField(description = "Sets the default author for commits in the destination.",
        required = true)
    public void setDefaultAuthor(String defaultAuthor) throws ConfigValidationException {
      this.defaultAuthor = defaultAuthor;
    }

    @DocField(description = "Mode used for author mapping from origin to destination.",
        required = false, defaultValue = "USE_DEFAULT")
    public void setMode(AuthoringMappingMode mode) {
      this.mode = mode;
    }

    /**
     * Sets the mapping of whitelisted authors from origin to destination.
     *
     * TODO(danielromero): Load this mapping from an external file.
     */
    @DocField(description = "List of whitelisted authors in the origin. "
        + "The authors must be unique.", required = false)
    public void setWhitelist(List<String> whitelist) throws ConfigValidationException {
      Set<String> uniqueAuthors = new HashSet<>();
      for (String author : whitelist) {
        if (!uniqueAuthors.add(author)) {
          throw new ConfigValidationException(
              String.format("Duplicated whitelist entry '%s'", author));
        }
      }
      this.whitelist = ImmutableSet.copyOf(whitelist);
    }

    public Authoring withOptions(Options options, String configName)
        throws ConfigValidationException, EnvironmentException {
      if (Strings.isNullOrEmpty(defaultAuthor)) {
        throw new ConfigValidationException("Field 'defaultAuthor' cannot be empty.");
      }
      if (mode == AuthoringMappingMode.WHITELIST && whitelist.isEmpty()) {
        throw new ConfigValidationException(
            "Mode 'WHITELIST' requires a non-empty 'whitelist' field. "
                + "For default mapping, use 'USE_DEFAULT' mode instead.");
      }
      return new Authoring(defaultAuthor, mode, whitelist);
    }
  }

  /**
   * Mode used for author mapping from origin to destination.
   */
  public enum AuthoringMappingMode {
    /**
     * Use the default author for all the submits in the destination.
     */
    @DocField(description = "Use the default author for all the submits in the destination.")
    USE_DEFAULT,
    /**
     * Use the origin author as the author in the destination, no whitelisting.
     */
    @DocField(description =
        "Use the origin author as the author in the destination, no whitelisting.")
    PASS_THRU,
    /**
     * Use the whitelist map to translate origin authors to destination. Use the default author for
     * non-whitelisted authors.
     */
    @DocField(description = "Use the whitelist map to translate origin authors to destination. "
        + "Use the default author for non-whitelisted authors.")
    WHITELIST
  }
}