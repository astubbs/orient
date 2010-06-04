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
package com.orientechnologies.orient.core.metadata.schema;

import java.io.IOException;
import java.text.ParseException;

import com.orientechnologies.common.collection.OTreeMap;
import com.orientechnologies.common.log.OLogManager;
import com.orientechnologies.orient.core.db.record.ODatabaseRecord;
import com.orientechnologies.orient.core.exception.ODatabaseException;
import com.orientechnologies.orient.core.exception.OSchemaException;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.serialization.serializer.stream.OStreamSerializerRID;
import com.orientechnologies.orient.core.serialization.serializer.stream.OStreamSerializerString;
import com.orientechnologies.orient.core.storage.OStorage;
import com.orientechnologies.orient.core.type.tree.OTreeMapDatabaseLazySave;

/**
 * Contains the description of a persistent class property.
 * 
 * @author Luca Garulli
 * 
 */
public class OProperty extends OSchemaRecord {

	private OClass																			owner;

	private int																					id;
	private String																			name;
	private OType																				type;
	private int																					offset;
	private int																					size;

	private OType																				linkedType;
	private OClass																			linkedClass;

	private OTreeMapDatabaseLazySave<String, ORecordId>	index;

	private boolean																			mandatory;
	private boolean																			notNull;
	private String																			min;
	private String																			max;

	/**
	 * Constructor used in unmarshalling.
	 */
	public OProperty() {
	}

	public OProperty(OClass iOwner) {
		owner = iOwner;
		database = iOwner.getDatabase();
		id = iOwner.properties.size();
	}

	public OProperty(OClass iOwner, String iName, OType iType, int iOffset) {
		this(iOwner);
		name = iName;
		type = iType;
		offset = iOffset;
	}

	public String getName() {
		return name;
	}

	public OType getType() {
		return type;
	}

	public int offset() {
		return offset;
	}

	public int getId() {
		return id;
	}

	public OClass getLinkedClass() {
		return linkedClass;
	}

	public OProperty setLinkedClass(OClass linkedClass) {
		this.linkedClass = linkedClass;
		setDirty();
		return this;
	}

	public OProperty fromDocument(final ODocument iSource) {
		name = iSource.field("name");
		if (iSource.field("type") != null)
			type = OType.getById(((Long) iSource.field("type")).byteValue());
		offset = ((Long) iSource.field("offset")).intValue();

		mandatory = (Boolean) iSource.field("mandatory");
		notNull = (Boolean) iSource.field("notNull");
		min = iSource.field("min");
		max = iSource.field("max");

		linkedClass = owner.owner.getClass((String) iSource.field("linkedClass"));
		if (iSource.field("linkedType") != null)
			linkedType = OType.getById(((Long) iSource.field("linkedType")).byteValue());

		final ODocument indexRecord = iSource.field("index");

		// LOAD THE INDEX
		if (indexRecord != null && indexRecord.getIdentity().isValid())
			try {
				index = new OTreeMapDatabaseLazySave<String, ORecordId>((ODatabaseRecord<?>) database, OStorage.CLUSTER_INDEX_NAME,
						new ORecordId(indexRecord.getIdentity().toString()));
				index.load();
			} catch (IOException e) {
				OLogManager.instance().error(this, "Can't load index for property %s", e, ODatabaseException.class, toString());
			}

		return this;
	}

	@Override
	public byte[] toStream() {
		field("name", name);
		field("type", type.id);
		field("offset", offset);
		field("mandatory", mandatory);
		field("notNull", notNull);
		field("min", min);
		field("max", max);

		field("linkedClass", linkedClass != null ? linkedClass.getName() : null);
		field("linkedType", linkedType != null ? linkedType.id : null);

		// SAVE THE INDEX
		if (index != null) {
			try {
				index.save();
			} catch (IOException e) {
				throw new ODatabaseException("Error on saving index for property: " + index);
			}
			field("index", index.getRecord().getIdentity());
		} else
			field("index", ORecordId.EMPTY_RECORD_ID);

		return super.toStream();
	}

