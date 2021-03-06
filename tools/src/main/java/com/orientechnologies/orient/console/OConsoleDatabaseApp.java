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
package com.orientechnologies.orient.console;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import com.orientechnologies.common.console.annotation.ConsoleCommand;
import com.orientechnologies.common.console.annotation.ConsoleParameter;
import com.orientechnologies.common.exception.OException;
import com.orientechnologies.orient.client.admin.OServerAdmin;
import com.orientechnologies.orient.client.remote.OEngineRemote;
import com.orientechnologies.orient.console.cmd.OConsoleDatabaseCompare;
import com.orientechnologies.orient.console.cmd.OConsoleDatabaseExport;
import com.orientechnologies.orient.console.cmd.OConsoleDatabaseImport;
import com.orientechnologies.orient.console.cmd.ODatabaseExportException;
import com.orientechnologies.orient.console.cmd.ODatabaseImportException;
import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.core.command.OCommandResultListener;
import com.orientechnologies.orient.core.db.document.ODatabaseDocument;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.iterator.ORecordIterator;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OProperty;
import com.orientechnologies.orient.core.metadata.schema.OProperty.INDEX_TYPE;
import com.orientechnologies.orient.core.record.ORecord;
import com.orientechnologies.orient.core.record.ORecordInternal;
import com.orientechnologies.orient.core.record.ORecordSchemaAwareAbstract;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.record.impl.ORecordBytes;
import com.orientechnologies.orient.core.record.impl.ORecordColumn;
import com.orientechnologies.orient.core.record.impl.ORecordFlat;
import com.orientechnologies.orient.core.serialization.serializer.record.ORecordSerializer;
import com.orientechnologies.orient.core.serialization.serializer.record.ORecordSerializerFactory;
import com.orientechnologies.orient.core.serialization.serializer.record.string.ORecordSerializerStringAbstract;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.orient.core.sql.query.OSQLAsynchQuery;
import com.orientechnologies.orient.core.storage.OStorage;
import com.orientechnologies.orient.enterprise.command.script.OCommandScript;

public class OConsoleDatabaseApp extends OrientConsole implements OCommandListener {
	protected ODatabaseDocument					currentDatabase;
	protected String										currentDatabaseName;
	protected ORecordInternal<?>				currentRecord;
	protected List<ORecordInternal<?>>	currentResultSet;

	public static void main(String[] args) {
		new OConsoleDatabaseApp(args);
	}

	public OConsoleDatabaseApp(String[] args) {
		super(args);
	}

	@Override
	protected void onBefore() {
		super.onBefore();

		currentResultSet = new ArrayList<ORecordInternal<?>>();

		properties.put("limit", "20");
	}

	@Override
	protected void onAfter() {
		super.onAfter();
		Orient.instance().shutdown();
	}

	@ConsoleCommand(aliases = { "use database" }, description = "Connect to a database")
	public void connect(
			@ConsoleParameter(name = "database-url", description = "The url of the database to connect in the format '<mode>:<path>'") String iDatabaseURL,
			@ConsoleParameter(name = "user", description = "User name") String iUserName,
			@ConsoleParameter(name = "password", description = "User password") String iUserPassword) {
		out.print("Connecting to database [" + iDatabaseURL + "] with user '" + iUserName + "'...");

		currentDatabase = new ODatabaseDocumentTx(iDatabaseURL);
		if (currentDatabase == null)
			throw new OException("Database " + iDatabaseURL + " not found.");
		currentDatabase.open(iUserName, iUserPassword);

		currentDatabaseName = currentDatabase.getName();

		out.println("OK");
	}

	@ConsoleCommand(aliases = { "close database" }, description = "Disconnect from the current database")
	public void disconnect() {
		checkCurrentDatabase();

		out.print("Disconnecting from the database [" + currentDatabaseName + "]...");

		currentDatabase.close();
		currentDatabase = null;
		currentDatabaseName = null;
		currentRecord = null;

		out.println("OK");
	}

