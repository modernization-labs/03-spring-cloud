package com.acme.myproduct;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import com.acme.myproduct.ws.arithmetics.AddRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Characterizes the WSDL/JAXB path: the maven-jaxb2-plugin generates JAXB-annotated sources
 * (AddRequest/AddResponse) that bind through the JDK's built-in javax.xml.bind runtime on
 * Java 8. This pins both the generated-sources contract and the in-JDK JAXB behaviour as the
 * regression oracle for two later hops: Java 8 -> 11 (JAXB removed from the JDK, must become an
 * explicit dependency) and the javax -> jakarta namespace migration.
 */
public class JaxbArithmeticsCharacterizationTest {

    private static final String NS = "http://acme.com/arithmetics";

    @Test
    public void addRequestMarshalsToQualifiedXml() throws Exception {
        AddRequest request = new AddRequest();
        request.setAugend(2);
        request.setAddend(3);

        JAXBContext context = JAXBContext.newInstance(AddRequest.class);
        Marshaller marshaller = context.createMarshaller();
        StringWriter writer = new StringWriter();
        marshaller.marshal(request, writer);
        String xml = writer.toString();

        assertTrue(xml.contains(NS), "expected target namespace in: " + xml);
        assertTrue(xml.contains(">2</"), "expected <augend>2 in: " + xml);
        assertTrue(xml.contains(">3</"), "expected <addend>3 in: " + xml);
    }

    @Test
    public void addRequestRoundTripsThroughJaxb() throws Exception {
        AddRequest original = new AddRequest();
        original.setAugend(7);
        original.setAddend(5);

        JAXBContext context = JAXBContext.newInstance(AddRequest.class);

        StringWriter writer = new StringWriter();
        context.createMarshaller().marshal(original, writer);

        Unmarshaller unmarshaller = context.createUnmarshaller();
        AddRequest roundTripped =
                (AddRequest) unmarshaller.unmarshal(new StringReader(writer.toString()));

        assertEquals(7, roundTripped.getAugend());
        assertEquals(5, roundTripped.getAddend());
    }
}
