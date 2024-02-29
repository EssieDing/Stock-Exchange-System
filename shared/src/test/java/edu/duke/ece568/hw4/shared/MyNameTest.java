package edu.duke.ece568.hw4.shared;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class MyNameTest {
  @Test
  public void test_getName() {
    assertEquals("teamX", MyName.getName());
  }

}
