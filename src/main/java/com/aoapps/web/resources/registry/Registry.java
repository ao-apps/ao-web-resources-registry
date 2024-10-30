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

import com.aoapps.lang.Iterables;
import com.aoapps.lang.NullArgumentException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A registry contains a set of groups, along with activations.
 *
 * <p>TODO: Idea: Have a default group that is always active (getDefaultGroup), and when group not provided this is
 * assumed.  This would save all the group naming and activation when the intent is to have always active.</p>
 *
 * @author  AO Industries, Inc.
 */
public class Registry implements Serializable {

  private static final long serialVersionUID = 1L;

  private final ConcurrentMap<Group.Name, Group> groups = new ConcurrentHashMap<>();

  private final Map<Group.Name, Boolean> activations = new ConcurrentHashMap<>();

  public Registry() {
    // Do nothing
  }

  /**
   * Copy constructor.
   */
  protected Registry(Registry other) {
    groups.putAll(other.groups);
    // Copy each
    for (Map.Entry<Group.Name, Group> entry : groups.entrySet()) {
      entry.setValue(entry.getValue().copy());
    }
    activations.putAll(other.activations);
  }

  /**
   * Gets a deep copy of this registry.
   */
  public Registry copy() {
    return new Registry(this);
  }

  // TODO: Is full registry union still used?  Or is it just group unions?
  //  /**
  //   * Union constructor.
  //   */
  //  protected Registry(Collection<? extends Registry> others) {
  //    // Find all groups
  //    Map<Group.Name, List<Group>> allGroups = new HashMap<>();
  //    for (Registry other : others) {
  //      for (Map.Entry<Group.Name, Group> entry : other.groups.entrySet()) {
  //        Group.Name name = entry.getKey();
  //        Group group = entry.getValue();
  //        List<Group> groupsForName = allGroups.get(name);
  //        if (groupsForName == null) {
  //          groupsForName = new ArrayList<>();
  //          allGroups.put(name, groupsForName);
  //        }
  //        groupsForName.add(group);
  //      }
  //    }
  //    // Union all groups
  //    for (Map.Entry<String, List<Group>> entry : allGroups.entrySet()) {
  //      groups.put(
  //        entry.getKey(),
  //        Group.union(entry.getValue())
  //      );
  //    }
  //    // Union all activations
  //    TODO
  //  }

  // TODO: Is full registry union still used?  Or is it just group unions?
  //  /**
  //   * Gets a the union of multiple registries.
  //   * This is a deep copy and may be manipulated without altering the source registries.
  //   */
  //  public static Registry union(Collection<? extends Registry> others) {
  //    // Empty registry when null or empty
  //    if (others == null || others.isEmpty()) {
  //      return new Registry();
  //    }
  //    // Perform a copy when a single registry
  //    if (others.size() == 1) {
  //      return others.iterator().next().copy();
  //    }
  //    // Use union constructor
  //    return new Registry(others);
  //  }

  /**
   * Gets the group for a given name, optionally creating it if not already present.
   *
   * @param  createIfMissing  When {@code true}, will create the group if missing
   *
   * @return  The group or {@code null} when the group does not exist and {@code createIfMissing} is {@code false}.
   */
  public Group getGroup(Group.Name name, boolean createIfMissing) {
    if (name == null) {
      throw new NullArgumentException("name");
    }
    Group group = groups.get(name);
    if (group == null && createIfMissing) {
      group = new Group();
      Group existing = groups.putIfAbsent(name, group);
      if (existing != null) {
        group = existing;
      }
    }
    return group;
  }

  /**
   * Gets the group for a given name, optionally creating it if not already present.
   *
   * @param  createIfMissing  When {@code true}, will create the group if missing
   *
   * @return  The group or {@code null} when the group does not exist and {@code createIfMissing} is {@code false}.
   *
   * @throws  IllegalArgumentException  See {@link Group.Name#checkName(java.lang.String)}.
   */
  public Group getGroup(String name, boolean createIfMissing) throws IllegalArgumentException {
    return getGroup(new Group.Name(name), createIfMissing);
  }

  /**
   * Gets the group for a given name, creating it if not already present.
   */
  public Group getGroup(Group.Name name) {
    return getGroup(name, true);
  }

  /**
   * Gets the group for a given name, creating it if not already present.
   *
   * @throws  IllegalArgumentException  See {@link Group.Name#checkName(java.lang.String)}.
   */
  public Group getGroup(String name) throws IllegalArgumentException {
    return getGroup(new Group.Name(name), true);
  }

  /**
   * Gets an unmodifiable view of the current activations.
   * <ul>
   * <li>{@link Boolean#TRUE} indicates the group is activated by this registry.</li>
   * <li>{@link Boolean#FALSE} indicates the group is deactivated by this registry.</li>
   * <li>{@code null} indicates the activation is unchanged.</li>
   * </ul>
   *
   * <p>Groups are inactive by default, when not activated in any of the applied registries.</p>
   *
   * <p>This mapping is not a snapshot and may reflect concurrent changes to the registry
   * (see {@link ConcurrentHashMap}).</p>
   */
  public Map<Group.Name, Boolean> getActivations() {
    return Collections.unmodifiableMap(activations);
  }

