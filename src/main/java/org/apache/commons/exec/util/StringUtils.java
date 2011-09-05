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

import static java.io.File.separatorChar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Supplement of commons-lang, the stringSubstitution() was in a simpler
 * implementation available in an older commons-lang implementation.
 * 
 * This class is not part of the public API and could change without warning.
 * 
 * @author <a href="mailto:siegfried.goeschl@it20one.at">Siegfried Goeschl</a>
 */
public class StringUtils {

	private static final String SINGLE_QUOTE = "\'";
	private static final String DOUBLE_QUOTE = "\"";
	private static final char OTHER_SEPARATOR = separatorChar == '/' ? '\\'
			: '/';

	/**
	 * Perform a series of substitutions. The substitutions are performed by
	 * replacing ${variable} in the target string with the value of provided by
	 * the key "variable" in the provided hash table.
	 * <p/>
	 * <p/>
	 * A key consists of the following characters:
	 * <ul>
	 * <li>letter
	 * <li>digit
	 * <li>dot character
	 * <li>hyphen character
	 * <li>plus character
	 * <li>underscore character
	 * </ul>
	 * 
	 * @param str
	 *            the argument string to be processed
	 * @param vars
	 *            name/value pairs used for substitution
	 * @param isLenient
	 *            ignore a key not found in vars or throw a RuntimeException?
	 * @return String target string with replacements.
	 */
	public static String stringSubstitution(@NonNull String str,
			@NonNull Map<String, String> vars, boolean isLenient) {

		if (vars.isEmpty() && isLenient) {
			return str;
		}

		int strLength = str.length();
		StringBuilder result = new StringBuilder(strLength);
		
		parse: for (int cIdx = 0; cIdx < strLength;) {
			char ch;
			if ((ch = str.charAt(cIdx++)) == '$') {
				if ((ch = str.charAt(cIdx++)) == '{') { // look ahead
					int vStart = cIdx;
					while (++cIdx < strLength) {
						switch (ch = str.charAt(cIdx)) {
						case '}':
							String name = str.substring(vStart, cIdx++);
							String value = vars.get(name);
							if (value == null) {
								if (isLenient) {
									result.append("${").append(name)
											.append('}');
								} else {
									throw new RuntimeException(
											"No value found for : " + name);
								}
							} else {
								result.append(value);
							}
							continue parse;
						case '_':
						case '.':
						case '+':
						case '-':
							continue;
						default:
							if (Character.isLetterOrDigit(ch)) {
								continue;
							}
						}
						throw new RuntimeException("Syntax error at "
								+ (cIdx + 1) + " in \"" + str + "\"");
					}
					throw new RuntimeException("Delimiter not found for : "
							+ str.substring(vStart, cIdx));
				} else {
					result.append('$');
				}
			}
			result.append(ch);
		}

		return result.toString();
	}

	/**
	 * Split a string into an array of strings based on a separator.
	 * 
	 * @param input
	 *            what to split
	 * @param splitChar
	 *            what to split on
	 * @return the array of strings
	 */
	public static String[] split(String input, char splitChar) {
		List<String> strList = new ArrayList<String>();
		for (int begin = 0, end = 0, length = input.length(); end < length;) {
			if (input.charAt(end) == splitChar) {
				strList.add(input.substring(begin, end));
				end = begin = end + 1;
			} else
				end++;
		}
		return strList.toArray(new String[strList.size()]);
	}

	/**
	 * Fixes the file separator char for the target platform using the following
	 * replacement.
	 * 
	 * <ul>
	 * <li>'/' ==> File.separatorChar
	 * <li>'\\' ==> File.separatorChar
	 * </ul>
	 * 
	 * @param arg
	 *            the argument to fix
	 * @return the transformed argument
	 */
	public static String fixFileSeparatorChar(String arg) {
		return arg.replace(OTHER_SEPARATOR, separatorChar);
	}

	/**
	 * Concatenates an array of string using a separator.
	 * 
	 * @param strings
	 *            the strings to concatenate
	 * @param separator
	 *            the separator between two strings
	 * @return the concatenated strings
	 */
	public static String toString(String[] strings, String separator) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < strings.length; i++) {
			if (i > 0) {
				sb.append(separator);
			}
			sb.append(strings[i]);
		}
		return sb.toString();
	}

	/**
	 * Put quotes around the given String if necessary.
	 * <p>
	 * If the argument doesn't include spaces or quotes, return it as is. If it
	 * contains double quotes, use single quotes - else surround the argument by
	 * double quotes.
	 * </p>
	 * 
	 * @param argument
	 *            the argument to be quoted
	 * @return the quoted argument
	 * @throws IllegalArgumentException
	 *             If argument contains both types of quotes
	 */
	public static String quoteArgument(final String argument) {

		String cleanedArgument = argument.trim();

		// strip the quotes from both ends
		while (cleanedArgument.startsWith(SINGLE_QUOTE)
				|| cleanedArgument.startsWith(DOUBLE_QUOTE)) {
			cleanedArgument = cleanedArgument.substring(1);
		}

		while (cleanedArgument.endsWith(SINGLE_QUOTE)
				|| cleanedArgument.endsWith(DOUBLE_QUOTE)) {
			cleanedArgument = cleanedArgument.substring(0,
					cleanedArgument.length() - 1);
		}

		final StringBuffer buf = new StringBuffer();
		if (cleanedArgument.indexOf(DOUBLE_QUOTE) > -1) {
			if (cleanedArgument.indexOf(SINGLE_QUOTE) > -1) {
				throw new IllegalArgumentException(
						"Can't handle single and double quotes in same argument");
			} else {
				return buf.append(SINGLE_QUOTE).append(cleanedArgument)
						.append(SINGLE_QUOTE).toString();
			}
		} else if (cleanedArgument.indexOf(SINGLE_QUOTE) > -1
				|| cleanedArgument.indexOf(" ") > -1) {
			return buf.append(DOUBLE_QUOTE).append(cleanedArgument)
					.append(DOUBLE_QUOTE).toString();
		} else {
			return cleanedArgument;
		}
	}

	/**
	 * Determines if this is a quoted argument - either single or double quoted.
	 * 
	 * @param argument
	 *            the argument to check
	 * @return true when the argument is quoted
	 */
	public static boolean isQuoted(final String argument) {
		return (argument.startsWith(SINGLE_QUOTE) && argument
				.endsWith(SINGLE_QUOTE))
				|| (argument.startsWith(DOUBLE_QUOTE) && argument
						.endsWith(DOUBLE_QUOTE));
	}
}