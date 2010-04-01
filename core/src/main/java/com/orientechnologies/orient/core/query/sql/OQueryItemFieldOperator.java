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
package com.orientechnologies.orient.core.query.sql;

public enum OQueryItemFieldOperator {
	SIZE(0, "SIZE"), LENGTH(1, "LENGTH"), TOUPPERCASE(2, "TOUPPERCASE"), TOLOWERCASE(3, "TOLOWERCASE"), TRIM(4, "TRIM"), LEFT(5,
			"LEFT", 1), RIGHT(6, "RIGHT", 1), SUBSTRING(7, "SUBSTRING", 2), CHARAT(8, "CHARAT", 1);

	protected static final OQueryItemFieldOperator[]	OPERATORS				= { SIZE, LENGTH, TOUPPERCASE, TOLOWERCASE, TRIM, LEFT, RIGHT,
			SUBSTRING, CHARAT																						};

	public static final String												CHAIN_SEPARATOR	= ".";

	public final int																	id;
	public final String																keyword;
	public final int																	arguments;

	OQueryItemFieldOperator(final int iId, final String iKeyword) {
		this(iId, iKeyword, 0);
	}

	OQueryItemFieldOperator(final int iId, final String iKeyword, int iArgs) {
		id = iId;
		keyword = iKeyword;
		arguments = iArgs;
	}

	@Override
	public String toString() {
		return keyword;
	}
}
