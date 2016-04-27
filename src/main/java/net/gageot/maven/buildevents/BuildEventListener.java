/**
 * Copyright (C) 2013 david@gageot.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */
package net.gageot.maven.buildevents;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.maven.execution.*;
import org.apache.maven.plugin.*;

import com.github.jknack.handlebars.*;
import com.github.jknack.handlebars.context.*;

import net.gageot.maven.TimelineDrawer;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

public class BuildEventListener extends AbstractExecutionListener {
  private final Map<MavenProject, ProjectBuildInfo> buildInfo =
      new ConcurrentHashMap<MavenProject, ProjectBuildInfo>();
  private TimelineDrawer tl;
  private Logger LOG = Logger.getLogger(BuildEventListener.class.getName());

  public BuildEventListener(TimelineDrawer tl) {
    this.tl = tl;
  }

  private void ensureInfoExists (MavenProject project) {
    if (!buildInfo.containsKey(project)) {
      buildInfo.put(project, new ProjectBuildInfo());
    }
  }
  
  @Override
  public void projectStarted(ExecutionEvent event) {
    ensureInfoExists(event.getProject());
    buildInfo.get(event.getProject()).setStartTime(System.currentTimeMillis());
    LOG.info("project started " + event.getProject().getArtifactId());;
  }
  
  @Override
  public void projectFailed(ExecutionEvent event) {
    ensureInfoExists(event.getProject());
    buildInfo.get(event.getProject()).setEndTime(System.currentTimeMillis());
    buildInfo.get(event.getProject()).setSuccess(false);
    LOG.info("project failed " + event.getProject().getArtifactId());;
  }

  @Override
  public void projectSucceeded(ExecutionEvent event) {
    ensureInfoExists(event.getProject());
    buildInfo.get(event.getProject()).setEndTime(System.currentTimeMillis());
    buildInfo.get(event.getProject()).setSuccess(true);
    LOG.info("project succeeded " + event.getProject().getArtifactId());;
  }

  @Override
  public void sessionEnded(ExecutionEvent event) {
    LOG.info("session ended");
    tl.report(buildInfo, event.getSession().getProjects(), event.getSession());
  }

}