package com.alorma.github.explore.parser;

import java.util.Date;
import java.util.List;

/**
 * An item in the feed.  This represents an RSS item or an Atom entry.
 */
public interface Item extends Element {

  /**
   * Returns the feed type.
   */
  FeedType getType();

  /**
   * Convenience method to retrieve the title.
   */
  String getTitle();

  /**
   * Convenience method to retrieve the link.
   */
  String getLink();

  /**
   * Convenience method to retrieve the description.
   */
  String getDescription();

  /**
   * Convenience method to retrieve the author.
   */
  String getAuthor();

  /**
   * Convenience method to retrieve the guid.
   */
  String getGuid();

  /**
   * Convenience method to retrieve the published date.
   */
  Date getPubDate();

  /**
   * Convenience method to retrieve a list of categories.
   */
  List<String> getCategories();

  /**
   * Indicates whether the specified object is equal to this Item based on
   * its unique identifier.
   */
  boolean equals(Object obj);

  /**
   * Returns a hash code based on the unique identifier referenced in the
   * equals() method.
   */
  int hashCode();
}
