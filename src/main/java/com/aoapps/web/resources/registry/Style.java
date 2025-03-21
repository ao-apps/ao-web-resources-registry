/*
 * ao-web-resources-registry - Central registry for web resource management.
 * Copyright (C) 2020, 2021, 2022, 2023, 2024  AO Industries, Inc.
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
 * along with ao-web-resources-registry.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.aoapps.web.resources.registry;

import com.aoapps.lang.Strings;
import com.aoapps.lang.i18n.Locales;
import com.aoapps.lang.text.SmartComparator;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;

/**
 * A CSS stylesheet is identified by URI, but has other constraints.  Including:
 * <ol>
 * <li>an optional media condition</li>
 * <li>an optional crossorigin attribute</li>
 * <li>a disabled attribute</li>
 * </ol>
 *
 * <p>Optimizers should be careful to only group styles that have equivalent
 * constraints.</p>
 *
 * <p>TODO: Support a "group" (or "position" / "category"?): prelude, main, and coda.
 * "prelude" is before all others (except possibly other prelude).
 * "main" (the default) is the middle.
 * "coda" is after all others (except possibly other coda).
 * It would be an error if the topological sort results in prelude that is not
 * first or coda that is not last.  Use "prelude" for "html5.css".</p>
 *
 * <p>TODO: Support inline styles without URI.</p>
 *
 * @author  AO Industries, Inc.
 */
public final class Style extends Resource<Style> implements Comparable<Style> {

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
   * <li>Order by {@linkplain #getMedia() media condition}, nulls first</li>
   * <li>Order by {@linkplain #getDirection() direction}, nulls first</li>
   * <li>Order by {@linkplain #getUri() URI}</li>
   * </ol>
   *
   * <p>Note: The {@linkplain #getCrossorigin() crossorigin policy} is not used in ordering.</p>
   *
   * <p>Note: The {@linkplain #isDisabled() disabled flag} is not used in ordering.</p>
   *
   * <p>All string comparisons are performed via {@link SmartComparator#ROOT}.</p>
   *
   * <p>As an example of this ordering, consider the following order would be the
   * default, which we expect will often match the way CSS styles override
   * base definitions:</p>
   *
   * <ol>
   * <li><code>global.css</code> (no media)</li>
   * <li><code>global-print.css</code> (media="print")</li>
   * </ol>
   */
  // TODO: This static COMPARATOR pattern for all classes that implement compareTo?
  public static final Comparator<Style> COMPARATOR = (Style ss1, Style ss2) -> {
    int diff;
    // Media condition, nulls first
    if (ss1.media == null) {
      if (ss2.media != null) {
        return -1;
      }
    } else {
      if (ss2.media == null) {
        return 1;
      }
      diff = SmartComparator.ROOT.compare(ss1.media, ss2.media);
      if (diff != 0) {
        return diff;
      }
    }

    // Direction, nulls first
    if (ss1.direction == null) {
      if (ss2.direction != null) {
        return -1;
      }
    } else {
      if (ss2.direction == null) {
        return 1;
      }
      diff = ss1.direction.compareTo(ss2.direction);
      if (diff != 0) {
        return diff;
      }
    }

    // URI (TODO: non-nulls first for inline)
    return SmartComparator.ROOT.compare(ss1.getUri(), ss2.getUri());
  };

  /**
   * Builder pattern for {@link Style}.
   */
  public static class Builder extends Resource.Builder<Style> {

    protected Builder() {
      // Do nothing
    }

    @Override
    public Builder uri(String href) {
      super.uri(href);
      return this;
    }

    private String media;

    /**
     * Sets the media, which defaults to {@code null}.
     */
    public Builder media(String media) {
      this.media = media;
      return this;
    }

    private Direction direction;

    /**
     * Sets the direction, which defaults to {@code null}.
     */
    public Builder direction(Direction direction) {
      this.direction = direction;
      return this;
    }

    private String crossorigin;

    /**
     * Sets the crossorigin, which defaults to {@code null}.
     */
    public Builder crossorigin(String crossorigin) {
      this.crossorigin = crossorigin;
      return this;
    }

    private boolean disabled;

    /**
     * Sets the disabled flag, which defaults to {@code false}.
     */
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
          crossorigin,
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
  private final String crossorigin;
  private final boolean disabled;

  /**
   * Creates a new style.
   *
   * @param href         See {@link #getUri()}
   * @param media        See {@link #getMedia()}
   * @param direction    See {@link #getDirection()}
   * @param crossorigin  See {@link #getCrossorigin()}
   * @param disabled     See {@link #isDisabled()}
   */
  public Style(
      String href,
      String media,
      Direction direction,
      String crossorigin,
      boolean disabled
  ) {
    super(href);
    this.media = Strings.trimNullIfEmpty(media);
    this.direction = direction;
    this.crossorigin = Strings.trimNullIfEmpty(crossorigin);
    this.disabled = disabled;
  }

  /**
   * Creates a new style.
   *
   * @param href  See {@link #getUri()}
   */
  public Style(String href) {
    this(href, null, null, null, false);
  }

  /**
   * {@inheritDoc}
   *
   * @see  Resource#toString()
   * @see  #getMedia()
   * @see  #getDirection()
   * @see  #getCrossorigin()
   * @see  #isDisabled()
   */
  @Override
  public String toString() {
    if (media == null && !disabled) {
      return super.toString();
    } else {
      StringBuilder sb = new StringBuilder(super.toString());
      sb.append('[');
      boolean needComma = false;
      if (media != null) {
        sb.append("media=\"").append(media).append('"');
        needComma = true;
      }
      if (direction != null) {
        if (needComma) {
          sb.append(", ");
        }
        sb.append("direction=").append(direction);
        needComma = true;
      }
      if (crossorigin != null) {
        if (needComma) {
          sb.append(", ");
        }
        sb.append("crossorigin=\"").append(crossorigin).append('"');
        needComma = true;
      }
      if (disabled) {
        if (needComma) {
          sb.append(", ");
        }
        sb.append("disabled");
      }
      sb.append(']');
      return sb.toString();
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Style)) {
      return false;
    }
    Style other = (Style) obj;
    return
        disabled == other.disabled
            && direction == other.direction
            && Objects.equals(getUri(), other.getUri())
            && Objects.equals(media, other.media)
            && Objects.equals(crossorigin, other.crossorigin);
  }

  @Override
  public int hashCode() {
    int hash = Objects.hashCode(getUri());
    hash = hash * 31 + Objects.hashCode(media);
    hash = hash * 31 + Objects.hashCode(direction);
    hash = hash * 31 + Objects.hashCode(crossorigin);
    if (disabled) {
      hash += 1;
    }
    return hash;
  }

  /**
   * {@inheritDoc}
   *
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
   * Gets the optional crossorigin policy.
   */
  public String getCrossorigin() {
    return crossorigin;
  }

  /**
   * Style may be disabled by default, then enabled via JavaScript.
   */
  public boolean isDisabled() {
    return disabled;
  }
}
