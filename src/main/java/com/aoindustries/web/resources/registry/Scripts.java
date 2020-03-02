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

/**
 * A partition with some extra convenience overloads for {@link Script}.
 *
 * @author  AO Industries, Inc.
 */
final public class Scripts extends Resources<Script> {

	private static final long serialVersionUID = 1L;

	Scripts() {}

	/**
	 * Adds a new script, if not already present.
	 *
	 * @see  #add(com.aoindustries.web.resources.registry.Resource)
	 */
	public boolean add(String src) {
		return add(new Script(src));
	}

	/**
	 * Removes a script.
	 *
	 * @see  #remove(com.aoindustries.web.resources.registry.Resource)
	 */
	public boolean remove(String src) {
		return remove(new Script(src));
	}

	/**
	 * Adds an ordering constraint between two scripts.
	 *
	 * @see  #addOrdering(com.aoindustries.web.resources.registry.Resource, com.aoindustries.web.resources.registry.Resource, boolean)
	 */
	public boolean addOrdering(String beforeSrc, String afterSrc, boolean required) {
		return addOrdering(
			new Script(beforeSrc),
			new Script(afterSrc),
			required
		);
	}

	/**
	 * Adds a required ordering constraint between two scripts.
	 *
	 * @see  #addOrdering(java.lang.String, java.lang.String, boolean)
	 */
	public boolean addOrdering(String beforeSrc, String afterSrc) {
		return addOrdering(beforeSrc, afterSrc, true);
	}

	/**
	 * Removes an ordering constraint between two scripts.
	 *
	 * @see  #removeOrdering(com.aoindustries.web.resources.registry.Resource, com.aoindustries.web.resources.registry.Resource, boolean)
	 */
	public boolean removeOrdering(String beforeSrc, String afterSrc, boolean required) {
		return removeOrdering(
			new Script(beforeSrc),
			new Script(afterSrc),
			required
		);
	}

	/**
	 * Removes a required ordering constraint between two scripts.
	 *
	 * @see  #removeOrdering(java.lang.String, java.lang.String, boolean)
	 */
	public boolean removeOrdering(String beforeUri, String afterUri) {
		return removeOrdering(beforeUri, afterUri, true);
	}
}
