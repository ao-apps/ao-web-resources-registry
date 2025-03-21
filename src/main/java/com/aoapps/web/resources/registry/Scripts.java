/*
 * ao-web-resources-registry - Central registry for web resource management.
 * Copyright (C) 2020, 2021, 2022, 2023, 2024, 2025  AO Industries, Inc.
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
 * A partition with some extra convenience overloads for {@link Script}.
 *
 * @author  AO Industries, Inc.
 */
public final class Scripts extends Resources<Script> {

  private static final long serialVersionUID = 1L;

  Scripts() {
    // Do nothing
  }

  /**
   * Copy constructor.
   */
  private Scripts(Scripts other) {
    super(other);
  }

  /**
   * Union constructor.
   */
  private Scripts(Collection<Scripts> others) {
    super(others);
  }

  /**
   * Gets a deep copy of these resources.
   */
  @Override
  protected Scripts copy() {
    return new Scripts(this);
  }

  /**
   * Gets a the union of multiple groups.
   */
  public static Scripts union(Collection<Scripts> others) {
    // Empty resources when null or empty
    if (others == null || others.isEmpty()) {
      return new Scripts();
    }
    // Perform a copy when a single resources
    if (others.size() == 1) {
      return others.iterator().next().copy();
    }
    // Use union constructor
    return new Scripts(others);
  }

  @Override
  public Scripts add(Iterable<Script> scripts) {
    super.add(scripts);
    return this;
  }

  @Override
  public Scripts add(Script ... scripts) {
    super.add(scripts);
    return this;
  }

  /**
   * Adds a new script, if not already present.
   */
  public boolean add(String src) {
    if (src == null) {
      throw new NullArgumentException("src");
    }
    return add(new Script(src));
  }

  /**
   * Adds new scripts, if not already present.
   */
  public Scripts add(Iterables.String<?> srcs) {
    if (srcs != null) {
      for (String src : srcs) {
        if (src != null) {
          add(src);
        }
      }
    }
    return this;
  }

  /**
   * Adds new scripts, if not already present.
   */
  public Scripts add(String ... srcs) {
    if (srcs != null) {
      for (String src : srcs) {
        if (src != null) {
          add(src);
        }
      }
    }
    return this;
  }

  @Override
  public Scripts remove(Iterable<Script> scripts) {
    super.remove(scripts);
    return this;
  }

  @Override
  public Scripts remove(Script ... scripts) {
    super.remove(scripts);
    return this;
  }

  /**
   * Removes a script.
   */
  public boolean remove(String src) {
    if (src != null) {
      return remove(new Script(src));
    } else {
      return false;
    }
  }

  /**
   * Removes scripts.
   */
  public Scripts remove(Iterables.String<?> srcs) {
    if (srcs != null) {
      for (String src : srcs) {
        if (src != null) {
          remove(src);
        }
      }
    }
    return this;
  }

  /**
   * Removes scripts.
   */
  public Scripts remove(String ... srcs) {
    if (srcs != null) {
      for (String src : srcs) {
        if (src != null) {
          remove(src);
        }
      }
    }
    return this;
  }

  /**
   * {@inheritDoc}
   *
   * <p>The before script must have a position that is before or equal to the after script.</p>
   */
  @Override
  protected void checkOrdering(Script before, Script after) {
    super.checkOrdering(before, after);
    Script.Position beforePos = before.getPosition();
    Script.Position afterPos = after.getPosition();
    if (beforePos.compareTo(afterPos) > 0) {
      throw new IllegalArgumentException("before.position > after.position: " + before + " > " + after);
    }
  }


  /**
   * {@inheritDoc}
   *
   * <p>Ordering may not violate {@link Script.Position}.</p>
   */
  @Override
  public Scripts addOrdering(boolean required, Iterable<Script> scripts) {
    super.addOrdering(required, scripts);
    return this;
  }

  /**
   * {@inheritDoc}
   *
   * <p>Ordering may not violate {@link Script.Position}.</p>
   */
  @Override
  public Scripts addOrdering(Iterable<Script> scripts) {
    super.addOrdering(scripts);
    return this;
  }

  /**
   * {@inheritDoc}
   *
   * <p>Ordering may not violate {@link Script.Position}.</p>
   */
  @Override
  public Scripts addOrdering(boolean required, Script ... scripts) {
    super.addOrdering(required, scripts);
    return this;
  }

  /**
   * {@inheritDoc}
   *
   * <p>Ordering may not violate {@link Script.Position}.</p>
   */
  @Override
  public Scripts addOrdering(Script ... scripts) {
    super.addOrdering(scripts);
    return this;
  }

