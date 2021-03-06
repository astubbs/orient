package com.orientechnologies.orient.kv.index;

import com.orientechnologies.orient.core.db.record.ODatabaseBinary;
import com.orientechnologies.orient.core.db.record.ODatabaseRecord;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.serialization.serializer.stream.OStreamSerializer;
import com.orientechnologies.orient.core.type.tree.OTreeMapDatabase;
import com.orientechnologies.orient.kv.OSharedBinaryDatabase;

/**
 * Wrapper class for persistent tree map. It handles the asynchronous commit of changes done by the external
 * OTreeMapPersistentAsynchThread singleton thread.
 * 
 * @author Luca Garulli
 * 
 * @param <K>
 * @param <V>
 * @see OTreeMapPersistentAsynchThread
 */
@SuppressWarnings("serial")
public class OTreeMapPersistentAsynch<K, V> extends OTreeMapDatabase<K, V> {

	public OTreeMapPersistentAsynch(final ODatabaseRecord<?> iDatabase, final String iClusterName,
			final OStreamSerializer iKeySerializer, final OStreamSerializer iValueSerializer) {
		super(iDatabase, iClusterName, iKeySerializer, iValueSerializer);
		OTreeMapPersistentAsynchThread.getInstance().registerMap(this);
	}

	public OTreeMapPersistentAsynch(final ODatabaseRecord<?> iDatabase, final ORID iRID) {
		super(iDatabase, iRID);
		OTreeMapPersistentAsynchThread.getInstance().registerMap(this);
	}

	/**
	 * Doesn't commit changes since they are scheduled by the external OTreeMapPersistentAsynchThread singleton thread.
	 * 
	 * @see OTreeMapPersistentAsynchThread#execute()
	 */
	@Override
	public void commitChanges(final ODatabaseRecord<?> iDatabase) {
	}

	/**
	 * Commits changes for real. It's called by OTreeMapPersistentAsynchThread singleton thread.
	 * 
	 * @see OTreeMapPersistentAsynchThread#execute()
	 */
	public void executeCommitChanges() {
		ODatabaseBinary db = null;

		try {
			db = OSharedBinaryDatabase.acquire(database.getName());

			super.commitChanges(db);

		} catch (InterruptedException e) {
			e.printStackTrace();

		} finally {

			if (db != null)
				OSharedBinaryDatabase.release(db);
		}
	}
}
