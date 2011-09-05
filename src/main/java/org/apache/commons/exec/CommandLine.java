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

package org.apache.commons.exec;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.exec.util.StringUtils;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * CommandLine objects help handling command lines specifying processes to
 * execute. The class can be used to a command line by an application.
 */
public class CommandLine {

	private ProcessBuilder builder;
	
	/**
	 * The arguments of the command.
	 */
	private final List<Argument> arguments = new ArrayList<Argument>();

	/**
	 * The program to execute.
	 */
	private final String executable;

	/**
	 * A map of name value pairs used to expand command line arguments
	 */
	private final Map<String, String> substitutionMap = new HashMap<String, String>();

	/**
	 * Create a command line from a string.
	 * 
	 * @param line
	 *            the first element becomes the executable, the rest the
	 *            arguments
	 * @return the parsed command line
	 * @throws IllegalArgumentException
	 *             If line is null or all whitespace
	 */
	public static CommandLine parse(final String line) {
		return parse(line, Collections.<String, String> emptyMap());
	}

	/**
	 * Create a command line from a string.
	 * 
	 * @param line
	 *            the first element becomes the executable, the rest the
	 *            arguments
	 * @param substitutionMap
	 *            the name/value pairs used for substitution
	 * @return the parsed command line
	 * @throws IllegalArgumentException
	 *             If line is null or all whitespace
	 */
	public static CommandLine parse(final String line,
			Map<String, String> substitutionMap) {

		if (line == null) {
			throw new IllegalArgumentException("Command line can not be null");
		} else if (line.trim().length() == 0) {
			throw new IllegalArgumentException("Command line can not be empty");
		} else {
			String[] tmp = translateCommandline(line);

			CommandLine cl = new CommandLine(tmp[0], substitutionMap);
			for (int i = 1; i < tmp.length; i++) {
				cl.addArgument(tmp[i]);
			}

			return cl;
		}
	}

	/**
	 * Create a command line without any arguments.
	 * 
	 * @param executable
	 *            the executable
	 */
	public CommandLine(String executable) {
		this(getExecutable(executable), Collections.<String, String> emptyMap());
	}

	/**
	 * Create a command line without any arguments.
	 * 
	 * @param executable
	 *            the executable file
	 */
	public CommandLine(File executable) {
		this.executable = getExecutable(executable.getAbsolutePath());
	}

	/**
	 * Copy constructor.
	 * 
	 * @param other
	 *            the instance to copy
	 */
	public CommandLine(CommandLine other) {
		this.executable = other.getExecutable();
		this.arguments.addAll(other.arguments);
		this.substitutionMap.putAll(other.substitutionMap);
	}

	public CommandLine(@NonNull String executable,
			@NonNull Map<String, String> substitutionMap) {
		this(getExecutable(executable), false, Collections
				.<Argument> emptyList(), substitutionMap);
	}

	private CommandLine(@NonNull String executable, boolean isFile,
			@NonNull List<Argument> arguments,
			@NonNull Map<String, String> substitutionMap) {
		this.executable = executable;
		this.substitutionMap.putAll(substitutionMap);
		this.arguments.addAll(arguments);
	}
	
	public void start() {
		
	}

	/**
	 * Returns the executable.
	 * 
	 * @return The executable
	 */
	public String getExecutable() {
		// Expand the executable and replace '/' and '\\' with the platform
		// specific file separator char. This is safe here since we know
		// that this is a platform specific command.
		return StringUtils.fixFileSeparatorChar(expandArgument(executable));
	}

	/**
	 * Add multiple arguments. Handles parsing of quotes and whitespace.
	 * 
	 * @param arguments
	 *            An array of arguments
	 * @return The command line itself
	 */
	public CommandLine addArguments(final String[] arguments) {
		return this.addArguments(arguments, true);
	}

	/**
	 * Add multiple arguments.
	 * 
	 * @param arguments
	 *            An array of arguments
	 * @param handleQuoting
	 *            Add the argument with/without handling quoting
	 * @return The command line itself
	 */
	public CommandLine addArguments(final String[] arguments,
			boolean handleQuoting) {
		if (arguments != null) {
			for (int i = 0; i < arguments.length; i++) {
				addArgument(arguments[i], handleQuoting);
			}
		}

		return this;
	}

	/**
	 * Add multiple arguments. Handles parsing of quotes and whitespace. Please
	 * note that the parsing can have undesired side-effects therefore it is
	 * recommended to build the command line incrementally.
	 * 
	 * @param arguments
	 *            An string containing multiple arguments.
	 * @return The command line itself
	 */
	public CommandLine addArguments(final String arguments) {
		return this.addArguments(arguments, true);
	}

	/**
	 * Add multiple arguments. Handles parsing of quotes and whitespace. Please
	 * note that the parsing can have undesired side-effects therefore it is
	 * recommended to build the command line incrementally.
	 * 
	 * @param arguments
	 *            An string containing multiple arguments.
	 * @param handleQuoting
	 *            Add the argument with/without handling quoting
	 * @return The command line itself
	 */
	public CommandLine addArguments(final String arguments,
			boolean handleQuoting) {
		if (arguments != null) {
			String[] argumentsArray = translateCommandline(arguments);
			addArguments(argumentsArray, handleQuoting);
		}

		return this;
	}

	/**
	 * Add a single argument. Handles quoting.
	 * 
	 * @param argument
	 *            The argument to add
	 * @return The command line itself
	 * @throws IllegalArgumentException
	 *             If argument contains both single and double quotes
	 */
	public CommandLine addArgument(final String argument) {
		return this.addArgument(argument, true);
	}

