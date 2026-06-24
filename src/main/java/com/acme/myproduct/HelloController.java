package com.acme.myproduct;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collection;

import javax.persistence.EnumType;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.Tika;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/")
    public String index() {
        return "hello world";
    }

    public String shout(String value) {
        return StringUtils.upperCase(StringUtils.trimToEmpty(value));
    }

    public boolean hasOverlap(Collection<?> a, Collection<?> b) {
        return CollectionUtils.containsAny(a, b);
    }

    public byte[] toPdf(String text) throws Exception {
        Document document = new Document();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();
        document.add(new Paragraph(text));
        document.close();
        return out.toByteArray();
    }

    public String extractText(byte[] content) throws Exception {
        return new Tika().parseToString(new ByteArrayInputStream(content)).trim();
    }

    public String mappingStrategyFor(boolean storeAsText) {
        return (storeAsText ? EnumType.STRING : EnumType.ORDINAL).name();
    }
}
