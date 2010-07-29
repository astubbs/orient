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
package com.orientechnologies.orient.core.sql.query;

import java.io.IOException;
import java.util.List;

import com.orientechnologies.orient.core.command.OCommandRequestText;
import com.orientechnologies.orient.core.query.OQueryAbstract;
import com.orientechnologies.orient.core.serialization.OMemoryInputStream;
import com.orientechnologies.orient.core.serialization.OMemoryOutputStream;
import com.orientechnologies.orient.core.serialization.OSerializableStream;

/**
 * SQL query implementation.
 * 
 * @author Luca Garulli
 * 
 * @param <T>
 *          Record type to return.
 */
public abstract class OSQLQuery<T extends Object> extends OQueryAbstract<T> implements OCommandRequestText {
	protected String	text;

	public OSQLQuery() {
	}

	public OSQLQuery(final String iText) {
		text = iText;
	}

	/**
	 * Delegates to the OQueryExecutor the query execution.
	 */
	@SuppressWarnings("unchecked")
	public List<T> run(final Object... iArgs) {
		parameters = iArgs;
		return (List<T>) database.getStorage().command(this);
	}

	/**
	 * Returns only the first record if any.
	 */
	public T runFirst(final Object... iArgs) {
		setLimit(1);
		final List<T> result = execute(iArgs);
		return result != null && result.size() > 0 ? result.get(0) : null;
	}

	public String getText() {
		return text;
	}

	@Override
	public String toString() {
		return "OSQLQuery [text=" + text + "]";
	}

	public OSerializableStream fromStream(final byte[] iStream) throws IOException {
		final OMemoryInputStream buffer = new OMemoryInputStream(iStream);
		text = buffer.getAsString();
		limit = buffer.getAsInteger();
		fetchPlan = buffer.getAsString();
		return this;
	}

	public byte[] toStream() throws IOException {
		final OMemoryOutputStream buffer = new OMemoryOutputStream();
		buffer.add(text);
		buffer.add(limit);
		buffer.add(fetchPlan);
		return buffer.toByteArray();
	}
}
