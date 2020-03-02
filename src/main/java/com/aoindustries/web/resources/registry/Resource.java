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

import com.aoindustries.text.SmartComparator;
import com.aoindustries.util.StringUtility;
import java.io.Serializable;

/**
 * A resource given its full URL or application context-relative path.
 * <p>
 * TODO: Support inline resources without URI.
 * </p>
 * <p>
 * TODO: Support comments, which would not be displayed in production mode.
 * </p>
 *
 * @author  AO Industries, Inc.
 */
// TODO: optional context relative? (adds request.contextPath when used)
abstract public class Resource<
	// All concrete implementations of Resource must be comparable to themselves
	R extends Resource<R> & Comparable<? super R>
> implements Serializable {

	abstract public static class Builder<R extends Resource<R> & Comparable<? super R>> {

		protected Builder() {}

		protected String uri;
		public Builder<R> uri(String uri) {
			this.uri = uri;
			return this;
		}

		abstract public R build();
	}

	private static final long serialVersionUID = 1L;

	private final String uri;

	/**
	 * @param uri  See {@link #getUri()}
	 */
	public Resource(String uri) {
		this.uri = StringUtility.nullIfEmpty(uri);
	}

	/**
	 * @see  #getUri()
	 */
	@Override
	public String toString() {
		return uri;
	}

	/**
	 * Two resources may be compared to see if they are exactly equal, including
	 * all relevant attributes.
	 */
	@Override
	abstract public boolean equals(Object obj);

	/**
	 * The hash key must be consistent with {@link #equals(java.lang.Object)}.
	 */
	@Override
	abstract public int hashCode();

	/**
	 * All concrete implementations of Resource must be comparable to themselves.
	 * <p>
	 * This default implementation compares by URI only via {@link SmartComparator#ROOT}.
	 * </p>
	 */
	protected int compareTo(R o) {
		return SmartComparator.ROOT.compare(uri, o.getUri());
	}

	/**
	 * Gets the URI if the resource.  May be any of:
	 * <ul>
	 * <li>
	 *   Absolute URI (Example <code>https://…/…</code>): Used verbatim.
	 * </li>
	 * <li>
	 *   Full path (Example <code>/…/…</code>): Path within the application.
	 *   This may be prefixed with a context-path, depending on application type.
	 * </li>
	 * <li>
	 *   Relative path (Example <code>../…/…</code>): Path relative to the application.
	 *   This will be relative to the top of the application, which can be used
	 *   to pull content from a different context-path, depending on application type.
	 *   It is an error if the series of <code>../</code> would go past the root path <code>"/"</code>.
	 * </li>
	 * </ul>
	 * <p>
	 * It is strongly recommended for this URI to be normalized, so that ordering
	 * will be consistent.  However, no normalization is performed within this API.
	 * </p>
	 * <p>
	 * All types of URI may be subjected to URL encoding, depending on
	 * application type.
	 * </p>
	 */
	final public String getUri() {
		return uri;
	}
}
