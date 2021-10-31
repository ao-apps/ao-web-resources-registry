/*
 * ao-web-resources-registry - Central registry for web resource management.
 * Copyright (C) 2020, 2021  AO Industries, Inc.
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

import com.aoapps.lang.Iterables;
import com.aoapps.lang.NullArgumentException;
import java.util.Collection;

/**
 * A partition with some extra convenience overloads for {@link Style}.
 *
 * @author  AO Industries, Inc.
 */
public final class Styles extends Resources<Style> {

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

	@Override
	public Styles add(Iterable<? extends Style> styles) {
		super.add(styles);
		return this;
	}

	@Override
	public Styles add(Style ... styles) {
		super.add(styles);
		return this;
	}

	/**
	 * Adds a new style, if not already present.
	 */
	public boolean add(String href) {
		if(href == null) throw new NullArgumentException("href");
		return add(new Style(href));
	}

	/**
	 * Adds new styles, if not already present.
	 */
	public Styles add(Iterables.String<?> hrefs) {
		if(hrefs != null) {
			for(String href : hrefs) {
				if(href != null) {
					add(href);
				}
			}
		}
		return this;
	}

	/**
	 * Adds new styles, if not already present.
	 */
	public Styles add(String ... hrefs) {
		if(hrefs != null) {
			for(String href : hrefs) {
				if(href != null) {
					add(href);
				}
			}
		}
		return this;
	}

	@Override
	public Styles remove(Iterable<? extends Style> styles) {
		super.remove(styles);
		return this;
	}

	@Override
	public Styles remove(Style ... styles) {
		super.remove(styles);
		return this;
	}

	/**
	 * Removes a style.
	 */
	public boolean remove(String href) {
		if(href != null) {
			return remove(new Style(href));
		} else {
			return false;
		}
	}

	/**
	 * Removes styles.
	 */
	public Styles remove(Iterables.String<?> hrefs) {
		if(hrefs != null) {
			for(String href : hrefs) {
				if(href != null) {
					remove(href);
				}
			}
		}
		return this;
	}

	/**
	 * Removes styles.
	 */
	public Styles remove(String ... hrefs) {
		if(hrefs != null) {
			for(String href : hrefs) {
				if(href != null) {
					remove(href);
				}
			}
		}
		return this;
	}

	@Override
	public Styles addOrdering(boolean required, Iterable<? extends Style> styles) {
		super.addOrdering(required, styles);
		return this;
	}

	@Override
	public Styles addOrdering(Iterable<? extends Style> styles) {
		super.addOrdering(styles);
		return this;
	}

	@Override
	public Styles addOrdering(boolean required, Style ... styles) {
		super.addOrdering(required, styles);
		return this;
	}

	@Override
	public Styles addOrdering(Style ... styles) {
		super.addOrdering(styles);
		return this;
	}

	/**
	 * Adds an ordering constraint between two styles.
	 */
	public boolean addOrdering(boolean required, String beforeHref, String afterHref) {
		if(beforeHref == null) throw new NullArgumentException("beforeHref");
		if(afterHref == null) throw new NullArgumentException("afterHref");
		return addOrdering(
			required,
			new Style(beforeHref),
			new Style(afterHref)
		);
	}

	/**
	 * Adds a required ordering constraint between two styles.
	 */
	public boolean addOrdering(String beforeHref, String afterHref) {
		return addOrdering(true, beforeHref, afterHref);
	}

	/**
	 * Adds ordering constraints between multiple styles, if not already present.
	 */
	public Styles addOrdering(boolean required, Iterables.String<?> hrefs) {
		if(hrefs != null) {
			Style lastStyle = null;
			for(String href : hrefs) {
				if(href != null) {
					Style style = new Style(href);
					if(lastStyle != null) {
						addOrdering(required, lastStyle, style);
					}
					lastStyle = style;
				}
			}
		}
		return this;
	}

	/**
	 * Adds required ordering constraints between multiple styles, if not already present.
	 */
	public Styles addOrdering(Iterables.String<?> hrefs) {
		return addOrdering(true, hrefs);
	}

	/**
	 * Adds ordering constraints between multiple styles, if not already present.
	 */
	public Styles addOrdering(boolean required, String ... hrefs) {
		if(hrefs != null) {
			Style lastStyle = null;
			for(String href : hrefs) {
				if(href != null) {
					Style style = new Style(href);
					if(lastStyle != null) {
						addOrdering(required, lastStyle, style);
					}
					lastStyle = style;
				}
			}
		}
		return this;
	}

	/**
	 * Adds required ordering constraints between multiple styles, if not already present.
	 */
	public Styles addOrdering(String ... hrefs) {
		return addOrdering(true, hrefs);
	}

	@Override
	public Styles removeOrdering(boolean required, Iterable<? extends Style> styles) {
		super.removeOrdering(required, styles);
		return this;
	}

	@Override
	public Styles removeOrdering(Iterable<? extends Style> styles) {
		super.removeOrdering(styles);
		return this;
	}

	@Override
	public Styles removeOrdering(boolean required, Style ... styles) {
		super.removeOrdering(required, styles);
		return this;
	}

	@Override
	public Styles removeOrdering(Style ... styles) {
		super.removeOrdering(styles);
		return this;
	}

	/**
	 * Removes an ordering constraint between two styles.
	 */
	public boolean removeOrdering(boolean required, String beforeHref, String afterHref) {
		if(beforeHref != null && afterHref != null) {
			return removeOrdering(
				required,
				new Style(beforeHref),
				new Style(afterHref)
			);
		} else {
			return false;
		}
	}

	/**
	 * Removes a required ordering constraint between two styles.
	 */
	public boolean removeOrdering(String beforeHref, String afterHref) {
		return removeOrdering(true, beforeHref, afterHref);
	}

	/**
	 * Removes ordering constraints between multiple styles.
	 */
	public Styles removeOrdering(boolean required, Iterables.String<?> hrefs) {
		if(hrefs != null) {
			Style lastStyle = null;
			for(String href : hrefs) {
				if(href != null) {
					Style style = new Style(href);
					if(lastStyle != null) {
						removeOrdering(required, lastStyle, style);
					}
					lastStyle = style;
				}
			}
		}
		return this;
	}

	/**
	 * Removes required ordering constraints between multiple styles.
	 */
	public Styles removeOrdering(Iterables.String<?> hrefs) {
		return removeOrdering(true, hrefs);
	}

	/**
	 * Removes ordering constraints between multiple styles.
	 */
	public Styles removeOrdering(boolean required, String ... hrefs) {
		if(hrefs != null) {
			Style lastStyle = null;
			for(String href : hrefs) {
				if(href != null) {
					Style style = new Style(href);
					if(lastStyle != null) {
						removeOrdering(required, lastStyle, style);
					}
					lastStyle = style;
				}
			}
		}
		return this;
	}

	/**
	 * Removes required ordering constraints between multiple styles.
	 */
	public Styles removeOrdering(String ... hrefs) {
		return removeOrdering(true, hrefs);
	}
}