	@ConsoleCommand(description = "Create a new database")
	public void createDatabase(
			@ConsoleParameter(name = "database-url", description = "The url of the database to create in the format '<mode>:<path>'") String iDatabaseURL,
			@ConsoleParameter(name = "user", description = "Server administrator name") String iUserName,
			@ConsoleParameter(name = "password", description = "Server administrator password") String iUserPassword,
			@ConsoleParameter(name = "storage-type", description = "The type of the storage between 'local' for disk-based database and 'memory' for in memory only database.") String iStorageType)
			throws IOException {
		out.println("Creating database [" + iDatabaseURL + "] using the storage type [" + iStorageType + "]...");

		if (iDatabaseURL.startsWith(OEngineRemote.NAME)) {
			// REMOTE CONNECTION
			final String dbURL = iDatabaseURL.substring(OEngineRemote.NAME.length() + 1);
			new OServerAdmin(dbURL).connect().createDatabase(iStorageType).close();
			connect(iDatabaseURL, iUserName, iUserPassword);

		} else {
			// LOCAL CONNECTION
			currentDatabase = new ODatabaseDocumentTx(iDatabaseURL);
			currentDatabase.create();
		}

		out.println("Database created successfully.");
		out.println("\nCurrent database is: " + iDatabaseURL);
	}

	@ConsoleCommand(description = "Create a new cluster in the current database. The cluster can be physical or logical.")
	public void createCluster(
			@ConsoleParameter(name = "cluster-name", description = "The name of the cluster to create") String iClusterName,
			@ConsoleParameter(name = "cluster-type", description = "Cluster type: 'physical' or 'logical'") String iClusterType) {
		checkCurrentDatabase();

		out.println("Creating cluster [" + iClusterName + "] of type '" + iClusterType + "' in database " + currentDatabaseName + "...");

		int clusterId = iClusterType.equalsIgnoreCase("physical") ? currentDatabase.addPhysicalCluster(iClusterName, iClusterName, -1)
				: currentDatabase.addLogicalCluster(iClusterName, currentDatabase.getClusterIdByName(OStorage.CLUSTER_INTERNAL_NAME));

		out.println((iClusterType.equalsIgnoreCase("physical") ? "Physical" : "Logical") + " cluster created correctly with id #"
				+ clusterId);
	}

	@ConsoleCommand(description = "Remove a cluster in the current database. The cluster can be physical or logical.")
	public void removeCluster(
			@ConsoleParameter(name = "cluster-name", description = "The name or the id of the cluster to remove") String iClusterName) {
		checkCurrentDatabase();

		out.println("Removing cluster [" + iClusterName + "] in database " + currentDatabaseName + "...");

		boolean result = currentDatabase.getStorage().removeCluster(iClusterName);

		if (!result) {
			// TRY TO GET AS CLUSTER ID
			try {
				int clusterId = Integer.parseInt(iClusterName);
				result = currentDatabase.getStorage().removeCluster(clusterId);
			} catch (Exception e) {
			}
		}

		if (result)
			out.println("Cluster correctly removed");
		else
			out.println("Can't find the cluster to remove");
	}

	@ConsoleCommand(description = "Load a record in memory and set it as the current one")
	public void loadRecord(
			@ConsoleParameter(name = "record-id", description = "The unique Record Id of the record to load. If you don't have the Record Id execute a query first") String iRecordId) {
		checkCurrentDatabase();

		currentRecord = currentDatabase.load(new ORecordId(iRecordId));
		displayRecord(null);

		out.println("OK");
	}

	@ConsoleCommand(splitInWords = false, description = "Insert a new record into the database")
	public void insert(@ConsoleParameter(name = "command-text", description = "The command text to execute") String iCommandText) {
		sqlCommand("insert", iCommandText, "\nInserted record %s in %f sec(s).\n");
	}

	@ConsoleCommand(splitInWords = false, description = "Update records in the database")
	public void update(@ConsoleParameter(name = "command-text", description = "The command text to execute") String iCommandText) {
		sqlCommand("update", iCommandText, "\nUpdated %d record(s) in %f sec(s).\n");
	}

	@ConsoleCommand(splitInWords = false, description = "Delete records from the database")
	public void delete(@ConsoleParameter(name = "command-text", description = "The command text to execute") String iCommandText) {
		sqlCommand("delete", iCommandText, "\nDelete %d record(s) in %f sec(s).\n");
	}

	@ConsoleCommand(splitInWords = false, description = "Grant privileges to a role")
	public void grant(@ConsoleParameter(name = "text", description = "Grant command") String iCommandText) {
		sqlCommand("grant", iCommandText, "\nPrivilege granted to the role: %s\n");
	}

	@ConsoleCommand(splitInWords = false, description = "Revoke privileges to a role")
	public void revoke(@ConsoleParameter(name = "text", description = "Revoke command") String iCommandText) {
		sqlCommand("revoke", iCommandText, "\nPrivilege revoked to the role: %s\n");
	}

