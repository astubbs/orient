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
package com.orientechnologies.common.collection;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Base class for OTreeMap Iterators
 */
abstract class AbstractEntryIterator<K, V, T> implements Iterator<T> {
	OTreeMap<K, V>			tree;
	OTreeMapEntry<K, V>	next;
	OTreeMapEntry<K, V>	lastReturned;
	int									expectedModCount;

	AbstractEntryIterator(OTreeMapEntry<K, V> first) {
		if (first == null)
			// IN CASE OF ABSTRACTMAP.HASHCODE()
			return;

		tree = first.getTree();
		next = first;
		expectedModCount = tree.modCount;
		lastReturned = null;
		tree.pageIndex = -1;
	}

	public final boolean hasNext() {
		return next != null && (OTreeMap.successor(next) != null || tree.pageIndex < next.getSize() - 1);
	}

	final OTreeMapEntry<K, V> nextEntry() {
		if (next == null)
			throw new NoSuchElementException();

		if (tree.pageIndex < next.getSize() - 1) {
			// ITERATE INSIDE THE NODE
			tree.pageIndex++;
		} else {
			// GET THE NEXT NODE
			if (tree.modCount != expectedModCount)
				throw new ConcurrentModificationException();

			tree.pageIndex = 0;
			next = OTreeMap.successor(next);
			lastReturned = next;
		}

		return next;
	}

	final OTreeMapEntry<K, V> prevEntry() {
		OTreeMapEntry<K, V> e = next;
		if (e == null)
			throw new NoSuchElementException();
		if (tree.modCount != expectedModCount)
			throw new ConcurrentModificationException();
		next = OTreeMap.predecessor(e);
		lastReturned = e;
		return e;
	}

	public void remove() {
		if (lastReturned == null)
			throw new IllegalStateException();
		if (tree.modCount != expectedModCount)
			throw new ConcurrentModificationException();
		// deleted entries are replaced by their successors
		if (lastReturned.getLeft() != null && lastReturned.getRight() != null)
			next = lastReturned;
		tree.deleteEntry(lastReturned);
		expectedModCount = tree.modCount;
		lastReturned = null;
	}
}
