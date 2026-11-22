package dev.tinyurl.linkly.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class Base62Test {
  @Test
  void encodesBoundariesAndLargeValues() {
    assertEquals("0", Base62.encode(0));
    assertEquals("Z", Base62.encode(61));
    assertEquals("10", Base62.encode(62));
    assertEquals("w7e", Base62.encode(123456));
    assertThrows(IllegalArgumentException.class, () -> Base62.encode(-1));
  }
}
