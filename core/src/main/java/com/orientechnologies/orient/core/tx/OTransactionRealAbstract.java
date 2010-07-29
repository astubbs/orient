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
package com.orientechnologies.orient.core.tx;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.orientechnologies.orient.core.db.record.ODatabaseRecordTx;
import com.orientechnologies.orient.core.exception.OTransactionException;
import com.orientechnologies.orient.core.record.ORecordInternal;

public abstract class OTransactionRealAbstract<REC extends ORecordInternal<?>> extends OTransactionAbstract<REC> {
	protected Map<String, OTransactionEntry<REC>>	entries						= new HashMap<String, OTransactionEntry<REC>>();
	protected int																	newObjectCounter	= 0;

	protected OTransactionRealAbstract(ODatabaseRecordTx<REC> iDatabase, int iId) {
		super(iDatabase, iId);
	}

	public Collection<OTransactionEntry<REC>> getEntries() {
		return entries.values();
	}

	public int size() {
		return entries.size();
	}

	@Override
	protected void checkTransaction() {
		if (status == TXSTATUS.INVALID)
			throw new OTransactionException("Invalid state of the transaction. The transaction must be begun.");
	}
}
