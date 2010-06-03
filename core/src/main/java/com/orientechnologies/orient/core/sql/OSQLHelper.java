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
package com.orientechnologies.orient.core.sql;

import java.util.ArrayList;
import java.util.List;

import com.orientechnologies.common.parser.OStringParser;
import com.orientechnologies.orient.core.command.OCommandToParse;
import com.orientechnologies.orient.core.db.record.ODatabaseRecord;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.serialization.serializer.OStringSerializerHelper;
import com.orientechnologies.orient.core.serialization.serializer.record.string.ORecordSerializerCSVAbstract;
import com.orientechnologies.orient.core.sql.filter.OSQLFilterItemField;
import com.orientechnologies.orient.core.sql.operator.OQueryOperator;
import com.orientechnologies.orient.core.sql.operator.OQueryOperatorAnd;
import com.orientechnologies.orient.core.sql.operator.OQueryOperatorContains;
import com.orientechnologies.orient.core.sql.operator.OQueryOperatorContainsAll;
import com.orientechnologies.orient.core.sql.operator.OQueryOperatorEquals;
import com.orientechnologies.orient.core.sql.operator.OQueryOperatorIn;
import com.orientechnologies.orient.core.sql.operator.OQueryOperatorIs;
import com.orientechnologies.orient.core.sql.operator.OQueryOperatorLike;
import com.orientechnologies.orient.core.sql.operator.OQueryOperatorMajor;
import com.orientechnologies.orient.core.sql.operator.OQueryOperatorMajorEquals;
import com.orientechnologies.orient.core.sql.operator.OQueryOperatorMinor;
import com.orientechnologies.orient.core.sql.operator.OQueryOperatorMinorEquals;
import com.orientechnologies.orient.core.sql.operator.OQueryOperatorNot;
import com.orientechnologies.orient.core.sql.operator.OQueryOperatorOr;
import com.orientechnologies.orient.core.sql.operator.OQueryOperatorTraverse;

/**
 * SQL Helper class
 * 
 * @author Luca Garulli
 * 
 */
public class OSQLHelper {
	public static final String			NAME							= "sql";

	public static final String			VALUE_NOT_PARSED	= "_NOT_PARSED_";

	public static OQueryOperator[]	OPERATORS					= { new OQueryOperatorAnd(), new OQueryOperatorOr(), new OQueryOperatorNot(),
			new OQueryOperatorEquals(), new OQueryOperatorMinorEquals(), new OQueryOperatorMinor(), new OQueryOperatorMajorEquals(),
			new OQueryOperatorContainsAll(), new OQueryOperatorMajor(), new OQueryOperatorLike(), new OQueryOperatorIs(),
			new OQueryOperatorIn(), new OQueryOperatorContains(), new OQueryOperatorTraverse() };

	public static int jumpWhiteSpaces(final String iText, int iCurrentPosition) {
		for (; iCurrentPosition < iText.length(); ++iCurrentPosition)
			if (!Character.isWhitespace(iText.charAt(iCurrentPosition)))
				break;
		return iCurrentPosition;
	}

	public static int nextWord(final String iText, final String iTextUpperCase, int ioCurrentPosition, final StringBuilder ioWord,
			final boolean iForceUpperCase) {
		return nextWord(iText, iTextUpperCase, ioCurrentPosition, ioWord, iForceUpperCase, " =><()");
	}

	public static int nextWord(final String iText, final String iTextUpperCase, int ioCurrentPosition, final StringBuilder ioWord,
			final boolean iForceUpperCase, final String iSeparatorChars) {
		ioWord.setLength(0);

		ioCurrentPosition = OSQLHelper.jumpWhiteSpaces(iText, ioCurrentPosition);
		if (ioCurrentPosition >= iText.length())
			return -1;

		final String word = OStringParser.getWord(iForceUpperCase ? iTextUpperCase : iText, ioCurrentPosition, iSeparatorChars);

		if (word != null && word.length() > 0) {
			ioWord.append(word);
			ioCurrentPosition += word.length();
		}

		return ioCurrentPosition;
	}

	public static OQueryOperator[] getOperators() {
		return OPERATORS;
	}

	public static void registerOperator(final OQueryOperator iOperator) {
		OQueryOperator[] ops = new OQueryOperator[OPERATORS.length + 1];
		System.arraycopy(OPERATORS, 0, ops, 0, OPERATORS.length);
		OPERATORS = ops;
	}

	/**
	 * Convert fields from text to real value. Supports: String, RID, Boolean, Float, Integer and NULL.
	 * 
	 * @param iDatabase
	 * @param iValue
	 *          Value to convert.
	 * @return The value converted if recognized, otherwise VALUE_NOT_PARSED
	 */
	public static Object parseValue(final ODatabaseRecord<?> iDatabase, String iValue) {
		if (iValue == null)
			return null;

		iValue = iValue.trim();

		Object fieldValue = VALUE_NOT_PARSED;

		if (iValue.startsWith("'") && iValue.endsWith("'"))
			// STRING
			fieldValue = stringContent(iValue);
		if (iValue.startsWith("[") && iValue.endsWith("]")) {
			// COLLECTION/ARRAY
			String[] items = OStringSerializerHelper.split(iValue.substring(1, iValue.length() - 1),
					OStringSerializerHelper.RECORD_SEPARATOR_AS_CHAR);

			List<Object> coll = new ArrayList<Object>();
			for (String item : items) {
				coll.add(parseValue(iDatabase, item));
			}
			fieldValue = coll;

		} else if (iValue.indexOf(":") > -1)
			// RID
			fieldValue = new ORecordId(iValue.trim());
		else {

			String upperCase = iValue.toUpperCase();
			if (upperCase.equals("NULL"))
				// NULL
				fieldValue = null;
			else if (upperCase.equals("TRUE"))
				// BOOLEAN, TRUE
				fieldValue = Boolean.TRUE;
			else if (upperCase.equals("FALSE"))
				// BOOLEAN, FALSE
				fieldValue = Boolean.FALSE;
			else {
				OType t = ORecordSerializerCSVAbstract.getNumber(iDatabase.getStorage().getConfiguration().getUnusualSymbols(), iValue);
				// NUMBER
				if (t == OType.LONG)
					fieldValue = Long.parseLong((String) iValue);
				else if (t == OType.DOUBLE)
					fieldValue = Double.parseDouble((String) iValue);
			}
		}

		return fieldValue;
	}

	public static Object parseValue(final ODatabaseRecord<?> iDatabase, final OCommandToParse iCommand, final String iWord) {
		// TRY TO PARSE AS RAW VALUE
		final Object v = parseValue(iDatabase, iWord);

		if (v == VALUE_NOT_PARSED)
			// PARSE FIELD
			return new OSQLFilterItemField(iCommand, iWord);

		return v;
	}

	public static String stringContent(final String iContent) {
		return iContent.substring(1, iContent.length() - 1);
	}
}