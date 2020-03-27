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

import com.aoindustries.lang.Strings;
import com.aoindustries.text.SmartComparator;
import com.aoindustries.util.i18n.Locales;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;

/**
 * A CSS stylesheet is identified by URI, but has other constraints, including:
 * <ol>
 * <li>an optional media condition</li>
 * <li>an optional Internet Explorer conditional comment (deprecated)</li>
 * <li>a disabled attribute</li>
 * </ol>
 * <p>
 * Optimizers should be careful to only group styles that have equivalent
 * constraints.
 * </p>
 * <p>
 * TODO: Support a "group" (or "position" / "category"?): prelude, main, and coda.
 * "prelude" is before all others (except possibly other prelude).
 * "main" (the default) is the middle.
 * "coda" is after all others (except possibly other coda).
 * It would be an error if the topological sort results in prelude that is not
 * first or coda that is not last.  Use "prelude" for "html5.css".
 * </p>
 * <p>
 * TODO: Support inline styles without URI.
 * </p>
 *
 * @author  AO Industries, Inc.
 */
final public class Style extends Resource<Style> implements Comparable<Style> {

	/**
	 * The direction of a {@link Style}.
	 *
	 * @author  AO Industries, Inc.
	 */
	public enum Direction {

		/**
		 * Left-to-right.
		 */
		LTR,

		/**
		 * Right-to-left.
		 */
		RTL;

		/**
		 * Gets the expected direction for the given locale.
		 *
		 * @see  Locales#isRightToLeft(java.util.Locale)
		 */
		public static Direction getDirection(Locale locale) {
			return Locales.isRightToLeft(locale) ? RTL : LTR;
		}

		/**
		 * Gets the expected direction for the given language.
		 *
		 * @see  Locales#parseLocale(java.lang.String)
		 * @see  #getDirection(java.util.Locale)
		 */
		public static Direction getDirection(String language) {
			return getDirection(Locales.parseLocale(language));
		}
	}

	/**
	 * Styles start with a default ordering that should minimize the number
	 * of explicit ordering declarations:
	 * TODO: Review that this is the best default ordering.
	 * <ol>
	 * <li>Order by {@linkplain #getIe() IE conditional comment}, nulls first</li>
	 * <li>Order by {@linkplain #getMedia() media condition}, nulls first</li>
	 * <li>Order by {@linkplain #getDirection() direction}, nulls first</li>
	 * <li>Order by {@linkplain #getUri() URI}</li>
	 * </ol>
	 * <p>
	 * Note: The {@linkplain #isDisabled() disabled flag} is not used in ordering.
	 * </p>
	 * <p>
	 * All string comparisons are performed via {@link SmartComparator#ROOT}.
	 * </p>
	 * <p>
	 * As an example of this ordering, consider the following order would be the
	 * default, which we expect will often match the way CSS styles override
	 * base definitions:
	 * </p>
	 * <ol>
	 * <li><code>global.css</code> (no ie, no media)</li>
	 * <li><code>global-print.css</code> (no ie, media="print")</li>
	 * <li><code>global-ie.css</code> (ie="IE", no media)</li>
	 * <li><code>global-ie-print.css</code> (ie="IE", media="print")</li>
	 * </ol>
	 */
	// TODO: This static COMPARATOR pattern for all classes that implement compareTo?
	public static final Comparator<Style> COMPARATOR = (ss1, ss2) -> {
		int diff;
		// IE condition, nulls first
		if(ss1.ie == null) {
			if(ss2.ie != null) return -1;
		} else {
			if(ss2.ie == null) return 1;
			diff = SmartComparator.ROOT.compare(ss1.ie, ss2.ie);
			if(diff != 0) return diff;
		}

		// Media condition, nulls first
		if(ss1.media == null) {
			if(ss2.media != null) return -1;
		} else {
			if(ss2.media == null) return 1;
			diff = SmartComparator.ROOT.compare(ss1.media, ss2.media);
			if(diff != 0) return diff;
		}

		// Direction, nulls first
		if(ss1.direction == null) {
			if(ss2.direction != null) return -1;
		} else {
			if(ss2.direction == null) return 1;
			diff = ss1.direction.compareTo(ss2.direction);
			if(diff != 0) return diff;
		}

		// URI (TODO: non-nulls first for inline)
		return SmartComparator.ROOT.compare(ss1.getUri(), ss2.getUri());
	};

	public static class Builder extends Resource.Builder<Style> {

		protected Builder() {}

		@Override
		public Builder uri(String href) {
			super.uri(href);
			return this;
		}

