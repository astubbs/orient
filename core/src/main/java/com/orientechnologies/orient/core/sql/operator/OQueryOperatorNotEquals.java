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
package com.orientechnologies.orient.core.sql.operator;

import com.orientechnologies.orient.core.db.record.ODatabaseRecord;
import com.orientechnologies.orient.core.sql.filter.OSQLFilterCondition;

/**
 * NOT EQUALS operator.
 * 
 * @author Luca Garulli
 * 
 */
public class OQueryOperatorNotEquals extends OQueryOperatorEqualityNotNulls {

	public OQueryOperatorNotEquals() {
		super("<>", 5, false);
	}

	@Override
	protected boolean evaluateExpression(final ODatabaseRecord<?> iDatabase, OSQLFilterCondition iCondition, final Object iLeft,
			final Object iRight) {
		return !OQueryOperatorEquals.equals(iLeft, iRight);
	}
}
