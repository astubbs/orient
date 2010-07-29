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
package com.orientechnologies.orient.core.db;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.orientechnologies.common.concur.lock.OLockException;
import com.orientechnologies.common.concur.resource.OResourcePool;
import com.orientechnologies.common.concur.resource.OResourcePoolListener;
import com.orientechnologies.common.log.OLogManager;

public abstract class ODatabasePoolAbstract<DB extends ODatabase> implements OResourcePoolListener<String, DB> {

	private static final int															DEF_WAIT_TIMEOUT	= 5000;
	private final Map<String, OResourcePool<String, DB>>	pools							= new HashMap<String, OResourcePool<String, DB>>();
	private int																						maxSize;
	private int																						timeout						= DEF_WAIT_TIMEOUT;
	protected Object																			owner;

	public ODatabasePoolAbstract(final Object iOwner, final int iMinSize, final int iMaxSize) {
		this(iOwner, iMinSize, iMaxSize, DEF_WAIT_TIMEOUT);
	}

	public ODatabasePoolAbstract(final Object iOwner, final int iMinSize, final int iMaxSize, final int iTimeout) {
		maxSize = iMaxSize;
		timeout = iTimeout;
		owner = iOwner;
	}

	public DB acquire(final String iURL, final String iUserName, final String iUserPassword) throws OLockException {
		final int separatorPos = iURL.lastIndexOf('/');
		final String name = separatorPos > -1 ? iURL.substring(separatorPos + 1) : iURL;

		OResourcePool<String, DB> pool = pools.get(name);
		if (pool == null) {
			synchronized (pools) {
				if (pool == null) {
					pool = new OResourcePool<String, DB>(maxSize, this);
					pools.put(name, pool);
				}
			}
		}

		return pool.getResource(iURL, timeout, iUserName, iUserPassword);
	}

	public void release(final DB iDatabase) {
		final OResourcePool<String, DB> pool = pools.get(iDatabase.getName());
		if (pool == null)
			throw new OLockException("Can't release a database URL not acquired before. URL: " + iDatabase.getName());

		pool.returnResource(iDatabase);
	}

	public DB reuseResource(final String iKey, final DB iValue) {
		return iValue;
	}

	public Map<String, OResourcePool<String, DB>> getPools() {
		return pools;
	}

	/**
	 * Closes all the databases.
	 */
	public void close() {
		for (Entry<String, OResourcePool<String, DB>> pool : pools.entrySet()) {
			for (DB db : pool.getValue().getResources()) {
				pool.getValue().close();
				try {
					OLogManager.instance().debug(this, "Closing pooled database '%s'...", db.getName());
					((ODatabasePooled) db).forceClose();
					OLogManager.instance().debug(this, "OK", db.getName());
				} catch (Exception e) {
					OLogManager.instance().debug(this, "Error: %d", e.toString());
				}
			}
		}
	}
}
