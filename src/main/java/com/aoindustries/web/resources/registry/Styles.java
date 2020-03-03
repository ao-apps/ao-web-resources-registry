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

import java.util.Collection;

/**
 * A partition with some extra convenience overloads for {@link Style}.
 *
 * @author  AO Industries, Inc.
 */
final public class Styles extends Resources<Style> {

	private static final long serialVersionUID = 1L;

	Styles() {}

	/**
	 * Copy constructor.
	 */
	private Styles(Styles other) {
		super(other);
	}

	/**
	 * Gets a deep copy of these resources.
	 */
	@Override
	protected Styles copy() {
		return new Styles(this);
	}

	/**
	 * Union constructor.
	 */
	private Styles(Collection<? extends Styles> others) {
		super(others);
	}

	/**
	 * Gets a the union of multiple groups.
	 */
	public static Styles union(Collection<? extends Styles> others) {
		// Empty resources when null or empty
		if(others == null || others.isEmpty()) return new Styles();
		// Perform a copy when a single resources
		if(others.size() == 1) return others.iterator().next().copy();
		// Use union constructor
		return new Styles(others);
	}

	/**
	 * Adds a new style, if not already present.
	 *
	 * @see  #add(com.aoindustries.web.resources.registry.Resource) 
	 */
	public boolean add(String href) {
		return add(new Style(href));
	}

	/**
	 * Removes a style.
	 *
	 * @see  #remove(com.aoindustries.web.resources.registry.Resource)
	 */
	public boolean remove(String href) {
		return remove(new Style(href));
	}

	/**
	 * Adds an ordering constraint between two styles.
	 *
	 * @see  #addOrdering(com.aoindustries.web.resources.registry.Resource, com.aoindustries.web.resources.registry.Resource, boolean)
	 */
	public boolean addOrdering(String beforeHref, String afterHref, boolean required) {
		return addOrdering(
			new Style(beforeHref),
			new Style(afterHref),
			required
		);
	}

	/**
	 * Adds a required ordering constraint between two styles.
	 *
	 * @see  #addOrdering(java.lang.String, java.lang.String, boolean)
	 */
	public boolean addOrdering(String beforeHref, String afterHref) {
		return addOrdering(beforeHref, afterHref, true);
	}

	/**
	 * Removes an ordering constraint between two styles.
	 *
	 * @see  #removeOrdering(com.aoindustries.web.resources.registry.Resource, com.aoindustries.web.resources.registry.Resource, boolean)
	 */
	public boolean removeOrdering(String beforeHref, String afterHref, boolean required) {
		return removeOrdering(
			new Style(beforeHref),
			new Style(afterHref),
			required
		);
	}

	/**
	 * Removes a required ordering constraint between two styles.
	 *
	 * @see  #removeOrdering(java.lang.String, java.lang.String, boolean)
	 */
	public boolean removeOrdering(String beforeHref, String afterHref) {
		return removeOrdering(beforeHref, afterHref, true);
	}
}
