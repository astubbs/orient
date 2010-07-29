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
package com.orientechnologies.orient.test.database.auto;

import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.orientechnologies.orient.core.db.document.ODatabaseDocument;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.query.nativ.ONativeSynchQuery;
import com.orientechnologies.orient.core.query.nativ.OQueryContextNativeSchema;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.test.database.base.OrientTest;

@Test(groups = "query", sequential = true)
public class NativeQueryTest {
	private ODatabaseDocument	database;
	private ODocument					record;

	@Parameters(value = "url")
	public NativeQueryTest(String iURL) {
		database = new ODatabaseDocumentTx(iURL);
	}

	@Test
	public void querySchemaAndLike() {
		database.open("admin", "admin");

		List<ODocument> result = database.query(

		new ONativeSynchQuery<ODocument, OQueryContextNativeSchema<ODocument>>("Account", new OQueryContextNativeSchema<ODocument>()) {

			@Override
			public boolean filter(OQueryContextNativeSchema<ODocument> iRecord) {
				return iRecord.field("location").field("city").field("name").eq("Rome").and().field("name").like("G%").go();
			};

		}).execute();

		for (int i = 0; i < result.size(); ++i) {
			record = result.get(i);

			OrientTest.printRecord(i, record);

			Assert.assertTrue(record.getClassName().equalsIgnoreCase("Person"));
			Assert.assertEquals(((ODocument) record.field("city")).field("name"), "Rome");
			Assert.assertTrue(record.field("name").toString().startsWith("G"));
		}

		database.close();
	}
}
