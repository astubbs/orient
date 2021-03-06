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
package com.orientechnologies.orient.core.iterator;

import com.orientechnologies.orient.core.db.record.ODatabaseRecord;
import com.orientechnologies.orient.core.db.record.ODatabaseRecordAbstract;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.record.ORecordInternal;

/**
 * Iterator class to browse forward and backward the records of a cluster.
 * 
 * @author Luca Garulli
 * 
 * @param <T>
 *          Record Type
 */
public class ORecordIteratorCluster<REC extends ORecordInternal<?>> extends ORecordIterator<REC> {
	protected int		currentClusterId;
	protected long	rangeFrom;
	protected long	rangeTo;
	protected long	lastClusterPosition;
	protected long	totalAvailableRecords;

	public ORecordIteratorCluster(final ODatabaseRecord<REC> iDatabase, final ODatabaseRecordAbstract<REC> iLowLevelDatabase,
			final int iClusterId) {
		super(iDatabase, iLowLevelDatabase);
		if (iClusterId == ORID.CLUSTER_ID_INVALID)
			throw new IllegalArgumentException("The clusterId is invalid");

		currentClusterId = iClusterId;
		rangeFrom = -1;
		rangeTo = -1;

		lastClusterPosition = database.getStorage().getClusterLastEntryPosition(currentClusterId);
		totalAvailableRecords = database.countClusterElements(currentClusterId);
	}

	@Override
	public boolean hasPrevious() {
		if (limit > -1 && browsedRecords >= limit)
			// LIMIT REACHED
			return false;

		return currentClusterPosition > getRangeFrom() + 1;
	}

	public boolean hasNext() {
		if (limit > -1 && browsedRecords >= limit)
			// LIMIT REACHED
			return false;

		if (browsedRecords >= totalAvailableRecords)
			return false;

		return currentClusterPosition < getRangeTo();
	}

	/**
	 * Return the element at the current position and move backward the cursor to the previous position available.
	 * 
	 * @return the previous record found, otherwise NULL when no more records are found.
	 */
	@Override
	public REC previous() {
		final REC record = getRecord();

		// ITERATE UNTIL THE PREVIOUS GOOD RECORD
		while (hasPrevious()) {
			if (readCurrentRecord(record, -1) != null)
				// FOUND
				return record;
		}

		return null;
	}

	/**
	 * Return the element at the current position and move forward the cursor to the next position available.
	 * 
	 * @return the next record found, otherwise NULL when no more records are found.
	 */
	public REC next() {
		// ITERATE UNTIL THE NEXT GOOD RECORD
		while (hasNext()) {
			REC record = getRecord();

			record = readCurrentRecord(record, +1);
			if (record != null)
				// FOUND
				return record;
		}

		return null;
	}

	public REC current() {
		final REC record = getRecord();
		return readCurrentRecord(record, 0);
	}

	/**
	 * Move the iterator to the begin of the range. If no range was specified move to the first record of the cluster.
	 * 
	 * @return The object itself
	 */
	@Override
	public ORecordIterator<REC> begin() {
		currentClusterPosition = getRangeFrom();
		return this;
	}

	/**
	 * Move the iterator to the end of the range. If no range was specified move to the last record of the cluster.
	 * 
	 * @return The object itself
	 */
	@Override
	public ORecordIterator<REC> last() {
		currentClusterPosition = getRangeTo();
		return this;
	}

	/**
	 * Define the range where move the iterator forward and backward.
	 * 
	 * @param iFrom
	 *          Lower bound limit of the range
	 * @param iEnd
	 *          Upper bound limit of the range
	 * @return
	 */
	public ORecordIteratorCluster<REC> setRange(final long iFrom, final long iEnd) {
		currentClusterPosition = iFrom;
		rangeTo = iEnd;
		return this;
	}

	/**
	 * Return the lower bound limit of the range if any, otherwise 0.
	 * 
	 * @return
	 */
	public long getRangeFrom() {
		return Math.max(rangeFrom, -1);
	}

	/**
	 * Return the upper bound limit of the range if any, otherwise the last record.
	 * 
	 * @return
	 */
	public long getRangeTo() {
		if (!liveUpdated)
			return lastClusterPosition + 1;

		final long limit = database.getStorage().getClusterLastEntryPosition(currentClusterId) + 1;
		if (rangeTo > -1)
			return Math.min(rangeTo, limit);
		return limit;
	}

	/**
	 * Tell to the iterator that the upper limit must be checked at every cycle. Useful when concurrent deletes or additions change
	 * the size of the cluster while you're browsing it. Default is false.
	 * 
	 * @param iLiveUpdated
	 *          True to activate it, otherwise false (default)
	 * @see #isLiveUpdated()
	 */
	@Override
	public ORecordIterator<REC> setLiveUpdated(boolean iLiveUpdated) {
		super.setLiveUpdated(iLiveUpdated);

		// SET THE UPPER LIMIT TO -1 IF IT'S ENABLED
		lastClusterPosition = iLiveUpdated ? -1 : database.getStorage().getClusterLastEntryPosition(currentClusterId);
		totalAvailableRecords = database.countClusterElements(currentClusterId);

		return this;
	}

	/**
	 * Read the current record and increment the counter if the record was found.
	 * 
	 * @param iRecord
	 * @return
	 */
	private REC readCurrentRecord(REC iRecord, final int iMovement) {
		if (limit > -1 && browsedRecords >= limit)
			// LIMIT REACHED
			return null;

		currentClusterPosition += iMovement;

		iRecord = lowLevelDatabase.executeReadRecord(currentClusterId, currentClusterPosition, iRecord, fetchPlan);
		if (iRecord != null)
			browsedRecords++;

		return iRecord;
	}
}
