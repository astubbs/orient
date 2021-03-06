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

import org.testng.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentPool;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.iterator.ORecordIterator;
import com.orientechnologies.orient.core.record.impl.ODocument;

@Test(groups = { "crud", "record-vobject" }, sequential = true)
public class CRUDDocumentPhysicalTest {
	protected static final int	TOT_RECORDS	= 100;
	protected long							startRecordNumber;
	private ODatabaseDocumentTx	database;
	private ODocument						record;
	private String							url;

	@Parameters(value = "url")
	public CRUDDocumentPhysicalTest(final String iURL) {
		url = iURL;
	}

	@Test
	public void cleanAll() {
		database = ODatabaseDocumentPool.global().acquire(url, "admin", "admin");
		record = database.newInstance();

		startRecordNumber = database.countClusterElements("Account");

		// DELETE ALL THE RECORDS IN THE CLUSTER
		for (ODocument rec : database.browseCluster("Account"))
			if (rec != null)
				rec.delete();

		Assert.assertEquals(database.countClusterElements("Account"), 0);

		database.close();
	}

	@Test(dependsOnMethods = "cleanAll")
	public void create() {
		database = ODatabaseDocumentPool.global().acquire(url, "admin", "admin");

		startRecordNumber = database.countClusterElements("Account");

		record.setClassName("Account");

		byte[] binary = new byte[100];
		for (int b = 0; b < binary.length; ++b)
			binary[b] = (byte) b;

		for (long i = startRecordNumber; i < startRecordNumber + TOT_RECORDS; ++i) {
			record.reset();

			record.field("id", i);
			record.field("name", "Gipsy");
			record.field("location", "Italy");
			record.field("salary", (i + 300));
			record.field("binary", binary);
			record.field("testLong", 10000000000L); // TEST LONG
			record.field("extra", "This is an extra field not included in the schema");

			record.save("Account");
		}

		database.close();
	}

	@Test(dependsOnMethods = "create")
	public void testCreate() {
		database = ODatabaseDocumentPool.global().acquire(url, "admin", "admin");
		database.open("admin", "admin");

		Assert.assertEquals(database.countClusterElements("Account") - startRecordNumber, TOT_RECORDS);

		database.close();
	}

	@Test(dependsOnMethods = "testCreate")
	public void readAndBrowseDescendingAndCheckHoleUtilization() {
		database = ODatabaseDocumentPool.global().acquire(url, "admin", "admin");

		// BROWSE IN THE OPPOSITE ORDER
		byte[] binary;
		int i = 0;
		ORecordIterator<ODocument> it = database.browseCluster("Account");
		for (ODocument rec = it.last().previous(); rec != null; rec = it.previous()) {

			Assert.assertTrue(((Number) rec.field("id")).intValue() == i);
			Assert.assertEquals(rec.field("name"), "Gipsy");
			Assert.assertEquals(rec.field("location"), "Italy");
			Assert.assertEquals(((Number) rec.field("testLong")).longValue(), 10000000000L);
			Assert.assertEquals(((Number) rec.field("salary")).intValue(), i + 300);
			Assert.assertNotNull(rec.field("extra"));

			binary = rec.field("binary");
			for (int b = 0; b < binary.length; ++b)
				Assert.assertEquals(binary[b], (byte) b);

			i++;
		}

		Assert.assertTrue(i == TOT_RECORDS);

		database.close();
	}

	@Test(dependsOnMethods = "readAndBrowseDescendingAndCheckHoleUtilization")
	public void update() {
		database = ODatabaseDocumentPool.global().acquire(url, "admin", "admin");

		record.reset();

		int i = 0;
		for (ODocument rec : database.browseCluster("Account")) {

			if (i % 2 == 0)
				rec.field("location", "Spain");

			rec.field("price", i + 100);

			rec.save();

			i++;
		}

		database.close();
	}

	@Test(dependsOnMethods = "update")
	public void testUpdate() {
		database = ODatabaseDocumentPool.global().acquire(url, "admin", "admin");

		int i = 0;
		for (ODocument rec : database.browseCluster("Account")) {

			if (i % 2 == 0)
				Assert.assertEquals(rec.field("location"), "Spain");
			else
				Assert.assertEquals(rec.field("location"), "Italy");

			Assert.assertEquals(((Number) rec.field("price")).intValue(), i + 100);

			i++;
		}

		database.close();
	}
}
