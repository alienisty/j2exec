package com.j2speed.exec;

import static org.junit.Assert.*;

import org.junit.Test;

import com.j2speed.exec.impl.Argument;

public class ArgumentTest {

   @Test
   public void testGetIndex() {
      Argument argument = new Argument("", "", 1);
      assertEquals(1, argument.getIndex());
   }

   @Test
   public void testApplyNoPostfix() {
      Argument argument = new Argument("-d=", "", 1);
      assertEquals("-d=value", argument.apply("value"));
      assertEquals("-d=12", argument.apply(12));
   }
}