		private String media;
		public Builder media(String media) {
			this.media = media;
			return this;
		}

		private Direction direction;
		public Builder direction(Direction direction) {
			this.direction = direction;
			return this;
		}

		/**
		 * @deprecated  Conditional comments were for IE 5-9, which are all end-of-life.
		 */
		@Deprecated
		private String ie;
		/**
		 * @deprecated  Conditional comments were for IE 5-9, which are all end-of-life.
		 */
		@Deprecated
		public Builder ie(String ie) {
			this.ie = ie;
			return this;
		}

		private boolean disabled;
		public Builder disabled(boolean disabled) {
			this.disabled = disabled;
			return this;
		}

		@Override
		public Style build() {
			return new Style(
				uri,
				media,
				direction,
				ie,
				disabled
			);
		}
	}

	public static Builder builder() {
		return new Builder();
	}

	private static final long serialVersionUID = 1L;

	private final String media;

	private final Direction direction;

	/**
	 * @deprecated  Conditional comments were for IE 5-9, which are all end-of-life.
	 */
	@Deprecated
	private final String ie;

	private final boolean disabled;

	/**
	 * @param href       See {@link #getUri()}
	 * @param media      See {@link #getMedia()}
	 * @param direction  See {@link #getDirection()}
	 * @param ie         See {@link #getIe()}
	 * @param disabled   See {@link #isDisabled()}
	 *
	 * @deprecated  Conditional comments were for IE 5-9, which are all end-of-life.
	 */
	@Deprecated
	public Style(
		String href,
		String media,
		Direction direction,
		String ie,
		boolean disabled
	) {
		super(href);
		this.media = Strings.trimNullIfEmpty(media);
		this.direction = direction;
		this.ie = Strings.trimNullIfEmpty(ie);
		this.disabled = disabled;
	}

	/**
	 * @param href       See {@link #getUri()}
	 * @param media      See {@link #getMedia()}
	 * @param direction  See {@link #getDirection()}
	 * @param disabled   See {@link #isDisabled()}
	 */
	public Style(
		String href,
		String media,
		Direction direction,
		boolean disabled
	) {
		this(href, media, direction, null, disabled);
	}

	/**
	 * @param href  See {@link #getUri()}
	 */
	public Style(String href) {
		this(href, null, null, false);
	}

	/**
	 * @see  Resource#toString()
	 * @see  #getMedia()
	 * @see  #getDirection()
	 * @see  #getIe()
	 * @see  #isDisabled()
	 */
	@Override
	public String toString() {
		if(media == null && ie == null && !disabled) {
			return super.toString();
		} else {
			StringBuilder sb = new StringBuilder(super.toString());
			sb.append('[');
			boolean needComma = false;
			if(media != null) {
				sb.append("media=\"").append(media).append('"');
				needComma = true;
			}
			if(direction != null) {
				if(needComma) sb.append(", ");
				sb.append("direction=").append(direction);
				needComma = true;
			}
			if(ie != null) {
				if(needComma) sb.append(", ");
				sb.append("ie=\"").append(ie).append('"');
				needComma = true;
			}
			if(disabled) {
				if(needComma) sb.append(", ");
				sb.append("disabled");
			}
			sb.append(']');
			return sb.toString();
		}
	}

	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof Style)) return false;
		Style other = (Style)obj;
		return
			disabled == other.disabled
			&& direction == other.direction
			&& Objects.equals(getUri(), other.getUri())
			&& Objects.equals(media, other.media)
			&& Objects.equals(ie, other.ie);
	}

	@Override
	public int hashCode() {
		int hash = Objects.hashCode(getUri());
		hash = hash * 31 + Objects.hashCode(media);
		hash = hash * 31 + Objects.hashCode(direction);
		hash = hash * 31 + Objects.hashCode(ie);
		if(disabled) hash += 1;
		return hash;
	}

	/**
	 * @see  #COMPARATOR
	 */
	@Override
	public int compareTo(Style o) {
		return COMPARATOR.compare(this, o);
	}

	/**
	 * Gets the optional media condition.
	 */
	public String getMedia() {
		return media;
	}

	/**
	 * Gets the direction for the style.
	 * This is matched against the current response language/locale, when know,
	 * to selectively include the style.
	 */
	public Direction getDirection() {
		return direction;
	}

	/**
	 * @deprecated  Conditional comments were for IE 5-9, which are all end-of-life.
	 */
	@Deprecated
	public String getIe() {
		return ie;
	}

	/**
	 * Style may be disabled by default, then enabled via JavaScript.
	 */
	public boolean isDisabled() {
		return disabled;
	}
}