	@ConsoleCommand(splitInWords = false, description = "Create a link from a JOIN")
	public void createLink(@ConsoleParameter(name = "command-text", description = "The command text to execute") String iCommandText) {
		sqlCommand("create", iCommandText, "\nCreated %d link(s) in %f sec(s).\n");
	}

	@ConsoleCommand(splitInWords = false, description = "Create a class")
	public void createClass(@ConsoleParameter(name = "command-text", description = "The command text to execute") String iCommandText) {
		sqlCommand("create", iCommandText, "\nClass created successfully with id=%d\n");
	}

	@ConsoleCommand(splitInWords = false, description = "Create a property")
	public void createProperty(
			@ConsoleParameter(name = "command-text", description = "The command text to execute") String iCommandText) {
		sqlCommand("create", iCommandText, "\nProperty created successfully with id=%d\n");
	}

	@ConsoleCommand(splitInWords = false, description = "Execute a query against the database and display the results")
	public void select(@ConsoleParameter(name = "query-text", description = "The query to execute") String iQueryText) {
		checkCurrentDatabase();

		if (iQueryText == null)
			return;

		iQueryText = iQueryText.trim();

		if (iQueryText.length() == 0 || iQueryText.equalsIgnoreCase("select"))
			return;

		iQueryText = "select " + iQueryText;

		currentResultSet.clear();

		final List<String> columns = new ArrayList<String>();

		final int limit = Integer.parseInt((String) properties.get("limit"));

		long start = System.currentTimeMillis();
		currentDatabase.query(new OSQLAsynchQuery<ODocument>(iQueryText, limit, new OCommandResultListener() {
			public boolean result(final Object iRecord) {
				// if (currentResultSet.size() >= limit) {
				// printHeaderLine(columns);
				// out.println("\nResultset contains more items not displayed (max=" + limit + ")");
				// return true;
				// }

				ORecordSchemaAwareAbstract<?> record = (ORecordSchemaAwareAbstract<?>) iRecord;

				dumpRecordInTable(currentResultSet.size(), record, columns);
				currentResultSet.add(record);
				return true;
			}

		}));

		if (currentResultSet.size() > 0 && currentResultSet.size() < limit)
			printHeaderLine(columns);

		out.println(currentResultSet.size() + " item(s) found. Query executed in " + (float) (System.currentTimeMillis() - start)
				/ 1000 + " sec(s).");
	}

	@ConsoleCommand(splitInWords = false, description = "Execute a script against the current database. If the database is remote, then the script will be executed remotely.")
	public void script(@ConsoleParameter(name = "script-text", description = "The script text to execute") final String iScriptText) {
		checkCurrentDatabase();

		if (iScriptText == null)
			return;

		long start = System.currentTimeMillis();

		currentResultSet.clear();

		Object result = new OCommandScript("Javascript", iScriptText).execute();

		out.printf("Script executed in %f sec(s). Value returned is: %s", (float) (System.currentTimeMillis() - start) / 1000, result);
	}

	@ConsoleCommand(description = "Create an index on a property")
	public void createIndex(
			@ConsoleParameter(name = "class.name", description = "Class and property names: <class>.<property>. Example: Account.name") String iTarget)
			throws IOException {
		createIndex(iTarget, "true");
	}

	@ConsoleCommand(description = "Create an index on a property")
	public void createIndex(
			@ConsoleParameter(name = "class.name", description = "Class and property names: <class>.<property>. Example: Account.name") final String iTarget,
			@ConsoleParameter(name = "type", description = "unique, not-unique, full-text") final String iType) throws IOException {
		out.println("\nCreating index on property [" + iTarget + "]...");

		String[] parts = iTarget.split("\\.");

		OClass cls = currentDatabase.getMetadata().getSchema().getClass(parts[0]);
		if (cls == null)
			throw new IllegalArgumentException("Class '" + parts[0] + "' not found");

		OProperty prop = cls.getProperty(parts[1]);
		if (prop == null)
			throw new IllegalArgumentException("Property '" + parts[1] + "' was not found in class '" + parts[0] + "'");

		out.println("Creating index, please wait...");

		prop.createIndex(INDEX_TYPE.valueOf(iType));

		out.println("\nIndex created succesfully");
	}