	/**
	 * Add a single argument.
	 * 
	 * @param argument
	 *            The argument to add
	 * @param handleQuoting
	 *            Add the argument with/without handling quoting
	 * @return The command line itself
	 */
	public CommandLine addArgument(final String argument, boolean handleQuoting) {

		if (argument == null) {
			return this;
		}

		// check if we can really quote the argument - if not throw an
		// IllegalArgumentException
		if (handleQuoting) {
			StringUtils.quoteArgument(argument);
		}

		arguments.add(new Argument(argument, handleQuoting));
		return this;
	}

	/**
	 * Returns the expanded and quoted command line arguments.
	 * 
	 * @return The quoted arguments
	 */
	public String[] getArguments() {

		Argument currArgument;
		String expandedArgument;
		String[] result = new String[arguments.size()];

		for (int i = 0; i < result.length; i++) {
			currArgument = (Argument) arguments.get(i);
			expandedArgument = expandArgument(currArgument.getValue());
			result[i] = (currArgument.isHandleQuoting() ? StringUtils
					.quoteArgument(expandedArgument) : expandedArgument);
		}

		return result;
	}

	/**
	 * @return the substitution map
	 */
	public Map<String, String> getSubstitutionMap() {
		return Collections.unmodifiableMap(substitutionMap);
	}

	/**
	 * Replaces all the substitutions with the ones specified.
	 * 
	 * @param substitutions
	 */
	public void setSubstitutions(@NonNull Map<String, String> substitutions) {
		substitutionMap.clear();
		substitutionMap.putAll(substitutions);
	}

	public void setSubstitution(@NonNull String key, @NonNull String value) {
		substitutionMap.put(key, value);
	}

	/**
	 * Returns the command line as an array of strings.
	 * 
	 * @return The command line as an string array
	 */
	public String[] toStrings() {
		final String[] result = new String[arguments.size() + 1];
		result[0] = this.getExecutable();
		System.arraycopy(getArguments(), 0, result, 1, result.length - 1);
		return result;
	}

	/**
	 * Stringify operator returns the command line as a string. Parameters are
	 * correctly quoted when containing a space or left untouched if the are
	 * already quoted.
	 * 
	 * @return the command line as single string
	 */
	public String toString() {
		return StringUtils.toString(toStrings(), " ");
	}

	// --- Implementation ---------------------------------------------------

	/**
	 * Expand variables in a command line argument.
	 * 
	 * @param argument
	 *            the argument
	 * @return the expanded string
	 */
	private String expandArgument(final String argument) {
		String string = StringUtils.stringSubstitution(argument,
				this.substitutionMap, true);
		return string;
	}

	/**
	 * Crack a command line.
	 * 
	 * @param toProcess
	 *            the command line to process
	 * @return the command line broken into strings. An empty or null toProcess
	 *         parameter results in a zero sized array
	 */
	private static String[] translateCommandline(final String toProcess) {
		if (toProcess == null || toProcess.length() == 0) {
			// no command? no string
			return new String[0];
		}

		// parse with a simple finite state machine

		final int normal = 0;
		final int inQuote = 1;
		final int inDoubleQuote = 2;
		int state = normal;
		StringTokenizer tok = new StringTokenizer(toProcess, "\"\' ", true);
		List<String> v = new ArrayList<String>();
		StringBuilder current = new StringBuilder();
		boolean lastTokenHasBeenQuoted = false;

		while (tok.hasMoreTokens()) {
			String nextTok = tok.nextToken();
			switch (state) {
			case inQuote:
				if ("\'".equals(nextTok)) {
					lastTokenHasBeenQuoted = true;
					state = normal;
				} else {
					current.append(nextTok);
				}
				break;
			case inDoubleQuote:
				if ("\"".equals(nextTok)) {
					lastTokenHasBeenQuoted = true;
					state = normal;
				} else {
					current.append(nextTok);
				}
				break;
			default:
				if ("\'".equals(nextTok)) {
					state = inQuote;
				} else if ("\"".equals(nextTok)) {
					state = inDoubleQuote;
				} else if (" ".equals(nextTok)) {
					if (lastTokenHasBeenQuoted || current.length() != 0) {
						v.add(current.toString());
						current.delete(0, current.length());
					}
				} else {
					current.append(nextTok);
				}
				lastTokenHasBeenQuoted = false;
				break;
			}
		}

		if (lastTokenHasBeenQuoted || current.length() != 0) {
			v.add(current.toString());
		}

		if (state == inQuote || state == inDoubleQuote) {
			throw new IllegalArgumentException("Unbalanced quotes in "
					+ toProcess);
		}

		return v.toArray(new String[v.size()]);
	}

	/**
	 * Get the executable - the argument is trimmed and '/' and '\\' are
	 * replaced with the platform specific file separator char
	 * 
	 * @param executable
	 *            the executable
	 * @return the platform-specific executable string
	 */
	private static String getExecutable(final @NonNull String executable) {
		if (executable.trim().isEmpty()) {
			throw new IllegalArgumentException("Executable can not be empty");
		} else {
			return StringUtils.fixFileSeparatorChar(executable);
		}
	}

	/**
	 * Encapsulates a command line argument.
	 */
	static class Argument {

		private final String value;
		private final boolean handleQuoting;

		private Argument(String value, boolean handleQuoting) {
			this.value = value.trim();
			this.handleQuoting = handleQuoting;
		}

		private String getValue() {
			return value;
		}

		private boolean isHandleQuoting() {
			return handleQuoting;
		}
	}
}
