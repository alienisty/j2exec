/* 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.exec.environment;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.exec.OS;

/**
 * Helper class to determine the environment variable for the OS. Depending on the JDK the
 * environment variables can be either retrieved directly from the JVM or requires starting a
 * process to get them running an OS command line.
 */
public class DefaultProcessingEnvironment {

   /** the environment variables of the process */
   protected Map<String, String> procEnvironment;

   /**
    * Find the list of environment variables for this process.
    * 
    * @return a map containing the environment variables
    * @throws IOException
    *            obtaining the environment variables failed
    */
   public synchronized Map<String, String> getProcEnvironment() throws IOException {

      if (procEnvironment == null) {
         procEnvironment = this.createProcEnvironment();
      }

      // create a copy of the map just in case that
      // anyone is going to modifiy it, e.g. removing
      // or setting an evironment variable
      Map<String, String> copy = createEnvironmentMap();
      copy.putAll(procEnvironment);
      return copy;
   }

   /**
    * Find the list of environment variables for this process.
    * 
    * @return a amp containing the environment variables
    * @throws IOException
    *            the operation failed
    * @throws SecurityException
    *            if a security manager exists and its {@link SecurityManager#checkPermission
    *            checkPermission} method doesn't allow access to the process environment
    */
   protected Map<String, String> createProcEnvironment() throws IOException {
      if (procEnvironment == null) {
         Map<String, String> env = System.getenv();
         procEnvironment = createEnvironmentMap();
         procEnvironment.putAll(env);
      }

      return procEnvironment;
   }

   /**
    * Creates a map that obeys the casing rules of the current platform for key lookup. E.g. on a
    * Windows platform, the map keys will be case-insensitive.
    * 
    * @return The map for storage of environment variables, never <code>null</code>.
    */
   private Map<String, String> createEnvironmentMap() {
      if (OS.isFamilyWindows()) {
         return new TreeMap<String, String>(new Comparator<String>() {
            public int compare(String key0, String key1) {
               return key0.compareToIgnoreCase(key1);
            }
         });
      } else {
         return new HashMap<String, String>();
      }
   }

}
