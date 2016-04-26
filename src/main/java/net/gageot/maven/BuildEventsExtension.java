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
package net.gageot.maven;

import java.io.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.gageot.maven.buildevents.*;

import org.apache.maven.*;
import org.apache.maven.execution.*;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.*;

@Component(role = AbstractMavenLifecycleParticipant.class, hint = "buildevents")
public class BuildEventsExtension extends AbstractMavenLifecycleParticipant {
    private static final String OUTPUT_FILE = "measures.output.file";
    private static final String DEFAULT_FILE_DESTINATION = "target/measures.xml";

    private static final String DEP_OUTPUT_FILE = "dependencies.output.file";
    private static final String DEP_DEFAULT_FILE_DESTINATION = "target/dependencies.xml";
    
    @Override
    public void afterProjectsRead(MavenSession session) {
        MavenExecutionRequest request = session.getRequest();
        
        ExecutionListener original = request.getExecutionListener();
        BuildEventListener listener = new BuildEventListener(logFile(session));
        ExecutionListener chain = new ExecutionListenerChain(original, listener);
        
        request.setExecutionListener(chain);
        
        
        FileWriter writer = null;
        try {
            File path = depFile(session);
          
            File dir = path.getParentFile();
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    throw new IOException("Unable to create " + dir);
                }
            }
            writer = new FileWriter(path);
            
            writer.write("<dependencies>\n");
            for (MavenProject project : session.getProjects()) {
                writer.write("  <project groupId=\"" + project.getGroupId() + "\"" + 
                        " artifactId=\"" + project.getArtifactId() + "\"" + 
                        " version=\"" + project.getVersion() + "\"" + 
                        " name=\"" + project.getName() + "\">\n");
                for (Dependency dependency : project.getDependencies()) {
                    writer.write("    <dependency groupId=\"" + dependency.getGroupId() + "\"" +
                            " artifactId=\"" + dependency.getArtifactId() + "\"" + 
                            " version=\"" + dependency.getVersion() + "\"" + 
                            " scope=\"" + dependency.getScope() + "\"" + 
                            " systemPath=\"" + dependency.getSystemPath() + "\" />\n");
                }
                writer.write("  </project>\n");
            }
            writer.write("</dependencies>\n");
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

  
  
  private File logFile(MavenSession session) {
    String path = session.getUserProperties().getProperty(OUTPUT_FILE, DEFAULT_FILE_DESTINATION);
    if (new File(path).isAbsolute()) {
      return new File(path);
    }

    String buildDir = session.getExecutionRootDirectory();
    return new File(buildDir, path);
  }

  private File depFile(MavenSession session) {
    String path = session.getUserProperties().getProperty(DEP_OUTPUT_FILE, DEP_DEFAULT_FILE_DESTINATION);
    if (new File(path).isAbsolute()) {
      return new File(path);
    }

    String buildDir = session.getExecutionRootDirectory();
    return new File(buildDir, path);
  }
}
