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

import java.util.List;

import com.orientechnologies.orient.core.db.ODatabaseComplex;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.index.OFullTextIndex;
import com.orientechnologies.orient.core.index.OPropertyIndex;
import com.orientechnologies.orient.core.metadata.schema.OProperty;
import com.orientechnologies.orient.core.sql.filter.OSQLFilterCondition;
import com.orientechnologies.orient.core.sql.filter.OSQLFilterItemField;

/**
 * CONTAINS KEY operator.
 * 
 * @author Luca Garulli
 * 
 */
public class OQueryTargetOperatorContainsText extends OQueryTargetOperator {

	private boolean	ignoreCase	= true;

	public OQueryTargetOperatorContainsText(final boolean iIgnoreCase) {
		super("CONTAINSTEXT", 5, false);
		ignoreCase = iIgnoreCase;
	}

	public OQueryTargetOperatorContainsText() {
		super("CONTAINSTEXT", 5, false);
	}

	@Override
	public String getSyntax() {
		return "<left> CONTAINSTEXT[( noignorecase ] )] <right>";
	}

	@Override
	public List<ORecordId> evaluate(final ODatabaseComplex<?> iDatabase, final List<String> iTargetClasses,
			final OSQLFilterCondition iCondition, final Object iLeft, final Object iRight) {

		final String fieldName;
		if (iCondition.getLeft() instanceof OSQLFilterItemField)
			fieldName = iCondition.getLeft().toString();
		else
			fieldName = iCondition.getRight().toString();

		final String fieldValue;
		if (iCondition.getLeft() instanceof OSQLFilterItemField)
			fieldValue = iCondition.getRight().toString();
		else
			fieldValue = iCondition.getLeft().toString();

		String className = iTargetClasses.get(0);

		final OProperty prop = iDatabase.getMetadata().getSchema().getClass(className).getProperty(fieldName);
		if (prop == null)
			// NO PROPERTY DEFINED
			return null;

		final OPropertyIndex index = prop.getIndex();
		if (index == null || !(index instanceof OFullTextIndex))
			// NO FULL TEXT INDEX
			return null;

		return index.get(fieldValue);
	}

	public boolean isIgnoreCase() {
		return ignoreCase;
	}
}
