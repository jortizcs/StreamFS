package local.analytics.util;

import java.util.Map;
import java.util.Hashtable;
import java.util.Collection;
import java.util.Enumeration;

public class BiHashtable<K, V> {

	private Hashtable<K,V> htable=null;
	private Hashtable<V, K> rhtable=null;

	public BiHashtable(){
		htable = new Hashtable<K,V>();
		rhtable = new Hashtable<V, K>();
	}

	public synchronized void clear(){
		htable.clear();
		rhtable.clear();
	}

	public boolean contains(Object value){
		return htable.contains(value);
	}

	public boolean containsKey(Object key){
		return htable.containsKey(key);
	}

	public boolean containsValue(Object value){
		return htable.containsValue(value);
	}

	public V getValue(Object key){
		return htable.get(key);
	}

	public K getKey(Object value){
		return (K)rhtable.get(value);
	}

	public boolean isEmpty(){
		return htable.isEmpty();
	}

	public synchronized V remove(Object key){
		V value = htable.remove(key);
		rhtable.remove(value);
		return value;
	}

	public Collection<V> values(){
		return htable.values();
	}

	public Enumeration<K> keys(){
		return htable.keys();
	}

	public synchronized V put(K key, V value){
		rhtable.put(value, key);
		return htable.put(key, value);
	}

	public String toString(){
		return htable.toString();
	}
}
