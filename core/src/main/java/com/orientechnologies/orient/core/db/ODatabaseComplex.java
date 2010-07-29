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

import java.util.List;
import java.util.Set;

import com.orientechnologies.orient.core.command.OCommandRequest;
import com.orientechnologies.orient.core.db.object.ODatabaseObject;
import com.orientechnologies.orient.core.db.record.ODatabaseRecord;
import com.orientechnologies.orient.core.dictionary.ODictionary;
import com.orientechnologies.orient.core.hook.ORecordHook;
import com.orientechnologies.orient.core.hook.ORecordHook.TYPE;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.metadata.OMetadata;
import com.orientechnologies.orient.core.metadata.security.OSecurity;
import com.orientechnologies.orient.core.metadata.security.OUser;
import com.orientechnologies.orient.core.query.OQuery;
import com.orientechnologies.orient.core.storage.OStorage;
import com.orientechnologies.orient.core.tx.OTransaction.TXTYPE;

/**
 * Database interface that represents a complex database. It extends the base ODatabase interface adding all the higher-level APIs
 * to treats records. Entities can be implementations of ORecord class for ODatabaseRecord or any POJO for ODatabaseObject. The
 * behaviour of the datastore depends by the OStorage implementation used.
 * 
 * @author Luca Garulli
 * 
 * @see ODatabaseRecord
 * @see ODatabaseObject
 * @see OStorage
 * @param <T>
 */
public interface ODatabaseComplex<T extends Object> extends ODatabase, OUserObject2RecordHandler {

	/**
	 * Creates a new entity instance.
	 * 
	 * @return The new instance.
	 */
	public T newInstance();

	/**
	 * Returns the current user logged into the database.
	 * 
	 * @see OSecurity
	 */
	public OUser getUser();

	/**
	 * Loads the entity and return it.
	 * 
	 * @param iObject
	 *          The entity to load. If the entity was already loaded it will be reloaded and all the changes will be lost.
	 * @return
	 */
	public T load(final T iObject);

	/**
	 * Loads the entity by the Record ID.
	 * 
	 * @param iRecordId
	 *          The unique record id of the entity to load.
	 * @return
	 */
	public T load(final ORID iRecordId);

	/**
	 * Saves an entity. If the entity is not dirty, then the operation will be ignored. For custom entity implementations assure to
	 * set the entity as dirty.
	 * 
	 * @param iObject
	 *          The entity to save
	 * @return The Database instance itself giving a "fluent interface". Useful to call multiple methods in chain.
	 */
	public ODatabaseComplex<T> save(T iObject);

	/**
	 * Saves an entity in the specified cluster. If the entity is not dirty, then the operation will be ignored. For custom entity
	 * implementations assure to set the entity as dirty. If the cluster doesn't exist, an error will be thrown.
	 * 
	 * @param iObject
	 *          The entity to save
	 * @param iClusterName
	 *          Name of the cluster where to save
	 * @return The Database instance itself giving a "fluent interface". Useful to call multiple methods in chain.
	 */
	public ODatabaseComplex<T> save(T iObject, String iClusterName);

	/**
	 * Deletes an entity from the database.
	 * 
	 * @param iObject
	 *          The entity to delete.
	 * @return The Database instance itself giving a "fluent interface". Useful to call multiple methods in chain.
	 */
	public ODatabaseComplex<T> delete(T iObject);

	/**
	 * Begins a new transaction. By default the type is OPTIMISTIC. If a previous transaction was started it will be rollbacked and
	 * closed before to start a new one. A transaction once begun has to be closed by calling the {@link #commit()} or
	 * {@link #rollback()}.
	 * 
	 * @return The Database instance itself giving a "fluent interface". Useful to call multiple methods in chain.
	 */
	public ODatabaseComplex<T> begin();

	/**
	 * Begins a new transaction specifying the transaction type. If a previous transaction was started it will be rollbacked and
	 * closed before to start a new one. A transaction once begun has to be closed by calling the {@link #commit()} or
	 * {@link #rollback()}.
	 * 
	 * @return The Database instance itself giving a "fluent interface". Useful to call multiple methods in chain.
	 */
	public ODatabaseComplex<T> begin(TXTYPE iStatus);

	/**
	 * Commits the current transaction. The approach is all or nothing. All changes will be permanent following the storage type. If
	 * the operation succeed all the entities changed inside the transaction context will be effectives. If the operation fails, all
	 * the changed entities will be restored in the datastore. Memory instances are not guaranteed to being restored as well.
	 * 
	 * @return
	 */
	public ODatabaseComplex<T> commit();

	/**
	 * Aborts the current running transaction. All the pending changed entities will be restored in the datastore. Memory instances
	 * are not guaranteed to being restored as well.
	 * 
	 * @return
	 */
	public ODatabaseComplex<T> rollback();

	/**
	 * Execute a query against the database.
	 * 
	 * @param iCommand
	 *          Query command
	 * @return List of POJOs
	 */
	public <RET extends List<?>> RET query(final OQuery<? extends Object> iCommand);

	/**
	 * Execute a command against the database. A command can be a SQL statement or a Procedure. If the OStorage used is remote
	 * (OStorageRemote) then the command will be executed remotely and the result returned back to the calling client.
	 * 
	 * @param iCommand
	 *          Command request to execute.
	 * @return The same Command request received as parameter.
	 * @see OStorageRemote
	 */
	public <RET extends OCommandRequest> RET command(OCommandRequest iCommand);

	/**
	 * Return the OMetadata instance. Can't be null.
	 * 
	 * @return The OMetadata instance.
	 */
	public OMetadata getMetadata();

	/**
	 * Return the database dictionary. Can't be null.
	 * 
	 * @return The ODictionary instance.
	 */
	public ODictionary<T> getDictionary();

	/**
	 * Returns the database owner. Used in wrapped instances to know the up level ODatabase instance.
	 * 
	 * @return Returns the database owner.
	 */
	public ODatabaseComplex<?> getDatabaseOwner();

	/**
	 * Internal. Sets the database owner.
	 */
	public ODatabaseComplex<?> setDatabaseOwner(ODatabaseComplex<?> iOwner);

	/**
	 * Return the underlying database. Used in wrapper instances to know the down level ODatabase instance.
	 * 
	 * @return The underlying ODatabase implementation.
	 */
	public <DB extends ODatabase> DB getUnderlying();

	/**
	 * Registers a hook to listen all events for Records.
	 * 
	 * @param iHookImpl
	 *          ORecordHook implementation
	 * @return The Database instance itself giving a "fluent interface". Useful to call multiple methods in chain.
	 */
	public <DB extends ODatabaseComplex<?>> DB registerHook(ORecordHook iHookImpl);

	/**
	 * Retrieves all the registered hooks.
	 * 
	 * @return A not-null unmodifiable set of ORecordHook instances. If there are no hooks registered, the Set is empty.
	 */
	public Set<ORecordHook> getHooks();

	/**
	 * Unregisters a previously registered hook.
	 * 
	 * @param iHookImpl
	 *          ORecordHook implementation
	 * @return The Database instance itself giving a "fluent interface". Useful to call multiple methods in chain.
	 */
	public <DB extends ODatabaseComplex<?>> DB unregisterHook(ORecordHook iHookImpl);

	/**
	 * Invokes the callback on all the configured hooks.
	 * 
	 * @param iObject
	 *          The object passed change based on the Database implementation: records for {@link ODatabaseRecord} implementations and
	 *          POJO for {@link ODatabaseObject} implementations.
	 */
	public void callbackHooks(TYPE iType, Object iObject);
}
