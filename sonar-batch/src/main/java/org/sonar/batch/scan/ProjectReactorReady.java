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
package org.sonar.batch.scan;

import org.sonar.api.batch.bootstrap.ProjectBuilder;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.config.Settings;

import javax.annotation.Nullable;

/**
 * Barrier to control the project lifecycle :
 * <p/>
 * <ul>
 * <li>initialize the project configuration by executing ProjectBuilder extensions</li>
 * <li>apply sub-project exclusions (sonar.skippedModules, ...)</li>
 * <li>---- this barrier ----</li>
 * <li>run optional dry run database</li>
 * <li>connect to dry-run or remote database</li>
 * </ul>
 */
public class ProjectReactorReady {

  private final ProjectReactor reactor;
  private final Settings settings;
  private ProjectBuilder[] projectBuilders;

  public ProjectReactorReady(ProjectExclusions exclusions, ProjectReactor reactor, Settings settings, @Nullable ProjectBuilder[] projectBuilders) {
    this.reactor = reactor;
    this.settings = settings;
    this.projectBuilders = projectBuilders;
  }

  public ProjectReactorReady(ProjectExclusions exclusions, ProjectReactor reactor, Settings settings) {
    this(exclusions, reactor, settings, new ProjectBuilder[0]);
  }

  public void start() {
    for (ProjectBuilder projectBuilder : projectBuilders) {
      projectBuilder.doBuild(reactor);
    }
    ProjectReactorValidator validator = new ProjectReactorValidator(settings);
    validator.validate(reactor);
  }
}
