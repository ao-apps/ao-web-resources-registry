/*
 * ao-web-resources-registry - Central registry for web resource management.
 * Copyright (C) 2020  AO Industries, Inc.
 *     support@aoindustries.com
 *     7262 Bull Pen Cir
 *     Mobile, AL 36695
 *
 * This file is part of ao-web-resources-registry.
 *
 * ao-web-resources-registry is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ao-web-resources-registry is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ao-web-resources-registry.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aoindustries.web.resources.registry;

import com.aoindustries.util.AoCollections;
import com.aoindustries.util.graph.Edge;
import com.aoindustries.util.graph.SymmetricGraph;
import com.aoindustries.util.graph.TopologicalSorter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A set of resources for a single class.
 *
 * @author  AO Industries, Inc.
 */
// TODO: When resources becomes empty, remove from Group (except Styles and Scripts)
public class Resources<R extends Resource<R> & Comparable<? super R>> implements Serializable {

	private static class Before<R extends Resource<R> & Comparable<? super R>> implements Serializable {

		private static final long serialVersionUID = 1L;

		private final R before;
		private final boolean required;

		private Before(R before, boolean required) {
			this.before = before;
			this.required = required;
		}

		@Override
		public String toString() {
			return
				before.toString()
				+ (required ? " (required)" : " (optional)");
		}

		@Override
		public boolean equals(Object obj) {
			if(!(obj instanceof Before)) return false;
			Before<?> other = (Before)obj;
			return
				required == other.required
				&& before.equals(other.before);
		}

		@Override
		public int hashCode() {
			int hash = before.hashCode();
			if(required) hash++;
			return hash;
		}
	}

	private static final long serialVersionUID = 1L;

	private final Set<R> resources = new HashSet<>();

	/**
	 * Ordering map: <code>after -&gt; Set&lt;Before&gt;</code>.
	 */
	private final Map<R,Set<Before<R>>> ordering = new HashMap<>();

	private Set<R> sorted;

	protected Resources() {
	}

	/**
	 * Copy constructor.
	 */
	protected Resources(Resources<R> other) {
		synchronized(other) {
			resources.addAll(other.resources);
			ordering.putAll(other.ordering);
			// Copy each
			for(Map.Entry<R,Set<Before<R>>> entry : ordering.entrySet()) {
				entry.setValue(new HashSet<>(entry.getValue()));
			}
			sorted = other.sorted;
		}
	}

	/**
	 * Gets a deep copy of these resources.
	 */
	protected Resources<R> copy() {
		return new Resources<R>(this);
	}

	/**
	 * Union constructor.
	 */
	@SafeVarargs
	protected Resources(Resources<R> ... others) {
		for(Resources<R> other : others) {
			synchronized(other) {
				resources.addAll(other.resources);
				for(Map.Entry<R,Set<Before<R>>> entry : other.ordering.entrySet()) {
					R after = entry.getKey();
					Set<Before<R>> befores = ordering.get(after);
					if(befores == null) {
						befores = new HashSet<>();
						ordering.put(after, befores);
					}
					befores.addAll(entry.getValue());
				}
			}
		}
		sorted = null;
	}

	/**
	 * Gets a the union of multiple groups.
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	static Resources<?> union(Class<? extends Resource<?>> clazz, Resources<?> ... others) {
		// Empty resources when null or empty
		if(others == null || others.length == 0) throw new IllegalArgumentException();
		// Perform a copy when a single resources
		if(others.length == 1) return others[0].copy();
		// Use union constructor
		if(clazz == Style.class) return new Styles((Styles[])others);
		if(clazz == Script.class) return new Scripts((Scripts[])others);
		// TODO: All "others" must be the same class
		// TODO: Use reflection to call a union constructor on this class
		// TODO: This will support other types of resources beyond style/script
		return new Resources(others);
	}

	/**
	 * Adds a new resource, if not already present.
	 *
	 * @return  {@code true} if the resource was added, or {@code false} if already exists and was not added
	 */
	synchronized public boolean add(R resource) {
		boolean added = resources.add(resource);
		if(added) sorted = null;
		return added;
	}

