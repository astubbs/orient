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

import java.util.ArrayList;

import org.testng.annotations.Test;

import com.orientechnologies.common.profiler.OProfiler;
import com.orientechnologies.orient.core.db.document.ODatabaseDocument;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.tx.OTransaction.TXTYPE;
import com.orientechnologies.orient.test.database.base.OrientMonoThreadTest;

@Test(enabled = false)
public class LocalCreateDocumentSpeedTest extends OrientMonoThreadTest {
	private ODatabaseDocument	database;
	private ODocument					record;

	public static void main(String[] iArgs) throws InstantiationException, IllegalAccessException {
		LocalCreateDocumentSpeedTest test = new LocalCreateDocumentSpeedTest();
		test.data.go(test);
	}

	public LocalCreateDocumentSpeedTest() throws InstantiationException, IllegalAccessException {
		super(1000000);
	}

	@Override
	public void init() {
		OProfiler.getInstance().startRecording();

		database = new ODatabaseDocumentTx(System.getProperty("url")).open("admin", "admin");
		record = database.newInstance();

		database.begin(TXTYPE.NOTX);
	}

	public void cycle() {
		record.reset();

		record.setClassName("Person");
		record.field("parent", null);
		record.field("name", "Luca");
		record.field("surname", "Garulli");
		record.field("children", new ArrayList<Object>());
		record.field("city", null);

		record.save();

		if (data.getCyclesDone() == data.getCycles() - 1)
			database.commit();
	}

	@Override
	public void deinit() {
		database.close();
		super.deinit();
	}
}
