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

import org.testng.Assert;
import org.testng.annotations.Test;

import com.orientechnologies.orient.core.db.record.ODatabaseFlat;
import com.orientechnologies.orient.core.record.impl.ORecordFlat;
import com.orientechnologies.orient.core.tx.OTransaction.TXTYPE;
import com.orientechnologies.orient.test.database.base.OrientMultiThreadTest;
import com.orientechnologies.orient.test.database.base.OrientThreadTest;

@Test(enabled = false)
public class LocalCreateFlatMultiThreadSpeedTest extends OrientMultiThreadTest {
	protected ODatabaseFlat	database;
	private long									foundObjects;

	public static void main(String[] iArgs) throws InstantiationException, IllegalAccessException {
		LocalCreateFlatMultiThreadSpeedTest test = new LocalCreateFlatMultiThreadSpeedTest();
		test.data.go(test);
	}

	public LocalCreateFlatMultiThreadSpeedTest() {
		super(1000000, 2, CreateObjectsThread.class);
	}

	public void init() {
		database = new ODatabaseFlat(System.getProperty("url")).open("admin", "admin");
		foundObjects = database.countClusterElements("Animal");

		System.out.println("\nTotal objects in Animal cluster before the test: " + foundObjects);
	}

	public static class CreateObjectsThread extends OrientThreadTest {
		protected ODatabaseFlat	database;
		protected ORecordFlat					record	= new ORecordFlat();

		@Override
		public void init() {
			database = new ODatabaseFlat(System.getProperty("url")).open("admin", "admin");
			record = database.newInstance();

			database.begin(TXTYPE.NOTX);
		}

		public void cycle() {
			record.reset();
			record.value(
					"id:" + data.getCyclesDone() + ",name:'Gipsy',type:'Cat',race:'European',country:'Italy',price:"
							+ (data.getCyclesDone() + 300) + ".00").save("Animal");

			if (data.getCyclesDone() == data.getCycles() - 1)
				database.commit();
		}

		@Override
		public void deinit() throws Exception {
			database.close();
			super.deinit();
		}
	}

	public void deinit() {
		System.out.println("\nTotal objects in Animal cluster after the test: " + (database.countClusterElements("Animal")));

		System.out.println("Created " + (database.countClusterElements("Animal") - foundObjects));

		Assert.assertEquals(threadCycles, database.countClusterElements("Animal") - foundObjects);

		if (database != null)
			database.close();
	}
}
