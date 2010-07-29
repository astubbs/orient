/*
 * Copyright 1999-2010 Luca Garulli (l.garulli--at--orientechnologies.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.orientechnologies.orient.core.serialization.serializer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.Arrays;

public class OJSONReader {
	private BufferedReader			in;
	private int									cursor						= 0;
	private StringBuilder				buffer						= new StringBuilder();
	private String							value;
	private char								c;
	public static final char[]	DEFAULT_JUMP			= new char[] { ' ', '\r', '\n', '\t' };
	public static final char[]	BEGIN_OBJECT			= new char[] { '{' };
	public static final char[]	END_OBJECT				= new char[] { '}' };
	public static final char[]	FIELD_ASSIGNMENT	= new char[] { ':' };
	public static final char[]	BEGIN_STRING			= new char[] { '"' };
	public static final char[]	COMMA_SEPARATOR		= new char[] { ',' };
	public static final char[]	NEXT_IN_OBJECT		= new char[] { ',', '}' };
	public static final char[]	NEXT_IN_ARRAY			= new char[] { ',', ']' };
	public static final char[]	NEXT_OBJ_IN_ARRAY	= new char[] { '{', ']' };
	public static final char[]	ANY_NUMBER				= new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' };
	public static final char[]	BEGIN_COLLECTION	= new char[] { '[' };
	public static final char[]	END_COLLECTION		= new char[] { ']' };

	public OJSONReader(InputStreamReader iIn) {
		this.in = new BufferedReader(iIn);
	}

	public int getCursor() {
		return cursor;
	}

	public OJSONReader checkContent(final String iExpected) throws ParseException {
		if (!value.equals(iExpected))
			throw new ParseException("Expected content is " + iExpected + " but found " + value, cursor);
		return this;
	}

	public int readInteger(final char[] iUntil) throws IOException, ParseException {
		return readNumber(iUntil, false);
	}

	public int readNumber(final char[] iUntil, final boolean iInclude) throws IOException, ParseException {
		if (readNext(iUntil, iInclude) == null)
			throw new ParseException("Expected integer", cursor);

		return Integer.parseInt(value);
	}

	public String readString(final char[] iUntil) throws IOException, ParseException {
		return readString(iUntil, false);
	}

	public String readString(final char[] iUntil, final boolean iInclude) throws IOException, ParseException {
		if (readNext(iUntil, iInclude) == null)
			return null;

		if (value.startsWith("\"")) {
			return value.substring(1, value.lastIndexOf("\""));
		}

		return value;
	}

	public OJSONReader readNext(final char[] iUntil) throws IOException, ParseException {
		readNext(iUntil, false);
		return this;
	}

	public OJSONReader readNext(final char[] iUntil, final boolean iInclude) throws IOException, ParseException {
		readNext(iUntil, iInclude, DEFAULT_JUMP);
		return this;
	}

	public OJSONReader readNext(final char[] iUntil, final boolean iInclude, final char[] iJumpChars) throws IOException,
			ParseException {
		jump(iJumpChars);

		// READ WHILE THERE IS SOMETHING OF AVAILABLE
		int openBrackets = 0;
		boolean found;
		do {
			found = false;
			if (openBrackets == 0) {
				for (char u : iUntil) {
					if (u == c) {
						found = true;
						break;
					}
				}
			} else if (c == '}')
				openBrackets--;

			if (!found) {
				if (buffer.length() > 1 && c == '{')
					openBrackets++;

				c = nextChar();
				buffer.append(c);
			}
		} while (!found && in.ready());

		if (buffer.length() == 0)
			throw new ParseException("Expected characters '" + Arrays.toString(iUntil) + "' not found", cursor);

		if (!iInclude)
			buffer.setLength(buffer.length() - 1);

		value = buffer.toString();
		return this;
	}

	public char jump(final char[] iJumpChars) throws IOException, ParseException {
		buffer.setLength(0);

		// READ WHILE THERE IS SOMETHING OF AVAILABLE
		boolean go = true;
		while (go && in.ready()) {
			c = nextChar();

			go = false;
			for (char j : iJumpChars) {
				if (j == c) {
					go = true;
					break;
				}
			}
		}
		buffer.append(c);
		return c;
	}

	private char nextChar() throws IOException {
		c = (char) in.read();
		cursor++;
		return c;
	}

	public char lastChar() {
		return c;
	}

	public String getValue() {
		return value;
	}
}
