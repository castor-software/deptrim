package se.kth.deptrim.util;

import com.google.common.collect.Sets;
import java.io.File;
import java.io.IOException;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import se.kth.deptrim.core.SpecializedDependency;

/**
 * Utility class for manipulating Maven pom.xml files.
 */
@Slf4j
public class PomUtils {

  Set<SpecializedDependency> specializedDependencies;
  String debloatedPomPath;

  boolean createPomSpecialized;
  boolean createAllPomSpecialized;

  /**
   * Constructor.
   *
   * @param specializedDependencies The set of trimmed dependencies.
   * @param debloatedPomPath        The path to the debloated pom.xml file.
   * @param createAllPomSpecialized Whether to create all pom.xml files trimmed.
   */
  public PomUtils(Set<SpecializedDependency> specializedDependencies, String debloatedPomPath, boolean createPomSpecialized, boolean createAllPomSpecialized) {
    this.specializedDependencies = specializedDependencies;
    this.debloatedPomPath = debloatedPomPath;
    this.createPomSpecialized = createPomSpecialized;
    this.createAllPomSpecialized = createAllPomSpecialized;
  }

  /**
   * This method produces a new pom file for each combination of trimmed dependencies.
   */
  public void createPoms() {
    if (createAllPomSpecialized) {
      createAllCombinationsOfSpecializedPoms();
    }
    if (createPomSpecialized) {
      createSinglePomSpecialized();
    }
  }

  private void createSinglePomSpecialized() {
    try {
      String generatedPomFile = createSpecializedPomFromDebloatedPom(specializedDependencies, 1);
      log.info("Created " + new File(generatedPomFile).getName());
    } catch (Exception e) {
      log.error("Error creating specialized pom file: " + e.getMessage());
    }
  }

  private void createAllCombinationsOfSpecializedPoms() {
    Set<Set<SpecializedDependency>> allCombinationsOfSpecializedDependencies = Sets.powerSet(specializedDependencies);
    log.info("Number of specialized poms: " + allCombinationsOfSpecializedDependencies.size());
    int combinationNumber = 1;
    for (Set<SpecializedDependency> oneCombinationOfSpecializedDependencies : allCombinationsOfSpecializedDependencies) {
      // Producing POM for combination.
      // oneCombinationOfSpecializedDependencies.forEach(c -> log.info(c.toString()));
      try {
        String generatedPomFile = createSpecializedPomFromDebloatedPom(oneCombinationOfSpecializedDependencies, combinationNumber);
        log.info("Created " + new File(generatedPomFile).getName());
        combinationNumber++;
      } catch (Exception e) {
        log.error("Error creating specialized POM.");
      }
    }
  }

  /**
   * Creates a pom-specialized.xml from the pom-debloated.xml produced by DepClean.
   *
   * @param oneCombinationOfSpecializedDependencies A combination of trimmed dependencies
   * @return The path of the generated pom-debloated-spl.xml file
   * @throws Exception when any of this goes wrong :)
   */
  private String createSpecializedPomFromDebloatedPom(Set<SpecializedDependency> oneCombinationOfSpecializedDependencies, Integer combinationNumber)
      throws TransformerException, ParserConfigurationException, IOException, SAXException {
    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
    documentBuilderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
    DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
    Document document = documentBuilder.parse(new File(debloatedPomPath));
    document.getDocumentElement().normalize();
    int nbSpecializedDependencies = oneCombinationOfSpecializedDependencies.size();
    NodeList dependencies = document.getDocumentElement().getElementsByTagName("dependency");
    for (SpecializedDependency thisDependency : oneCombinationOfSpecializedDependencies) {
      for (int i = 0; i < dependencies.getLength(); i++) {
        Element dependencyNode = (Element) dependencies.item(i);
        Node groupIdNode = dependencyNode.getElementsByTagName("groupId").item(0);
        Node artifactIdNode = dependencyNode.getElementsByTagName("artifactId").item(0);
        // When original groupId and artifactId are found in debloated pom,
        // replace with new coordinates
        if (groupIdNode.getTextContent().equals(thisDependency.getOriginalGroupId())
            && artifactIdNode.getTextContent().equals(thisDependency.getOriginalArtifactId())
        ) {
          // Found original dependency in debloated POM.
          // Replacing with specialized dependency.
          Node versionNode = dependencyNode.getElementsByTagName("version").item(0);
          groupIdNode.setTextContent(thisDependency.getSpecializedGroupId());
          artifactIdNode.setTextContent(thisDependency.getSpecializedArtifactId());
          versionNode.setTextContent(thisDependency.getSpecializedVersion());
        }
      }
    }
    String debloatedAndSpecializedPom = debloatedPomPath.replace("-debloated.xml", "-specialized.xml");
    if (createAllPomSpecialized) {
      debloatedAndSpecializedPom = debloatedAndSpecializedPom.replace(
          "-specialized.xml",
          "-specialized_" + combinationNumber + "_" + nbSpecializedDependencies + "_" + specializedDependencies.size() + ".xml"
      );
      System.out.println("POM number: " + " " + debloatedAndSpecializedPom + " " + combinationNumber + " " + nbSpecializedDependencies + " " + specializedDependencies.size());
    }
    saveUpdatedDomInANewPom(document, debloatedAndSpecializedPom);
    return debloatedAndSpecializedPom;
  }

  /**
   * Generates a new XML file based with the changes to the XML document.
   *
   * @param document                The XML document structure to save to file
   * @param debloatedSpecializedPom The path to the pom-debloated-spl.xml file to generate
   * @throws TransformerException If XML document cannot be saved to file
   */
  private void saveUpdatedDomInANewPom(Document document, String debloatedSpecializedPom) throws TransformerException {
    DOMSource dom = new DOMSource(document);
    TransformerFactory transformerFactory = TransformerFactory.newInstance();
    transformerFactory.setAttribute("http://javax.xml.XMLConstants/property/accessExternalDTD", "");
    transformerFactory.setAttribute("http://javax.xml.XMLConstants/property/accessExternalStylesheet", "");
    Transformer transformer = transformerFactory.newTransformer();
    StreamResult result = new StreamResult(new File(debloatedSpecializedPom));
    transformer.transform(dom, result);
  }
}
