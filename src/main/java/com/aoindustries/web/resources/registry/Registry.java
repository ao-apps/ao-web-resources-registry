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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A registry contains a set of resources, along with ordering requirements.
 *
 * @author  AO Industries, Inc.
 */
public class Registry {

	private static class Entry<R extends Resource<R> & Comparable<? super R>> {

		private static class Before<R extends Resource<R> & Comparable<? super R>> {

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

		private final Set<R> resources = new HashSet<>();

		/**
		 * Ordering map: <code>after -&gt; Set&lt;Before&gt;</code>.
		 */
		private final Map<R,Set<Before<R>>> ordering = new HashMap<>();

		private Set<R> sorted;

		private Entry() {
		}

		/**
		 * @see Registry#add(java.lang.Class, com.aoindustries.web.resources.registry.Resource)
		 */
		synchronized private boolean add(R resource) {
			boolean added = resources.add(resource);
			if(added) sorted = null;
			return added;
		}

		/**
		 * @see Registry#remove(java.lang.Class, com.aoindustries.web.resources.registry.Resource)
		 */
		synchronized private boolean remove(R resource) {
			boolean removed = resources.remove(resource);
			if(removed) sorted = null;
			return removed;
		}

		/**
		 * @see Registry#addOrdering(java.lang.Class, com.aoindustries.web.resources.registry.Resource, com.aoindustries.web.resources.registry.Resource, boolean)
		 */
		synchronized private boolean addOrdering(R before, R after, boolean required) {
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
		 * @see Registry#removeOrdering(java.lang.Class, com.aoindustries.web.resources.registry.Resource, com.aoindustries.web.resources.registry.Resource, boolean)
		 */
		synchronized private boolean removeOrdering(R before, R after, boolean required) {
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
		 * @see Registry#getSorted(java.lang.Class)
		 */
		synchronized private Set<R> getSorted() {
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

	/**
	 * All concrete implementations of Resource must be comparable to themselves,
	 * so we maintain one sorted set per type.
	 */
	private final ConcurrentMap<
		Class<? extends Resource<?>>,
		Entry<? extends Resource<?>>
	> resourceMaps = new ConcurrentHashMap<>();

	/**
	 * Gets the resources for a given type.
	 */
	private <R extends Resource<R> & Comparable<? super R>> Entry<R> getEntry(Class<R> clazz) {
		@SuppressWarnings("unchecked")
		Entry<R> entry = (Entry)resourceMaps.get(clazz);
		if(entry == null) {
			entry = new Entry<>();
			@SuppressWarnings("unchecked")
			Entry<R> existing = resourceMaps.putIfAbsent(clazz, (Entry)entry);
			if(existing != null) entry = existing;
		}
		return entry;
	}

	public Registry() {}

	/**
	 * Adds a new resource, if not already present.
	 *
	 * @return  {@code true} if the resource was added, or {@code false} if already exists and was not added
	 */
	public <R extends Resource<R> & Comparable<? super R>> boolean add(Class<R> clazz, R resource) {
		return getEntry(clazz).add(resource);
	}

	/**
	 * Adds a new style, if not already present.
	 *
	 * @see  #add(java.lang.Class, com.aoindustries.web.resources.registry.Resource)
	 */
	public boolean add(Style style) {
		return add(Style.class, style);
	}

	/**
	 * Adds a new script, if not already present.
	 *
	 * @see  #add(java.lang.Class, com.aoindustries.web.resources.registry.Resource)
	 */
	public boolean add(Script script) {
		return add(Script.class, script);
	}

	/**
	 * Removes a resource.
	 *
	 * @return  {@code true} if the resource was removed, or {@code false} if the resource was not found
	 */
	public <R extends Resource<R> & Comparable<? super R>> boolean remove(Class<R> clazz, R resource) {
		return getEntry(clazz).remove(resource);
	}

	/**
	 * Removes a style.
	 *
	 * @see  #remove(java.lang.Class, com.aoindustries.web.resources.registry.Resource)
	 */
	public boolean remove(Style style) {
		return add(Style.class, style);
	}

	/**
	 * Removes a script.
	 *
	 * @see  #remove(java.lang.Class, com.aoindustries.web.resources.registry.Resource)
	 */
	public boolean remove(Script script) {
		return add(Script.class, script);
	}

	/**
	 * Adds an ordering constraint between two resources.
	 *
	 * @return  {@code true} if the ordering was added, or {@code false} if already exists and was not added
	 */
	public <R extends Resource<R> & Comparable<? super R>> boolean addOrdering(Class<R> clazz, R before, R after, boolean required) {
		return getEntry(clazz).addOrdering(before, after, required);
	}

	/**
	 * Adds a required ordering constraint between two resources.
	 *
	 * @see  #addOrdering(java.lang.Class, com.aoindustries.web.resources.registry.Resource, com.aoindustries.web.resources.registry.Resource, boolean)
	 */
	public <R extends Resource<R> & Comparable<? super R>> boolean addOrdering(Class<R> clazz, R before, R after) {
		return addOrdering(clazz, before, after, true);
	}

	/**
	 * Adds an ordering constraint between two styles.
	 *
	 * @see  #addOrdering(java.lang.Class, com.aoindustries.web.resources.registry.Resource, com.aoindustries.web.resources.registry.Resource, boolean)
	 */
	public boolean addOrdering(Style before, Style after, boolean required) {
		return addOrdering(Style.class, before, after, required);
	}

	/**
	 * Adds a required ordering constraint between two styles.
	 *
	 * @see  #addOrdering(com.aoindustries.web.resources.registry.Style, com.aoindustries.web.resources.registry.Style, boolean)
	 */
	public boolean addOrdering(Style before, Style after) {
		return addOrdering(before, after, true);
	}

	/**
	 * Adds an ordering constraint between two scripts.
	 *
	 * @see  #addOrdering(java.lang.Class, com.aoindustries.web.resources.registry.Resource, com.aoindustries.web.resources.registry.Resource, boolean)
	 */
	public boolean addOrdering(Script before, Script after, boolean required) {
		return addOrdering(Script.class, before, after, required);
	}

	/**
	 * Adds a required ordering constraint between two scripts.
	 *
	 * @see  #addOrdering(com.aoindustries.web.resources.registry.Style, com.aoindustries.web.resources.registry.Style, boolean)
	 */
	public boolean addOrdering(Script before, Script after) {
		return addOrdering(before, after, true);
	}

	/**
	 * Removes an ordering constraint between two resources.
	 *
	 * @return  {@code true} if the ordering was removed, or {@code false} if the ordering was not found
	 */
	public <R extends Resource<R> & Comparable<? super R>> boolean removeOrdering(Class<R> clazz, R before, R after, boolean required) {
		return getEntry(clazz).removeOrdering(before, after, required);
	}

	/**
	 * Removes a required ordering constraint between two resources.
	 *
	 * @see  #removeOrdering(java.lang.Class, com.aoindustries.web.resources.registry.Resource, com.aoindustries.web.resources.registry.Resource, boolean)
	 */
	public <R extends Resource<R> & Comparable<? super R>> boolean removeOrdering(Class<R> clazz, R before, R after) {
		return removeOrdering(clazz, before, after, true);
	}

	/**
	 * Removes an ordering constraint between two styles.
	 *
	 * @see  #removeOrdering(java.lang.Class, com.aoindustries.web.resources.registry.Resource, com.aoindustries.web.resources.registry.Resource, boolean)
	 */
	public boolean removeOrdering(Style before, Style after, boolean required) {
		return removeOrdering(Style.class, before, after, required);
	}

	/**
	 * Removes a required ordering constraint between two styles.
	 *
	 * @see  #removeOrdering(com.aoindustries.web.resources.registry.Style, com.aoindustries.web.resources.registry.Style, boolean)
	 */
	public boolean removeOrdering(Style before, Style after) {
		return removeOrdering(before, after, true);
	}

	/**
	 * Removes an ordering constraint between two scripts.
	 *
	 * @see  #removeOrdering(java.lang.Class, com.aoindustries.web.resources.registry.Resource, com.aoindustries.web.resources.registry.Resource, boolean)
	 */
	public boolean removeOrdering(Script before, Script after, boolean required) {
		return removeOrdering(Script.class, before, after, required);
	}

	/**
	 * Removes a required ordering constraint between two scripts.
	 *
	 * @see  #removeOrdering(com.aoindustries.web.resources.registry.Style, com.aoindustries.web.resources.registry.Style, boolean)
	 */
	public boolean removeOrdering(Script before, Script after) {
		return removeOrdering(before, after, true);
	}

	/**
	 * Gets the set of all resources of the given class, first with their
	 * {@linkplain Resource#compareTo(com.aoindustries.web.resources.registry.Resource) natural ordering},
	 * then with a topological sort to manage ordering constraints;
	 */
	public <R extends Resource<R> & Comparable<? super R>> Set<R> getSorted(Class<R> clazz) {
		return getEntry(clazz).getSorted();
	}

	/**
	 * @see  #getSorted(java.lang.Class)
	 */
	public Set<Style> getSortedStyles() {
		return getSorted(Style.class);
	}

	/**
	 * @see  #getSorted(java.lang.Class)
	 */
	public Set<Script> getSortedScripts() {
		return getSorted(Script.class);
	}
}
