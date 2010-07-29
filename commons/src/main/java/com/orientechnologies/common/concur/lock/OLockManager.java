package com.orientechnologies.common.concur.lock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.orientechnologies.common.profiler.OProfiler;

/**
 * Manage the locks across all the client connections. To optimize speed and space in memory the shared lock map holds the client
 * connections directly if only one client is locking the resource. When multiple clients acquire the same resource, then a List is
 * put in place of the single object.<br/>
 * On lock removing the list is maintained even if the client remains only one because the cost to replace the List to the object
 * directly is higher then just remove the item and the probability to add another again is high.
 */
@SuppressWarnings("unchecked")
public class OLockManager<RESOURCE_TYPE, REQUESTER_TYPE> {
	public enum LOCK {
		SHARED, EXCLUSIVE
	}

	private static final int														DEFAULT_ACQUIRE_TIMEOUT				= 5000;
	private static final int														DEFAULT_CONCURRENCY_LEVEL			= 1;

	protected final OLockQueue<RESOURCE_TYPE>						lockQueue											= new OLockQueue<RESOURCE_TYPE>();
	protected final Map<RESOURCE_TYPE, Object>					sharedLocks										= new HashMap<RESOURCE_TYPE, Object>();
	protected final Map<RESOURCE_TYPE, REQUESTER_TYPE>	exclusiveLocks								= new HashMap<RESOURCE_TYPE, REQUESTER_TYPE>();
	protected int																				concurrencyLevel							= DEFAULT_CONCURRENCY_LEVEL;
	protected boolean																		downsizeSharedLockRetainList	= true;
	protected final long																acquireTimeout								= DEFAULT_ACQUIRE_TIMEOUT;											// MS

	public OLockManager() {
	}

	public void acquireLock(final REQUESTER_TYPE iRequester, final RESOURCE_TYPE iResourceId, LOCK iLockType, long iTimeout) {
		if (tryToAcquireLock(iRequester, iResourceId, iLockType))
			return;

		// PUT CURRENT THREAD IN WAIT UNTIL TIMEOUT OR UNLOCK BY ANOTHER THREAD THAT UNLOCK THE RESOURCE
		if (lockQueue.waitForResource(iResourceId, iTimeout))
			// TIMEOUT EXPIRED
			throw new OLockException("Resource " + iResourceId + " is locked");

		// THREAD UNLOCKED: TRY TO RE-ACQUIRE
		if (!tryToAcquireLock(iRequester, iResourceId, iLockType))
			// TIMEOUT EXPIRED
			throw new OLockException("Resource " + iResourceId + " is locked");
	}

	public synchronized void releaseLock(final REQUESTER_TYPE iRequester, final RESOURCE_TYPE iResourceId, final LOCK iLockType)
			throws OLockException {
		if (iLockType == LOCK.SHARED) {
			final Object sharedLock = sharedLocks.get(iResourceId);
			if (sharedLock == null)
				throw new OLockException("Error on releasing a non acquired SHARED lock by the requester " + iRequester + " on resource: "
						+ iResourceId);

			downsizeSharedLock(iResourceId, sharedLock);
		} else {
			final REQUESTER_TYPE exclusiveLock = exclusiveLocks.remove(iResourceId);
			if (exclusiveLock == null)
				throw new OLockException("Error on releasing a non acquired EXCLUSIVE lock by the requester " + iRequester
						+ " on resource: " + iResourceId);
		}
		lockQueue.wakeupWaiters(iResourceId);
	}

	public int getConcurrencyLevel() {
		return concurrencyLevel;
	}

	public void setDefaultConcurrencyLevel(final int concurrencyLevel) {
		this.concurrencyLevel = concurrencyLevel;
	}

	public boolean isDownsizeSharedLockRetainList() {
		return downsizeSharedLockRetainList;
	}

	public void setDownsizeSharedLockRetainList(final boolean downsizeSharedLockRetainList) {
		this.downsizeSharedLockRetainList = downsizeSharedLockRetainList;
	}

	protected synchronized boolean tryToAcquireLock(final REQUESTER_TYPE iRequester, final RESOURCE_TYPE iResourceId,
			final LOCK iLockType) {
		OProfiler.getInstance().updateStatistic("LockMgr.tryToAcquire", +1);

		REQUESTER_TYPE client = exclusiveLocks.get(iResourceId);
		if (client != null) {
			// THE RESOURCE IS ALREADY LOCKED IN EXCLUSIVE MODE
			OProfiler.getInstance().updateStatistic("LockMgr.tryToAcquire.locked", +1);
			return false;
		}

		// CHECK IF THERE ALREADY ARE SHARED LOCKS
		final Object sharedLock = sharedLocks.get(iResourceId);
		List<REQUESTER_TYPE> clients;

		if (iLockType == LOCK.SHARED) {
			if (sharedLock == null) {
				// CREATE IT
				sharedLocks.put(iResourceId, iRequester);
				return true;
			}

			if (sharedLock instanceof List<?>) {
				clients = (List<REQUESTER_TYPE>) sharedLock;
			} else {
				// FROM 1 TO 2 MULTIPLE SHARED CLIENTS: CREATE A LIST TO PUT ALL TOGETHER IN PLACE OF SINGLE OBJECT
				clients = new ArrayList<REQUESTER_TYPE>(concurrencyLevel);
				sharedLocks.put(iResourceId, clients);

				// ADD THE FIRST CLIENT
				clients.add((REQUESTER_TYPE) sharedLock);
			}

			// ADD THE SHARED LOCK
			clients.add(iRequester);
		} else {
			if (sharedLock == null) {
				// NO ONE IS LOCKING IN SHARED MODE: ACQUIRE THE EXCLUSIVE LOCK
				exclusiveLocks.put(iResourceId, iRequester);
				return true;
			}

			// CHECK IF CAN GAIN THE EXCLUSIVE LOCK
			if (sharedLock instanceof List<?>) {
				clients = (List<REQUESTER_TYPE>) sharedLock;
				if (clients.size() == 1 && clients.get(0).equals(iRequester)) {
					// EXCALATION FROM SHARED TO EXCLUSIVE LOCK
					promoteLock(iRequester, iResourceId, sharedLock);
					return true;
				}
			} else {
				if (sharedLock.equals(iRequester)) {
					// EXCALATION FROM SHARED TO EXCLUSIVE LOCK
					promoteLock(iRequester, iResourceId, sharedLock);
					return true;
				}
			}
		}
		return false;
	}

	private void promoteLock(final REQUESTER_TYPE iRequester, final RESOURCE_TYPE iResourceId, final Object iSharedLock) {
		downsizeSharedLock(iResourceId, iSharedLock);
		exclusiveLocks.put(iResourceId, iRequester);
	}

	private void downsizeSharedLock(final RESOURCE_TYPE iResourceId, final Object iSharedLock) {
		if (downsizeSharedLockRetainList && iSharedLock instanceof List<?>)
			// RETAIN THE LIST FOR FUTURE DOWNGRADE TO SHARED
			((List<REQUESTER_TYPE>) iSharedLock).clear();
		else
			// REMOVE THE SHARED LOCK
			sharedLocks.remove(iResourceId);
	}

	public void clear() {
		sharedLocks.clear();
		exclusiveLocks.clear();
	}
}
