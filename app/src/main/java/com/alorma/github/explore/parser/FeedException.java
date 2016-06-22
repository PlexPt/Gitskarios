package com.alorma.github.explore.parser;

/**
 * A general exception with the feed.
 */
public class FeedException extends Exception {

  public FeedException() {
  }

  public FeedException(String message) {
    super(message);
  }

  public FeedException(Throwable cause) {
    super(cause);
  }

  public FeedException(String message, Throwable cause) {
    super(message, cause);
  }
}
