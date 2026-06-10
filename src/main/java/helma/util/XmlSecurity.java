/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 1998-2003 Helma Software. All Rights Reserved.
 */

package helma.util;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParserFactory;

/**
 * Central place for hardening JAXP parser factories against XML External
 * Entity (XXE) and related entity-expansion attacks. By default the factories
 * returned by the JDK resolve external entities and DOCTYPE declarations, which
 * allows local file disclosure and server-side request forgery when parsing
 * untrusted XML. Helma parses its own database/session XML as well as arbitrary
 * XML handed to application code (e.g. {@code Xml.read()}), so every factory
 * created in the framework is routed through here.
 *
 * <p>The primary defense is to disallow DOCTYPE declarations outright, which is
 * sufficient to block XXE. External entity and DTD loading are additionally
 * disabled as defense in depth. Features unsupported by a given parser
 * implementation are ignored rather than failing hard.</p>
 */
public final class XmlSecurity {

    // Apache/Xerces feature identifiers (also honored by the JDK default parser)
    private static final String DISALLOW_DOCTYPE =
            "http://apache.org/xml/features/disallow-doctype-decl";
    private static final String EXTERNAL_GENERAL_ENTITIES =
            "http://xml.org/sax/features/external-general-entities";
    private static final String EXTERNAL_PARAMETER_ENTITIES =
            "http://xml.org/sax/features/external-parameter-entities";
    private static final String LOAD_EXTERNAL_DTD =
            "http://apache.org/xml/features/nonvalidating/load-external-dtd";

    private XmlSecurity() {
    }

    /**
     * Harden a {@link DocumentBuilderFactory} against XXE.
     *
     * @param factory the factory to harden
     * @return the same factory, for chaining
     */
    public static DocumentBuilderFactory harden(DocumentBuilderFactory factory) {
        trySetFeature(factory, XMLConstants.FEATURE_SECURE_PROCESSING, true);
        trySetFeature(factory, DISALLOW_DOCTYPE, true);
        trySetFeature(factory, EXTERNAL_GENERAL_ENTITIES, false);
        trySetFeature(factory, EXTERNAL_PARAMETER_ENTITIES, false);
        trySetFeature(factory, LOAD_EXTERNAL_DTD, false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        return factory;
    }

    /**
     * Harden a {@link SAXParserFactory} against XXE.
     *
     * @param factory the factory to harden
     * @return the same factory, for chaining
     */
    public static SAXParserFactory harden(SAXParserFactory factory) {
        trySetFeature(factory, XMLConstants.FEATURE_SECURE_PROCESSING, true);
        trySetFeature(factory, DISALLOW_DOCTYPE, true);
        trySetFeature(factory, EXTERNAL_GENERAL_ENTITIES, false);
        trySetFeature(factory, EXTERNAL_PARAMETER_ENTITIES, false);
        trySetFeature(factory, LOAD_EXTERNAL_DTD, false);
        factory.setXIncludeAware(false);
        return factory;
    }

    private static void trySetFeature(DocumentBuilderFactory factory, String feature, boolean value) {
        try {
            factory.setFeature(feature, value);
        } catch (Exception ignore) {
            // feature not supported by this parser implementation; skip it
        }
    }

    private static void trySetFeature(SAXParserFactory factory, String feature, boolean value) {
        try {
            factory.setFeature(feature, value);
        } catch (Exception ignore) {
            // feature not supported by this parser implementation; skip it
        }
    }
}