	public OType getLinkedType() {
		return linkedType;
	}

	public OProperty setLinkedType(OType linkedType) {
		this.linkedType = linkedType;
		setDirty();
		return this;
	}

	public boolean isNotNull() {
		return notNull;
	}

	public OProperty setNotNull(boolean iNotNull) {
		notNull = iNotNull;
		setDirty();
		return this;
	}

	public boolean isMandatory() {
		return mandatory;
	}

	public OProperty setMandatory(boolean mandatory) {
		this.mandatory = mandatory;
		setDirty();
		return this;
	}

	public String getMin() {
		return min;
	}

	public OProperty setMin(String min) {
		this.min = min;
		checkForDateFormat(min);
		setDirty();
		return this;
	}

	public String getMax() {
		return max;
	}

	public OProperty setMax(String max) {
		this.max = max;
		checkForDateFormat(max);
		setDirty();
		return this;
	}

	/**
	 * Creates an index on this property. Indexes speed up queries but slow down insert and update operations. Now only unique indexes
	 * are supported. For massive inserts we suggest to remove the index, make the massive insert and recreate it.
	 * 
	 * @param iUnique
	 *          Don't allow duplicates. Now is supported only unique indexes, so pass always TRUE
	 * @return
	 */
	public OTreeMap<?, ?> createIndex(boolean iUnique) {
		if (!iUnique)
			throw new UnsupportedOperationException(
					"OrientDB supports only unique indexes. Contact the staff to know when no-unique indexes will be available");

		if (index != null)
			throw new IllegalStateException("Index already created");

		try {
			index = new OTreeMapDatabaseLazySave<String, ORecordId>((ODatabaseRecord<?>) database, OStorage.CLUSTER_INDEX_NAME,
					OStreamSerializerString.INSTANCE, new OStreamSerializerRID());
			setDirty();

			populateIndex();

			if (database != null) {
				// / SAVE ONLY IF THE PROPERTY IS ALREADY PERSISTENT
				index.lazySave();
				database.getMetadata().getSchema().save();
			}

		} catch (Exception e) {
			OLogManager.instance().exception("Unable to create %s index for property %s", e, ODatabaseException.class,
					iUnique ? "unique" : "not unique", toString());

		}

		return index;
	}

	/**
	 * Populate the index with all the existent records.
	 */
	private void populateIndex() {
		Object fieldValue;
		ODocument document;

		index.clear();

		final int[] clusterIds = owner.getClusterIds();
		for (int clusterId : clusterIds)
			for (Object record : database.browseCluster(database.getClusterNameById(clusterId)).setReuseSameRecord(true)) {
				if (record instanceof ODocument) {
					document = (ODocument) record;
					fieldValue = document.field(name);

					if (fieldValue != null)
						index.put(fieldValue.toString(), (ORecordId) document.getIdentity());
				}
			}
	}

	/**
	 * Remove the index on property
	 */
	public void removeIndex() {
		if (index != null) {
			index.clear();
			index.getRecord().delete();
			index = null;
			setDirty();
		}
	}

	public OTreeMapDatabaseLazySave<String, ORecordId> getIndex() {
		return index;
	}

	public boolean isIndexed() {
		return index != null;
	}

	@Override
	public OProperty setDirty() {
		super.setDirty();
		if (owner != null)
			owner.setDirty();
		return this;
	}

	@Override
	public String toString() {
		return name + " (type=" + type + ")";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((owner == null) ? 0 : owner.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		OProperty other = (OProperty) obj;
		if (owner == null) {
			if (other.owner != null)
				return false;
		} else if (!owner.equals(other.owner))
			return false;
		return true;
	}

	private void checkForDateFormat(String min) {
		if (type == OType.DATE) {
			try {
				owner.owner.getDatabase().getStorage().getConfiguration().getDateTimeFormatInstance().parse(min);
			} catch (ParseException e) {
				throw new OSchemaException("Invalid date format setted", e);
			}
		}
	}
}
