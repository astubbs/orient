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
package com.orientechnologies.orient.test.database.speed;

import java.io.UnsupportedEncodingException;

import com.orientechnologies.common.test.SpeedTestMonoThread;
import com.orientechnologies.orient.client.remote.OEngineRemote;
import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.core.command.OCommandResultListener;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.query.nativ.ONativeAsynchQuery;
import com.orientechnologies.orient.core.query.nativ.OQueryContextNativeSchema;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.test.database.base.OrientTest;

public class NativeAsynchQuerySpeedTest extends SpeedTestMonoThread implements
		OCommandResultListener {
	private ODatabaseDocumentTx database;
	protected int resultCount = 0;

	public NativeAsynchQuerySpeedTest() {
		super(1);
		Orient.instance().registerEngine(new OEngineRemote());
	}

	@Override
	public void cycle() throws UnsupportedEncodingException {

		new ONativeAsynchQuery<ODocument, OQueryContextNativeSchema<ODocument>>(
				database, "Animal", new OQueryContextNativeSchema<ODocument>(),
				this) {

			@Override
			public boolean filter(OQueryContextNativeSchema<ODocument> iRecord) {
				return iRecord.column("id").toInt().minor(10).go();
			}
		}.execute2();
	}

	public boolean result(final Object iRecord) {
		OrientTest.printRecord(resultCount++, iRecord);
		return true;
	}
}