	@ConsoleCommand(description = "Browse all the records of a class")
	public void browseClass(@ConsoleParameter(name = "class-name", description = "The name of the class") final String iClassName) {
		checkCurrentDatabase();

		currentResultSet.clear();

		final List<String> columns = new ArrayList<String>();

		final int limit = Integer.parseInt((String) properties.get("limit"));

		ORecordIterator<?> it = currentDatabase.browseClass(iClassName);

		browseRecords(columns, limit, it);
	}

	@ConsoleCommand(description = "Browse all the records of a cluster")
	public void browseCluster(
			@ConsoleParameter(name = "cluster-name", description = "The name of the cluster") final String iClusterName) {
		checkCurrentDatabase();

		currentResultSet.clear();

		final List<String> columns = new ArrayList<String>();

		final int limit = Integer.parseInt((String) properties.get("limit"));

		ORecordIterator<?> it = currentDatabase.browseCluster(iClusterName);

		browseRecords(columns, limit, it);
	}

	@ConsoleCommand(aliases = { "display" }, description = "Display current record's attributes")
	public void displayRecord(
			@ConsoleParameter(name = "number", description = "The number of the record in the last result set") final String iRecordNumber) {
		checkCurrentDatabase();

		if (iRecordNumber == null)
			checkCurrentObject();
		else {
			int recNumber = Integer.parseInt(iRecordNumber);
			if (currentResultSet.size() == 0)
				throw new OException("No result set where to find the requested record. Execute a query first.");

			if (currentResultSet.size() <= recNumber)
				throw new OException("The record requested is not part of current result set (0"
						+ (currentResultSet.size() > 0 ? "-" + (currentResultSet.size() - 1) : "") + ")");

			currentRecord = currentResultSet.get(recNumber);
		}

		dumpRecordDetails();
	}

	@ConsoleCommand(aliases = { "status" }, description = "Display information about current status")
	public void info() {
		if (currentDatabaseName != null) {
			out.println("Current database: " + currentDatabaseName);
			clusters();
			classes();
		}
	}

	@ConsoleCommand(description = "Display all the configured clusters")
	public void clusters() {
		if (currentDatabaseName != null) {
			out.println("CLUSTERS:");
			out.println("--------------------+------+--------------------+-----------+");
			out.println("NAME                |  ID  | TYPE               | ELEMENTS  |");
			out.println("--------------------+------+--------------------+-----------+");

			int clusterId;
			long totalElements = 0;
			long count;
			for (String clusterName : currentDatabase.getClusterNames()) {
				try {
					clusterId = currentDatabase.getClusterIdByName(clusterName);
					count = currentDatabase.countClusterElements(clusterName);
					totalElements += count;
					out.printf("%-20s|%6d|%-20s|%10d |\n", clusterName, clusterId, clusterId < -1 ? "Logical" : "Physical", count);
				} catch (Exception e) {
				}
			}
			out.println("--------------------+------+--------------------+-----------+");
			out.printf("TOTAL                                            %10d |\n", totalElements);
			out.println("------------------------------------------------------------+\n");
		} else
			out.println("No database selected yet.");
	}

	@ConsoleCommand(description = "Display all the configured classes")
	public void classes() {
		if (currentDatabaseName != null) {
			out.println("CLASSES:");
			out.println("--------------------+------+------------------------------------------+-----------+");
			out.println("NAME                |  ID  | CLUSTERS                                 | ELEMENTS  |");
			out.println("--------------------+------+------------------------------------------+-----------+");

			long totalElements = 0;
			long count;
			for (OClass cls : currentDatabase.getMetadata().getSchema().getClasses()) {
				try {
					StringBuilder clusters = new StringBuilder();
					for (int i = 0; i < cls.getClusterIds().length; ++i) {
						if (i > 0)
							clusters.append(", ");
						clusters.append(currentDatabase.getClusterNameById(cls.getClusterIds()[i]));
					}

					count = currentDatabase.countClass(cls.getName());
					totalElements += count;

					out.printf("%-20s|%6d| %-40s |%10d |\n", cls.getName(), cls.getId(), clusters, count);
				} catch (Exception e) {
				}
			}
			out.println("--------------------+------+------------------------------------------+-----------+");
			out.printf("TOTAL                                                                  %10d |\n", totalElements);
			out.println("----------------------------------------------------------------------------------+");

		} else
			out.println("No database selected yet.");
	}

