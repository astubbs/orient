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
package com.orientechnologies.orient.core.record;

import java.util.LinkedHashMap;
import java.util.Map;

import com.orientechnologies.orient.core.db.record.ODatabaseRecord;
import com.orientechnologies.orient.core.metadata.schema.OType;

/**
 * Abstract implementation for record-free implementations. The object can be reused across calls to the database by using the
 * reset() at every re-use. Field population and serialization occurs always in lazy way.
 */
@SuppressWarnings("unchecked")
public abstract class ORecordVirtualAbstract<T> extends ORecordSchemaAwareAbstract<T> {
	protected Map<String, T>			_fieldValues;
	protected Map<String, T>			_fieldOriginalValues;
	protected Map<String, OType>	_fieldTypes;
	protected boolean							_trackingChanges	= true;

	public ORecordVirtualAbstract() {
	}

	public ORecordVirtualAbstract(byte[] iSource) {
		_source = iSource;
	}

	public ORecordVirtualAbstract(String iClassName) {
		setClassName(iClassName);
	}

	public ORecordVirtualAbstract(ODatabaseRecord<?> iDatabase) {
		super(iDatabase);
	}

	public ORecordVirtualAbstract(ODatabaseRecord<?> iDatabase, String iClassName) {
		super(iDatabase);
		setClassName(iClassName);
	}

	/**
	 * Returns the forced field type if any.
	 * 
	 * @param iPropertyName
	 */
	public OType fieldType(final String iPropertyName) {
		return _fieldTypes != null ? _fieldTypes.get(iPropertyName) : null;
	}

	@Override
	public ORecordSchemaAwareAbstract<T> reset() {
		super.reset();
		if (_fieldValues != null)
			_fieldValues.clear();
		return this;
	}

	public boolean isTrackingChanges() {
		return _trackingChanges;
	}

	public <RET extends ORecordVirtualAbstract<?>> RET setTrackingChanges(final boolean iTrackingChanges) {
		this._trackingChanges = iTrackingChanges;
		return (RET) this;
	}

	@Override
	public boolean equals(Object obj) {
		if (!super.equals(obj))
			return false;

		return _recordId.isValid();
	}

	@Override
	protected void checkForFields() {
		if (_fieldValues == null)
			_fieldValues = new LinkedHashMap<String, T>();

		if (_status == STATUS.LOADED && (_fieldValues == null || size() == 0))
			// POPULATE FIELDS LAZY
			deserializeFields();
	}
}
