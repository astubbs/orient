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
import java.lang.ref.SoftReference;

import com.orientechnologies.common.collection.OTreeMapEntry;
import com.orientechnologies.common.log.OLogManager;
import com.orientechnologies.common.profiler.OProfiler;
import com.orientechnologies.orient.core.exception.OConfigurationException;
import com.orientechnologies.orient.core.exception.ODatabaseException;
import com.orientechnologies.orient.core.exception.OSerializationException;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.record.impl.ORecordBytes;
import com.orientechnologies.orient.core.serialization.OMemoryInputStream;
import com.orientechnologies.orient.core.serialization.OMemoryOutputStream;
import com.orientechnologies.orient.core.serialization.OSerializableStream;

@SuppressWarnings("unchecked")
public abstract class OTreeMapEntryPersistent<K, V> extends OTreeMapEntry<K, V> implements OSerializableStream {
	protected OTreeMapPersistent<K, V>						pTree;

	byte[][]																			serializedKeys;
	byte[][]																			serializedValues;

	protected ORID																parentRid;
	protected ORID																leftRid;
	protected ORID																rightRid;

	public ORecordBytes														record;

	protected SoftReference<OTreeMapEntry<K, V>>	parent;
	protected SoftReference<OTreeMapEntry<K, V>>	left;
	protected SoftReference<OTreeMapEntry<K, V>>	right;

	//
	// private int keySize = 0;
	// private int valueSIze = 0;
	// private int itemChanged;
	// private boolean otherChanged;
	//
	// private final static int PAGESIZE_OFFSET = 0;
	// private final static int LEFTRID_OFFSET = 2;
	// private final static int RIGHTRID_OFFSET = 12;
	// private final static int COLOR_OFFSET = 22;
	// private final static int SIZE_OFFSET = 23;
	// private final static int CONTENT_OFFSET = 27;

