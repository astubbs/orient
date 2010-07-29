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
package com.orientechnologies.orient.core.query.nativ;

import java.io.IOException;

import com.orientechnologies.orient.core.db.record.ODatabaseRecord;
import com.orientechnologies.orient.core.exception.OSerializationException;
import com.orientechnologies.orient.core.query.OQueryAbstract;
import com.orientechnologies.orient.core.record.ORecordInternal;
import com.orientechnologies.orient.core.serialization.OSerializableStream;

public abstract class ONativeQuery<T extends ORecordInternal<?>, CTX extends OQueryContextNative<T>> extends OQueryAbstract<T> {
	protected String	cluster;
	protected CTX			queryRecord;

	public abstract boolean filter(CTX iRecord);

	protected ONativeQuery(final ODatabaseRecord<T> iDatabase, final String iCluster) {
		super(iDatabase);
		cluster = iCluster;
	}

	public byte[] toStream() throws IOException {
		throw new OSerializationException("Native queries can't be serialized");
	}

	public OSerializableStream fromStream(byte[] iStream) throws IOException {
		throw new OSerializationException("Native queries can't be deserialized");
	}

}
