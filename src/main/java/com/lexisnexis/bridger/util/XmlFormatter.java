package com.lexisnexis.bridger.util;

import com.lexisnexis.bridger.generated.SearchResults;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.annotation.XmlSchema;

import javax.xml.namespace.QName;
import java.io.StringWriter;
import java.util.Objects;

/**
 * Formats API response objects for logging and file output.
 */
public final class XmlFormatter {

    private XmlFormatter() {
    }

    public static String toPrettyXml(SearchResults searchResults) {
        Objects.requireNonNull(searchResults, "searchResults must not be null");

        try {
            JAXBContext context = JAXBContext.newInstance(SearchResults.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

            XmlSchema schema = SearchResults.class.getPackage().getAnnotation(XmlSchema.class);
            String namespace = schema == null ? "" : schema.namespace();
            QName name = new QName(namespace, "SearchResults");
            JAXBElement<SearchResults> root =
                    new JAXBElement<>(name, SearchResults.class, searchResults);

            StringWriter writer = new StringWriter();
            marshaller.marshal(root, writer);
            return writer.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to convert SearchResults to XML", e);
        }
    }
}
