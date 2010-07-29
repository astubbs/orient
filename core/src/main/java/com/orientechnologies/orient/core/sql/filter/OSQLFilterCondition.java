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
package com.orientechnologies.orient.core.sql.filter;

import com.orientechnologies.orient.core.query.OQueryRuntimeValueMulti;
import com.orientechnologies.orient.core.record.ORecord.STATUS;
import com.orientechnologies.orient.core.record.ORecordSchemaAware;
import com.orientechnologies.orient.core.serialization.serializer.OStringSerializerHelper;
import com.orientechnologies.orient.core.sql.operator.OQueryOperator;

/**
 * Run-time query condition evaluator.
 * 
 * @author Luca Garulli
 * 
 */
public class OSQLFilterCondition {
	private static final String	NULL_VALUE	= "null";
	protected Object						left;
	protected OQueryOperator		operator;
	protected Object						right;

	public OSQLFilterCondition(Object iLeft, OQueryOperator iOperator) {
		this.left = iLeft;
		this.operator = iOperator;
	}

	public OSQLFilterCondition(Object iLeft, OQueryOperator iOperator, Object iRight) {
		this.left = iLeft;
		this.operator = iOperator;
		this.right = iRight;
	}

	public Object evaluate(ORecordSchemaAware<?> iRecord) {
		Object l = evaluate(iRecord, left);
		Object r = evaluate(iRecord, right);

		Object[] convertedValues = checkForConversion(l, r);
		if (convertedValues != null) {
			l = convertedValues[0];
			r = convertedValues[1];
		}

		return operator.evaluate(this, l, r);
	}

	private Object[] checkForConversion(Object l, Object r) {
		Object[] result = null;

		// INTEGERS
		if (r instanceof Integer && !(l instanceof Integer)) {
			if (l instanceof String && ((String) l).indexOf(".") > -1)
				result = new Object[] { new Float((String) l).intValue(), r };
			else if (!(l instanceof OQueryRuntimeValueMulti))
				result = new Object[] { getInteger(l), r };
		} else if (l instanceof Integer && !(r instanceof Integer)) {
			if (r instanceof String && ((String) r).indexOf(".") > -1)
				result = new Object[] { l, new Float((String) r).intValue() };
			else if (!(r instanceof OQueryRuntimeValueMulti))
				result = new Object[] { l, getInteger(r) };
		}

		// FLOATS
		else if (r instanceof Float && !(l instanceof Float))
			result = new Object[] { getFloat(l), r };
		else if (l instanceof Float && !(r instanceof Float))
			result = new Object[] { l, getFloat(r) };

		return result;
	}

	protected Integer getInteger(Object iValue) {
		if (iValue == null)
			return null;

		String stringValue = iValue.toString();

		if (NULL_VALUE.equals(stringValue))
			return null;

		if (OStringSerializerHelper.contains(stringValue, '.') || OStringSerializerHelper.contains(stringValue, ','))
			return (int) Float.parseFloat(stringValue);
		else
			return stringValue.length() > 0 ? new Integer(stringValue) : new Integer(0);
	}

	protected Float getFloat(Object iValue) {
		if (iValue == null)
			return null;

		String stringValue = iValue.toString();

		if (NULL_VALUE.equals(stringValue))
			return null;

		return stringValue.length() > 0 ? new Float(stringValue) : new Float(0);
	}

	protected Object evaluate(ORecordSchemaAware<?> iRecord, Object iValue) {
		if (iValue instanceof OSQLFilterItem) {
			if (iRecord.getInternalStatus() == STATUS.NOT_LOADED)
				iRecord.load();

			return ((OSQLFilterItem) iValue).getValue(iRecord);
		}

		if (iValue instanceof OSQLFilterCondition)
			// NESTED CONDITION: EVALUATE IT RECURSIVELY
			return ((OSQLFilterCondition) iValue).evaluate(iRecord);

		// SIMPLE VALUE: JUST RETURN IT
		return iValue;
	}

	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder();

		buffer.append('(');
		buffer.append(left);
		buffer.append(' ');
		buffer.append(operator);
		buffer.append(' ');
		buffer.append(right);
		buffer.append(')');

		return buffer.toString();
	}

	public Object getLeft() {
		return left;
	}

	public Object getRight() {
		return right;
	}

	public OQueryOperator getOperator() {
		return operator;
	}
}
