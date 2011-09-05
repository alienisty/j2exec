/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.commons.exec.util;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.commons.exec.environment.EnvironmentUtils;

public class MapUtilTest extends TestCase {
   /**
    * Test copying of map
    */
   public void testCopyMap() throws Exception {

      HashMap<String, String> procEnvironment = new HashMap<String, String>();
      procEnvironment.put("JAVA_HOME", "/usr/opt/java");

      Map<String, String> result = MapUtils.copy(procEnvironment);
      assertTrue(result.size() == 1);
      assertTrue(procEnvironment.size() == 1);
      assertEquals("/usr/opt/java", result.get("JAVA_HOME"));

      result.remove("JAVA_HOME");
      assertTrue(result.size() == 0);
      assertTrue(procEnvironment.size() == 1);
   }

   /**
    * Test merging of maps
    */
   public void testMergeMap() throws Exception {

      Map<String, String> procEnvironment = EnvironmentUtils
            .getProcEnvironment();
      HashMap<String, String> applicationEnvironment = new HashMap<String, String>();

      applicationEnvironment.put("appMainClass", "foo.bar.Main");
      Map<String, String> result = MapUtils.merge(procEnvironment,
            applicationEnvironment);
      assertTrue((procEnvironment.size() + applicationEnvironment.size()) == result
            .size());
      assertEquals("foo.bar.Main", result.get("appMainClass"));
   }

   /**
    * Test prefixing of map
    */
   public void testPrefixMap() throws Exception {

      HashMap<String, String> procEnvironment = new HashMap<String, String>();
      procEnvironment.put("JAVA_HOME", "/usr/opt/java");

      Map<String, String> result = MapUtils.prefix(procEnvironment, "env");
      assertTrue(procEnvironment.size() == result.size());
      assertEquals("/usr/opt/java", result.get("env.JAVA_HOME"));
   }
}