  /**
   * Sets the activation for the given group.
   * <ul>
   * <li>{@link Boolean#TRUE} indicates the group is activated by this registry.</li>
   * <li>{@link Boolean#FALSE} indicates the group is deactivated by this registry.</li>
   * <li>{@code null} indicates the activation is unchanged.</li>
   * </ul>
   *
   * <p>The group does not need to be part of this registry.  In fact, it will
   * often be the case that the application-scope registry contains the
   * resources while request/theme/view/page-scope registries activate them.</p>
   *
   * @param  activation  When {@code null}, the activation is removed.
   *
   * @return  The previous activation value for the group
   */
  public Boolean setActivation(Group.Name group, Boolean activation) {
    if (group == null) {
      throw new NullArgumentException("group");
    }
    if (activation == null) {
      return activations.remove(group);
    } else {
      return activations.put(group, activation);
    }
  }

  /**
   * Activates the given group.
   */
  public Registry activate(Group.Name group) {
    setActivation(group, true);
    return this;
  }

  /**
   * Activates the given group.
   *
   * @throws  IllegalArgumentException  See {@link Group.Name#checkName(java.lang.String)}.
   */
  public Registry activate(String group) throws IllegalArgumentException {
    setActivation(new Group.Name(group), true);
    return this;
  }

  /**
   * Activates the given groups.
   */
  public Registry activate(Iterable<Group.Name> groups) {
    if (groups != null) {
      for (Group.Name group : groups) {
        if (group != null) {
          activate(group);
        }
      }
    }
    return this;
  }

  /**
   * Activates the given groups.
   */
  public Registry activate(Group.Name ... groups) {
    if (groups != null) {
      for (Group.Name group : groups) {
        if (group != null) {
          activate(group);
        }
      }
    }
    return this;
  }

  /**
   * Activates the given groups.
   *
   * @throws  IllegalArgumentException  See {@link Group.Name#checkName(java.lang.String)}.
   */
  public Registry activate(Iterables.String<?> groups) throws IllegalArgumentException {
    if (groups != null) {
      for (String group : groups) {
        if (group != null) {
          activate(group);
        }
      }
    }
    return this;
  }

  /**
   * Activates the given groups.
   *
   * @throws  IllegalArgumentException  See {@link Group.Name#checkName(java.lang.String)}.
   */
  public Registry activate(String ... groups) throws IllegalArgumentException {
    if (groups != null) {
      for (String group : groups) {
        if (group != null) {
          activate(group);
        }
      }
    }
    return this;
  }

  /**
   * Deactivates the given group.
   */
  public Registry deactivate(Group.Name group) {
    setActivation(group, false);
    return this;
  }

  /**
   * Deactivates the given group.
   *
   * @throws  IllegalArgumentException  See {@link Group.Name#checkName(java.lang.String)}.
   */
  public Registry deactivate(String group) throws IllegalArgumentException {
    setActivation(new Group.Name(group), false);
    return this;
  }

  /**
   * Deactivates the given groups.
   */
  public Registry deactivate(Iterable<Group.Name> groups) {
    if (groups != null) {
      for (Group.Name group : groups) {
        if (group != null) {
          deactivate(group);
        }
      }
    }
    return this;
  }

  /**
   * Deactivates the given groups.
   */
  public Registry deactivate(Group.Name ... groups) {
    if (groups != null) {
      for (Group.Name group : groups) {
        if (group != null) {
          deactivate(group);
        }
      }
    }
    return this;
  }

  /**
   * Deactivates the given groups.
   *
   * @throws  IllegalArgumentException  See {@link Group.Name#checkName(java.lang.String)}.
   */
  public Registry deactivate(Iterables.String<?> groups) throws IllegalArgumentException {
    if (groups != null) {
      for (String group : groups) {
        if (group != null) {
          deactivate(group);
        }
      }
    }
    return this;
  }

  /**
   * Deactivates the given groups.
   *
   * @throws  IllegalArgumentException  See {@link Group.Name#checkName(java.lang.String)}.
   */
  public Registry deactivate(String ... groups) throws IllegalArgumentException {
    if (groups != null) {
      for (String group : groups) {
        if (group != null) {
          deactivate(group);
        }
      }
    }
    return this;
  }

  /**
   * Empty when there are no activations and all groups are empty.
   *
   * @see  Group#isEmpty()
   */
  public boolean isEmpty() {
    if (!activations.isEmpty()) {
      return false;
    }
    for (Group group : groups.values()) {
      if (!group.isEmpty()) {
        return false;
      }
    }
    return true;
  }
}
