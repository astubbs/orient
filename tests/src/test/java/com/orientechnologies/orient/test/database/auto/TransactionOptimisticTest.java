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

import java.io.IOException;

import org.testng.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.orientechnologies.orient.client.OEngineRemote;
import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.core.db.record.ODatabaseFlat;
import com.orientechnologies.orient.core.exception.OConcurrentModificationException;
import com.orientechnologies.orient.core.record.impl.ORecordFlat;

@Test(groups = "dictionary")
public class TransactionOptimisticTest {
	private String	url;

	@Parameters(value = "url")
	public TransactionOptimisticTest(String iURL) {
		Orient.instance().registerEngine(new OEngineRemote());
		url = iURL;
	}

	@Test
	public void testTransactionOptimisticRollback() throws IOException {
		ODatabaseFlat db1 = new ODatabaseFlat(url);
		db1.open("admin", "admin");

		long rec = db1.countClusterElements("binary");

		db1.begin();

		ORecordFlat record1 = new ORecordFlat(db1);
		record1.value("This is the first version").save();

		db1.rollback();

		Assert.assertEquals(db1.countClusterElements("binary"), rec);

		db1.close();
	}

	@Test(dependsOnMethods = "testTransactionOptimisticRollback")
	public void testTransactionOptimisticCommit() throws IOException {
		ODatabaseFlat db1 = new ODatabaseFlat(url);
		db1.open("admin", "admin");

		long tot = db1.countClusterElements("binary");

		db1.begin();

		ORecordFlat record1 = new ORecordFlat(db1);
		record1.value("This is the first version").save("binary");

		db1.commit();

		Assert.assertEquals(db1.countClusterElements("binary"), tot + 1);

		db1.close();
	}

	@Test(dependsOnMethods = "testTransactionOptimisticCommit")
	public void testTransactionOptimisticCuncurrentException() throws IOException {
		ODatabaseFlat db1 = new ODatabaseFlat(url);
		db1.open("admin", "admin");

		ODatabaseFlat db2 = new ODatabaseFlat(url);
		db2.open("admin", "admin");

		ORecordFlat record1 = new ORecordFlat(db1);
		record1.value("This is the first version").save();

		try {
			db1.begin();

			// RE-READ THE RECORD
			record1.load();
			ORecordFlat record2 = db2.load(record1.getIdentity());

			record2.value("This is the second version").save();
			record1.value("This is the third version").save();

			db1.commit();

			Assert.assertTrue(false);

		} catch (OConcurrentModificationException e) {
			Assert.assertTrue(true);
			db1.rollback();

		} finally {

			db1.close();
			db2.close();
		}
	}
}
