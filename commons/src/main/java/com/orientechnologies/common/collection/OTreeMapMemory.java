package com.orientechnologies.common.collection;

import java.util.Comparator;
import java.util.Map;
import java.util.SortedMap;

@SuppressWarnings("serial")
public class OTreeMapMemory<K, V> extends OTreeMap<K, V> {
	/**
	 * Constructs a new, empty tree map, using the natural ordering of its keys. All keys inserted into the map must implement the
	 * {@link Comparable} interface. Furthermore, all such keys must be <i>mutually comparable</i>: <tt>k1.compareTo(k2)</tt> must not
	 * throw a <tt>ClassCastException</tt> for any keys <tt>k1</tt> and <tt>k2</tt> in the map. If the user attempts to put a key into
	 * the map that violates this constraint (for example, the user attempts to put a string key into a map whose keys are integers),
	 * the <tt>put(Object key, Object value)</tt> call will throw a <tt>ClassCastException</tt>.
	 */
	public OTreeMapMemory() {
	}

	public OTreeMapMemory(final int iSize, final float iLoadFactor) {
		super(iSize, iLoadFactor);
	}

	public OTreeMapMemory(final OTreeMapEventListener<K, V> iListener) {
		super(iListener);
	}

	/**
	 * Constructs a new, empty tree map, ordered according to the given comparator. All keys inserted into the map must be <i>mutually
	 * comparable</i> by the given comparator: <tt>comparator.compare(k1,
	 * k2)</tt> must not throw a <tt>ClassCastException</tt> for any keys <tt>k1</tt> and <tt>k2</tt> in the map. If the user attempts
	 * to put a key into the map that violates this constraint, the <tt>put(Object
	 * key, Object value)</tt> call will throw a <tt>ClassCastException</tt>.
	 * 
	 * @param comparator
	 *          the comparator that will be used to order this map. If <tt>null</tt>, the {@linkplain Comparable natural ordering} of
	 *          the keys will be used.
	 */
	public OTreeMapMemory(final Comparator<? super K> comparator) {
		super(comparator);
	}

	/**
	 * Constructs a new tree map containing the same mappings as the given map, ordered according to the <i>natural ordering</i> of
	 * its keys. All keys inserted into the new map must implement the {@link Comparable} interface. Furthermore, all such keys must
	 * be <i>mutually comparable</i>: <tt>k1.compareTo(k2)</tt> must not throw a <tt>ClassCastException</tt> for any keys <tt>k1</tt>
	 * and <tt>k2</tt> in the map. This method runs in n*log(n) time.
	 * 
	 * @param m
	 *          the map whose mappings are to be placed in this map
	 * @throws ClassCastException
	 *           if the keys in m are not {@link Comparable}, or are not mutually comparable
	 * @throws NullPointerException
	 *           if the specified map is null
	 */
	public OTreeMapMemory(final Map<? extends K, ? extends V> m) {
		super(m);
	}

	/**
	 * Constructs a new tree map containing the same mappings and using the same ordering as the specified sorted map. This method
	 * runs in linear time.
	 * 
	 * @param m
	 *          the sorted map whose mappings are to be placed in this map, and whose comparator is to be used to sort this map
	 * @throws NullPointerException
	 *           if the specified map is null
	 */
	public OTreeMapMemory(final SortedMap<K, ? extends V> m) {
		super(m);
	}

	protected OTreeMapEntry<K, V> createEntry(final K key, final V value) {
		return new OTreeMapEntryMemory<K, V>(this, key, value, null);
	}

	protected OTreeMapEntry<K, V> createEntry(final OTreeMapEntry<K, V> parent) {
		return new OTreeMapEntryMemory<K, V>(parent, parent.getPageSplitItems());
	}

}