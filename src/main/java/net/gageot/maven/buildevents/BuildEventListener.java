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
import java.util.logging.Level;
import java.util.logging.Logger;

public class BuildEventListener extends AbstractExecutionListener {
  private final File output;
  private final Map<String, Long> startTimes = new ConcurrentHashMap<String, Long>();
  private final Map<String, Long> endTimes = new ConcurrentHashMap<String, Long>();

  public BuildEventListener(File output) {
    this.output = output;
  }

  @Override
  public void mojoStarted(ExecutionEvent event) {
    startTimes.put(key(event), System.currentTimeMillis());
  }

  @Override
  public void mojoSkipped(ExecutionEvent event) {
    mojoEnd(event);
  }

  @Override
  public void mojoSucceeded(ExecutionEvent event) {
    mojoEnd(event);
  }

  @Override
  public void mojoFailed(ExecutionEvent event) {
    mojoEnd(event);
  }

  private void mojoEnd(ExecutionEvent event) {
    endTimes.put(key(event), System.currentTimeMillis());
  }

  @Override
  public void sessionEnded(ExecutionEvent event) {
    report();
  }

  private String key(ExecutionEvent event) {
    MojoExecution mojo = event.getMojoExecution();
    String goal = mojo.getGoal();
    String phase = mojo.getLifecyclePhase();
    String group = event.getProject().getGroupId();
    String project = event.getProject().getArtifactId();
    return group + "/" + project + "/" + phase + "/" + goal;
  }

  public void report() {
      long buildStartTime = Long.MAX_VALUE;
      for (Long start : startTimes.values()) {
          buildStartTime = Math.min(buildStartTime, start);
      }     long buildEndTime = 0;
      for (Long end : endTimes.values()) {
          buildEndTime = Math.max(buildEndTime, end);
      }     List<Measure> measures = new ArrayList<Measure>();
      for (String key : startTimes.keySet()) {
          String[] keyParts = key.split("/");
          
          Measure measure = new Measure();
          measure.group = keyParts[0];
          measure.project = keyParts[1];
          measure.phase = keyParts[2];
          measure.goal = keyParts[3];
          measure.durationSeconds = (endTimes.get(key) - startTimes.get(key)) / 1000L;
          measure.left = ((startTimes.get(key) - buildStartTime) * 10000L) / (buildEndTime - buildStartTime);
          measure.width = (((endTimes.get(key) - buildStartTime) * 10000L) / (buildEndTime - buildStartTime)) - measure.left;
          measures.add(measure);
      }     
      
      Collections.sort(measures);
      FileWriter writer = null;
      try {
          File path = output.getParentFile();
          if (!path.exists()) {
              if (!path.mkdirs()) {
                  throw new IOException("Unable to create " + path);
              }
          }     
          writer = new FileWriter(output);
          writer.write("<measures>");
          for(Measure measure : measures) {
              write(writer, measure);
          }     
          writer.write("</measures>");
          writer.close();
      } catch (IOException ex) {
          Logger.getLogger(BuildEventListener.class.getName()).log(Level.SEVERE, null, ex);
      } finally {
          try {
              writer.close();
          } catch (IOException ex) {
              Logger.getLogger(BuildEventListener.class.getName()).log(Level.SEVERE, null, ex);
          }
      }
  }

  private void write(FileWriter writer, Measure measure) throws IOException {
    writer.write("  <measure");
    writer.write(" group=\"" + measure.group + "\"");
    writer.write(" project=\"" + measure.project + "\"");
    writer.write(" phase=\"" + measure.phase + "\"");
    writer.write(" goal=\"" + measure.goal + "\"");
    writer.write(" durationSeconds=\"" + measure.durationSeconds + "\"");
    writer.write(" left=\"" + measure.left + "\"");
    writer.write(" width=\"" + measure.width + "\"");
    writer.write(" />\n");
  }

  public static class Measure implements Comparable<Measure> {
    String group;
    String project;
    String phase;
    String goal;
    Long durationSeconds;
    Long left;
    Long width;

    @Override
    public int compareTo(Measure other) {
      return left.compareTo(other.left);
    }
  }
}