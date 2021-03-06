/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.api.batch.bootstrap;

import org.sonar.api.BatchExtension;
import org.sonar.api.batch.InstantiationStrategy;

/**
 * This extension point allows to change project structure at runtime. It is executed once during task startup.
 * Some use-cases :
 * <ul>
 *   <li>Add sub-projects which are not defined in batch bootstrapper. For example the C# plugin gets the hierarchy
 *   of sub-projects from the Visual Studio metadata file. The single root pom.xml does not contain any declarations of
 *   modules</li>
 *   <li>Change project metadata like description or source directories.</li>
 * </ul>
 *
 * @since 2.9
 */
@InstantiationStrategy(InstantiationStrategy.PER_BATCH)
public abstract class ProjectBuilder implements BatchExtension {

  /**
   * Don't inject ProjectReactor as it may not be available
   * @deprecated since 3.7 use {@link #ProjectBuilder()}
   */
  @Deprecated
  protected ProjectBuilder(final ProjectReactor reactor) {
  }

  /**
   * @since 3.7
   */
  protected ProjectBuilder() {
  }

  /**
   * This method was introduced to relax visibility of {@link #build(ProjectReactor)}
   * @since 3.7
   */
  public final void doBuild(ProjectReactor reactor) {
    build(reactor);
  }

  /**
   * This method will be called by Sonar core to let you a chance to change project reactor structure.
   * @param reactor
   */
  protected abstract void build(ProjectReactor reactor);
}
