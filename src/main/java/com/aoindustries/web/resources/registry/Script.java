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
import java.util.Comparator;
import java.util.Objects;

/**
 * A script is identified by URI, but has other constraints, including:
 * <ol>
 * <li>an async attribute</li>
 * <li>an defer attribute</li>
 * <li>an optional crossorigin attribute</li>
 * <li>TODO: Add more attributes as-needed</li>
 * </ol>
 * <p>
 * Optimizers should be careful to only group stylesheets that have equivalent
 * constraints.
 * </p>
 * <p>
 * TODO: Support inline scripts without URI (defer and async not allowed for inline).
 * </p>
 * <p>
 * TODO: Support modules.
 * </p>
 *
 * @author  AO Industries, Inc.
 */
public class Script extends Resource<Script> implements Comparable<Script> {

	/**
	 * Scripts start with a default ordering that should minimize the number
	 * of explicit ordering declarations:
	 * <ol>
	 * <li>Order by {@linkplain #isAsync() async}, true first</li>
	 * <li>Order by {@linkplain #isDefer() defer}, true first</li>
	 * <li>Order by {@linkplain #getUri() URI}</li>
	 * </ol>
	 * <p>
	 * Note: The {@linkplain #getCrossorigin() crossorigin policy} is not used in ordering.
	 * </p>
	 * <p>
	 * All string comparisons are performed via {@link SmartComparator#ROOT}.
	 * </p>
	 */
	public static final Comparator<Script> COMPARATOR = (ss1, ss2) -> {
		int diff;
		// async, true first
		diff = -Boolean.compare(ss1.async, ss2.async);
		if(diff != 0) return diff;

		// defer, true first
		diff = -Boolean.compare(ss1.defer, ss2.defer);
		if(diff != 0) return diff;

		// URI
		return SmartComparator.ROOT.compare(ss1.getUri(), ss2.getUri());
	};

	public static class Builder extends Resource.Builder<Script> {

		protected Builder() {}

		@Override
		public Builder uri(String uri) {
			super.uri(uri);
			return this;
		}

		private boolean async;
		public Builder async(boolean async) {
			this.async = async;
			return this;
		}

		private boolean defer;
		public Builder defer(boolean defer) {
			this.defer = defer;
			return this;
		}

		private String crossorigin;
		public Builder crossorigin(String crossorigin) {
			this.crossorigin = crossorigin;
			return this;
		}

		@Override
		public Script build() {
			return new Script(
				uri,
				async,
				defer,
				crossorigin
			);
		}
	}

	public static Builder builder() {
		return new Builder();
	}

	private static final long serialVersionUID = 1L;

	private final boolean async;
	private final boolean defer;
	private final String crossorigin;

	/**
	 * @param uri          See {@link #getUri()}
	 * @param async        See {@link #isAsync()}
	 * @param defer        See {@link #isDefer()}
	 * @param crossorigin  See {@link #getCrossorigin()}
	 */
	public Script(String uri, boolean async, boolean defer, String crossorigin) {
		super(uri);
		this.async = async;
		this.defer = defer;
		this.crossorigin = StringUtility.trimNullIfEmpty(crossorigin);
	}

	/**
	 * @see  Resource#toString()
	 * @see  #isAsync()
	 * @see  #isDefer()
	 * @see  #getCrossorigin()
	 */
	@Override
	public String toString() {
		if(!async && !defer && crossorigin == null) {
			return super.toString();
		} else {
			StringBuilder sb = new StringBuilder(super.toString());
			sb.append('[');
			boolean needComma = false;
			if(async) {
				sb.append("async");
				needComma = true;
			}
			if(defer) {
				if(needComma) sb.append(", ");
				sb.append("defer");
				needComma = true;
			}
			if(crossorigin != null) {
				if(needComma) sb.append(", ");
				sb.append("crossorigin=\"").append(crossorigin).append('"');
			}
			sb.append(']');
			return sb.toString();
		}
	}

	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof Script)) return false;
		Script other = (Script)obj;
		return
			async == other.async
			&& defer == other.defer
			&& Objects.equals(getUri(), other.getUri())
			&& Objects.equals(crossorigin, other.crossorigin);
	}

	@Override
	public int hashCode() {
		int hash = Objects.hashCode(getUri());
		hash = hash * 31 + Objects.hashCode(crossorigin);
		if(async) hash += 1;
		if(defer) hash += 2;
		return hash;
	}

	/**
	 * @see  #COMPARATOR
	 */
	@Override
	final public int compareTo(Script o) {
		return COMPARATOR.compare(this, o);
	}

	/**
	 * Is this script asynchronous?
	 */
	final public boolean isAsync() {
		return async;
	}

	/**
	 * Is this script deferred?
	 */
	final public boolean isDefer() {
		return defer;
	}

	/**
	 * Gets the optional crossorigin policy.
	 */
	final public String getCrossorigin() {
		return crossorigin;
	}
}
