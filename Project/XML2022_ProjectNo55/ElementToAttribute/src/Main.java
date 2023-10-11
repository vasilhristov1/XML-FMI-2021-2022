import java.util.*;

import java.io.File;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.io.FileWriter;

public class Main {

    /*
     * slightly modified version of the algorithm to suit the need of the current
     * problem
     */
    private static String longestCommonPrefix(List<String> list) {
        List<String> listCopy = new LinkedList<>(list);

        int size = listCopy.size();

        if (size == 0)
            return "";

        if (size == 1)
            return listCopy.get(0);

        Collections.sort(listCopy);

        int end = Math.min(listCopy.get(0).length(), listCopy.get(size - 1).length());

        int i = 0;
        while (i < end && listCopy.get(0).charAt(i) == listCopy.get(size - 1).charAt(i))
            i++;

        String prefix = listCopy.get(0).substring(0, i);

        /*
         * exclude prefixes if the next letter is not capital
         * ex: {Images, ImageURL} do not share a common prefix by our definition
         */
        for (String s : listCopy) {

            if (s.length() == i || s.charAt(i) > 'Z') {
                return "";
            }
        }

        return prefix;
    }

    /* Extract data from an item node */
    private static Map<String, String> parseItemProps(Node item) {

        NodeList props = item.getChildNodes();
        Map<String, String> map = new LinkedHashMap<String, String>();

        for (int i = 0; i < props.getLength(); i++) {

            if (props.item(i).getNodeType() == Node.ELEMENT_NODE) {
                String element = props.item(i).getNodeName();
                String value = props.item(i).getTextContent();
                map.put(element, value);
            }
        }

        return map;
    }

    /* group elements by common prefixes and store the suffixes in a list */
    private static Map<String, List<String>> groupElements(Map<String, String> map) {

        /* make an array from the keys */
        Set<String> keys = map.keySet();
        String[] array = keys.toArray(new String[keys.size()]);

        Map<String, List<String>> groupedElements = new LinkedHashMap<String, List<String>>();

        /*
         * contains a group of elements with common prefixes from which we'll extract
         * the attributes
         */
        List<String> attributes = new LinkedList<String>();

        String lcp = ""; /* longest common prefix */

        int end = 0; /*
                      * marks where the loop appends the last element, the last will be added
                      * manually
                      */

        for (int i = 0; i < array.length; i++) {

            attributes.add(array[i]);

            String prefix = longestCommonPrefix(attributes); /* current lcp */

            lcp = prefix != "" ? prefix : lcp;

            if (prefix == "") {

                attributes.remove(attributes.size() - 1);
                i--; /* should be considered for the next iteration */

                if (attributes.size() == 1) {
                    /*
                     * list contained only a single element so we'll map an empty list to the prefix
                     */
                    groupedElements.put(lcp, new LinkedList<String>());
                } else {
                    /* make a list out f the suffixes and map them to the prefix */
                    List<String> attributes_cpy = new LinkedList<String>(attributes);
                    for (final ListIterator<String> list_itr = attributes_cpy.listIterator(); list_itr.hasNext();) {
                        final String element = list_itr.next();
                        list_itr.set(element.substring(lcp.length()));
                    }
                    groupedElements.put(lcp, new LinkedList<String>(attributes_cpy));
                }

                end = i;
                attributes.clear();
            }
        }

        /* add the last element manually */
        if (array.length - end - 1 == 1) {
            groupedElements.put(lcp, new LinkedList<String>());
        } else {
            List<String> temp = new LinkedList<String>();
            for (int i = end + 1; i < array.length; i++) {
                temp.add(array[i].substring(lcp.length()));
            }
            groupedElements.put(lcp, new LinkedList<String>(temp));
        }

        return groupedElements;
    }

    /* combine the two structures into another which would be easier to work with */
    private static Map<String, Map<String, String>> combineData(Map<String, String> parsedItems,
            Map<String, List<String>> groupedItems) {
        Map<String, Map<String, String>> combinedData = new LinkedHashMap<String, Map<String, String>>();

        Iterator<Map.Entry<String, String>> itrParsedItems = parsedItems.entrySet().iterator();
        Iterator<Map.Entry<String, List<String>>> itrGroupedItems = groupedItems.entrySet().iterator();

        Map.Entry<String, String> entryParsedItems = itrParsedItems.next();
        Map.Entry<String, List<String>> entryGroupedItems = itrGroupedItems.next();
        Map<String, String> temp = new LinkedHashMap<String, String>();

        int j = 0;

        for (int i = 0; i < parsedItems.size(); i++) {

            if (entryGroupedItems.getValue().isEmpty()) {

                temp.put("", entryParsedItems.getValue());
                combinedData.put(entryGroupedItems.getKey(), new LinkedHashMap<String, String>(temp));
                temp.clear();
                if (i != parsedItems.size() - 1)
                    entryGroupedItems = itrGroupedItems.next();
            } else {
                temp.put(entryGroupedItems.getValue().get(j++), entryParsedItems.getValue());
                if (j == entryGroupedItems.getValue().size()) {
                    combinedData.put(entryGroupedItems.getKey(), new LinkedHashMap<String, String>(temp));
                    j = 0;
                    if (i != parsedItems.size() - 1)
                        entryGroupedItems = itrGroupedItems.next();
                    temp.clear();
                }
            }

            if (i != parsedItems.size() - 1)
                entryParsedItems = itrParsedItems.next();
        }

        return combinedData;
    }