  /**
   * Adds an ordering constraint between two scripts.
   *
   * <p>Ordering may not violate {@link Script.Position}.</p>
   */
  public boolean addOrdering(boolean required, String beforeSrc, String afterSrc) {
    if (beforeSrc == null) {
      throw new NullArgumentException("beforeSrc");
    }
    if (afterSrc == null) {
      throw new NullArgumentException("afterSrc");
    }
    return addOrdering(
        required,
        new Script(beforeSrc),
        new Script(afterSrc)
    );
  }

  /**
   * Adds a required ordering constraint between two scripts.
   *
   * <p>Ordering may not violate {@link Script.Position}.</p>
   */
  public boolean addOrdering(String beforeSrc, String afterSrc) {
    return addOrdering(true, beforeSrc, afterSrc);
  }

  /**
   * Adds ordering constraints between multiple scripts, if not already present.
   *
   * <p>Ordering may not violate {@link Script.Position}.</p>
   */
  public Scripts addOrdering(boolean required, Iterables.String<?> srcs) {
    if (srcs != null) {
      Script lastScript = null;
      for (String src : srcs) {
        if (src != null) {
          Script script = new Script(src);
          if (lastScript != null) {
            addOrdering(required, lastScript, script);
          }
          lastScript = script;
        }
      }
    }
    return this;
  }

  /**
   * Adds required ordering constraints between multiple scripts, if not already present.
   *
   * <p>Ordering may not violate {@link Script.Position}.</p>
   */
  public Scripts addOrdering(Iterables.String<?> srcs) {
    return addOrdering(true, srcs);
  }

  /**
   * Adds ordering constraints between multiple scripts, if not already present.
   *
   * <p>Ordering may not violate {@link Script.Position}.</p>
   */
  public Scripts addOrdering(boolean required, String ... srcs) {
    if (srcs != null) {
      Script lastScript = null;
      for (String src : srcs) {
        if (src != null) {
          Script script = new Script(src);
          if (lastScript != null) {
            addOrdering(required, lastScript, script);
          }
          lastScript = script;
        }
      }
    }
    return this;
  }

  /**
   * Adds required ordering constraints between multiple scripts, if not already present.
   *
   * <p>Ordering may not violate {@link Script.Position}.</p>
   */
  public Scripts addOrdering(String ... srcs) {
    return addOrdering(true, srcs);
  }

  @Override
  public Scripts removeOrdering(boolean required, Iterable<Script> scripts) {
    super.removeOrdering(required, scripts);
    return this;
  }

  @Override
  public Scripts removeOrdering(Iterable<Script> scripts) {
    super.removeOrdering(scripts);
    return this;
  }

  @Override
  public Scripts removeOrdering(boolean required, Script ... scripts) {
    super.removeOrdering(required, scripts);
    return this;
  }

  @Override
  public Scripts removeOrdering(Script ... scripts) {
    super.removeOrdering(scripts);
    return this;
  }

  /**
   * Removes an ordering constraint between two scripts.
   */
  public boolean removeOrdering(boolean required, String beforeSrc, String afterSrc) {
    if (beforeSrc != null && afterSrc != null) {
      return removeOrdering(
          required,
          new Script(beforeSrc),
          new Script(afterSrc)
      );
    } else {
      return false;
    }
  }

  /**
   * Removes a required ordering constraint between two scripts.
   */
  public boolean removeOrdering(String beforeUri, String afterUri) {
    return removeOrdering(true, beforeUri, afterUri);
  }

  /**
   * Removes ordering constraints between multiple scripts.
   */
  public Scripts removeOrdering(boolean required, Iterables.String<?> srcs) {
    if (srcs != null) {
      Script lastScript = null;
      for (String src : srcs) {
        if (src != null) {
          Script script = new Script(src);
          if (lastScript != null) {
            removeOrdering(required, lastScript, script);
          }
          lastScript = script;
        }
      }
    }
    return this;
  }

  /**
   * Removes required ordering constraints between multiple scripts.
   */
  public Scripts removeOrdering(Iterables.String<?> srcs) {
    return removeOrdering(true, srcs);
  }

  /**
   * Removes ordering constraints between multiple scripts.
   */
  public Scripts removeOrdering(boolean required, String ... srcs) {
    if (srcs != null) {
      Script lastScript = null;
      for (String src : srcs) {
        if (src != null) {
          Script script = new Script(src);
          if (lastScript != null) {
            removeOrdering(required, lastScript, script);
          }
          lastScript = script;
        }
      }
    }
    return this;
  }

  /**
   * Removes required ordering constraints between multiple scripts.
   */
  public Scripts removeOrdering(String ... srcs) {
    return removeOrdering(true, srcs);
  }
}
