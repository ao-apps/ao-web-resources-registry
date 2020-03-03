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

import java.io.Serializable;
import java.util.ArrayList;
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
	 */
	public static boolean isValidName(String name) {
		return name.indexOf(' ') == -1 && name.indexOf(',') == -1;
	}

	/**
	 * Checks a group name.
	 *
	 * @return  the group name when valid
	 *
	 * @throws  IllegalArgumentException when the group name is invalid
	 */
	public static String checkName(String name) throws IllegalArgumentException {
		if(!isValidName(name)) throw new IllegalArgumentException("Group names may not contain spaces or commas: " + name);
		return name;
	}

	private static final long serialVersionUID = 1L;

	/**
	 * All concrete implementations of Resource must be comparable to themselves,
	 * so we maintain one sorted set per type.
	 */
	private final ConcurrentMap<
		Class<? extends Resource<?>>,
		Resources<? extends Resource<?>>
	> resourcesByClass = new ConcurrentHashMap<>();

	/**
	 * The partition for CSS styles.
	 *
	 * @see  #getResources(java.lang.Class)
	 */
	public final Styles styles;

	/**
	 * The partition for scripts.
	 *
	 * @see  #getResources(java.lang.Class)
	 */
	public final Scripts scripts;

	public Group() {
		styles = new Styles();
		if(resourcesByClass.put(Style.class, styles) != null) throw new IllegalStateException();
		scripts = new Scripts();
		if(resourcesByClass.put(Script.class, scripts) != null) throw new IllegalStateException();
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
				Resources<? extends Resource<?>>
			> entry : resourcesByClass.entrySet()
		) {
			entry.setValue(entry.getValue().copy());
		}
		// Set styles
		styles = (Styles)resourcesByClass.get(Style.class);
		if(styles == null) throw new IllegalStateException();
		// Set scripts
		scripts = (Scripts)resourcesByClass.get(Scripts.class);
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
	protected Group(Group ... others) {
		// Find all resources
		Map<
			Class<? extends Resource<?>>,
			List<Resources<? extends Resource<?>>>
		> allResources = new HashMap<>();
		for(Group other : others) {
			for(
				Map.Entry<
					Class<? extends Resource<?>>,
					Resources<? extends Resource<?>>
				> entry : other.resourcesByClass.entrySet()
			) {
				Class<? extends Resource<?>> clazz = entry.getKey();
				Resources<? extends Resource<?>> resources = entry.getValue();
				List<Resources<? extends Resource<?>>> resourcesForClass = allResources.get(clazz);
				if(resourcesForClass == null) {
					resourcesForClass = new ArrayList<>();
					allResources.put(clazz, resourcesForClass);
				}
				resourcesForClass.add(resources);
			}
		}
		// Union all resources
		for(
			Map.Entry<
				Class<? extends Resource<?>>,
				List<Resources<? extends Resource<?>>>
			> entry : allResources.entrySet()
		) {
			Class<? extends Resource<?>> clazz = entry.getKey();
			List<Resources<? extends Resource<?>>> resourcesForClass = entry.getValue();
			resourcesByClass.put(
				clazz,
				Resources.union(
					(Class)clazz,
					resourcesForClass.toArray(new Resources[resourcesForClass.size()])
				)
			);
		}
		// Set styles
		styles = (Styles)resourcesByClass.get(Style.class);
		if(styles == null) throw new IllegalStateException();
		// Set scripts
		scripts = (Scripts)resourcesByClass.get(Scripts.class);
		if(scripts == null) throw new IllegalStateException();
	}

	/**
	 * Gets a the union of multiple groups.
	 */
	static Group union(Group ... others) {
		// Empty group when null or empty
		if(others == null || others.length == 0) throw new IllegalArgumentException();
		// Perform a copy when a single group
		if(others.length == 1) return others[0].copy();
		// Use union constructor
		return new Group(others);
	}

	/**
	 * Gets the resources for a given type.
	 */
	public <R extends Resource<R> & Comparable<? super R>> Resources<R> getResources(Class<R> clazz) {
		@SuppressWarnings("unchecked")
		Resources<R> entry = (Resources)resourcesByClass.get(clazz);
		if(entry == null) {
			entry = new Resources<>();
			@SuppressWarnings("unchecked")
			Resources<R> existing = resourcesByClass.putIfAbsent(clazz, (Resources)entry);
			if(existing != null) entry = existing;
		}
		return entry;
	}
}