    private static void removeAllChildNodes(Node node) {
        for (Node child; (child = node.getFirstChild()) != null; node.removeChild(child))
            ;
    }

    private static List<Map<String, Map<String, String>>> parseItemsData(Document doc) {

        List<Map<String, Map<String, String>>> data = new LinkedList<Map<String, Map<String, String>>>();

        int itemsCounts = doc.getElementsByTagName("Item").getLength();

        for (int i = 0; i < itemsCounts; i++) {
            Node itemNode = doc.getElementsByTagName("Item").item(i);
            Map<String, String> parsedItems = parseItemProps(itemNode);
            Map<String, List<String>> groupedItems = groupElements(parsedItems);
            Map<String, Map<String, String>> combinedItems = combineData(parsedItems, groupedItems);
            System.out.println(combinedItems);
            System.out.println("\n");
            data.add(combinedItems);
        }

        return data;
    }

    private static List<Map<String, String>> parseItemsDataSimple(Document doc) {

        List<Map<String, String>> data = new LinkedList<Map<String, String>>();

        int itemsCounts = doc.getElementsByTagName("Item").getLength();

        for (int i = 0; i < itemsCounts; i++) {
            Node itemNode = doc.getElementsByTagName("Item").item(i);
            Map<String, String> parsedItems = parseItemProps(itemNode);
            // System.out.println(parsedItems);
            // System.out.println("\n");
            data.add(parsedItems);
        }

        return data;
    }

    private static void pureElementToAttributeBased(Document doc,
            List<Map<String, Map<String, String>>> parsedItemsData) {

        NodeList rootChildren = doc.getDocumentElement().getChildNodes();

        for (int i = 0; i < rootChildren.getLength(); i++) {

            if (rootChildren.item(i).getNodeType() == Node.ELEMENT_NODE
                    && rootChildren.item(i).getNodeName() == "Items") {
                removeAllChildNodes(rootChildren.item(i));

                for (ListIterator<Map<String, Map<String, String>>> iter = parsedItemsData.listIterator(); iter
                        .hasNext();) {
                    Map<String, Map<String, String>> itemData = iter.next();

                    final Element item = (Element) doc.createElement("Item");

                    Iterator<Map.Entry<String, Map<String, String>>> itr = itemData.entrySet().iterator();

                    while (itr.hasNext()) {
                        Map.Entry<String, Map<String, String>> entry = itr.next();
                        final Element prop = (Element) doc.createElement(entry.getKey());

                        Iterator<Map.Entry<String, String>> itr_2 = entry.getValue().entrySet().iterator();
                        Map.Entry<String, String> entry_2 = itr_2.next();

                        if (entry.getValue().size() == 1) {
                            prop.setTextContent(entry_2.getValue());
                        } else {
                            for (int j = 0; j < entry.getValue().size(); j++) {
                                prop.setAttribute(entry_2.getKey(), entry_2.getValue());
                                if (j != entry.getValue().size() - 1)
                                    entry_2 = itr_2.next();
                            }
                        }

                        item.appendChild(prop);
                    }

                    rootChildren.item(i).appendChild(item);
                }
            }
        }
    }

    private static void pureElementToPureAttribute(Document doc,
            List<Map<String, String>> parsedItemsData) {

        NodeList rootChildren = doc.getDocumentElement().getChildNodes();

        for (int i = 0; i < rootChildren.getLength(); i++) {

            if (rootChildren.item(i).getNodeType() == Node.ELEMENT_NODE
                    && rootChildren.item(i).getNodeName() == "Items") {
                removeAllChildNodes(rootChildren.item(i));

                for (ListIterator<Map<String, String>> iter = parsedItemsData.listIterator(); iter
                        .hasNext();) {
                    Map<String, String> itemData = iter.next();
                    final Element item = (Element) doc.createElement("Item");

                    Iterator<Map.Entry<String, String>> itr = itemData.entrySet().iterator();

                    while (itr.hasNext()) {
                        Map.Entry<String, String> entry = itr.next();
                        item.setAttribute(entry.getKey(), entry.getValue());
                    }

                    rootChildren.item(i).appendChild(item);
                }
            }
        }
    }

    private static void printXMLToFile(Document doc, String fileName) {

        String path = "../resources/" + fileName + ".xml";

        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            FileWriter writer = new FileWriter(new File(path));
            StreamResult result = new StreamResult(writer);

            DOMSource source = new DOMSource(doc);

            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.transform(source, result);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void main(String[] args) {

        try {
            File inputFile = new File("../resources/XML_Element_Focused.xml");
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();
            System.out.println("Root element :" + doc.getDocumentElement().getNodeName());
            System.out.println("----------------------------");

            List<Map<String, Map<String, String>>> parsedData1 = parseItemsData(doc);
            List<Map<String, String>> parsedData2 = parseItemsDataSimple(doc);

            pureElementToAttributeBased(doc, parsedData1);

            printXMLToFile(doc, "pure_element_to_attribute");

            pureElementToPureAttribute(doc, parsedData2);
            printXMLToFile(doc, "pure_element_to_attribute_simple");

        } catch (

        Exception e) {
            e.printStackTrace();
        }
    }
}
