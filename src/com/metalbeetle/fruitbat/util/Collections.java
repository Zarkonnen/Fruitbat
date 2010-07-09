package com.metalbeetle.fruitbat.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static java.util.Collections.*;

public final class Collections {
	private Collections() {}

	/** @return Immutable list of items. */
	public static <T> List<T> l(T... ts) {
		return immute(Arrays.asList(ts));
	}

	/** @return Immutable list copy of collection. */
	public static <T> List<T> immute(Collection<T> l) {
		return unmodifiableList(new ArrayList<T>(l));
	}

	/** @return A pair. */
	public static <A, B> Pair<A, B> p(A a, B b) { return new Pair<A, B>(a, b); }

	/** @return Immutable hashmap from pairs. */
	public static <A, B> Map<A, B> m(Pair<A, B>... ps) {
		HashMap<A, B> m = new HashMap<A, B>(ps.length * 2);
		for (Pair<A, B> p : ps) {
			m.put(p.a, p.b);
		}
		return unmodifiableMap(m);
	}

	/** @return Joined version of the lists. */
	public static <T> List<T> join(List<T>... ls) {
		int size = 0;
		for (List<T> l : ls) { size += l.size(); }
		ArrayList<T> joint = new ArrayList<T>(size);
		for (List<T> l : ls) { joint.addAll(l); }
		return unmodifiableList(joint);
	}
}
