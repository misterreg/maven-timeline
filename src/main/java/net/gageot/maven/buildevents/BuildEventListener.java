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
  private final Map<String, ProjectBuildInfo> buildInfo =
      new ConcurrentHashMap<String, ProjectBuildInfo>();
  private TimelineDrawer tl;
  private Logger LOG = Logger.getLogger(BuildEventListener.class.getName());

  public BuildEventListener(TimelineDrawer tl) {
    this.tl = tl;
  }

  private void ensureInfoExists (String key) {
    if (!buildInfo.containsKey(key)) {
      buildInfo.put(key, new ProjectBuildInfo());
    }
  }
  
  @Override
  public void projectStarted(ExecutionEvent event) {
    String key = ProjectBuildInfo.getProjectKey(event.getProject());
    ensureInfoExists(key);
    buildInfo.get(key).setStartTime(System.currentTimeMillis());
    LOG.info("project started " + key);;
  }
  
  @Override
  public void projectFailed(ExecutionEvent event) {
    
    String key = ProjectBuildInfo.getProjectKey(event.getProject());
    ensureInfoExists(key);
    buildInfo.get(key).setEndTime(System.currentTimeMillis());
    buildInfo.get(key).setSuccess(2);
    LOG.info("project failed " + key);;
  }

  @Override
  public void projectSucceeded(ExecutionEvent event) {
    String key = ProjectBuildInfo.getProjectKey(event.getProject());
    ensureInfoExists(key);
    buildInfo.get(key).setEndTime(System.currentTimeMillis());
    buildInfo.get(key).setSuccess(1);
    LOG.info("project suceeded " + key);;
  }

  @Override
  public void forkStarted(ExecutionEvent event) {
    String key = ProjectBuildInfo.getProjectKey(event.getProject());
    LOG.info("fork started " + key);;
  }

  @Override
  public void forkFailed(ExecutionEvent event) {
    String key = ProjectBuildInfo.getProjectKey(event.getProject());
    LOG.info("fork failed " + key);;
  }

  @Override
  public void forkSucceeded(ExecutionEvent event) {
    String key = ProjectBuildInfo.getProjectKey(event.getProject());
    LOG.info("fork suceeded " + key);;
  }
  
  @Override
  public void forkedProjectStarted(ExecutionEvent event) {
    String key = ProjectBuildInfo.getProjectKey(event.getProject());
    LOG.info("fork project started " + key);;
  }

  @Override
  public void forkedProjectFailed(ExecutionEvent event) {
    String key = ProjectBuildInfo.getProjectKey(event.getProject());
    LOG.info("fork project failed " + key);;
  }
  
  @Override
  public void forkedProjectSucceeded(ExecutionEvent event) {
    String key = ProjectBuildInfo.getProjectKey(event.getProject());
    LOG.info("fork project succeeded " + key);;
  }
  
  @Override
  public void sessionEnded(ExecutionEvent event) {
    LOG.info("session ended");
    try {
      tl.report(buildInfo, event.getSession());
    } catch (Exception e) {
      LOG.info(e.getMessage());
    }
  }

}