	@ConsoleCommand(description = "Display all the keys in the database dictionary")
	public void dictionaryKeys() {
		checkCurrentDatabase();

		Set<String> keys = currentDatabase.getDictionary().keySet();

		out.println("Found " + keys.size() + " keys:");

		int i = 0;
		for (String k : keys) {
			out.print(String.format("#%d: %s\n", i++, k));
		}
	}

	@ConsoleCommand(description = "Loookup for a record using the dictionary. If found set it as the current record")
	public void dictionaryGet(@ConsoleParameter(name = "key", description = "The key to search") final String iKey) {
		checkCurrentDatabase();

		currentRecord = currentDatabase.getDictionary().get(iKey);
		if (currentRecord == null)
			out.println("Entry not found in dictionary.");
		else {
			currentRecord.load();
			displayRecord(null);
		}
	}

	@ConsoleCommand(description = "Insert or modify an entry in the database dictionary. The entry is composed by key=String, value=record-id")
	public void dictionaryPut(
			@ConsoleParameter(name = "key", description = "The key to bind") final String iKey,
			@ConsoleParameter(name = "record-id", description = "The record-id of the record to bind to the key passes") final String iRecordId) {
		checkCurrentDatabase();

		currentRecord = currentDatabase.load(new ORecordId(iRecordId));
		if (currentRecord == null)
			out.println("Error: record with id '" + iRecordId + "' was not found in database");
		else {
			currentDatabase.getDictionary().put(iKey, (ODocument) currentRecord);
			displayRecord(null);
			out.println("The entry " + iKey + "=" + iRecordId + " has been inserted in the database dictionary");
		}
	}

	@ConsoleCommand(description = "Remove the association in the dictionary")
	public void dictionaryRemove(@ConsoleParameter(name = "key", description = "The key to remove") final String iKey) {
		checkCurrentDatabase();

		currentRecord = currentDatabase.getDictionary().remove(iKey);
		if (currentRecord == null)
			out.println("Entry not found in dictionary.");
		else {
			out.println("Entry removed from the dictionary. Last value of entry was: ");
			displayRecord(null);
		}
		currentRecord = null;
	}

	@ConsoleCommand(description = "Export a database")
	public void exportDatabase(@ConsoleParameter(name = "output-file", description = "Output file path") final String iOutputFilePath)
			throws IOException {
		out.println("Exporting current database to: " + iOutputFilePath + "...");

		try {
			new OConsoleDatabaseExport(currentDatabase, iOutputFilePath, this).exportDatabase().close();
		} catch (ODatabaseExportException e) {
			out.println("ERROR: " + e.toString());
		}
	}

	@ConsoleCommand(description = "Compare two databases")
	public void compareDatabases(@ConsoleParameter(name = "db1-url", description = "URL of the first database") final String iDb1URL,
			@ConsoleParameter(name = "db2-url", description = "URL of the second database") final String iDb2URL) throws IOException {
		try {
			new OConsoleDatabaseCompare(iDb1URL, iDb2URL, this).compare();
		} catch (ODatabaseExportException e) {
			out.println("ERROR: " + e.toString());
		}
	}

	@ConsoleCommand(description = "Import a database into the current one")
	public void importDatabase(@ConsoleParameter(name = "imput-file", description = "Input file path") final String iInputFilePath)
			throws IOException {
		out.println("Importing database from file " + iInputFilePath + "...");

		try {
			new OConsoleDatabaseImport(currentDatabase, iInputFilePath, this).importDatabase().close();
		} catch (ODatabaseImportException e) {
			out.println("ERROR: " + e.toString());
		}
	}

	@ConsoleCommand(description = "Export the current record in the requested format")
	public void exportRecord(@ConsoleParameter(name = "format", description = "Format, such as 'json'") final String iFormat)
			throws IOException {
		checkCurrentDatabase();
		checkCurrentObject();

		final ORecordSerializer serializer = ORecordSerializerFactory.instance().getFormat(iFormat.toLowerCase());

		if (serializer == null) {
			out.println("ERROR: Format '" + iFormat + "' was not found.");
			printSupportedSerializerFormat();
			return;
		} else if (!(serializer instanceof ORecordSerializerStringAbstract)) {
			out.println("ERROR: Format '" + iFormat + "' doesn't export as text.");
			printSupportedSerializerFormat();
			return;
		}

		try {
			out.println(((ORecordSerializerStringAbstract) serializer).toString(currentRecord, null));
		} catch (ODatabaseExportException e) {
			out.println("ERROR: " + e.toString());
		}
	}

