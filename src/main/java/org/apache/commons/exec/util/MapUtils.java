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
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Helper classes to manipulate maps to pass substition map to the CommandLine.
 * This class is not part of the public API and could change without warning.
 * 
 * @author <a href="mailto:siegfried.goeschl@it20one.at">Siegfried Goeschl</a>
 */
public class MapUtils {
	/**
	 * Clones a map.
	 * 
	 * @param source
	 *            the source map
	 * @return the clone of the source map
	 */
	public static <K, V> Map<K, V> copy(Map<K, V> source) {

		if (source == null) {
			return null;
		}
		if (source instanceof SortedMap<?, ?>) {
			return new TreeMap<K, V>((SortedMap<K, V>) source);
		}
		return new HashMap<K, V>(source);
	}

	/**
	 * Clones a map and prefixes the keys in the clone, e.g. for mapping
	 * "JAVA_HOME" to "env.JAVA_HOME" to simulate the behaviour of ANT.
	 * 
	 * @param source
	 *            the source map
	 * @param prefix
	 *            the prefix used for all names
	 * @return the clone of the source map
	 */
	public static <K, V> Map<String, V> prefix(Map<K, V> source, String prefix) {

		if (source == null) {
			return null;
		}

		Map<String, V> result = new HashMap<String, V>();

		for (Map.Entry<K, V> entry : source.entrySet()) {
			K key = entry.getKey();
			V value = entry.getValue();
			result.put(prefix + '.' + key.toString(), value);
		}

		return result;
	}

	/**
	 * Clones the lhs map and add all things from the rhs map.
	 * 
	 * @param lhs
	 *            the first map
	 * @param rhs
	 *            the second map
	 * @return the merged map
	 */
	public static <K,V> Map<K,V> merge(Map<K,V> lhs, Map<K,V> rhs) {

		Map<K,V> result;

		if ((lhs == null) || (lhs.isEmpty())) {
			result = copy(rhs);
		} else if ((rhs == null) || (rhs.isEmpty())) {
			result = copy(lhs);
		} else {
			result = copy(lhs);
			result.putAll(rhs);
		}

		return result;
	}
}
