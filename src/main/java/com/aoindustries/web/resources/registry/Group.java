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

import com.aoindustries.util.StringUtility;
import com.aoindustries.util.function.SerializableFunction;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Groups are named sets of resources.
 *
 * @author  AO Industries, Inc.
 */
// TODO: When group becomes empty, remove from Registry (except Global)
public class Group implements Serializable {

	/**
	 * The name of the global group.
	 */
	public static final String GLOBAL = "global";

	/**
	 * Gets the group for a given name.
	 *
	 * @param  name  Group names may not contain commas or spaces
	 *
	 * @see  StringUtility#splitStringCommaSpace(java.lang.String)
	 */
	public static boolean isValidName(String name) {
		int len = name.length();
		if(len == 0) {
			return false;
		}
		int pos = 0;
		while(pos < len) {
			int cp = name.codePointAt(pos);
			if(cp == ',' || StringUtility.isWhitespace(cp)) {
				return false;
			}
			pos += Character.charCount(cp);
		}
		return true;
	}

	/**
	 * Checks a group name.
	 *
	 * @return  the group name when valid
	 *
	 * @throws  IllegalArgumentException when the group name is invalid
	 */
	public static String checkName(String name) throws IllegalArgumentException {
		if(!isValidName(name)) throw new IllegalArgumentException("Group names may not be empty or contain whitespaces or commas: " + name);
		return name;
	}

	private static final long serialVersionUID = 1L;

	// TODO: Why is "RS" required?
	private static class ResourcesEntry<
		R extends Resource<R> & Comparable<? super R>,
		RS extends Resources<R>
	> implements Serializable {

		private static final long serialVersionUID = 1L;

		private final SerializableFunction<? super Collection<? extends RS>,RS> unionizer;
		private final Resources<R> resources;

		private ResourcesEntry(
			SerializableFunction<? super Collection<? extends RS>,RS> unionizer,
			Resources<R> resources
		) {
			this.unionizer = unionizer;
			this.resources = resources;
		}

		private ResourcesEntry<R,RS> copy() {
			return new ResourcesEntry<>(unionizer, resources.copy());
		}
	}

	/**
	 * All concrete implementations of Resource must be comparable to themselves,
	 * so we maintain one sorted set per type.
	 */
	private final ConcurrentMap<
		Class<? extends Resource<?>>,
		ResourcesEntry<?,?>
	> resourcesByClass = new ConcurrentHashMap<>();

	/**
	 * The partition for CSS styles.
	 *
	 * @see  #getResources(java.lang.Class, com.aoindustries.util.function.SerializableFunction)
	 */
	public final Styles styles;

	/**
	 * The partition for scripts.
	 *
	 * @see  #getResources(java.lang.Class, com.aoindustries.util.function.SerializableFunction)
	 */
	public final Scripts scripts;

	public Group() {
		styles = new Styles();
		if(
			resourcesByClass.put(
				Style.class,
				new ResourcesEntry<Style,Styles>(
					Styles::union,
					styles
				)
			) != null
		) throw new IllegalStateException();
		scripts = new Scripts();
		if(
			resourcesByClass.put(
				Script.class,
				new ResourcesEntry<Script,Scripts>(
					Scripts::union,
					scripts
				)
			) != null
		) throw new IllegalStateException();
	}

	/**
	 * Copy constructor.
	 */
	protected Group(Group other) {
		resourcesByClass.putAll(other.resourcesByClass);
		// Copy each
		for(
			Map.Entry<
				Class<? extends Resource<?>>,
				ResourcesEntry<?,?>
			> entry : resourcesByClass.entrySet()
		) {
			entry.setValue(entry.getValue().copy());
		}
		// Set styles
		styles = (Styles)resourcesByClass.get(Style.class).resources;
		if(styles == null) throw new IllegalStateException();
		// Set scripts
		scripts = (Scripts)resourcesByClass.get(Script.class).resources;
		if(scripts == null) throw new IllegalStateException();
	}

	/**
	 * Gets a deep copy of this group.
	 */
	protected Group copy() {
		return new Group(this);
	}

	/**
	 * Union constructor.
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	protected Group(Collection<? extends Group> others) {
		// Find all resources
		Map<
			Class<? extends Resource<?>>,
			List<ResourcesEntry<?,?>>
		> allResources = new HashMap<>();
		for(Group other : others) {
			for(
				Map.Entry<
					Class<? extends Resource<?>>,
					ResourcesEntry<?,?>
				> entry : other.resourcesByClass.entrySet()
			) {
				Class<? extends Resource<?>> clazz = entry.getKey();
				List<ResourcesEntry<?,?>> resourcesForClass = allResources.get(clazz);
				if(resourcesForClass == null) {
					resourcesForClass = new ArrayList<>();
					allResources.put(clazz, resourcesForClass);
				}
				resourcesForClass.add(entry.getValue());
			}
		}
		// Union all resources
		for(
			Map.Entry<
				Class<? extends Resource<?>>,
				List<ResourcesEntry<?,?>>
			> entry : allResources.entrySet()
		) {
			Class<? extends Resource<?>> clazz = entry.getKey();
			List<ResourcesEntry<?,?>> resourcesEntries = entry.getValue();
			List<Resources<?>> resourcesList = new ArrayList<>();
			SerializableFunction unionizer = null;
			for(ResourcesEntry<?,?> resourcesEntry : resourcesEntries) {
				if(unionizer == null) unionizer = resourcesEntry.unionizer;
				resourcesList.add(resourcesEntry.resources);
			}
			if(unionizer != null) {
				resourcesByClass.put(
					clazz,
					new ResourcesEntry(
						unionizer,
						(Resources)unionizer.apply(resourcesList)
					)
				);
			}
		}
		// Set styles
		styles = (Styles)resourcesByClass.get(Style.class).resources;
		if(styles == null) throw new IllegalStateException();
		// Set scripts
		scripts = (Scripts)resourcesByClass.get(Script.class).resources;
		if(scripts == null) throw new IllegalStateException();
	}

	/**
	 * Gets a the union of multiple groups.
	 */
	static Group union(Collection<? extends Group> others) {
		// Empty group when null or empty
		if(others == null || others.isEmpty()) throw new IllegalArgumentException();
		// Perform a copy when a single group
		if(others.size() == 1) return others.iterator().next().copy();
		// Use union constructor
		return new Group(others);
	}

	/**
	 * Gets the resources for a given type.
	 */
	public <
		R extends Resource<R> & Comparable<? super R>,
		RS extends Resources<R>
	> Resources<R> getResources(Class<R> clazz, SerializableFunction<? super Collection<? extends RS>,RS> unionizer) {
		@SuppressWarnings("unchecked")
		ResourcesEntry<R,RS> entry = (ResourcesEntry)resourcesByClass.get(clazz);
		if(entry == null) {
			entry = new ResourcesEntry<>(unionizer, new Resources<>());
			@SuppressWarnings("unchecked")
			ResourcesEntry<R,RS> existing = (ResourcesEntry)resourcesByClass.putIfAbsent(clazz, (ResourcesEntry)entry);
			if(existing != null) entry = existing;
		}
		return entry.resources;
	}

	/**
	 * Are all resources empty?
	 *
	 * @see  Resources#isEmpty()
	 */
	public boolean isEmpty() {
		for(ResourcesEntry<?,?> entry : resourcesByClass.values()) {
			if(!entry.resources.isEmpty()) return false;
		}
		return true;
	}
}
