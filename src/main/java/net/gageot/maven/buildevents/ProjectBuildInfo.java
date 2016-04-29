package net.gageot.maven.buildevents;

import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;

public class ProjectBuildInfo {
  private Long startTime;
  private Long endTime;
  private int success = 0;
  public int getSuccess() {
    return success;
  }
  public void setSuccess(int success) {
    this.success = success;
  }
  public Long getStartTime() {
    return startTime;
  }
  public void setStartTime(Long startTime) {
    this.startTime = startTime;
  }
  public Long getEndTime() {
    return endTime;
  }
  public void setEndTime(Long endTime) {
    this.endTime = endTime;
  }
  
  public static String getProjectKey (MavenProject prj) {
    return prj.getArtifactId();
  }
  public static String getDependencyKey(Dependency d) {
    return d.getArtifactId();
  }
}
