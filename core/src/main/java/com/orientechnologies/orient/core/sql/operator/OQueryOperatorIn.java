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

import java.util.Collection;

import com.orientechnologies.orient.core.sql.filter.OSQLFilterCondition;

/**
 * IN operator.
 * 
 * @author Luca Garulli
 * 
 */
public class OQueryOperatorIn extends OQueryOperatorEqualityNotNulls {

	public OQueryOperatorIn() {
		super("IN", 5, false);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected boolean evaluateExpression(OSQLFilterCondition iCondition, final Object iLeft, final Object iRight) {
		if (iLeft instanceof Collection<?>) {
			Collection<Object> collection = (Collection<Object>) iLeft;
			for (Object o : collection) {
				if (OQueryOperatorEquals.equals(iRight, o))
					return true;
			}
		} else if (iRight instanceof Collection<?>) {

			Collection<Object> collection = (Collection<Object>) iRight;
			for (Object o : collection) {
				if (OQueryOperatorEquals.equals(iLeft, o))
					return true;
			}
		}
		return false;
	}

}
