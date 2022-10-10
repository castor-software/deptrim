package se.kth.deptrim.mojo;

import java.util.Set;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import se.kth.depclean.core.DepCleanManager;
import se.kth.depclean.core.analysis.AnalysisFailureException;
import se.kth.depclean.wrapper.MavenDependencyManager;

@Mojo(name = "depclean", defaultPhase = LifecyclePhase.PACKAGE,
    requiresDependencyCollection = ResolutionScope.TEST,
    requiresDependencyResolution = ResolutionScope.TEST, threadSafe = true)
@Slf4j
public class DepTrimMojo extends AbstractMojo {

  /**
   * The Maven project to analyze.
   */
  @Parameter(defaultValue = "${project}", readonly = true)
  private MavenProject project;

  /**
   * The Maven session to analyze.
   */
  @Parameter(defaultValue = "${session}", readonly = true)
  private MavenSession session;

  /**
   * If this is true, DepClean creates a debloated version of the pom without unused dependencies, called
   * "debloated-pom.xml", in root of the project.
   */
  @Parameter(property = "createPomDebloated", defaultValue = "false")
  private boolean createPomDebloated;

  /**
   * If this is true, DepClean creates a JSON file with the result of the analysis. The file is called
   * "debloat-result.json" and it is located in /target.
   */
  @Parameter(property = "createResultJson", defaultValue = "false")
  private boolean createResultJson;


  /**
   * If this is true, DepClean creates a CSV file with the result of the analysis with the columns:
   * OriginClass,TargetClass,OriginDependency,TargetDependency. The file is called "depclean-callgraph.csv" and it is located in /target.
   */
  @Parameter(property = "createCallGraphCsv", defaultValue = "false")
  private boolean createCallGraphCsv;

  /**
   * Add a list of dependencies, identified by their coordinates, to be ignored by DepClean during the analysis and
   * considered as used dependencies. Useful to override incomplete result caused by bytecode-level analysis Dependency
   * format is <code>groupId:artifactId:version</code>.
   */
  @Parameter(property = "ignoreDependencies")
  private Set<String> ignoreDependencies;

  /**
   * Ignore dependencies with specific scopes from the DepClean analysis.
   */
  @Parameter(property = "ignoreScopes")
  private Set<String> ignoreScopes;

  /**
   * If this is true, DepClean will not analyze the test sources in the project, and, therefore, the dependencies that
   * are only used for testing will be considered unused. This property is useful to detect dependencies that have a
   * compile scope but are only used during testing. Hence, these dependencies should have a test scope.
   */
  @Parameter(property = "ignoreTests", defaultValue = "false")
  private boolean ignoreTests;

  /**
   * If this is true, and DepClean reported any unused direct dependency in the dependency tree, then the project's
   * build fails immediately after running DepClean.
   */
  @Parameter(property = "failIfUnusedDirect", defaultValue = "false")
  private boolean failIfUnusedDirect;

  /**
   * If this is true, and DepClean reported any unused transitive dependency in the dependency tree, then the project's
   * build fails immediately after running DepClean.
   */
  @Parameter(property = "failIfUnusedTransitive", defaultValue = "false")
  private boolean failIfUnusedTransitive;

  /**
   * If this is true, and DepClean reported any unused inherited dependency in the dependency tree, then the project's
   * build fails immediately after running DepClean.
   */
  @Parameter(property = "failIfUnusedInherited", defaultValue = "false")
  private boolean failIfUnusedInherited;

  /**
   * Skip plugin execution completely.
   */
  @Parameter(property = "skipDepClean", defaultValue = "false")
  private boolean skipDepClean;

  /**
   * To build the dependency graph.
   */
  @Component(hint = "default")
  private DependencyGraphBuilder dependencyGraphBuilder;

  @SneakyThrows
  @Override
  public final void execute() {
    try {
      new DepCleanManager(
          new MavenDependencyManager(
              getLog(),
              project,
              session,
              dependencyGraphBuilder
          ),
          skipDepClean,
          ignoreTests,
          ignoreScopes,
          ignoreDependencies,
          failIfUnusedDirect,
          failIfUnusedTransitive,
          failIfUnusedInherited,
          createPomDebloated,
          createResultJson,
          createCallGraphCsv
      ).execute();
    } catch (AnalysisFailureException e) {
      throw new MojoExecutionException(e.getMessage(), e);
    }
  }
}