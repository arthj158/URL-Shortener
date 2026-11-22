package dev.tinyurl.linkly.service;

public final class Base62 {
  private static final char[] DIGITS =
      "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

  private Base62() {}

  /**
   * Encodes the database's unique, monotonically assigned ID. Uniqueness follows from the primary
   * key, so no random-code collision loop is needed.
   */
  public static String encode(long value) {
    if (value < 0) throw new IllegalArgumentException("ID must not be negative");
    if (value == 0) return "0";
    StringBuilder out = new StringBuilder();
    while (value > 0) {
      out.append(DIGITS[(int) (value % 62)]);
      value /= 62;
    }
    return out.reverse().toString();
  }
}
