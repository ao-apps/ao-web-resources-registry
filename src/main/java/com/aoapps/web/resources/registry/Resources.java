/*
 * ao-web-resources-registry - Central registry for web resource management.
 * Copyright (C) 2020, 2021, 2022  AO Industries, Inc.
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

import com.aoapps.collections.AoCollections;
import com.aoapps.hodgepodge.graph.Edge;
import com.aoapps.hodgepodge.graph.SymmetricGraph;
import com.aoapps.hodgepodge.graph.TopologicalSorter;
import com.aoapps.lang.NullArgumentException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A set of resources for a single class.
 *
 * @author  AO Industries, Inc.
 */
// TODO: When resources becomes empty, remove from Group (except Styles and Scripts)
public class Resources<R extends Resource<R> & Comparable<? super R>> implements Serializable {

  private static final Logger logger = Logger.getLogger(Resources.class.getName());

  private static final String EOL = System.lineSeparator();

  private static class Before<R extends Resource<R> & Comparable<? super R>> implements Serializable {

    private static final long serialVersionUID = 1L;

    private final R before;
    private final boolean required;

    private Before(R before, boolean required) {
      if (before == null) {
        throw new NullArgumentException("before");
      }
      this.before = before;
      this.required = required;
    }

    @Override
    public String toString() {
      return
          before.toString()
              + (required ? " (required)" : " (optional)");
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof Before)) {
        return false;
      }
      Before<?> other = (Before) obj;
      return
          required == other.required
              && before.equals(other.before);
    }

    @Override
    public int hashCode() {
      int hash = before.hashCode();
      if (required) {
        hash++;
      }
      return hash;
    }
  }

  private static final long serialVersionUID = 1L;

  private final Set<R> resources = new HashSet<>();

  /**
   * Ordering map: <code>after -&gt; Set&lt;Before&gt;</code>.
   */
  private final Map<R, Set<Before<R>>> ordering = new HashMap<>();

  private Set<R> sorted;

  protected Resources() {
    // Do nothing
  }

  /**
   * Copy constructor.
   */
  protected Resources(Resources<R> other) {
    synchronized (other) {
      resources.addAll(other.resources);
      ordering.putAll(other.ordering);
      // Copy each
      for (Map.Entry<R, Set<Before<R>>> entry : ordering.entrySet()) {
        entry.setValue(new HashSet<>(entry.getValue()));
      }
      sorted = other.sorted;
    }
  }

  /**
   * Gets a deep copy of these resources.
   */
  protected Resources<R> copy() {
    return new Resources<>(this);
  }

  /**
   * Union constructor.
   */
  protected Resources(Collection<? extends Resources<R>> others) {
    if (logger.isLoggable(Level.FINER)) {
      logger.finer("others: " + others);
    }
    for (Resources<R> other : others) {
      synchronized (other) {
        if (logger.isLoggable(Level.FINER)) {
          logger.finer("addAll: " + other.resources);
        }
        resources.addAll(other.resources);
        for (Map.Entry<R, Set<Before<R>>> entry : other.ordering.entrySet()) {
          R after = entry.getKey();
          Set<Before<R>> befores = ordering.get(after);
          if (befores == null) {
            befores = new HashSet<>();
            ordering.put(after, befores);
          }
          befores.addAll(entry.getValue());
        }
      }
    }
    sorted = null;
  }

  /**
   * Adds a new resource, if not already present.
   *
   * @return  {@code true} if the resource was added, or {@code false} if already exists and was not added
   */
  public synchronized boolean add(R resource) {
    if (resource == null) {
      throw new NullArgumentException("resource");
    }
    boolean added = resources.add(resource);
    if (added) {
      sorted = null;
    }
    return added;
  }

  /**
   * Adds new resources, if not already present.
   */
  public Resources<R> add(Iterable<? extends R> resources) {
    if (resources != null) {
      for (R resource : resources) {
        if (resource != null) {
          add(resource);
        }
      }
    }
    return this;
  }

  /**
   * Adds new resources, if not already present.
   */
  @SuppressWarnings({"unchecked", "varargs"})
  public Resources<R> add(R ... resources) {
    if (resources != null) {
      for (R resource : resources) {
        if (resource != null) {
          add(resource);
        }
      }
    }
    return this;
  }

  /**
   * Removes a resource.
   *
   * @return  {@code true} if the resource was removed, or {@code false} if the resource was not found
   */
  public synchronized boolean remove(R resource) {
    boolean removed = resources.remove(resource);
    if (removed) {
      sorted = null;
    }
    return removed;
  }

  /**
   * Removes resources.
   */
  public Resources<R> remove(Iterable<? extends R> resources) {
    if (resources != null) {
      for (R resource : resources) {
        if (resource != null) {
          remove(resource);
        }
      }
    }
    return this;
  }

  /**
   * Removes resources.
   */
  @SuppressWarnings({"unchecked", "varargs"})
  public Resources<R> remove(R ... resources) {
    if (resources != null) {
      for (R resource : resources) {
        if (resource != null) {
          remove(resource);
        }
      }
    }
    return this;
  }

  /**
   * Adds an ordering constraint between two resources.
   *
   * @return  {@code true} if the ordering was added, or {@code false} if already exists and was not added
   */
  public synchronized boolean addOrdering(boolean required, R before, R after) {
    if (before == null) {
      throw new NullArgumentException("before");
    }
    if (after == null) {
      throw new NullArgumentException("after");
    }
    Set<Before<R>> set = ordering.get(after);
    if (set == null) {
      set = new HashSet<>();
      ordering.put(after, set);
    }
    boolean added = set.add(new Before<>(before, required));
    if (added) {
      sorted = null;
    }
    return added;
  }

  /**
   * Adds a required ordering constraint between two resources.
   */
  public boolean addOrdering(R before, R after) {
    return addOrdering(true, before, after);
  }

  /**
   * Adds ordering constraints between multiple resources, if not already present.
   */
  public Resources<R> addOrdering(boolean required, Iterable<? extends R> resources) {
    if (resources != null) {
      R lastResource = null;
      for (R resource : resources) {
        if (resource != null) {
          if (lastResource != null) {
            addOrdering(required, lastResource, resource);
          }
          lastResource = resource;
        }
      }
    }
    return this;
  }

  /**
   * Adds required ordering constraints between multiple resources, if not already present.
   */
  public Resources<R> addOrdering(Iterable<? extends R> resources) {
    return addOrdering(true, resources);
  }

  /**
   * Adds ordering constraints between multiple resources, if not already present.
   */
  @SuppressWarnings({"unchecked", "varargs"})
  public Resources<R> addOrdering(boolean required, R ... resources) {
    if (resources != null) {
      R lastResource = null;
      for (R resource : resources) {
        if (resource != null) {
          if (lastResource != null) {
            addOrdering(required, lastResource, resource);
          }
          lastResource = resource;
        }
      }
    }
    return this;
  }

  /**
   * Adds required ordering constraints between multiple resources, if not already present.
   */
  @SuppressWarnings({"unchecked", "varargs"})
  public Resources<R> addOrdering(R ... resources) {
    return addOrdering(true, resources);
  }

  /**
   * Removes an ordering constraint between two resources.
   *
   * @return  {@code true} if the ordering was removed, or {@code false} if the ordering was not found
   */
  public synchronized boolean removeOrdering(boolean required, R before, R after) {
    Set<Before<R>> set = ordering.get(after);
    if (set != null) {
      boolean removed = set.remove(new Before<>(before, required));
      if (removed) {
        sorted = null;
      }
      return removed;
    } else {
      return false;
    }
  }

  /**
   * Removes a required ordering constraint between two resources.
   */
  public boolean removeOrdering(R before, R after) {
    return removeOrdering(true, before, after);
  }

  /**
   * Removes ordering constraints between multiple resources.
   */
  public Resources<R> removeOrdering(boolean required, Iterable<? extends R> resources) {
    if (resources != null) {
      R lastResource = null;
      for (R resource : resources) {
        if (resource != null) {
          if (lastResource != null) {
            removeOrdering(required, lastResource, resource);
          }
          lastResource = resource;
        }
      }
    }
    return this;
  }

  /**
   * Removes required ordering constraints between multiple resources.
   */
  public Resources<R> removeOrdering(Iterable<? extends R> resources) {
    return removeOrdering(true, resources);
  }

  /**
   * Removes ordering constraints between multiple resources.
   */
  @SuppressWarnings({"unchecked", "varargs"})
  public Resources<R> removeOrdering(boolean required, R ... resources) {
    if (resources != null) {
      R lastResource = null;
      for (R resource : resources) {
        if (resource != null) {
          if (lastResource != null) {
            removeOrdering(required, lastResource, resource);
          }
          lastResource = resource;
        }
      }
    }
    return this;
  }

  /**
   * Removes required ordering constraints between multiple resources.
   */
  @SuppressWarnings({"unchecked", "varargs"})
  public Resources<R> removeOrdering(R ... resources) {
    return removeOrdering(true, resources);
  }

  /**
   * Performs the topological sort.
   *
   * @return  An unmodifiable set, in the sorted order.
   */
  private Set<R> topologicalSort(List<R> list) {
    assert Thread.holdsLock(this);
    // Build a SymmetricGraph, while making sure all required are found
    Map<R, Set<Edge<R>>> edgesTo = new HashMap<>();
    Map<R, Set<Edge<R>>> edgesFrom = new HashMap<>();
    Set<R> vertices = new LinkedHashSet<>(list);
    for (Map.Entry<R, Set<Before<R>>> entry : ordering.entrySet()) {
      R from = entry.getKey();
      for (Before<R> before : entry.getValue()) {
        R to = before.before;
        if (vertices.contains(to)) {
          Edge<R> edge = new Edge<>(from, to);
          Set<Edge<R>> toSet = edgesTo.get(to);
          if (toSet == null) {
            toSet = new LinkedHashSet<>();
            edgesTo.put(to, toSet);
          }
          toSet.add(edge);

          Set<Edge<R>> fromSet = edgesFrom.get(from);
          if (fromSet == null) {
            fromSet = new LinkedHashSet<>();
            edgesFrom.put(from, fromSet);
          }
          fromSet.add(edge);
        } else if (before.required) {
          throw new IllegalStateException(
              "Required resource not found:\n"
                  + "    before = " + to + "\n"
                  + "    after  = " + from
          );
        }
      }
    }
    // Java 9: new SymmetricGraph<>
    SymmetricGraph<R, Edge<R>, RuntimeException> graph = new SymmetricGraph<R, Edge<R>, RuntimeException>() {
      @Override
      public Set<Edge<R>> getEdgesTo(R to) {
        Set<Edge<R>> set = edgesTo.get(to);
        return (set == null) ? Collections.emptySet() : set;
      }
      @Override
      public Set<Edge<R>> getEdgesFrom(R from) {
        Set<Edge<R>> set = edgesFrom.get(from);
        return (set == null) ? Collections.emptySet() : set;
      }
      @Override
      public Set<R> getVertices() {
        return vertices;
      }
    };
    // Perform the sort
    return AoCollections.optimalUnmodifiableSet(new TopologicalSorter<>(graph, true).sortGraph());
  }

  /**
   * Gets a snapshot copy of the current set of resources, in no particular order.
   */
  public synchronized Set<R> getSnapshot() {
    return AoCollections.unmodifiableCopySet(resources);
  }

  /**
   * Gets the set of all resources of the given class, first with their
   * {@linkplain Resource#compareTo(com.aoapps.web.resources.registry.Resource) natural ordering},
   * then with a topological sort to manage ordering constraints;
   */
  public synchronized Set<R> getSorted() {
    Set<R> s = sorted;
    if (s == null) {
      // Natural sort
      List<R> list = new ArrayList<>(resources);
      Collections.sort(list);
      if (logger.isLoggable(Level.FINER)) {
        StringBuilder message = new StringBuilder("list sorted:");
        for (R resource : list) {
          message.append(EOL).append("    ").append(resource);
        }
        logger.finer(message.toString());
      }
      // Topological sort
      s = topologicalSort(list);
      if (logger.isLoggable(Level.FINER)) {
        StringBuilder message = new StringBuilder("topological sorted:");
        for (R resource : s) {
          message.append(EOL).append("    ").append(resource);
        }
        logger.finer(message.toString());
      }
      // Cache the value, unmodifiable
      sorted = s;
    }
    return s;
  }

  /**
   * Is this resources empty?
   */
  public synchronized boolean isEmpty() {
    return
        resources.isEmpty()
            && ordering.isEmpty();
  }
}