	@ConsoleCommand(description = "Return all the configured properties")
	public void properties() {
		out.println("PROPERTIES:");
		out.println("+---------------------+----------------------+");
		out.printf("| %-20s| %-20s |\n", "NAME", "VALUE");
		out.println("+---------------------+----------------------+");
		for (Entry<String, Object> p : properties.entrySet()) {
			out.printf("| %-20s= %-20s |\n", p.getKey(), p.getValue());
		}
		out.println("+---------------------+----------------------+");
	}

	@ConsoleCommand(description = "Return the value of a property")
	public void get(@ConsoleParameter(name = "property-name", description = "Name of the property") final String iPropertyName) {
		Object value = properties.get(iPropertyName);

		out.println();

		if (value == null)
			out.println("Property '" + iPropertyName + "' is not setted");
		else
			out.println(iPropertyName + " = " + value);
	}

	@ConsoleCommand(description = "Change the value of a property")
	public void set(@ConsoleParameter(name = "property-name", description = "Name of the property") final String iPropertyName,
			@ConsoleParameter(name = "property-value", description = "Value to set") final String iPropertyValue) {
		Object prevValue = properties.get(iPropertyName);

		out.println();

		if (prevValue != null)
			out.println("Previous value was: " + prevValue);

		properties.put(iPropertyName, iPropertyValue);

		out.println();
		out.println(iPropertyName + " = " + iPropertyValue);
	}

	protected void checkCurrentDatabase() {
		if (currentDatabase == null)
			throw new OException("Database not selected. Use 'connect <database-name>' to connect to a database.");
	}

	protected void checkCurrentObject() {
		if (currentRecord == null)
			throw new OException("The is no current object selected: create a new one or load it");
	}

	protected void dumpRecordInTable(final int iIndex, final ORecordSchemaAwareAbstract<?> iRecord, final List<String> iColumns) {
		// CHECK IF HAVE TO ADD NEW COLUMN (BECAUSE IT CAN BE SCHEMA-LESS)
		List<String> recordColumns = new ArrayList<String>();
		for (String fieldName : iRecord.fieldNames())
			recordColumns.add(fieldName);

		dumpRecordInTable(iIndex, iRecord, recordColumns, iColumns);
	}

	protected void dumpRecordInTable(final int iIndex, final ORecordInternal<?> iRecord, final List<String> iRecordColumns,
			final List<String> iColumns) {
		// CHECK IF HAVE TO ADD NEW COLUMN (BECAUSE IT CAN BE SCHEMA-LESS)
		for (String fieldName : iRecordColumns) {
			boolean foundCol = false;
			for (String colName : iColumns) {
				if (fieldName.equals(colName)) {
					foundCol = true;
					break;
				}
			}

			if (!foundCol)
				// NEW COLUMN: ADD IT
				iColumns.add(fieldName);
		}

		if (iIndex == 0) {
			out.printf("\n");
			printHeaderLine(iColumns);
			out.print("  #| REC ID |");
			int col = 0;
			for (String colName : iColumns) {
				if (col++ > 0)
					out.printf("|");
				out.printf("%-20s", colName.toUpperCase());
			}
			out.printf("\n");
			printHeaderLine(iColumns);
		}

		// FORMAT THE LINE DYNAMICALLY
		StringBuilder format = new StringBuilder("%3d|%8s");
		List<Object> vargs = new ArrayList<Object>();
		vargs.add(iIndex);
		vargs.add(iRecord.getIdentity());

		try {
			Object value = null;
			for (String colName : iColumns) {
				format.append("|%-20s");

				if (iRecord instanceof ORecordSchemaAwareAbstract<?>)
					value = ((ORecordSchemaAwareAbstract<?>) iRecord).field(colName);
				else if (iRecord instanceof ORecordColumn)
					value = ((ORecordColumn) iRecord).field(Integer.parseInt(colName));

				if (value instanceof Collection<?>)
					value = "[" + ((Collection<?>) value).size() + "]";
				else if (value instanceof ORecord<?>)
					value = ((ORecord<?>) value).getIdentity().toString();

				vargs.add(value);
			}

			out.println(String.format(format.toString(), vargs.toArray()));
		} catch (Throwable t) {
			out.printf("%3d|%8s|%s\n", iIndex, iRecord.getIdentity(), "Error on loading record dued to: " + t);
		}
	}

