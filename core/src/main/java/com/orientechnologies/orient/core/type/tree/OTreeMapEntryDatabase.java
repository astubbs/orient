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
package com.orientechnologies.orient.core.type.tree;

import java.io.IOException;

import com.orientechnologies.common.collection.OTreeMapEntry;
import com.orientechnologies.orient.core.id.ORID;

/**
 * Persistent TreeMap implementation that use a ODatabase instance to handle the entries. This class can be used also from the user.
 * It's transaction aware.
 * 
 * @author Luca Garulli
 * 
 * @param <K>
 *          Key type
 * @param <V>
 *          Value type
 */
public class OTreeMapEntryDatabase<K, V> extends OTreeMapEntryPersistent<K, V> {
	/**
	 * Called on event of splitting an entry.
	 * 
	 * @param iParent
	 *          Parent node
	 * @param iPosition
	 *          Current position
	 */
	public OTreeMapEntryDatabase(OTreeMapEntry<K, V> iParent, int iPosition) {
		super(iParent, iPosition);
		record.setDatabase(((OTreeMapDatabase<K, V>) pTree).database);
	}

	/**
	 * Called upon unmarshalling.
	 * 
	 * @param iTree
	 *          Tree which belong
	 * @param iParent
	 *          Parent node if any
	 * @param iRecordId
	 *          Record to unmarshall
	 */
	public OTreeMapEntryDatabase(OTreeMapDatabase<K, V> iTree, OTreeMapEntryDatabase<K, V> iParent, ORID iRecordId)
			throws IOException {
		super(iTree, iParent, iRecordId);
		record.setDatabase(iTree.database);
		load();
	}

	public OTreeMapEntryDatabase(OTreeMapDatabase<K, V> iTree, K key, V value, OTreeMapEntryDatabase<K, V> iParent) {
		super(iTree, key, value, iParent);
		record.setDatabase(iTree.database);
	}

	public OTreeMapEntryDatabase<K, V> load() throws IOException {
		record.load();
		fromStream(record.toStream());
		return this;
	}

	public OTreeMapEntryDatabase<K, V> save() throws IOException {
		record.fromStream(toStream());
		record.save(pTree.getClusterName());
		return this;
	}
}