	/**
	 * Called on event of splitting an entry.
	 * 
	 * @param iParent
	 *          Parent node
	 * @param iPosition
	 *          Current position
	 */
	public OTreeMapEntryPersistent(final OTreeMapEntry<K, V> iParent, final int iPosition) {
		super(iParent, iPosition);
		pTree = (OTreeMapPersistent<K, V>) tree;

		parentRid = new ORecordId();
		leftRid = new ORecordId();
		rightRid = new ORecordId();

		record = new ORecordBytes();

		pageSize = pTree.getPageSize();

		// COPY ALSO THE SERIALIZED KEYS/VALUES
		serializedKeys = new byte[pageSize][];
		serializedValues = new byte[pageSize][];

		System.arraycopy(((OTreeMapEntryPersistent<K, V>) iParent).serializedKeys, iPosition, serializedKeys, 0, size);
		System.arraycopy(((OTreeMapEntryPersistent<K, V>) iParent).serializedValues, iPosition, serializedValues, 0, size);

		markDirty();
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
	public OTreeMapEntryPersistent(final OTreeMapPersistent<K, V> iTree, final OTreeMapEntryPersistent<K, V> iParent,
			final ORID iRecordId) throws IOException {
		super(iTree);
		pTree = iTree;
		record = new ORecordBytes();
		record.setIdentity(iRecordId.getClusterId(), iRecordId.getClusterPosition());
		parent = new SoftReference<OTreeMapEntry<K, V>>(iParent);
	}

	public OTreeMapEntryPersistent(OTreeMapPersistent<K, V> iTree, K key, V value, OTreeMapEntryPersistent<K, V> iParent) {
		super(iTree, key, value, iParent);
		pTree = iTree;

		parentRid = new ORecordId();
		leftRid = new ORecordId();
		rightRid = new ORecordId();

		record = new ORecordBytes();

		pageSize = pTree.getPageSize();

		serializedKeys = new byte[pageSize][];
		serializedValues = new byte[pageSize][];

		markDirty();
	}

	public abstract OTreeMapEntryPersistent<K, V> load() throws IOException;

	public abstract OTreeMapEntryPersistent<K, V> save() throws IOException;

	@Override
	public OTreeMapEntry<K, V> getParent() {
		if (parentRid == null)
			return null;

		if (parentRid.isValid() && (parent == null || parent.get() == null)) {
			try {
				// LAZY LOADING OF THE PARENT NODE
				final OTreeMapEntry<K, V> node = pTree.createEntry(this, parentRid).load();

				parent = new SoftReference<OTreeMapEntry<K, V>>(node);
			} catch (IOException e) {
				OLogManager.instance().error(this, "Can't load the tree. The tree could be invalid.", e, ODatabaseException.class);
			}
		}

		return parent == null ? null : parent.get();
	}

	@Override
	public OTreeMapEntry<K, V> setParent(final OTreeMapEntry<K, V> iParent) {
		if (iParent != getParent()) {
			markDirty();

			this.parent = iParent == null ? null : new SoftReference<OTreeMapEntry<K, V>>(iParent);
			this.parentRid = iParent == null ? ORecordId.EMPTY_RECORD_ID : ((OTreeMapEntryPersistent<K, V>) iParent).record.getIdentity();
		}
		return iParent;
	}

	@Override
	public OTreeMapEntry<K, V> getLeft() {
		if (leftRid.isValid() && (left == null || left.get() == null)) {
			try {
				// LAZY LOADING OF THE LEFT LEAF
				final OTreeMapEntry<K, V> node = pTree.createEntry(this, leftRid).load();

				left = new SoftReference<OTreeMapEntry<K, V>>(node);
			} catch (IOException e) {
				OLogManager.instance().error(this, "Can't load the tree. The tree could be invalid.", e, ODatabaseException.class);
			}
		}

		return left == null ? null : left.get();
	}

	@Override
	public void setLeft(final OTreeMapEntry<K, V> iLeft) {
		if (iLeft == getLeft())
			return;

		markDirty();

		this.left = iLeft == null ? null : new SoftReference<OTreeMapEntry<K, V>>(iLeft);
		this.leftRid = iLeft == null ? ORecordId.EMPTY_RECORD_ID : ((OTreeMapEntryPersistent<K, V>) iLeft).record.getIdentity();

		if (iLeft != null && iLeft.getParent() != this)
			iLeft.setParent(this);
	}

	@Override
	public OTreeMapEntry<K, V> getRight() {
		if (rightRid.isValid() && (right == null || right.get() == null)) {
			// LAZY LOADING OF THE RIGHT LEAF
			try {
				final OTreeMapEntry<K, V> node = pTree.createEntry(this, rightRid).load();

				right = new SoftReference<OTreeMapEntry<K, V>>(node);
			} catch (IOException e) {
				OLogManager.instance().error(this, "Can't load tree. The tree could be invalid.", e, ODatabaseException.class);
			}
		}

		return right == null ? null : right.get();
	}

	@Override
	public OTreeMapEntry<K, V> setRight(final OTreeMapEntry<K, V> iRight) {
		if (iRight == getRight())
			return this;

		markDirty();

		this.right = iRight == null ? null : new SoftReference<OTreeMapEntry<K, V>>(iRight);
		this.rightRid = iRight == null ? ORecordId.EMPTY_RECORD_ID : ((OTreeMapEntryPersistent<K, V>) iRight).record.getIdentity();

		if (iRight != null && iRight.getParent() != this)
			iRight.setParent(this);

		return right.get();
	}

	@Override
	protected void copyFrom(final OTreeMapEntry<K, V> iSource) {
		markDirty();

		final OTreeMapEntryPersistent<K, V> source = (OTreeMapEntryPersistent<K, V>) iSource;

		parentRid = source.parentRid;
		leftRid = source.leftRid;
		rightRid = source.rightRid;

		serializedKeys = source.serializedKeys;
		serializedValues = source.serializedValues;

		super.copyFrom(source);
	}

	@Override
	protected void insert(final int iPosition, final K key, final V value) {
		markDirty();

		if (iPosition < size) {
			System.arraycopy(serializedKeys, iPosition, serializedKeys, iPosition + 1, size - iPosition);
			System.arraycopy(serializedValues, iPosition, serializedValues, iPosition + 1, size - iPosition);
		}

		serializedKeys[iPosition] = null;
		serializedValues[iPosition] = null;

		super.insert(iPosition, key, value);
	}

	@Override
	protected void remove() {
		markDirty();

		final int index = tree.getPageIndex();

		if (index == size - 1) {
			// LAST ONE: JUST REMOVE IT
		} else if (index > -1) {
			// SHIFT LEFT THE VALUES
			System.arraycopy(serializedKeys, index + 1, serializedKeys, index, size - index - 1);
			System.arraycopy(serializedValues, index + 1, serializedValues, index, size - index - 1);
		}

		// FREE RESOURCES
		serializedKeys[size - 1] = null;
		serializedValues[size - 1] = null;

		super.remove();
	}

	/**
	 * Return the key. Keys are lazy loaded.
	 * 
	 * @param iIndex
	 * @return
	 */
	@Override
	public K getKeyAt(final int iIndex) {
		if (keys[iIndex] == null)
			try {
				OProfiler.getInstance().updateStatistic("OTreeMapEntryP.unserializeKey", 1);

				keys[iIndex] = (K) pTree.keySerializer.fromStream(serializedKeys[iIndex]);
			} catch (IOException e) {

				OLogManager.instance().error(this, "Can't lazy load the key #" + iIndex + " in tree node " + this, e,
						OSerializationException.class);
			}

		return keys[iIndex];
	}

	@Override
	protected V getValueAt(final int iIndex) {
		if (values[iIndex] == null)
			try {
				OProfiler.getInstance().updateStatistic("OTreeMapEntryP.unserializeValue", 1);

				values[iIndex] = (V) pTree.valueSerializer.fromStream(serializedValues[iIndex]);
			} catch (IOException e) {

				OLogManager.instance().error(this, "Can't lazy load the value #" + iIndex + " in tree node " + this, e,
						OSerializationException.class);
			}

		return values[iIndex];
	}

	/**
	 * Invalidate serialized Value associated in order to be re-marshalled on the next node storing.
	 */
	@Override
	public V setValue(final V value) {
		markDirty();

		V oldValue = super.setValue(value);
		serializedValues[tree.getPageIndex()] = null;
		return oldValue;
	}

	/**
	 * Delete all the nodes recursively. IF they are not loaded in memory, load all the tree.
	 * 
	 * @throws IOException
	 */
	public void delete() throws IOException {
		// EARLY LOAD LEFT AND DELETE IT RECURSIVELY
		if (getLeft() != null)
			((OTreeMapEntryPersistent<K, V>) getLeft()).delete();

		// EARLY LOAD RIGHT AND DELETE IT RECURSIVELY
		if (getRight() != null)
			((OTreeMapEntryPersistent<K, V>) getRight()).delete();

		// DELETE MYSELF
		record.delete();

		// FORCE REMOVING OF K/V AND SEIALIZED K/V AS WELL
		keys = null;
		values = null;
		serializedKeys = null;
		serializedValues = null;
	}

	public final OSerializableStream fromStream(final byte[] iStream) throws IOException {
		final long timer = OProfiler.getInstance().startChrono();

		final OMemoryInputStream record = new OMemoryInputStream(iStream);

		try {
			pageSize = record.getAsShort();
			parentRid = getParent() != null ? ((OTreeMapEntryPersistent<K, V>) getParent()).record.getIdentity() : null;
			leftRid = new ORecordId().fromStream(record.getAsByteArray());
			rightRid = new ORecordId().fromStream(record.getAsByteArray());

			color = record.getAsBoolean();
			init();
			size = record.getAsShort();

			if (size > pageSize)
				throw new OConfigurationException("Loaded index with page size setted to " + pageSize
						+ " while the loaded was built with: " + size);

			// UNCOMPACT KEYS SEPARATELY
			serializedKeys = new byte[pageSize][];
			for (int i = 0; i < size; ++i) {
				serializedKeys[i] = record.getAsByteArray();
			}

			// KEYS WILL BE LOADED LAZY
			keys = (K[]) new Object[pageSize];

			// UNCOMPACT VALUES SEPARATELY
			serializedValues = new byte[pageSize][];
			for (int i = 0; i < size; ++i) {
				serializedValues[i] = record.getAsByteArray();
			}

			// VALUES WILL BE LOADED LAZY
			values = (V[]) new Object[pageSize];

			return this;
		} finally {
			record.close();

			OProfiler.getInstance().stopChrono("OTreeMapEntryP.fromStream", timer);
		}
	}

	public final byte[] toStream() throws IOException {
		final long timer = OProfiler.getInstance().startChrono();

		OMemoryOutputStream stream = pTree.entryRecordBuffer;

		try {
			stream.add((short) pageSize);
			stream.add(leftRid.toStream());
			stream.add(rightRid.toStream());

			stream.add(color);
			stream.add((short) size);

			serializeNewKeys();
			serializeNewValues();

			for (int i = 0; i < size; ++i)
				stream.add(serializedKeys[i]);

			for (int i = 0; i < size; ++i)
				stream.add(serializedValues[i]);

			stream.flush();

			return stream.getByteArray();

		} finally {
			stream.close();

			OProfiler.getInstance().stopChrono("OTreeMapEntryP.toStream", timer);
		}
	}

	/**
	 * Serialize only the new keys or the changed.
	 * 
	 * @throws IOException
	 */
	private void serializeNewKeys() throws IOException {
		for (int i = 0; i < size; ++i) {
			if (serializedKeys[i] == null) {
				OProfiler.getInstance().updateStatistic("OTreeMapEntryP.serializeValue", 1);

				serializedKeys[i] = pTree.keySerializer.toStream(keys[i]);
			}
		}
	}

	/**
	 * Serialize only the new values or the changed.
	 * 
	 * @throws IOException
	 */
	private void serializeNewValues() throws IOException {
		for (int i = 0; i < size; ++i) {
			if (serializedValues[i] == null) {
				OProfiler.getInstance().updateStatistic("OTreeMapEntryP.serializeKey", 1);

				serializedValues[i] = pTree.valueSerializer.toStream(values[i]);
			}
		}
	}

	@Override
	protected void setColor(boolean iColor) {
		if (iColor == color)
			return;

		markDirty();
		super.setColor(color);
	}

	private void markDirty() {
		if (record == null)
			return;

		record.setDirty();
		tree.getListener().signalNodeChanged(this);
	}

	@Override
	public int hashCode() {
		final ORID rid = record.getIdentity();
		return rid == null ? 0 : rid.hashCode();
	}
}
