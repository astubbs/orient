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
package com.orientechnologies.orient.core.command;

import com.orientechnologies.orient.core.db.record.ODatabaseRecord;

/**
 * Text based Command Request abstract class.
 * 
 * @author Luca Garulli
 * 
 */
public abstract class OCommandRequestAbstract implements OCommandRequestInternal {
	protected ODatabaseRecord<?>			database;
	protected OCommandResultListener	resultListener;
	protected Object[]								parameters;

	protected OCommandRequestAbstract() {
	}

	protected OCommandRequestAbstract(final ODatabaseRecord<?> iDatabase) {
		database = iDatabase;
	}

	public ODatabaseRecord<?> getDatabase() {
		return database;
	}

	public OCommandRequestInternal setDatabase(final ODatabaseRecord<?> iDatabase) {
		this.database = iDatabase;
		return this;
	}

	public OCommandResultListener getResultListener() {
		return resultListener;
	}

	public void setResultListener(OCommandResultListener iListener) {
		resultListener = iListener;
	}

	public Object[] getParameters() {
		return parameters;
	}
}
