/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.rat.configuration;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Consumer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.beanutils.MethodUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.rat.ConfigurationException;
import org.apache.rat.analysis.IHeaderMatcher;
import org.apache.rat.analysis.matchers.FullTextMatcher;
import org.apache.rat.analysis.matchers.SimpleTextMatcher;
import org.apache.rat.configuration.builders.AbstractBuilder;
import org.apache.rat.configuration.builders.ChildContainerBuilder;
import org.apache.rat.configuration.builders.MatcherRefBuilder;
import org.apache.rat.configuration.builders.TextCaptureBuilder;
import org.apache.rat.license.ILicense;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * A class that reads the XML configuration file format.
 * <p>
 * {@code <rat-config>}<br/>
 * {@code   <licenses>}<br/>
 * {@code     <license id=id name=name >}<br/>
 * {@code       <notes></notes>}<br/>
 * {@code       <text>  </text>}<br/>
 * {@code       <copyright start='' end='' owner=''/>}<br/>
 * {@code       <spdx></spdx> }<br/>
 * {@code       <and> <matcher/>...</and>}<br/>
 * {@code       <or> <matcher/>...</or> }<br/>
 * {@code       <matcher_ref refid='' />}<br/>
 * {@code       <not><matcher /></not>}<br/>
 * {@code     </license>}<br/>
 * {@code   </licenses>}<br/>
 * {@code   <approved>}<br/>
 * {@code     <family refid=''>}<br/>
 * {@code   </approved>}<br/>
 * {@code   <matchers>}<br/>
 * {@code     <matcher className=''/>}<br/>
 * {@code     <matcher className=''/>}<br/>
 * {@code   </matchers>}<br/>
 * {@code </rat-config>}<br/>
 * </p>
 */

public class XMLConfigurationReader implements LicenseReader, MatcherReader {

    private final static String ATT_ID = "id";
    private final static String ATT_NAME = "name";
    private final static String ATT_DERIVED_FROM = "derived_from";
    private final static String ATT_LICENSE_REF = "license_ref";
    private final static String ATT_CLASS_NAME = "class";

    private final static String ROOT = "rat-config";
    private final static String LICENSES = "licenses";
    private final static String LICENSE = "license";
    private final static String APPROVED = "approved";
    private final static String FAMILY = "family";
    private final static String NOTE = "note";
    private final static String MATCHERS = "matchers";
    private final static String MATCHER = "matcher";

    private Document document;
    private final Element rootElement;
    private final Element licensesElement;
    private final Element approvedElement;
    private final Element matchersElement;

    private final SortedSet<ILicense> licenses;
    private final Map<String, IHeaderMatcher> matchers;
    private final SortedSet<String> licenseFamilies;

    /**
     * Constructs the XML configuration read.
     */
    public XMLConfigurationReader() {
        try {
            document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            throw new IllegalStateException("No XML parser defined", e);
        }
        rootElement = document.createElement(ROOT);
        document.appendChild(rootElement);
        licensesElement = document.createElement(LICENSES);
        rootElement.appendChild(licensesElement);
        approvedElement = document.createElement(APPROVED);
        rootElement.appendChild(approvedElement);
        matchersElement = document.createElement(MATCHERS);
        rootElement.appendChild(matchersElement);
        licenses = new TreeSet<>((x, y) -> x.getLicenseFamily().compareTo(y.getLicenseFamily()));
        licenseFamilies = new TreeSet<>();
        matchers = new HashMap<>();
    }

    @Override
    public void addLicenses(URL url) {
        read(url);
    }

    /**
     * Read the urls and create a single document to process.
     * 
     * @param urls The URLs to read.
     */
    public void read(URL... urls) {
        DocumentBuilder builder;
        try {
            builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new ConfigurationException("Unable to create DOM builder", e);
        }
        for (URL url : urls) {
            try {
                add(builder.parse(url.openStream()));
            } catch (SAXException | IOException e) {
                throw new ConfigurationException("Unable to read url: " + url, e);
            }
        }
    }

    /**
     * Applies the {@code consumer} to each node in the {@code list}
     * 
     * @param list the NodeList to process
     * @param consumer the consumer to apply to each node in the list.
     */
    private void nodeListConsumer(NodeList list, Consumer<Node> consumer) {
        for (int i = 0; i < list.getLength(); i++) {
            consumer.accept(list.item(i));
        }
    }

    /**
     * Merge the new document into the document that this reader processes.
     * 
     * @param newDoc the Document to merge.
     */
    public void add(Document newDoc) {
        List<Node> lst = new ArrayList<>();
        nodeListConsumer(newDoc.getElementsByTagName(LICENSE), lst::add);
        nodeListConsumer(newDoc.getElementsByTagName(LICENSE),
                (n) -> licensesElement.appendChild(rootElement.getOwnerDocument().adoptNode(n.cloneNode(true))));
        nodeListConsumer(newDoc.getElementsByTagName(APPROVED),
                (n) -> approvedElement.appendChild(rootElement.getOwnerDocument().adoptNode(n.cloneNode(true))));
        nodeListConsumer(newDoc.getElementsByTagName(MATCHERS),
                (n) -> matchersElement.appendChild(rootElement.getOwnerDocument().adoptNode(n.cloneNode(true))));
    }

    /**
     * Get a map of Node attribute names to values.
     * 
     * @param node The node to process
     * @return the map of attributes on the node
     */
    private Map<String, String> attributes(Node node) {
        NamedNodeMap nnm = node.getAttributes();
        Map<String, String> result = new HashMap<>();
        for (int i = 0; i < nnm.getLength(); i++) {
            Node n = nnm.item(i);
            result.put(n.getNodeName(), n.getNodeValue());
        }
        return result;
    }

