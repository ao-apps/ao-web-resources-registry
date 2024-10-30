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
import com.aoapps.lang.text.SmartComparator;
import java.util.Comparator;
import java.util.Objects;

/**
 * A script is identified by URI, but has other constraints.  Including:
 * <ol>
 * <li>an async attribute</li>
 * <li>an defer attribute</li>
 * <li>an optional crossorigin attribute</li>
 * <li>TODO: Add more attributes as-needed</li>
 * </ol>
 *
 * <p>Optimizers should be careful to only group stylesheets that have equivalent
 * constraints.</p>
 *
 * <p>TODO: Support inline scripts without URI (defer and async not allowed for inline).</p>
 *
 * <p>TODO: Support modules.</p>
 *
 * @author  AO Industries, Inc.
 */
public final class Script extends Resource<Script> implements Comparable<Script> {

  /**
   * Scripts start with a default ordering that should minimize the number
   * of explicit ordering declarations.
   * <ol>
   * <li>Order by {@linkplain #isAsync() async}, true first</li>
   * <li>Order by {@linkplain #isDefer() defer}, true first</li>
   * <li>Order by {@linkplain #getUri() URI}</li>
   * </ol>
   *
   * <p>Note: The {@linkplain #getCrossorigin() crossorigin policy} is not used in ordering.</p>
   *
   * <p>All string comparisons are performed via {@link SmartComparator#ROOT}.</p>
   */
  public static final Comparator<Script> COMPARATOR = (Script ss1, Script ss2) -> {
    int diff;
    // async, true first
    diff = -Boolean.compare(ss1.async, ss2.async);
    if (diff != 0) {
      return diff;
    }

    // defer, true first
    diff = -Boolean.compare(ss1.defer, ss2.defer);
    if (diff != 0) {
      return diff;
    }

    // URI (TODO: non-nulls first for inline)
    return SmartComparator.ROOT.compare(ss1.getUri(), ss2.getUri());
  };

  /**
   * The set of allowed script locations.
   */
  public enum Position {

    /**
     * Scripts added just after the head opening tag.
     */
    HEAD_START,

    /**
     * Scripts added just before the head closing tag.
     */
    HEAD_END,

    /**
     * Scripts added just after the body opening tag.
     */
    BODY_START,

    /**
     * Scripts added just before the body closing tag.
     */
    BODY_END;

    /**
     * The default position.
     */
    public static final Position DEFAULT = HEAD_END;
  }

  /**
   * Builder pattern for {@link Script}.
   */
  public static class Builder extends Resource.Builder<Script> {

    protected Builder() {
      // Do nothing
    }

    @Override
    public Builder uri(String src) {
      super.uri(src);
      return this;
    }

    private Position position = Position.DEFAULT;

    /**
     * Sets the position, which defaults to {@link Position#DEFAULT}.
     */
    public Builder position(Position position) {
      this.position = position;
      return this;
    }

    /**
     * Sets the async, which defaults to {@code false}.
     */
    private boolean async;

    public Builder async(boolean async) {
      this.async = async;
      return this;
    }

    private boolean defer;

    /**
     * Sets the defer, which defaults to {@code false}.
     */
    public Builder defer(boolean defer) {
      this.defer = defer;
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

    @Override
    public Script build() {
      return new Script(
          uri,
          position,
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

  private final Position position;
  private final boolean async;
  private final boolean defer;
  private final String crossorigin;

  /**
   * Creates a new script.
   *
   * @param src          See {@link #getUri()}
   * @param position     See {@link #getPosition()}
   * @param async        See {@link #isAsync()}
   * @param defer        See {@link #isDefer()}
   * @param crossorigin  See {@link #getCrossorigin()}
   */
  public Script(String src, Position position, boolean async, boolean defer, String crossorigin) {
    super(src);
    this.position = position;
    this.async = async;
    this.defer = defer;
    this.crossorigin = Strings.trimNullIfEmpty(crossorigin);
  }

  /**
   * Creates a new script.
   *
   * @param src  See {@link #getUri()}
   */
  public Script(String src) {
    this(src, Position.DEFAULT, false, false, null);
  }

  /**
   * {@inheritDoc}
   *
   * @see  Resource#toString()
   * @see  #getPosition()
   * @see  #isAsync()
   * @see  #isDefer()
   * @see  #getCrossorigin()
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(super.toString());
    sb.append('[').append(position);
    if (async) {
      sb.append(", async");
    }
    if (defer) {
      sb.append(", defer");
    }
    if (crossorigin != null) {
      sb.append(", crossorigin=\"").append(crossorigin).append('"');
    }
    sb.append(']');
    return sb.toString();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Script)) {
      return false;
    }
    Script other = (Script) obj;
    return
        position == other.position
            && async == other.async
            && defer == other.defer
            && Objects.equals(getUri(), other.getUri())
            && Objects.equals(crossorigin, other.crossorigin);
  }

  @Override
  public int hashCode() {
    int hash = Objects.hashCode(getUri());
    hash = hash * 31 + position.hashCode();
    hash = hash * 31 + Objects.hashCode(crossorigin);
    if (async) {
      hash += 1;
    }
    if (defer) {
      hash += 2;
    }
    return hash;
  }

  /**
   * {@inheritDoc}
   *
   * @see  #COMPARATOR
   */
  @Override
  public int compareTo(Script o) {
    return COMPARATOR.compare(this, o);
  }

  /**
   * Gets this script position.
   */
  public Position getPosition() {
    return position;
  }

  /**
   * Gets this script is asynchronous.
   */
  public boolean isAsync() {
    return async;
  }

  /**
   * Gets this script is deferred.
   */
  public boolean isDefer() {
    return defer;
  }

  /**
   * Gets the optional crossorigin policy.
   */
  public String getCrossorigin() {
    return crossorigin;
  }
}
