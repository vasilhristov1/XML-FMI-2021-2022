import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.PrintStream;

public class AttributesToElements {
    private static boolean skipNL; // used to check if a node list should be skipped
    private static int count = 0; // it is used to check if it is the first usage of the printXML function

    // main function
    public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
        // creating a document builder
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setValidating(false);
        DocumentBuilder builder = dbf.newDocumentBuilder();
        // creating a document from a file
        InputSource source = new InputSource("./resources/XML_Attr_Focused.xml");
        Document doc = builder.parse(source);
        System.out.println(printXML(doc.getDocumentElement()));

        // here we write the output in a xml file
        PrintStream fileOut = new PrintStream("./XML_Element_Focused_result.xml");
        System.setOut(fileOut);
        System.out.println(printXML(doc.getDocumentElement()));
    }

    private static String printXML(Node rootNode) {
        String tab = "";
        skipNL = false;

        return printXML(rootNode, tab);
    }

    // function to print the XML tree
    private static String printXML(Node rootNode, String tab) {
        StringBuilder print = new StringBuilder(); // the string that have to be printed

        // checks if it is the first usage of the printXML function
        if (count == 0) {
            print.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"); // adding the beginning of the xml file to the output string
            count++; // increases the count variable
        }

        if (rootNode.getNodeType() == Node.ELEMENT_NODE) {
            print.append("<").append(rootNode.getNodeName()); // making the start tag of the current element
            print.append(">");

            NamedNodeMap attributes = rootNode.getAttributes(); // list of the attributes

            // adding each attribute to the starting tag of the element
            for (int i = 0; i < attributes.getLength(); i++) {
                Attr attr = (Attr) attributes.item(i);

                // if the attribute is not null it is being added to the print string
                if (attr != null) {
                    print.append("\n").append(tab).append("\t").append("<").append(attr.getNodeName()).append(">");
                    print.append(attr.getNodeValue());
                    print.append("</").append(attr.getNodeName()).append(">");
                }
            }
        }

        NodeList nl = rootNode.getChildNodes(); // list with children of the current element

        // each child of the root is being added to the tree
        // id there are any children of the current element, for each child we do the printXML function
        if (nl.getLength() > 0) {
            for (int i = 0; i < nl.getLength(); ++i) {
                print.append(printXML(nl.item(i), tab + '\t'));
            }
        }
        // if there are no children in the node list
        else {
            if (rootNode.getNodeValue() != null) {
                print = new StringBuilder(rootNode.getNodeValue());
            }

            skipNL = true; // the node list can be skipped because there are no children in it
        }

        // making the end tag for the current element
        if (rootNode.getNodeType() == Node.ELEMENT_NODE) {
            // adding the end tag of the current element to the output
            print.append("</").append(rootNode.getNodeName()).append(">");
            if (!skipNL) {
                print.append("\n").append(tab);
            }

            skipNL = false;
        }



        return (print.toString()); // returning the result in a string format
    }
}