	/**
	 * Removes a resource.
	 *
	 * @return  {@code true} if the resource was removed, or {@code false} if the resource was not found
	 */
	synchronized public boolean remove(R resource) {
		boolean removed = resources.remove(resource);
		if(removed) sorted = null;
		return removed;
	}

	/**
	 * Adds an ordering constraint between two resources.
	 *
	 * @return  {@code true} if the ordering was added, or {@code false} if already exists and was not added
	 */
	synchronized public boolean addOrdering(R before, R after, boolean required) {
		Set<Before<R>> set = ordering.get(after);
		if(set == null) {
			set = new HashSet<>();
			ordering.put(after, set);
		}
		boolean added = set.add(new Before<>(before, required));
		if(added) sorted = null;
		return added;
	}

	/**
	 * Adds a required ordering constraint between two resources.
	 *
	 * @see  #addOrdering(com.aoindustries.web.resources.registry.Resource, com.aoindustries.web.resources.registry.Resource, boolean)
	 */
	public boolean addOrdering(R before, R after) {
		return addOrdering(before, after, true);
	}

	/**
	 * Removes an ordering constraint between two resources.
	 *
	 * @return  {@code true} if the ordering was removed, or {@code false} if the ordering was not found
	 */
	synchronized public boolean removeOrdering(R before, R after, boolean required) {
		Set<Before<R>> set = ordering.get(after);
		if(set != null) {
			boolean removed = set.remove(new Before<>(before, required));
			if(removed) sorted = null;
			return removed;
		} else {
			return false;
		}
	}

	/**
	 * Removes a required ordering constraint between two resources.
	 *
	 * @see  #removeOrdering(com.aoindustries.web.resources.registry.Resource, com.aoindustries.web.resources.registry.Resource, boolean)
	 */
	public boolean removeOrdering(R before, R after) {
		return removeOrdering(before, after, true);
	}

	/**
	 * Performs the topological sort.
	 *
	 * @return  An unmodifiable set, in the sorted order.
	 */
	private Set<R> topologicalSort(List<R> list) {
		assert Thread.holdsLock(this);
		// Build a SymmetricGraph, while making sure all required are found
		Set<Edge<R>> edgesTo = new LinkedHashSet<>();
		Set<Edge<R>> edgesFrom = new LinkedHashSet<>();
		Set<R> vertices = new LinkedHashSet<>(list);
		for(Map.Entry<R,Set<Before<R>>> entry : ordering.entrySet()) {
			R afterResource = entry.getKey();
			for(Before<R> before : entry.getValue()) {
				R beforeResource = before.before;
				if(vertices.contains(beforeResource)) {
					edgesTo.add(new Edge<>(beforeResource, afterResource));
					edgesFrom.add(new Edge<>(afterResource, beforeResource));
				} else if(before.required) {
					throw new IllegalStateException(
						"Required resource not found:\n"
						+ "    before = " + beforeResource + "\n"
						+ "    after  = " + afterResource
					);
				}
			}
		}
		SymmetricGraph<R,Edge<R>,RuntimeException> graph = new SymmetricGraph<R,Edge<R>,RuntimeException>() {
			@Override
			public Set<Edge<R>> getEdgesTo(R to) {
				return edgesTo;
			}
			@Override
			public Set<Edge<R>> getEdgesFrom(R from) {
				return edgesFrom;
			}
			@Override
			public Set<R> getVertices() {
				return vertices;
			}
		};
		// Perform the sort
		return AoCollections.optimalUnmodifiableSet(new TopologicalSorter<>(graph, true).sortGraph());
	}

	/**
	 * Gets the set of all resources of the given class, first with their
	 * {@linkplain Resource#compareTo(com.aoindustries.web.resources.registry.Resource) natural ordering},
	 * then with a topological sort to manage ordering constraints;
	 */
	synchronized public Set<R> getSorted() {
		Set<R> s = sorted;
		if(s == null) {
			// Natural sort
			List<R> list = new ArrayList<>(resources);
			Collections.sort(list);
			// Topological sort
			s = topologicalSort(list);
			// Cache the value, unmodifiable
			sorted = s;
		}
		return s;
	}
}