	private void printHeaderLine(final List<String> iColumns) {
		if (iColumns.size() > 0) {
			out.print("---+--------");
			for (int i = 0; i < iColumns.size(); ++i) {
				out.print("+");
				for (int k = 0; k < 20; ++k)
					out.print("-");
			}
		}
		out.print("\n");
	}

	private void dumpRecordDetails() {
		if (currentRecord instanceof ODocument) {
			ODocument rec = (ODocument) currentRecord;
			out.println("--------------------------------------------------");
			out.printf("ODocument - Class: %s   id: %s   v.%d\n", rec.getClassName(), rec.getIdentity().toString(), rec.getVersion());
			out.println("--------------------------------------------------");
			for (String fieldName : rec.fieldNames()) {
				out.printf("%20s : %-20s\n", fieldName, rec.field(fieldName));
			}

		} else if (currentRecord instanceof ORecordColumn) {
			ORecordColumn rec = (ORecordColumn) currentRecord;
			out.println("--------------------------------------------------");
			out.printf("Column - id: %s   v.%d\n", rec.getIdentity().toString(), rec.getVersion());
			out.println("--------------------------------------------------");
			for (int i = 0; i < rec.size(); ++i) {
				if (i > 0)
					out.print(", ");
				out.print(rec.field(i));
			}

		} else if (currentRecord instanceof ORecordFlat) {
			ORecordFlat rec = (ORecordFlat) currentRecord;
			out.println("--------------------------------------------------");
			out.printf("Flat - record id: %s   v.%d\n", rec.getIdentity().toString(), rec.getVersion());
			out.println("--------------------------------------------------");
			out.print(rec.value());

		} else if (currentRecord instanceof ORecordBytes) {
			ORecordBytes rec = (ORecordBytes) currentRecord;
			out.println("--------------------------------------------------");
			out.printf("Flat - record id: %s   v.%d\n", rec.getIdentity().toString(), rec.getVersion());
			out.println("--------------------------------------------------");
			byte[] value = rec.toStream();
			for (int i = 0; i < Array.getLength(value); ++i) {
				out.printf("%03d", Array.getByte(value, i));
			}

		} else {
			out.println("--------------------------------------------------");
			out.printf("%s - record id: %s   v.%d\n", currentRecord.getClass().getSimpleName(), currentRecord.getIdentity().toString(),
					currentRecord.getVersion());
		}
		out.println();
	}

	public void onMessage(String iText) {
		out.print(iText);
	}

	private void printSupportedSerializerFormat() {
		out.println("Supported formats are:");

		for (ORecordSerializer s : ORecordSerializerFactory.instance().getFormats()) {
			if (s instanceof ORecordSerializerStringAbstract)
				out.println("- " + s.toString());
		}
	}

	private void browseRecords(final List<String> columns, final int limit, ORecordIterator<?> it) {
		while (it.hasNext()) {
			currentRecord = it.next();

			if (currentRecord instanceof ORecordSchemaAwareAbstract<?>)
				dumpRecordInTable(currentResultSet.size(), (ORecordSchemaAwareAbstract<?>) currentRecord, columns);
			else if (currentRecord instanceof ORecordColumn) {
				// CREATE NUMBERED COLUMNS
				List<String> cols = new ArrayList<String>();
				for (int i = 0; i < ((ORecordColumn) currentRecord).size(); ++i)
					cols.add(String.valueOf(i));
				dumpRecordInTable(currentResultSet.size(), (ORecordColumn) currentRecord, cols, columns);
			} else if (currentRecord != null) {
				dumpRecordDetails();
				out.println();
			}

			currentResultSet.add(currentRecord);

			if (currentResultSet.size() >= limit) {
				printHeaderLine(columns);
				out.println("\nResultset contains more items not displayed (max=" + limit + ")");
				return;
			}
		}

		printHeaderLine(columns);
	}

	private Object sqlCommand(final String iExpectedCommand, String iReceivedCommand, final String iMessage) {
		checkCurrentDatabase();

		if (iReceivedCommand == null)
			return null;

		long start = System.currentTimeMillis();

		iReceivedCommand = iExpectedCommand + " " + iReceivedCommand.trim();

		currentResultSet.clear();

		final Object result = new OCommandSQL(iReceivedCommand).setDatabase(currentDatabase).execute();

		if (result != null)
			out.printf(iMessage, result, (float) (System.currentTimeMillis() - start) / 1000);

		return result;
	}
}
