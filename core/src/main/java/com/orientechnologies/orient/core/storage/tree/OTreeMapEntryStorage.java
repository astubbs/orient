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
package com.orientechnologies.orient.core.storage.tree;

import java.io.IOException;

import com.orientechnologies.common.collection.OTreeMapEntry;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.storage.ORawBuffer;
import com.orientechnologies.orient.core.type.tree.OTreeMapEntryPersistent;
import com.orientechnologies.orient.core.type.tree.OTreeMapPersistent;

/**
 * Persistent TreeMap implementation that use a OStorage instance to handle the entries. This class can't be used also from the
 * user. It's not transaction aware.
 * 
 * @author Luca Garulli
 * 
 * @param <K>
 *          Key type
 * @param <V>
 *          Value type
 */
public class OTreeMapEntryStorage<K, V> extends OTreeMapEntryPersistent<K, V> {

	public OTreeMapEntryStorage(OTreeMapEntry<K, V> iParent, int iPosition) {
		super(iParent, iPosition);
		record.setIdentity(pTree.getRecord().getIdentity().getClusterId(), ORID.CLUSTER_POS_INVALID);
	}

	public OTreeMapEntryStorage(OTreeMapPersistent<K, V> iTree, K key, V value, OTreeMapEntryPersistent<K, V> iParent) {
		super(iTree, key, value, iParent);
		record.setIdentity(pTree.getRecord().getIdentity().getClusterId(), ORID.CLUSTER_POS_INVALID);
	}

	public OTreeMapEntryStorage(OTreeMapPersistent<K, V> iTree, OTreeMapEntryPersistent<K, V> iParent, ORID iRecordId)
			throws IOException {
		super(iTree, iParent, iRecordId);
		load();
	}

	@Override
	public OTreeMapEntryStorage<K, V> load() throws IOException {
		ORawBuffer raw = ((OTreeMapStorage<K, V>) tree).storage.readRecord(null, -1, record.getIdentity().getClusterId(), record
				.getIdentity().getClusterPosition(), null);

		record.setVersion(raw.version);

		fromStream(raw.buffer);
		return this;
	}

	@Override
	public OTreeMapEntryStorage<K, V> save() throws IOException {
		record.fromStream(toStream());
		if (record.getIdentity().isValid())
			// UPDATE IT
			record.setVersion(((OTreeMapStorage<K, V>) tree).storage.updateRecord(0, record.getIdentity().getClusterId(), record
					.getIdentity().getClusterPosition(), record.toStream(), record.getVersion(), record.getRecordType()));
		else {
			// CREATE IT
			record.setIdentity(((OTreeMapStorage<K, V>) tree).clusterId, ((OTreeMapStorage<K, V>) tree).storage.createRecord(record
					.getIdentity().getClusterId(), record.toStream(), record.getRecordType()));
		}
		record.unsetDirty();
		return this;
	}
}
