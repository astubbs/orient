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
package com.orientechnologies.orient.core.query;

import com.orientechnologies.orient.core.command.OCommandRequestAbstract;
import com.orientechnologies.orient.core.db.record.ODatabaseRecord;

public abstract class OQueryAbstract<T extends Object> extends OCommandRequestAbstract implements OQuery<T> {
	protected int	limit	= -1;

	public OQueryAbstract() {
	}

	public OQueryAbstract(final ODatabaseRecord<?> iDatabase) {
		super(iDatabase);
	}

	@SuppressWarnings("unchecked")
	public <RET> RET execute(Object... iArgs) {
		return (RET) execute2(iArgs);
	}

	public int getLimit() {
		return limit;
	}

	public OQueryAbstract<T> setLimit(int limit) {
		this.limit = limit;
		return this;
	}
}
