package com.acme.myproduct;

import org.junit.Test;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HelloControllerTest {

    @Test
    public void indexReturnsHelloWorld() {
        HelloController controller = new HelloController();
        assertEquals("hello world", controller.index());
    }

    @Test
    public void shoutTrimsAndUppercases() {
        HelloController controller = new HelloController();
        assertEquals("HELLO", controller.shout("  hello  "));
    }

    @Test
    public void hasOverlapDetectsSharedElement() {
        HelloController controller = new HelloController();
        assertTrue(controller.hasOverlap(asList(1, 2, 3), asList(3, 4)));
        assertFalse(controller.hasOverlap(asList(1, 2), asList(3, 4)));
    }

    @Test
    public void toPdfProducesPdfBytes() throws Exception {
        HelloController controller = new HelloController();
        byte[] pdf = controller.toPdf("hello world");
        assertTrue(pdf.length > 0);
        assertEquals("%PDF", new String(pdf, 0, 4, "US-ASCII"));
    }

    @Test
    public void extractTextReadsPlainText() throws Exception {
        HelloController controller = new HelloController();
        String text = controller.extractText("hello tika".getBytes("UTF-8"));
        assertTrue(text.contains("hello tika"));
    }

    @Test
    public void mappingStrategyMapsBooleanToJpaEnumType() {
        HelloController controller = new HelloController();
        assertEquals("STRING", controller.mappingStrategyFor(true));
        assertEquals("ORDINAL", controller.mappingStrategyFor(false));
    }
}
