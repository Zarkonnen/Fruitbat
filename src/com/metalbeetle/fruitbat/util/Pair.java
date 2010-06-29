package com.metalbeetle.fruitbat.util;

public final class Pair<A, B> {
	public final A a;
	public final B b;

	public Pair(A a, B b) { this.a = a; this.b = b; }

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof Pair)) { return false; }
		final Pair<A, B> p2 = (Pair<A, B>) o;
		return
				(a == null ? p2.a == null : a.equals(p2.a)) &&
				(b == null ? p2.b == null : b.equals(p2.b));
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 83 * hash + (a != null ? a.hashCode() : 0);
		hash = 83 * hash + (b != null ? b.hashCode() : 0);
		return hash;
	}
}
