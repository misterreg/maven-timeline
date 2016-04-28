package net.gageot.maven;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.RepaintManager;
import javax.xml.transform.TransformerConfigurationException;

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.project.MavenProject;
import org.jgraph.JGraph;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.DefaultGraphModel;
import org.jgraph.graph.GraphConstants;
import org.jgraph.plaf.basic.BasicGraphUI;
import org.jgrapht.DirectedGraph;
import org.jgrapht.ext.GraphMLExporter;
import org.jgrapht.ext.JGraphModelAdapter;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import net.gageot.maven.buildevents.BuildEventListener;
import net.gageot.maven.buildevents.ProjectBuildInfo;

public class TimelineDrawer {
  private static final String OUTPUT_FILE = "measures.output.file";
  private static final String DEFAULT_FILE_DESTINATION = "target/buildgraph.svg";

  /*void print () {
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
  }*/
  
  private File getFile(MavenSession session) throws IOException {
    String path = session.getUserProperties().getProperty(OUTPUT_FILE, DEFAULT_FILE_DESTINATION);
    if (new File(path).isAbsolute()) {
      return new File(path);
    }
    String buildDir = session.getExecutionRootDirectory();

    File result = new File(buildDir, path); 
    
    File dir = result.getParentFile();
    if (!dir.exists()) {
      if (!dir.mkdirs()) {
        throw new IOException("Unable to create " + dir);
      }
    }
    
    return result;
  }

  private Long getProjectStartTime(Collection<ProjectBuildInfo> buildInfo) {
    Long result = 0l;
    for (ProjectBuildInfo pbInfo : buildInfo) {
      if (result == 0l || pbInfo.getStartTime() < result) {
        result = pbInfo.getStartTime();
      }
    }
    return result;
  }
  
  private Logger LOG = Logger.getLogger(TimelineDrawer.class.getName());
  private final static long rowHeight = 100l;
  
  public void report(Map<MavenProject, ProjectBuildInfo> buildInfo, List<MavenProject> projectList, MavenSession session) throws Exception {
    LOG.info("START LOGGING");
    Long startTime = getProjectStartTime(buildInfo.values());
    DirectedGraph<String, DefaultEdge> g = new DefaultDirectedGraph<String, DefaultEdge>(DefaultEdge.class);
    
    for (MavenProject prj : projectList) {
      LOG.info("project from reactor: " + prj.getArtifactId());;
      g.addVertex(prj.getArtifactId());
    }
    for (MavenProject prj : projectList) {
      for (Dependency d : prj.getDependencies()) {
        g.addEdge(prj.getArtifactId(), d.getArtifactId());
      }
    }
    int y = 0;
    JGraphModelAdapter<String, DefaultEdge> mAdapter = new JGraphModelAdapter<String, DefaultEdge>(g);
    for (MavenProject prj : buildInfo.keySet()) {
      ProjectBuildInfo pbInfo = buildInfo.get(prj);
      DefaultGraphCell cell = mAdapter.getVertexCell(prj.getArtifactId());
      if (cell == null) {
        throw new Exception("cant find vertex " + prj.getArtifactId());
      }
      Map attr = cell.getAttributes();
      GraphConstants.setBounds(attr, new Rectangle(
          (int) (pbInfo.getStartTime() - startTime),
          (int) (y * rowHeight),
          (int) (pbInfo.getEndTime() - pbInfo.getStartTime()),
          (int) rowHeight
          ));
      y ++ ;
      Map cellAttr = new HashMap( );
      cellAttr.put( cell, attr ); 
      mAdapter.edit( cellAttr, null, null, null );
    }

    JGraph jg = new JGraph(mAdapter);
    writeSVGFile(jg, getFile(session));
  }
  
  private void writeSVGFile (JGraph jg, File f) {
    FileWriter writer = null;
    try {
      writer = new FileWriter(f);
    
      Object[] cells = jg.getRoots();
      Rectangle2D bounds = jg.toScreen(jg.getCellBounds(cells));
       
      DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();
      Document document = domImpl.createDocument(null, "svg", null);
       
      SVGGraphics2D svgGraphics = new SVGGraphics2D(document);
       
      svgGraphics.setSVGCanvasSize(new Dimension((int)Math.round(bounds.getWidth()), (int)Math.round(bounds.getHeight())));
       
      RepaintManager repaintManager = RepaintManager.currentManager(jg);
      repaintManager.setDoubleBufferingEnabled(false);
       
      BasicGraphUI gui = (BasicGraphUI) jg.getUI(); // The magic is those two lines
      gui.drawGraph(svgGraphics, bounds);
       
      svgGraphics.stream(writer, false);
    } catch (IOException e) {
      LOG.info(e.getMessage());
    } finally {
      try {
          writer.close();
      } catch (Exception ex) {
      }
    }
  }
}
