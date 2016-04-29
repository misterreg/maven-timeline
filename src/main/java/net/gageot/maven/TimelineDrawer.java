package net.gageot.maven;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.RepaintManager;
import javax.swing.SwingConstants;
import javax.swing.plaf.ScrollPaneUI;
import javax.xml.transform.TransformerConfigurationException;

import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.maven.execution.BuildFailure;
import org.apache.maven.execution.BuildSuccess;
import org.apache.maven.execution.BuildSummary;
import org.apache.maven.execution.MavenExecutionResult;
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
import org.jgrapht.ext.JGraphXAdapter;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.ListenableDirectedGraph;
import org.jgrapht.graph.ListenableDirectedWeightedGraph;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.mxgraph.canvas.mxSvgCanvas;
import com.mxgraph.layout.mxGraphLayout;
import com.mxgraph.layout.mxIGraphLayout;
import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.model.mxICell;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.swing.mxGraphComponent.mxGraphControl;
import com.mxgraph.util.mxCellRenderer;
import com.mxgraph.util.png.mxPngEncodeParam;
import com.mxgraph.util.png.mxPngImageEncoder;
import com.mxgraph.view.mxGraph;

import net.gageot.maven.buildevents.BuildEventListener;
import net.gageot.maven.buildevents.ProjectBuildInfo;

public class TimelineDrawer {
  private static final String OUTPUT_FILE = "measures.output.file";
  private static final String DEFAULT_FILE_DESTINATION = "target/buildgraph.png";

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
  private final static int ROW_HEIGHT = 16;
  private final static int COLUMN_WIDTH = 70;
  private final static int JOB_HEIGHT = 14;
  private final static int JOB_WIDTH = 50;
  
  public void report(Map<String, ProjectBuildInfo> buildInfo, MavenSession session) throws Exception {
    
    LOG.info("START LOGGING");
    Long startTime = getProjectStartTime(buildInfo.values());
    DirectedGraph<String, MyEdge> g = new DefaultDirectedGraph<String, MyEdge>(MyEdge.class);
    
    for (MavenProject prj : session.getProjects()) {
      g.addVertex(ProjectBuildInfo.getProjectKey(prj));
    }
    for (MavenProject prj : session.getProjects()) {
      for (Dependency d : prj.getDependencies()) {
        g.addEdge(ProjectBuildInfo.getDependencyKey(d), ProjectBuildInfo.getProjectKey(prj));
      }
    }
    int y = 0;
    //JGraphModelAdapter<String, DefaultEdge> mAdapter = new JGraphModelAdapter<String, DefaultEdge>(g);
    JGraphXAdapter<String, MyEdge> xAdapter = new JGraphXAdapter<String, MyEdge>(g);
    mxIGraphLayout layout = new mxHierarchicalLayout(xAdapter, SwingConstants.WEST);
    layout.execute(xAdapter.getDefaultParent());
    mxGraphComponent gComponent = new mxGraphComponent(xAdapter);
    
    HashMap<String, mxICell> vertexToCellMap = xAdapter.getVertexToCellMap();
    for (mxICell cell : vertexToCellMap.values()) {
      markCell(xAdapter, cell, 0);
    }
    MavenExecutionResult er = session.getResult();
    for (MavenProject prj : session.getProjects()) {
      mxICell cell = vertexToCellMap.get(ProjectBuildInfo.getProjectKey(prj));
      BuildSummary sum = er.getBuildSummary(prj);
      if (sum != null) {
        if (sum instanceof BuildSuccess) {
          markCell(xAdapter, cell, 1);
        } else if (sum instanceof BuildFailure) {
          markCell(xAdapter, cell, 2);
        }/* else {
          LOG.info(sum.getClass().getName() + " " + sum);
        }*/
      }
    }
    /*
    for (MavenProject prj : buildInfo.keySet()) {
      ProjectBuildInfo pbInfo = buildInfo.get(prj);
      setRectangle(mAdapter, prj.getArtifactId(),
          (int) (pbInfo.getStartTime() - startTime),
          (int) (y * ROW_HEIGHT),
          (int) (pbInfo.getEndTime() - pbInfo.getStartTime()),
          (int) ROW_HEIGHT);
    }
    */
    //JGraph jg = new JGraph(mAdapter);
    
    writePNGFile(gComponent, getFile(session));
  }
  
  private void markCell(JGraphXAdapter xAdapter, mxICell cell, int status) {
    if (status == 0) {
      xAdapter.setCellStyle("fillColor=#DDDDDD", new mxICell[]{cell});
    } else if (status == 1) {
      xAdapter.setCellStyle("fillColor=#DDFFDD", new mxICell[]{cell});
    } else if (status == 2) {
      xAdapter.setCellStyle("fillColor=#FFDDDD", new mxICell[]{cell});
    }
  }
  
  /*
  private void setRectangle (JGraphModelAdapter<String, DefaultEdge> mAdapter, String vertex, int x, int y, int w, int h) throws Exception {
    DefaultGraphCell cell = mAdapter.getVertexCell(vertex);
    if (cell == null) {
      throw new Exception("cant find vertex " + vertex);
    }
    Map attr = cell.getAttributes();
    GraphConstants.setBounds(attr, new Rectangle(
        x, y, w, h
        ));
    y ++ ;
    Map cellAttr = new HashMap( );
    cellAttr.put( cell, attr ); 
    mAdapter.edit( cellAttr, null, null, null );
  }
  */
  private void writePNGFile (mxGraphComponent jg, File f) {
    FileOutputStream outputStream = null;
    try {
      BufferedImage image = mxCellRenderer.createBufferedImage(jg.getGraph(), null, 1, Color.WHITE, jg.isAntiAlias(), null, jg.getCanvas());
      mxPngEncodeParam param = mxPngEncodeParam.getDefaultEncodeParam(image);
      //param.setCompressedText(new String[] { "mxGraphModel", erXmlString });

      outputStream = new FileOutputStream(f);
      mxPngImageEncoder encoder = new mxPngImageEncoder(outputStream, param);

      if (image != null) {
          encoder.encode(image);
      }
    /*
      writer = new FileWriter(f);
      //Object[] cells = jg.getRoots();
      Rectangle2D bounds = jg.getBounds();
       
      DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();
      Document document = domImpl.createDocument(null, "svg", null);
       
      SVGGraphics2D svgGraphics = new SVGGraphics2D(document);
       
      svgGraphics.setSVGCanvasSize(new Dimension((int)Math.round(bounds.getWidth()), (int)Math.round(bounds.getHeight())));
       
      //jg.paint(svgGraphics);
      RepaintManager repaintManager = RepaintManager.currentManager(jg);
      repaintManager.setDoubleBufferingEnabled(false);
      
      ScrollPaneUI gui = jg.getUI(); // The magic is those two lines
      BasicGraphUI gui = jg.getUI(); // The magic is those two lines
      gui.drawGraph(svgGraphics, bounds);
       
      svgGraphics.stream(writer, false);
      */
    } catch (IOException e) {
      LOG.info(e.getMessage());
    } finally {
      try {
          outputStream.close();
      } catch (Exception ex) {
      }
    }
  }
}