    /**
     * Create a text matcher. Will construct a FullTextMatcher or a
     * SimpleTextMatcher depending on the complexity of the text.
     * 
     * @param id the id for the Matcher.
     * @param txt the text to match
     * @return the IHeaderMatcher that matches the text.
     */
    public static IHeaderMatcher createTextMatcher(String id, String txt) {
        boolean complex = txt.contains(" ") | txt.contains("\\t") | txt.contains("\\n") | txt.contains("\\r")
                | txt.contains("\\f") | txt.contains("\\v");
        return complex ? new FullTextMatcher(id, txt) : new SimpleTextMatcher(id, txt);
    }

    private AbstractBuilder parseMatcher(Node matcherNode) {
        AbstractBuilder builder = MatcherBuilderTracker.getMatcherBuilder(matcherNode.getNodeName());

        NamedNodeMap nnm = matcherNode.getAttributes();
        for (int i = 0; i < nnm.getLength(); i++) {
            Node n = nnm.item(i);
            String methodName = "set" + StringUtils.capitalize(n.getNodeName());
            try {
                MethodUtils.invokeExactMethod(builder, methodName, n.getNodeValue());
            } catch (NoSuchMethodException e) {
                throw new ConfigurationException(
                        String.format("'%s' does not have a setter '%s' that takes a String argument",
                                matcherNode.getNodeName(), methodName));
            } catch (IllegalAccessException | InvocationTargetException | DOMException e) {
                throw new ConfigurationException(e);
            }
        }
        if (builder instanceof ChildContainerBuilder) {
            ChildContainerBuilder ccb = (ChildContainerBuilder) builder;
            nodeListConsumer(matcherNode.getChildNodes(), x -> {
                if (x.getNodeType() == Node.ELEMENT_NODE) {
                    ccb.add(parseMatcher(x));
                }
            });
        }
        if (builder instanceof TextCaptureBuilder) {
            ((TextCaptureBuilder) builder).setText(matcherNode.getTextContent().trim());
        }

        if (builder instanceof MatcherRefBuilder) {
            ((MatcherRefBuilder) builder).setMatchers(matchers);
        }

        if (builder.hasId()) {
            builder = new DelegatingBuilder(builder) {
                @Override
                public IHeaderMatcher build() {
                    IHeaderMatcher result = delegate.build();
                    matchers.put(result.getId(), result);
                    return result;
                }
            };
        }
        return builder;
    }

    private ILicense parseLicense(Node licenseNode) {
        Map<String, String> attributes = attributes(licenseNode);
        ILicense.Builder builder = ILicense.builder();

        builder.setLicenseFamilyCategory(attributes.get(ATT_ID));
        builder.setLicenseFamilyName(attributes.get(ATT_NAME));

        StringBuilder notesBuilder = new StringBuilder();
        nodeListConsumer(licenseNode.getChildNodes(), x -> {
            if (x.getNodeType() == Node.ELEMENT_NODE) {
                if (x.getNodeName().equals(NOTE)) {
                    notesBuilder.append(x.getTextContent()).append("\n");
                } else {
                    builder.setMatcher(parseMatcher(x));
                }
            }
        });
        builder.setDerivedFrom(StringUtils.defaultIfBlank(attributes.get(ATT_DERIVED_FROM), null));
        builder.setNotes(StringUtils.defaultIfBlank(notesBuilder.toString().trim(), null));
        return builder.build();
    }

    @Override
    public SortedSet<ILicense> readLicenses() {
        readMatcherBuilders();
        if (licenses.size() == 0) {
            nodeListConsumer(document.getElementsByTagName(LICENSE), x -> licenses.add(parseLicense(x)));
            nodeListConsumer(document.getElementsByTagName(FAMILY), x -> licenseFamilies.add(parseFamily(x)));
            document = null;
        }
        return Collections.unmodifiableSortedSet(licenses);
    }

    private String parseFamily(Node familyNode) {
        Map<String, String> attributes = attributes(familyNode);
        if (attributes.containsKey(ATT_LICENSE_REF)) {
            return attributes.get(ATT_LICENSE_REF);
        }
        throw new ConfigurationException("family tag requires " + ATT_LICENSE_REF + " attribute");
    }

    @Override
    public SortedSet<String> approvedLicenseId() {
        if (licenses.isEmpty()) {
            this.readLicenses();
        }
        if (licenseFamilies.isEmpty()) {
            SortedSet<String> result = new TreeSet<>();
            licenses.stream().map(x -> x.getLicenseFamily().getFamilyCategory()).forEach(result::add);
            return result;
        }
        return Collections.unmodifiableSortedSet(licenseFamilies);
    }

    private void parseMatcherBuilder(Node classNode) {
        Map<String, String> attributes = attributes(classNode);
        if (attributes.get(ATT_CLASS_NAME) == null) {
            throw new ConfigurationException("matcher must have a " + ATT_CLASS_NAME + " attribute");
        }
        MatcherBuilderTracker.addBuilder(attributes.get(ATT_CLASS_NAME), attributes.get(ATT_NAME));
    }

    @Override
    public void readMatcherBuilders() {
        nodeListConsumer(document.getElementsByTagName(MATCHER), x -> parseMatcherBuilder(x));
    }

    @Override
    public void addMatchers(URL url) {
        read(url);
    }

    /**
     * An abstract builder that delegates to another abstract builder.
     */
    abstract class DelegatingBuilder extends AbstractBuilder {
        protected final AbstractBuilder delegate;

        DelegatingBuilder(AbstractBuilder delegate) {
            this.delegate = delegate;
        }
    }
}
