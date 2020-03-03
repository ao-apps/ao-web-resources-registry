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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Groups are named sets of resources.
 *
 * @author  AO Industries, Inc.
 */
public class Group implements Serializable {

	/**
	 * The name of the global group.
	 */
	public static final String GLOBAL = "global";

	/**
	 * Gets the group for a given name.
	 *
	 * @param  Group names may not contain commas or spaces
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
