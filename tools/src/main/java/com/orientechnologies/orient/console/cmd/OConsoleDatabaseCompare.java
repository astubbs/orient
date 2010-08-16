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
package com.orientechnologies.orient.console.cmd;

import java.io.IOException;

import com.orientechnologies.orient.console.OCommandListener;
import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.record.impl.ORecordColumn;
import com.orientechnologies.orient.core.record.impl.ORecordFlat;
import com.orientechnologies.orient.core.storage.ORawBuffer;
import com.orientechnologies.orient.core.storage.OStorage;

public class OConsoleDatabaseCompare extends OConsoleDatabaseImpExpAbstract {
	private OStorage	storage1;
	private OStorage	storage2;
	private int				differences	= 0;

	public OConsoleDatabaseCompare(final String iDb1URL, final String iDb2URL, final OCommandListener iListener) throws IOException {
		super(null, null, iListener);
		listener.onMessage("\nComparing two databases:\n1) " + iDb1URL + "\n2) " + iDb2URL + "\n");

		storage1 = Orient.instance().accessToLocalStorage(iDb1URL, "rw");
		storage1.open(0, null, null);

		storage2 = Orient.instance().accessToLocalStorage(iDb2URL, "rw");
		storage2.open(0, null, null);
	}

	public boolean compare() {
		try {
			compareClusters();
			compareRecords();

			if (differences == 0) {
				listener.onMessage("\n\nDatabases match.");
				return true;
			} else {
				listener.onMessage("\n\nDatabases not match. Found " + differences + " difference(s).");
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new ODatabaseExportException("Error on compare of databases '" + storage1.getName() + "' against '"
					+ storage2.getName() + "'", e);
		} finally {
			storage1.close();
			storage2.close();
		}
	}

	private boolean compareClusters() {
		listener.onMessage("\nStarting shallow comparison of clusters:");

		listener.onMessage("\nChecking the number of clusters...");

		if (storage1.getClusterNames().size() != storage1.getClusterNames().size()) {
			listener.onMessage("KO: cluster sizes are different: " + storage1.getClusterNames().size() + " <-> "
					+ storage1.getClusterNames().size());
			++differences;
		}

		int cluster2Id;
		boolean ok;

		for (String clusterName : storage1.getClusterNames()) {
			// CHECK IF THE CLUSTER IS INCLUDED
			if (includeClusters != null) {
				if (!includeClusters.contains(clusterName))
					continue;
			} else if (excludeClusters != null) {
				if (excludeClusters.contains(clusterName))
					continue;
			}

			ok = true;
			cluster2Id = storage2.getClusterIdByName(clusterName);

			listener.onMessage("\n- Checking cluster " + String.format("%-25s: ", "'" + clusterName + "'"));

			if (cluster2Id == -1) {
				listener.onMessage("KO: cluster name " + clusterName + " was not found on database " + storage2);
				++differences;
				ok = false;
			}

			if (cluster2Id != storage1.getClusterIdByName(clusterName)) {
				listener.onMessage("KO: cluster id is different for cluster " + clusterName + ": "
						+ storage1.getClusterIdByName(clusterName) + " <-> " + cluster2Id);
				++differences;
				ok = false;
			}

			if (storage1.count(cluster2Id) != storage2.count(cluster2Id)) {
				listener.onMessage("KO: number of records differents in cluster '" + clusterName + "' (id=" + cluster2Id + "): "
						+ storage1.count(cluster2Id) + " <-> " + storage2.count(cluster2Id));
				++differences;
				ok = false;
			}

			if (ok)
				listener.onMessage("OK");
		}

		listener.onMessage("\n\nShallow analysis done.");
		return true;
	}

	private boolean compareRecords() {
		listener.onMessage("\nStarting deep comparison record by record. It can takes some minutes. Wait please...");

		int clusterId;

		ORawBuffer buffer1, buffer2;

		for (String clusterName : storage1.getClusterNames()) {
			// CHECK IF THE CLUSTER IS INCLUDED
			if (includeClusters != null) {
				if (!includeClusters.contains(clusterName))
					continue;
			} else if (excludeClusters != null) {
				if (excludeClusters.contains(clusterName))
					continue;
			}

			clusterId = storage1.getClusterIdByName(clusterName);

			long db1Max = storage1.getClusterLastEntryPosition(clusterId);
			long db2Max = storage2.getClusterLastEntryPosition(clusterId);

			long clusterMax = Math.max(db1Max, db2Max);
			for (int i = 0; i < clusterMax; ++i) {
				buffer1 = i <= db1Max ? storage1.readRecord(null, 0, clusterId, i, null) : null;
				buffer2 = i <= db2Max ? storage2.readRecord(null, 0, clusterId, i, null) : null;

				if (buffer1 == null && buffer2 == null)
					// BOTH RECORD NULL, OK
					continue;
				else if (buffer1 == null && buffer2 != null) {
					// REC1 NULL
					listener.onMessage("\n- KO: RID=" + clusterId + ":" + i + " is null in DB1");
					++differences;
				} else if (buffer1 != null && buffer2 == null) {
					// REC2 NULL
					listener.onMessage("\n- KO: RID=" + clusterId + ":" + i + " is null in DB2");
					++differences;
				} else {
					if (buffer1.recordType != buffer2.recordType) {
						listener.onMessage("\n- KO: RID=" + clusterId + ":" + i + " recordType is different: " + (char) buffer1.recordType
								+ " <-> " + (char) buffer2.recordType);
						++differences;
					}

					if (buffer1.buffer == null && buffer2.buffer != null) {
						listener.onMessage("\n- KO: RID=" + clusterId + ":" + i + " content is different: null <-> " + buffer2.buffer.length);
						++differences;

					} else if (buffer1.buffer != null && buffer2.buffer == null) {
						listener.onMessage("\n- KO: RID=" + clusterId + ":" + i + " content is different: " + buffer1.buffer.length
								+ " <-> null");
						++differences;

					} else if (buffer1.buffer.length != buffer2.buffer.length) {
						listener.onMessage("\n- KO: RID=" + clusterId + ":" + i + " content length is different: " + buffer1.buffer.length
								+ " <-> " + buffer2.buffer.length);
						if (buffer1.recordType == ODocument.RECORD_TYPE || buffer1.recordType == ORecordFlat.RECORD_TYPE
								|| buffer1.recordType == ORecordColumn.RECORD_TYPE)
							listener.onMessage("\n--- REC1: " + new String(buffer1.buffer));
						if (buffer2.recordType == ODocument.RECORD_TYPE || buffer2.recordType == ORecordFlat.RECORD_TYPE
								|| buffer2.recordType == ORecordColumn.RECORD_TYPE)
							listener.onMessage("\n--- REC2: " + new String(buffer2.buffer));
						listener.onMessage("\n");

						++differences;

					} else {
						// CHECK BYTE PER BYTE
						for (int b = 0; b < buffer1.buffer.length; ++b) {
							if (buffer1.buffer[b] != buffer2.buffer[b]) {
								listener.onMessage("\n- KO: RID=" + clusterId + ":" + i + " content is different at byte #" + b + ": "
										+ buffer1.buffer[b] + " <-> " + buffer2.buffer[b]);
								listener.onMessage("\n--- REC1: " + new String(buffer1.buffer));
								listener.onMessage("\n--- REC2: " + new String(buffer2.buffer));
								listener.onMessage("\n");
								++differences;
								break;
							}
						}
					}
				}
			}
		}

		return true;
	}
}
