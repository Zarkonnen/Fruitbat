package com.metalbeetle.fruitbat.util;

import java.util.HashSet;

public class PrefixBTree<T> {
	static final int FULL_ARRAY_THRESHOLD = 4;
	final Node<T> root = new Node<T>();

	public HashSet<T> get(String key) { return get(key.getBytes()); }
	public void put(String key, T value) { put(key.getBytes(), value); }
	public void remove(String key, T value) { remove(key.getBytes(), value); }

	public HashSet<T> get(byte[] prefix) {
		HashSet<T> l = new HashSet<T>();
		//root.get(l, prefix, 0);
		// Sped-up version of get that finds the required node nonrecursively.
		Node<T> node = root;
		int prefixIndex = 0;
		while (prefixIndex < prefix.length) {
			int index = prefix[prefixIndex] + node.childrenOffset;
			if (node.children != null && index >= 0 && index < node.children.length) {
				node = node.children[index];
				if (node == null) { return l; }
				prefixIndex++;
			} else {
				return l;
			}
		}
		l.addAll(node.leaves);
		if (node.children != null) {
			for (Node<T> child : node.children) {
				if (child != null) {
					child.get(l, prefix, prefixIndex);
				}
			}
		}
		return l;
	}

	public void put(byte[] key, T value) {
		root.put(key, 0, value);
	}

	public void remove(byte[] key, T value) {
		root.remove(key, 0, value);
	}
	
	public int size() {
		return get(new byte[0]).size();
	}

	public int numberOfNodes() {
		return root.numberOfNodes();
	}

	static class Node<T> {
		Node<T>[] children;
		int childrenOffset = 0;
		int childCount = 0;
		HashSet<T> leaves = new HashSet<T>(4);

		int numberOfNodes() {
			int n = 1;
			if (children != null) {
				for (Node<T> child : children) {
					if (child != null) { n += child.numberOfNodes(); }
				}
			}
			return n;
		}

		void get(HashSet<T> l, byte[] prefix, int prefixIndex) {
			if (prefixIndex == prefix.length) {
				l.addAll(leaves);
				if (children != null) {
					for (Node<T> child : children) {
						if (child != null) {
							child.get(l, prefix, prefixIndex);
						}
					}
				}
			} else {
				int index = prefix[prefixIndex] + childrenOffset;
				if (index >= 0 && index < children.length) {
					Node<T> child = children[index];
					if (child != null) {
						child.get(l, prefix, prefixIndex + 1);
					}
				}
			}
		}

		void put(byte[] key, int keyIndex, T value) {
			if (keyIndex == key.length) {
				if (!leaves.contains(value)) {
					leaves.add(value);
				}
			} else {
				int index = key[keyIndex] + childrenOffset;
				if (children == null) {
					children = new Node[1];
					childrenOffset = -key[keyIndex];
					index = 0;
				} else {
					if (index < 0) {
						Node<T>[] newChildren = new Node[children.length - index];
						System.arraycopy(children, 0, newChildren, -index, children.length);
						children = newChildren;
						childrenOffset = -key[keyIndex];
						index = 0;
					} else {
						if (index >= children.length) {
							Node<T>[] newChildren = new Node[index + 1];
							System.arraycopy(children, 0, newChildren, 0, children.length);
							children = newChildren;
						}
					}
				}
				if (children[index] == null) {
					children[index] = new Node<T>();
					childCount++;
				}
				children[index].put(key, keyIndex + 1, value);
			}
		}

		boolean remove(byte[] key, int keyIndex, T value) {
			if (keyIndex == key.length) {
				leaves.remove(value);
				return leaves.size() == 0 && childCount == 0;
			}

			int index = key[keyIndex] + childrenOffset;
			if (children != null &&
			    index >= 0 &&
			    index < children.length &&
			    children[index] != null)
			{
				if (children[index].remove(key, keyIndex + 1, value)) {
					children[index] = null;
					childCount--;
					return childCount == 0;
				}
			}
			return false;
		}
	}
}
