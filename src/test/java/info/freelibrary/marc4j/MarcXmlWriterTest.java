
package info.freelibrary.marc4j;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import info.freelibrary.marc4j.converter.impl.AnselToUnicode;
import info.freelibrary.marc4j.utils.StaticTestRecords;
import info.freelibrary.marc4j.utils.TestUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.xml.transform.dom.DOMResult;

import org.junit.Test;
import org.marc4j.MarcException;
import org.marc4j.MarcReader;
import org.marc4j.MarcStreamReader;
import org.marc4j.MarcXmlWriter;
import org.marc4j.marc.DataField;
import org.marc4j.marc.MarcFactory;
import org.marc4j.marc.Record;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class MarcXmlWriterTest {

    /**
     * Tests the {@link MarcXmlWriter}
     *
     * @throws Exception
     */
    @Test
    public void testMarcXmlWriter() throws Exception {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final MarcXmlWriter writer = new MarcXmlWriter(out, true);
        for (final Record record : StaticTestRecords.summerland) {
            writer.write(record);
        }
        writer.close();
        TestUtils
                .validateStringAgainstFile(new String(out.toByteArray()), StaticTestRecords.RESOURCES_SUMMERLAND_XML);
    }

    /**
     * Tests that {@link MarcXmlWriter} can write normalized XML.
     *
     * @throws Exception
     */
    @Test
    public void testMarcXmlWriterNormalized() throws Exception {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        final InputStream input = getClass().getResourceAsStream(StaticTestRecords.RESOURCES_BRKRTEST_MRC);
        assertNotNull(input);
        final MarcXmlWriter writer = new MarcXmlWriter(out, true);
        writer.setConverter(new AnselToUnicode());
        final MarcStreamReader reader = new MarcStreamReader(input);

        while (reader.hasNext()) {
            final Record record = reader.next();
            writer.write(record);
        }

        input.close();
        writer.close();

        final BufferedReader testoutput =
                new BufferedReader(new InputStreamReader(new ByteArrayInputStream(out.toByteArray()), "UTF-8"));
        String line;

        while ((line = testoutput.readLine()) != null) {
            if (line.matches("[ ]*<subfield code=\"a\">This is a test of diacritics.*")) {
                final String lineParts[] = line.split(", ");
                for (final String linePart : lineParts) {
                    if (linePart.startsWith("the tilde in ")) {
                        assertTrue("Incorrect value for tilde", linePart.equals("the tilde in man\u0303ana"));
                    } else if (linePart.startsWith("the grave accent in ")) {
                        assertTrue("Incorrect value for grave", linePart.equals("the grave accent in tre\u0300s"));
                    } else if (linePart.startsWith("the acute accent in ")) {
                        assertTrue("Incorrect value for acute", linePart
                                .equals("the acute accent in de\u0301sire\u0301e"));
                    } else if (linePart.startsWith("the circumflex in ")) {
                        assertTrue("Incorrect value for macron", linePart.equals("the circumflex in co\u0302te"));
                    } else if (linePart.startsWith("the macron in ")) {
                        assertTrue("Incorrect value for macron", linePart.equals("the macron in To\u0304kyo"));
                    } else if (linePart.startsWith("the breve in ")) {
                        assertTrue("Incorrect value for breve", linePart.equals("the breve in russkii\u0306"));
                    } else if (linePart.startsWith("the dot above in ")) {
                        assertTrue("Incorrect value for dot above", linePart.equals("the dot above in z\u0307aba"));
                    } else if (linePart.startsWith("the dieresis (umlaut) in ")) {
                        assertTrue("Incorrect value for umlaut", linePart
                                .equals("the dieresis (umlaut) in Lo\u0308wenbra\u0308u"));
                    }
                }
            }
        }
        testoutput.close();
    }

    /**
     * Tests catching of an indicator less subfield during write.
     *
     * @throws Exception
     */
    @Test(expected = MarcException.class)
    public void testWriteOfRecordWithIndicatorlessSubfield() throws Exception {
        final Record record = StaticTestRecords.getSummerlandRecord();
        final MarcFactory factory = StaticTestRecords.getFactory();
        final DataField badField = factory.newDataField();

        badField.setTag("911");
        badField.addSubfield(factory.newSubfield('a', "HAZMARC - INDICATORLESS FIELD DETECTED - MOPP LEVEL 4"));
        record.addVariableField(badField);

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final MarcXmlWriter writer = new MarcXmlWriter(out, true);
        writer.write(record);
        writer.close();
    }

    /**
     * Tests outputting to DOM result.
     *
     * @throws Exception
     */
    @Test
    public void testOutputToDOMResult() throws Exception {
        final InputStream input = getClass().getResourceAsStream(StaticTestRecords.RESOURCES_SUMMERLAND_MRC);
        assertNotNull("can't find summerland.mrc resource", input);
        final MarcReader reader = new MarcStreamReader(input);

        final DOMResult result = new DOMResult();
        final MarcXmlWriter writer = new MarcXmlWriter(result);

        writer.setConverter(new AnselToUnicode());

        while (reader.hasNext()) {
            final Record record = reader.next();
            writer.write(record);
        }

        writer.close();

        final Document doc = (Document) result.getNode();
        final Element documentElement = doc.getDocumentElement();
        assertEquals("document type should be collection", "collection", documentElement.getLocalName());

        final NodeList children = documentElement.getChildNodes();
        assertEquals("only one child", 1, children.getLength());

        final Element child = (Element) children.item(0);
        assertEquals("child should be a record", "record", child.getNodeName());
        assertEquals("one leader expected", 1, child.getElementsByTagName("leader").getLength());
    }

    /**
     * Tests {@link MarcXmlWriter} converted to UTF-8 and normalized.
     *
     * @throws Exception
     */
    @Test
    public void testMarcXmlWriterConvertedToUTF8AndNormalized() throws Exception {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        final InputStream input = getClass().getResourceAsStream(StaticTestRecords.RESOURCES_BRKRTEST_MRC);

        assertNotNull(input);

        final MarcXmlWriter writer = new MarcXmlWriter(out, true);
        writer.setConverter(new AnselToUnicode());
        writer.setUnicodeNormalization(true);

        final MarcStreamReader reader = new MarcStreamReader(input);

        while (reader.hasNext()) {
            final Record record = reader.next();
            writer.write(record);
        }

        input.close();
        writer.close();

        final BufferedReader testoutput =
                new BufferedReader(new InputStreamReader(new ByteArrayInputStream(out.toByteArray()), "UTF-8"));
        String line;

        while ((line = testoutput.readLine()) != null) {
            if (line.matches("[ ]*<subfield code=\"a\">This is a test of diacritics.*")) {
                final String lineParts[] = line.split(", ");

                for (int i = 0; i < lineParts.length; i++) {
                    if (lineParts[i].startsWith("the tilde in ")) {
                        assertTrue("Incorrect normalized value for tilde accent", lineParts[i]
                                .equals("the tilde in ma\u00F1ana"));
                    } else if (lineParts[i].startsWith("the grave accent in ")) {
                        assertTrue("Incorrect normalized value for grave accent", lineParts[i]
                                .equals("the grave accent in tr\u00E8s"));
                    } else if (lineParts[i].startsWith("the acute accent in ")) {
                        assertTrue("Incorrect normalized value for acute accent", lineParts[i]
                                .equals("the acute accent in d\u00E9sir\u00E9e"));
                    } else if (lineParts[i].startsWith("the circumflex in ")) {
                        assertTrue("Incorrect normalized value for circumflex", lineParts[i]
                                .equals("the circumflex in c\u00F4te"));
                    } else if (lineParts[i].startsWith("the macron in ")) {
                        assertTrue("Incorrect normalized value for macron", lineParts[i]
                                .equals("the macron in T\u014Dkyo"));
                    } else if (lineParts[i].startsWith("the breve in ")) {
                        assertTrue("Incorrect normalized value for breve", lineParts[i]
                                .equals("the breve in russki\u012D"));
                    } else if (lineParts[i].startsWith("the dot above in ")) {
                        assertTrue("Incorrect normalized value for dot above", lineParts[i]
                                .equals("the dot above in \u017Caba"));
                    } else if (lineParts[i].startsWith("the dieresis (umlaut) in ")) {
                        assertTrue("Incorrect normalized value for umlaut", lineParts[i]
                                .equals("the dieresis (umlaut) in L\u00F6wenbr\u00E4u"));
                    }
                }
            }
        }
    }
}
