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

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import com.orientechnologies.orient.core.cache.OCacheRecord;
import com.orientechnologies.orient.core.intent.OIntent;
import com.orientechnologies.orient.core.storage.OStorage;

/**
 * Generic Database interface. Represents the lower level of the Database providing raw API to access to the raw records.<br/>
 * Limits:
 * <ul>
 * <li>Maximum records per cluster/class = <b>9.223.372.036 Billions</b>: 2^63 = 9.223.372.036.854.775.808 records</li>
 * <li>Maximum records per database = <b>302.231.454.903.657 Billions</b>: 2^15 clusters x 2^63 records = (2^78) 32.768 *
 * 9,223.372.036.854.775.808 = 302.231,454.903.657.293.676.544 records</li>
 * <li>Maximum storage per data-segment = <b>9.223.372 Terabytes</b>: 2^63 bytes = 9,223.372.036.854.775.808 Exabytes</li>
 * <li>Maximum storage per database = <b>19.807.040.628.566.084 Terabytes</b>: 2^31 data-segments x 2^63 bytes = (2^94)
 * 2.147.483.648 x 9,223.372.036.854.775.808 Exabytes = 19.807,040.628.566.084.398.385.987.584 Yottabytes</li>
 * </ul>
 * 
 * @author Luca Garulli
 * 
 */
public interface ODatabase {
	/**
	 * Opens a database using the user and password received as arguments.
	 * 
	 * @param iUserName
	 *          Username to login
	 * @param iUserPassword
	 *          Password associated to the user
	 * @return The Database instance itself giving a "fluent interface". Useful to call multiple methods in chain.
	 */
	public <DB extends ODatabase> DB open(final String iUserName, final String iUserPassword);

	/**
	 * Creates a new database.
	 * 
	 * @return The Database instance itself giving a "fluent interface". Useful to call multiple methods in chain.
	 */
	public <DB extends ODatabase> DB create();

	/**
	 * Declares an intent to the database. Intents aim to optimize common use cases.
	 * 
	 * @param iIntent
	 *          The intent
	 * @param iParams
	 *          Additional parameters
	 */
	public void declareIntent(OIntent iIntent, Object... iParams);

	/**
	 * Checks if the database exists.
	 * 
	 * @return True if already exists, otherwise false.
	 */
	public boolean exists();

	/**
	 * Closes an opened database.
	 */
	public void close();

	/**
	 * Returns the database id. The id is auto-generated on opening and creation and it's unique in the current JVM.
	 * 
	 * @return
	 */
	public int getId();

	/**
	 * Returns the database name.
	 * 
	 * @return Name of the database
	 */
	public String getName();

	/**
	 * Returns the underlying storage implementation.
	 * 
	 * @return The underlying storage implementation
	 * @see OStorage
	 */
	public OStorage getStorage();

	/**
	 * Returns the database cache. Can't be null.
	 * 
	 * @return Current cache.
	 */
	public OCacheRecord getCache();

	/**
	 * Returns the default cluster id. If not specified all the new entities will be stored in the default cluster.
	 * 
	 * @return The default cluster id
	 */
	public int getDefaultClusterId();

	/**
	 * Returns all the names of the clusters.
	 * 
	 * @return Collection of cluster names.
	 */
	public Collection<String> getClusterNames();

	/**
	 * Returns the cluster id by name.
	 * 
	 * @param iClusterName
	 *          Cluster name
	 * @return The id of searched cluster.
	 */
	public int getClusterIdByName(String iClusterName);

	/**
	 * Returns the cluster type.
	 * 
	 * @param iClusterName
	 *          Cluster name
	 * @return The cluster type as string
	 */
	public String getClusterType(String iClusterName);

	/**
	 * Returns the cluster name by id.
	 * 
	 * @param iClusterId
	 *          Cluster id
	 * @return The name of searched cluster.
	 */
	public String getClusterNameById(int iClusterId);

	/**
	 * Checks if the database is closed.
	 * 
	 * @return true if is closed, otherwise false.
	 */
	public boolean isClosed();

	/**
	 * Counts all the entities in the specified cluster id.
	 * 
	 * @param iCurrentClusterId
	 *          Cluster id
	 * @return Total number of entities contained in the specified cluster
	 */
	public long countClusterElements(int iCurrentClusterId);

	/**
	 * Counts all the entities in the specified cluster ids.
	 * 
	 * @param iClusterIds
	 *          Array of cluster ids Cluster id
	 * @return Total number of entities contained in the specified clusters
	 */
	public long countClusterElements(int[] iClusterIds);

	/**
	 * Counts all the entities in the specified cluster name.
	 * 
	 * @param iClusterName
	 *          Cluster name
	 * @return Total number of entities contained in the specified cluster
	 */
	public long countClusterElements(String iClusterName);

	/**
	 * Adds a logical cluster. Logical clusters don't need separate files since are stored inside a OTreeMap instance. Access is
	 * slower than the physical cluster but the database size is reduced and less files are requires. This matters in some OS where a
	 * single process has limitation for the number of files can open. Most accessed entities should be stored inside a physical
	 * cluster.
	 * 
	 * @param iClusterName
	 *          Cluster name
	 * @param iPhyClusterContainerId
	 *          Physical cluster where to store all the entities of this logical cluster
	 * @return Cluster id
	 */
	public int addLogicalCluster(String iClusterName, int iPhyClusterContainerId);

	/**
	 * Adds a physical cluster. Physical clusters need separate files. Access is faster than the logical cluster but the database size
	 * is higher and more files are requires. This matters in some OS where a single process has limitation for the number of files
	 * can open. Most accessed entities should be stored inside a physical cluster.
	 * 
	 * @param iClusterName
	 *          Cluster name
	 * @param iPhyClusterContainerId
	 *          Physical cluster where to store all the entities of this logical cluster
	 * @return Cluster id
	 */
	public int addPhysicalCluster(String iClusterName, String iClusterFileName, int iStartSize);

	/**
	 * Internal. Adds a data segment where to store record content.
	 */
	public int addDataSegment(String iSegmentName, String iSegmentFileName);

	/**
	 * Sets a property value
	 * 
	 * @param iName
	 *          Property name
	 * @param iValue
	 *          new value to set
	 * @return The previous value if any, otherwise null
	 */
	public Object setProperty(String iName, Object iValue);

	/**
	 * Gets the property value.
	 * 
	 * @param iName
	 *          Property name
	 * @return The previous value if any, otherwise null
	 */
	public Object getProperty(String iName);

	/**
	 * Returns an iterator of the property entries
	 */
	public Iterator<Map.Entry<String, Object>> getProperties();
}
