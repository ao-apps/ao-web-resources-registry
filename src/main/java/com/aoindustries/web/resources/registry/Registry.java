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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A registry contains a set of resources, along with ordering requirements.
 *
 * @author  AO Industries, Inc.
 */
public class Registry implements Serializable {

	private static final long serialVersionUID = 1L;

	private final ConcurrentMap<String,Group> groups = new ConcurrentHashMap<>();

	/**
	 * The global group.
	 *
	 * @see  #getGroup(java.lang.String)
	 */
	public final Group global;

	public Registry() {
		global = new Group();
		if(groups.put(Group.GLOBAL, global) != null) throw new IllegalStateException();
	}

	/**
	 * Copy constructor.
	 */
	protected Registry(Registry other) {
		groups.putAll(other.groups);
		// Copy each
		for(Map.Entry<String,Group> entry : groups.entrySet()) {
			entry.setValue(entry.getValue().copy());
		}
		// Set global
		global = groups.get(Group.GLOBAL);
		if(global == null) throw new IllegalStateException();
	}

	/**
	 * Gets a deep copy of this registry.
	 */
	public Registry copy() {
		return new Registry(this);
	}

	/**
	 * Union constructor.
	 */
	protected Registry(Collection<? extends Registry> others) {
		// Find all groups
		Map<String,List<Group>> allGroups = new HashMap<>();
		for(Registry other : others) {
			for(Map.Entry<String,Group> entry : other.groups.entrySet()) {
				String name = entry.getKey();
				Group group = entry.getValue();
				List<Group> groupsForName = allGroups.get(name);
				if(groupsForName == null) {
					groupsForName = new ArrayList<>();
					allGroups.put(name, groupsForName);
				}
				groupsForName.add(group);
			}
		}
		// Union all groups
		for(Map.Entry<String,List<Group>> entry : allGroups.entrySet()) {
			groups.put(
				entry.getKey(),
				Group.union(entry.getValue())
			);
		}
		// Set global
		global = groups.get(Group.GLOBAL);
		if(global == null) throw new IllegalStateException();
	}

	/**
	 * Gets a the union of multiple registries.
	 * This is a deep copy and may be manipulated without altering the source registries.
	 */
	// TODO: Is full registry union still used?  Or is it just group unions?
	/*
	public static Registry union(Collection<? extends Registry> others) {
		// Empty registry when null or empty
		if(others == null || others.isEmpty()) return new Registry();
		// Perform a copy when a single registry
		if(others.size() == 1) return others.iterator().next().copy();
		// Use union constructor
		return new Registry(others);
	}
	 */

	/**
	 * Gets the group for a given name, optionally creating it if not already present.
	 *
	 * @param  createIfMissing  When {@code true}, will create the group if missing
	 *
	 * @return  The group or {@code null} when the group does not exist and {@code createIfMissing} is {@code false}.
	 *
	 * @throws  IllegalArgumentException when {@linkplain Group#checkName(java.lang.String) group name is invalid}.
	 *
	 * @see  Group#checkName(java.lang.String)
	 */
	public Group getGroup(String name, boolean createIfMissing) throws IllegalArgumentException {
		Group.checkName(name);
		Group group = groups.get(name);
		if(group == null && createIfMissing) {
			group = new Group();
			Group existing = groups.putIfAbsent(name, group);
			if(existing != null) group = existing;
		}
		return group;
	}

	/**
	 * Gets the group for a given name, creating it if not already present.
	 *
	 * @throws  IllegalArgumentException when {@linkplain Group#checkName(java.lang.String) group name is invalid}.
	 *
	 * @see  Group#checkName(java.lang.String)
	 */
	public Group getGroup(String name) throws IllegalArgumentException {
		return getGroup(name, true);
	}

	/**
	 * Are all groups empty?
	 *
	 * @see  Group#isEmpty()
	 */
	public boolean isEmpty() {
		for(Group group : groups.values()) {
			if(!group.isEmpty()) return false;
		}
		return true;
	}
}
