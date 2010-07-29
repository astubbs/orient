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
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.orientechnologies.orient.core.db.object.ODatabaseObjectTx;
import com.orientechnologies.orient.core.hook.ORecordHook;
import com.orientechnologies.orient.core.hook.ORecordHookAbstract;
import com.orientechnologies.orient.core.record.ORecord;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import com.orientechnologies.orient.test.domain.whiz.Profile;

@Test(groups = "hook")
public class HookTest extends ORecordHookAbstract {
	private ODatabaseObjectTx	database;
	private int								callbackCount	= 0;
	private Profile						p;

	@Parameters(value = "url")
	public HookTest(String iURL) {
		database = new ODatabaseObjectTx(iURL);
	}

	@Test
	public void testRegisterHook() throws IOException {
		database.open("writer", "writer");
		database.registerHook(this);
		database.getEntityManager().registerEntityClasses("com.orientechnologies.orient.test.domain");
	}

	@Test(dependsOnMethods = "testRegisterHook")
	public void testHooksIsRegistered() throws IOException {
		for (ORecordHook hook : database.getHooks()) {
			if (hook.equals(this))
				return;
		}

		Assert.assertTrue(false);
	}

	@Test(dependsOnMethods = "testHooksIsRegistered")
	public void testHookCreate() throws IOException {
		p = new Profile("HookTest");

		// TEST HOOKS ON CREATE
		Assert.assertEquals(callbackCount, 0);
		database.save(p);
		Assert.assertEquals(callbackCount, 11);
	}

	@Test(dependsOnMethods = "testHookCreate")
	public void testHookRead() {
		// TEST HOOKS ON READ
		List<Profile> result = database.query(new OSQLSynchQuery<Profile>("select * from Profile where nick = 'HookTest'"));
		Assert.assertFalse(result.size() == 0);

		for (int i = 0; i < result.size(); ++i) {
			Assert.assertEquals(callbackCount, 61);

			p = result.get(i);
		}
		Assert.assertEquals(callbackCount, 61);
	}

	@Test(dependsOnMethods = "testHookRead")
	public void testHookUpdate() {
		// TEST HOOKS ON UPDATE
		database.save(p);
		Assert.assertEquals(callbackCount, 151);
	}

	@Test(dependsOnMethods = "testHookUpdate")
	public void testHookDelete() {
		// TEST HOOKS ON DELETE
		database.delete(p);
		Assert.assertEquals(callbackCount, 281);
	}

	@Test(dependsOnMethods = "testHookDelete")
	public void testUnregisterHook() throws IOException {
		database.unregisterHook(this);
		database.close();
	}

	@Override
	@Test(enabled = false)
	public void onRecordBeforeCreate(ORecord<?> iRecord) {
		callbackCount += 1;
	}

	@Override
	@Test(enabled = false)
	public void onRecordAfterCreate(ORecord<?> iRecord) {
		callbackCount += 10;
	}

	@Override
	@Test(enabled = false)
	public void onRecordBeforeRead(ORecord<?> iRecord) {
		callbackCount += 20;
	}

	@Override
	@Test(enabled = false)
	public void onRecordAfterRead(ORecord<?> iRecord) {
		callbackCount += 30;
	}

	@Override
	@Test(enabled = false)
	public void onRecordBeforeUpdate(ORecord<?> iRecord) {
		callbackCount += 40;
	}

	@Override
	@Test(enabled = false)
	public void onRecordAfterUpdate(ORecord<?> iRecord) {
		callbackCount += 50;
	}

	@Override
	@Test(enabled = false)
	public void onRecordBeforeDelete(ORecord<?> iRecord) {
		callbackCount += 60;
	}

	@Override
	@Test(enabled = false)
	public void onRecordAfterDelete(ORecord<?> iRecord) {
		callbackCount += 70;
	}

}
