/*
    This file is part of the iText (R) project.
    Copyright (c) 1998-2020 iText Group NV
    Authors: iText Software.

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License version 3
    as published by the Free Software Foundation with the addition of the
    following permission added to Section 15 as permitted in Section 7(a):
    FOR ANY PART OF THE COVERED WORK IN WHICH THE COPYRIGHT IS OWNED BY
    ITEXT GROUP. ITEXT GROUP DISCLAIMS THE WARRANTY OF NON INFRINGEMENT
    OF THIRD PARTY RIGHTS

    This program is distributed in the hope that it will be useful, but
    WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
    or FITNESS FOR A PARTICULAR PURPOSE.
    See the GNU Affero General Public License for more details.
    You should have received a copy of the GNU Affero General Public License
    along with this program; if not, see http://www.gnu.org/licenses or write to
    the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
    Boston, MA, 02110-1301 USA, or download the license from the following URL:
    http://itextpdf.com/terms-of-use/

    The interactive user interfaces in modified source and object code versions
    of this program must display Appropriate Legal Notices, as required under
    Section 5 of the GNU Affero General Public License.

    In accordance with Section 7(b) of the GNU Affero General Public License,
    a covered work must retain the producer line in every PDF that is created
    or manipulated using iText.

    You can be released from the requirements of the license by purchasing
    a commercial license. Buying such a license is mandatory as soon as you
    develop commercial activities involving the iText software without
    disclosing the source code of your own applications.
    These activities include: offering paid services to customers as an ASP,
    serving PDFs on the fly in a web application, shipping iText with a closed
    source product.

    For more information, please contact iText Software Corp. at this
    address: sales@itextpdf.com
 */
package com.itextpdf.styledxmlparser.resolver.resource;

import com.itextpdf.kernel.pdf.xobject.PdfImageXObject;
import com.itextpdf.kernel.pdf.xobject.PdfXObject;
import com.itextpdf.styledxmlparser.LogMessageConstant;
import com.itextpdf.test.ExtendedITextTest;
import com.itextpdf.test.LogLevelConstants;
import com.itextpdf.test.annotations.LogMessage;
import com.itextpdf.test.annotations.LogMessages;
import com.itextpdf.test.annotations.type.UnitTest;

import java.nio.file.Files;
import java.net.URL;
import java.net.MalformedURLException;
import java.nio.file.Paths;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;

@Category(UnitTest.class)
public class ResourceResolverTest extends ExtendedITextTest {

    private final String baseUri = "./src/test/resources/com/itextpdf/styledxmlparser/resolver/retrieveStreamTest/";

    private final String bLogoIncorrect = "data:image/png;base,iVBORw0KGgoAAAANSUhEUgAAAVoAAAAxCAMAAACsy5FpAAAABGdBTUEAALGPC/xhBQAAAAFzUkdCAK7OHOkAAAAqUExURQAAAPicJAdJdQdJdQdJdficJjBUbPicJgdJdQdJdficJficJQdJdficJlrFe50AAAAMdFJOUwCBe8I/Phe+65/saIJg0K4AAAMOSURBVHja7ZvbmqsgDIU5Bo/v/7q7/WZXsQYNuGy1muuZFH7DIiSglFLU6pZUbGQQNvXpNcC4caoNRvNxOuDUdf80HXk3VYewKp516DHWxuOc/0ye/U00duAwU+/qkWzfh9F9hzIHJxuzNa+fsa4I7Ihx+H+qUFN/sKVhzP7lH+a+qwY1gJHtmwFDPBHK1wLLjLOGTb2jIWhHScAF7RgOGod2CAGTFB8J2JodJ3Dq5kNow95oH3BdtsjGHE6LVu+P9iG5UlVwNjXOndGeRWuZEBBJLtWcMMK11nFoDfDL4TOEMUu0K/leIpNNpUrYFVsrDi2Mbb1DXqv5PV4quWzKHikJKq99utTsoI1dsMjBkr2dctoAMO3XQS2ogrNrJ5vH1OvtU6/ddIPR0k1g9K++bcSKo6Htf8wbdxpK2rnRigJRqAU3WiEylzzVlubCF0TLb/pTyZXH9o1WoKLVoKK8yBbUHS6IdjksZYpxo82WXIzIXhptYtmDRPbQaDXiPBZaaQl26ZBI6pfQ+gZ00A3CxkH6COo2rIwjom12KM/IJRehBUdF2wLrtUWS+56P/Q7aPUrheYnYRpE9LtrwSbSp7cxuJnv1qCWzk9AeEy3t0MAp2ccq93NogWHry3QWowqHPDK0mPSr8aXZAWQzO+hB17ebb9P5ZbDCu2obJPeiNQQWbAUse10VbbKqSLm9yRutQGT/8wO0G6+LdvV2Aaq0eDW0kmI3SHKvhZZkESnoTd5o5SIr+gb0A2g9wGQi67KUw5wdLajNEHymyCqo5B4RLawWHp10XcEC528suBOjJVwDZ2iOca9lBNsSl4jZE6Ntd6jXmtKVzeiIOy/aDzwTydmPZpJrzov2A89EsrKod8mVoq1y0LbsE02Zf/sVQSAObXa5ZSq5UkGoZw9LlqwRNkai5ZT7rRXyHkJgQqioSBipgjhGHPdMYy3hbLx8UDbDPTatndyeeW1HpaXtodxYyUO+zmoDUWjeUnHRB7d5E/KQnazRs0VdbWjI/EluloPnb26+KXIGI+e+7CBt/wAetDeCKwxY6QAAAABJRU5ErkJggg==";

    private final String bLogoCorruptedData = "data:image/png;base64,,,iVBORw0KGgoAAAANSUhEUgAAAVoAAAAxCAMAAACsy5FpAAAABGdBTUEAALGPC/xhBQAAAAFzUkdCAK7OHOkAAAAqUExURQAAAPicJAdJdQdJdQdJdficJjBUbPicJgdJdQdJdficJficJQdJdficJlrFe50AAAAMdFJOUwCBe8I/Phe+65/saIJg0K4AAAMOSURBVHja7ZvbmqsgDIU5Bo/v/7q7/WZXsQYNuGy1muuZFH7DIiSglFLU6pZUbGQQNvXpNcC4caoNRvNxOuDUdf80HXk3VYewKp516DHWxuOc/0ye/U00duAwU+/qkWzfh9F9hzIHJxuzNa+fsa4I7Ihx+H+qUFN/sKVhzP7lH+a+qwY1gJHtmwFDPBHK1wLLjLOGTb2jIWhHScAF7RgOGod2CAGTFB8J2JodJ3Dq5kNow95oH3BdtsjGHE6LVu+P9iG5UlVwNjXOndGeRWuZEBBJLtWcMMK11nFoDfDL4TOEMUu0K/leIpNNpUrYFVsrDi2Mbb1DXqv5PV4quWzKHikJKq99utTsoI1dsMjBkr2dctoAMO3XQS2ogrNrJ5vH1OvtU6/ddIPR0k1g9K++bcSKo6Htf8wbdxpK2rnRigJRqAU3WiEylzzVlubCF0TLb/pTyZXH9o1WoKLVoKK8yBbUHS6IdjksZYpxo82WXIzIXhptYtmDRPbQaDXiPBZaaQl26ZBI6pfQ+gZ00A3CxkH6COo2rIwjom12KM/IJRehBUdF2wLrtUWS+56P/Q7aPUrheYnYRpE9LtrwSbSp7cxuJnv1qCWzk9AeEy3t0MAp2ccq93NogWHry3QWowqHPDK0mPSr8aXZAWQzO+hB17ebb9P5ZbDCu2obJPeiNQQWbAUse10VbbKqSLm9yRutQGT/8wO0G6+LdvV2Aaq0eDW0kmI3SHKvhZZkESnoTd5o5SIr+gb0A2g9wGQi67KUw5wdLajNEHymyCqo5B4RLawWHp10XcEC528suBOjJVwDZ2iOca9lBNsSl4jZE6Ntd6jXmtKVzeiIOy/aDzwTydmPZpJrzov2A89EsrKod8mVoq1y0LbsE02Zf/sVQSAObXa5ZSq5UkGoZw9LlqwRNkai5ZT7rRXyHkJgQqioSBipgjhGHPdMYy3hbLx8UDbDPTatndyeeW1HpaXtodxYyUO+zmoDUWjeUnHRB7d5E/KQnazRs0VdbWjI/EluloPnb26+KXIGI+e+7CBt/wAetDeCKwxY6QAAAABJRU5ErkJggg==";

    private final String bLogo = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAVoAAAAxCAMAAACsy5FpAAAABGdBTUEAALGPC/xhBQAAAAFzUkdCAK7OHOkAAAAqUExURQAAAPicJAdJdQdJdQdJdficJjBUbPicJgdJdQdJdficJficJQdJdficJlrFe50AAAAMdFJOUwCBe8I/Phe+65/saIJg0K4AAAMOSURBVHja7ZvbmqsgDIU5Bo/v/7q7/WZXsQYNuGy1muuZFH7DIiSglFLU6pZUbGQQNvXpNcC4caoNRvNxOuDUdf80HXk3VYewKp516DHWxuOc/0ye/U00duAwU+/qkWzfh9F9hzIHJxuzNa+fsa4I7Ihx+H+qUFN/sKVhzP7lH+a+qwY1gJHtmwFDPBHK1wLLjLOGTb2jIWhHScAF7RgOGod2CAGTFB8J2JodJ3Dq5kNow95oH3BdtsjGHE6LVu+P9iG5UlVwNjXOndGeRWuZEBBJLtWcMMK11nFoDfDL4TOEMUu0K/leIpNNpUrYFVsrDi2Mbb1DXqv5PV4quWzKHikJKq99utTsoI1dsMjBkr2dctoAMO3XQS2ogrNrJ5vH1OvtU6/ddIPR0k1g9K++bcSKo6Htf8wbdxpK2rnRigJRqAU3WiEylzzVlubCF0TLb/pTyZXH9o1WoKLVoKK8yBbUHS6IdjksZYpxo82WXIzIXhptYtmDRPbQaDXiPBZaaQl26ZBI6pfQ+gZ00A3CxkH6COo2rIwjom12KM/IJRehBUdF2wLrtUWS+56P/Q7aPUrheYnYRpE9LtrwSbSp7cxuJnv1qCWzk9AeEy3t0MAp2ccq93NogWHry3QWowqHPDK0mPSr8aXZAWQzO+hB17ebb9P5ZbDCu2obJPeiNQQWbAUse10VbbKqSLm9yRutQGT/8wO0G6+LdvV2Aaq0eDW0kmI3SHKvhZZkESnoTd5o5SIr+gb0A2g9wGQi67KUw5wdLajNEHymyCqo5B4RLawWHp10XcEC528suBOjJVwDZ2iOca9lBNsSl4jZE6Ntd6jXmtKVzeiIOy/aDzwTydmPZpJrzov2A89EsrKod8mVoq1y0LbsE02Zf/sVQSAObXa5ZSq5UkGoZw9LlqwRNkai5ZT7rRXyHkJgQqioSBipgjhGHPdMYy3hbLx8UDbDPTatndyeeW1HpaXtodxYyUO+zmoDUWjeUnHRB7d5E/KQnazRs0VdbWjI/EluloPnb26+KXIGI+e+7CBt/wAetDeCKwxY6QAAAABJRU5ErkJggg==";

    @Rule
    public ExpectedException junitExpectedException = ExpectedException.none();

    // Constructor tests block

    @Test
    public void constructorWithBaseUriTest() throws MalformedURLException {
        ResourceResolver resolver = new ResourceResolver(null);

        UriResolver uriResolver = new UriResolver("");
        String resolveUrl = resolver.resolveAgainstBaseUri("").toString();
        String expectedUrl = uriResolver.resolveAgainstBaseUri("").toString();

        Assert.assertEquals(resolveUrl, expectedUrl);
        Assert.assertEquals(DefaultResourceRetriever.class, resolver.getRetriever().getClass());
    }

    @Test
    public void constructorWithBaseUriAndResourceRetrieverTest() throws MalformedURLException {
        ResourceResolver resolver = new ResourceResolver("folder", new CustomResourceRetriever());

        UriResolver uriResolver = new UriResolver("folder");
        String resolveUrl = resolver.resolveAgainstBaseUri("").toString();
        String expectedUrl = uriResolver.resolveAgainstBaseUri("").toString();

        Assert.assertEquals(resolveUrl, expectedUrl);
        Assert.assertEquals(CustomResourceRetriever.class, resolver.getRetriever().getClass());
    }

    private static class CustomResourceRetriever extends DefaultResourceRetriever {

    }

    // Malformed resource name tests block

    @Test
    @LogMessages(messages = @LogMessage(messageTemplate = LogMessageConstant.UNABLE_TO_RETRIEVE_STREAM_WITH_GIVEN_BASE_URI, logLevel = LogLevelConstants.ERROR))
    public void retrieveStreamByMalformedResourceNameTest() {
        String fileName = "resourceResolverTest .png";
        ResourceResolver resourceResolver = new ResourceResolver(baseUri);
        byte[] bytes = resourceResolver.retrieveStream(fileName);
        Assert.assertNull(bytes);
    }

    @Test
    public void retrieveStyleSheetByMalformedResourceNameTest() throws IOException {
        junitExpectedException.expect(IOException.class);
        String fileName = "retrieveStyl eSheetTest.css";
        ResourceResolver resourceResolver = new ResourceResolver(baseUri);
        resourceResolver.retrieveStyleSheet(fileName);
    }

    @Test
    @LogMessages(messages = @LogMessage(messageTemplate = LogMessageConstant.UNABLE_TO_RETRIEVE_STREAM_WITH_GIVEN_BASE_URI, logLevel = LogLevelConstants.ERROR))
    public void retrieveResourceAsInputStreamByMalformedResourceNameTest() {
        String fileName = "retrieveStyl eSheetTest.css";
        ResourceResolver resourceResolver = new ResourceResolver(baseUri);
        InputStream stream = resourceResolver.retrieveResourceAsInputStream(fileName);
        Assert.assertNull(stream);
    }

    @Test
    @LogMessages(messages = @LogMessage(messageTemplate = LogMessageConstant.UNABLE_TO_RETRIEVE_STREAM_WITH_GIVEN_BASE_URI, logLevel = LogLevelConstants.ERROR))
    public void retrieveBytesFromResourceByMalformedResourceNameTest() {
        String fileName = "retrieveStyl eSheetTest.css";
        ResourceResolver resourceResolver = new ResourceResolver(baseUri);
        byte[] bytes = resourceResolver.retrieveBytesFromResource(fileName);
        Assert.assertNull(bytes);
    }

    @Test
    @LogMessages(messages = @LogMessage(messageTemplate = LogMessageConstant.UNABLE_TO_RETRIEVE_IMAGE_WITH_GIVEN_BASE_URI, logLevel = LogLevelConstants.ERROR))
    public void retrieveImageExtendedByMalformedResourceNameTest() {
        String fileName = "retrieveStyl eSheetTest.css";

        ResourceResolver resourceResolver = new ResourceResolver(baseUri);
        PdfXObject pdfXObject = resourceResolver.retrieveImageExtended(fileName);
        Assert.assertNull(pdfXObject);
    }

    @Test
    public void malformedResourceNameTest07() throws IOException {
        String fileName = "%23%5B%5D@!$&'()+,;=._~-/styles09.css";

        InputStream expected = new FileInputStream(baseUri + "#[]@!$&'()+,;=._~-/styles09.css");

        ResourceResolver resourceResolver = new ResourceResolver(baseUri);
        InputStream stream = resourceResolver.retrieveStyleSheet(fileName);

        Assert.assertNotNull(stream);
        Assert.assertEquals(expected.read(), stream.read());
    }

    // Boolean method tests block

    @Test
    public void isDataSrcTest() {
        ResourceResolver resourceResolver = new ResourceResolver(baseUri);
        Assert.assertTrue(resourceResolver.isDataSrc(bLogo));
        Assert.assertTrue(resourceResolver.isDataSrc(bLogoCorruptedData));
        Assert.assertTrue(resourceResolver.isDataSrc(bLogoIncorrect));
        Assert.assertFalse(resourceResolver.isDataSrc("https://data.com/data"));
    }

    @Test
    public void isImageTypeSupportedTest() {
        ResourceResolver resourceResolver = new ResourceResolver(baseUri);
        Assert.assertTrue(resourceResolver.isImageTypeSupportedByImageDataFactory("resourceResolverTest.png"));
        Assert.assertFalse(resourceResolver.isImageTypeSupportedByImageDataFactory("test.txt"));
        Assert.assertFalse(resourceResolver.isImageTypeSupportedByImageDataFactory("htt://test.png"));
    }

    // Retrieve pdfXObject tests block

    @Test
    public void retrieveImageExtendedBase64Test() {
        ResourceResolver resourceResolver = new ResourceResolver(baseUri);
        PdfXObject image = resourceResolver.retrieveImageExtended(bLogo);
        Assert.assertNotNull(image);
    }

    @Test
    @LogMessages(messages = @LogMessage(messageTemplate = LogMessageConstant.UNABLE_TO_RETRIEVE_IMAGE_WITH_GIVEN_DATA_URI))
    public void retrieveImageExtendedIncorrectBase64Test() {
        ResourceResolver resourceResolver = new ResourceResolver(baseUri);
        PdfXObject image = resourceResolver.retrieveImageExtended(bLogoCorruptedData);
        Assert.assertNull(image);
    }

    @Test
    @LogMessages(messages = @LogMessage(messageTemplate = LogMessageConstant.UNABLE_TO_RETRIEVE_IMAGE_WITH_GIVEN_DATA_URI, logLevel = LogLevelConstants.ERROR))
    public void retrieveImageExtendedCorruptedDataBase64Test() {
        ResourceResolver resourceResolver = new ResourceResolver(baseUri);
        PdfXObject image = resourceResolver.retrieveImageExtended(bLogoCorruptedData);
        Assert.assertNull(image);
    }

    @Test
    @LogMessages(messages = @LogMessage(messageTemplate = LogMessageConstant.UNABLE_TO_RETRIEVE_IMAGE_WITH_GIVEN_BASE_URI, logLevel = LogLevelConstants.ERROR))
    public void retrieveImageExtendedNullTest() {
        ResourceResolver resourceResolver = new ResourceResolver(baseUri);
        PdfXObject image = resourceResolver.retrieveImageExtended(null);
        Assert.assertNull(image);
    }

    @Test
    public void retrieveImageTest() {
        String fileName = "resourceResolverTest.png";
        ResourceResolver resourceResolver = new ResourceResolver(baseUri);
        PdfImageXObject image = resourceResolver.retrieveImage(fileName);
        Assert.assertNotNull(image);
        Assert.assertTrue(image.identifyImageFileExtension().equalsIgnoreCase("png"));
    }

    // Retrieve byte array tests block

    @Test
    public void retrieveBytesFromResourceBase64Test() {
        ResourceResolver resourceResolver = new ResourceResolver(baseUri);
        byte[] bytes = resourceResolver.retrieveBytesFromResource(bLogo);
        Assert.assertNotNull(bytes);
    }

    @Test
    @LogMessages(messages = @LogMessage(messageTemplate = LogMessageConstant.UNABLE_TO_RETRIEVE_STREAM_WITH_GIVEN_BASE_URI, logLevel = LogLevelConstants.ERROR))
    public void retrieveBytesFromResourceIncorrectBase64Test() {
        ResourceResolver resourceResolver = new ResourceResolver(baseUri);
        byte[] bytes = resourceResolver.retrieveBytesFromResource(bLogoIncorrect);
        Assert.assertNull(bytes);
    }

    @Test
    @LogMessages(messages = @LogMessage(messageTemplate = LogMessageConstant.UNABLE_TO_RETRIEVE_STREAM_WITH_GIVEN_BASE_URI, logLevel = LogLevelConstants.ERROR))
    public void retrieveBytesFromResourceCorruptedDataBase64Test() {
        ResourceResolver resourceResolver = new ResourceResolver(baseUri);
        byte[] bytes = resourceResolver.retrieveBytesFromResource(bLogoCorruptedData);
        Assert.assertNull(bytes);
    }

    @Test
    public void retrieveBytesFromResourcePngImageTest() throws IOException {
        String fileName = "resourceResolverTest.png";
        ResourceResolver resourceResolver = new ResourceResolver(baseUri);
        byte[] expected = Files.readAllBytes(new File(baseUri + fileName).toPath());
        byte[] bytes = resourceResolver.retrieveBytesFromResource(fileName);
        Assert.assertNotNull(bytes);
        Assert.assertEquals(expected.length, bytes.length);
    }

    @Test
    public void retrieveStreamPngImageTest() throws IOException {
        String fileName = "resourceResolverTest.png";
        ResourceResolver resourceResolver = new ResourceResolver(baseUri);
        byte[] expected = Files.readAllBytes(new File(baseUri + fileName).toPath());
        byte[] stream = resourceResolver.retrieveStream(fileName);
        Assert.assertNotNull(resourceResolver.retrieveStream(fileName));
        Assert.assertEquals(expected.length, stream.length);
    }

    @Test
    public void retrieveBytesFromResourceStyleSheetTest() throws IOException {
        String fileName = "retrieveStyleSheetTest.css";
        ResourceResolver resourceResolver = new ResourceResolver(baseUri);
        byte[] expected = Files.readAllBytes(new File(baseUri + fileName).toPath());
        byte[] bytes = resourceResolver.retrieveBytesFromResource(fileName);
        Assert.assertNotNull(bytes);
        Assert.assertEquals(expected.length, bytes.length);
    }

    @Test
    @LogMessages(messages = @LogMessage(messageTemplate = LogMessageConstant.RESOURCE_WITH_GIVEN_URL_WAS_FILTERED_OUT, logLevel = LogLevelConstants.WARN))
    public void attemptToRetrieveBytesFromResourceStyleSheetWithFilterRetrieverTest() {
        String fileName = "retrieveStyleSheetTest.css";
        ResourceResolver resourceResolver = new ResourceResolver(baseUri);
        resourceResolver.setRetriever(new FilterResourceRetriever());
        byte[] bytes = resourceResolver.retrieveBytesFromResource(fileName);
        Assert.assertNull(bytes);
    }

    @Test
    @LogMessages(messages = @LogMessage(messageTemplate = LogMessageConstant.UNABLE_TO_RETRIEVE_IMAGE_WITH_GIVEN_BASE_URI))
    public void retrieveImageWrongPathTest() {
        String fileName = "/itextpdf.com/itis.jpg";
        ResourceResolver resourceResolver = new ResourceResolver(baseUri);
        PdfImageXObject image = resourceResolver.retrieveImage(fileName);
        Assert.assertNull(image);
    }

    @Test
    public void retrieveImageRightPathTest() {
        String fileName = "itextpdf.com/itis.jpg";
        ResourceResolver resourceResolver = new ResourceResolver(baseUri);
        PdfImageXObject image = resourceResolver.retrieveImage(fileName);
        Assert.assertNotNull(image);
        Assert.assertTrue(image.identifyImageFileExtension().equalsIgnoreCase("jpg"));
    }

    @Test
    public void retrieveImagePathWithSpacesTest() {
        String fileName = "retrieveImagePathWithSpaces.jpg";
        ResourceResolver resourceResolver = new ResourceResolver(baseUri + "path with spaces/");
        PdfImageXObject image = resourceResolver.retrieveImage(fileName);
        Assert.assertNotNull(image);
    }

    @Test
    @LogMessages(messages = @LogMessage(messageTemplate = LogMessageConstant.UNABLE_TO_RETRIEVE_STREAM_WITH_GIVEN_BASE_URI))
    public void retrieveBytesMalformedResourceNameTest() {
        String fileName = "resourceResolverTest .png";
        ResourceResolver resourceResolver = new ResourceResolver(baseUri);
        byte[] bytes =resourceResolver.retrieveBytesFromResource(fileName);
        Assert.assertNull(bytes);
    }

    @Test
    public void retrieveBytesFromResourceWithRetryRetrieverTest() throws IOException {
        String fileName = "!invalid! StyleSheetName.css";
        ResourceResolver resourceResolver = new ResourceResolver(baseUri, new RetryResourceRetriever(baseUri));
        byte[] expected = Files.readAllBytes(new File(baseUri + "retrieveStyleSheetTest.css").toPath());
        byte[] bytes = resourceResolver.retrieveBytesFromResource(fileName);
        Assert.assertNotNull(bytes);
        Assert.assertEquals(expected.length, bytes.length);
    }

    @Test
    @LogMessages(messages = @LogMessage(messageTemplate = LogMessageConstant.UNABLE_TO_RETRIEVE_RESOURCE_WITH_GIVEN_RESOURCE_SIZE_BYTE_LIMIT, logLevel = LogLevelConstants.WARN))
    public void attemptToRetrieveBytesFromLocalWithResourceSizeByteLimitTest() {
        String fileName = "retrieveStyleSheetTest.css.dat";
        // retrieveStyleSheetTest.css.dat size is 89 bytes
        IResourceRetriever retriever = new DefaultResourceRetriever().setResourceSizeByteLimit(88);
        ResourceResolver resourceResolver = new ResourceResolver(baseUri, retriever);
        byte[] bytes = resourceResolver.retrieveBytesFromResource(fileName);
        Assert.assertNull(bytes);
    }

    @Test
    public void retrieveBytesFromLocalWithResourceSizeByteLimitTest() {
        String fileName = "retrieveStyleSheetTest.css.dat";
        // retrieveStyleSheetTest.css.dat size is 89 bytes
        IResourceRetriever retriever = new DefaultResourceRetriever().setResourceSizeByteLimit(89);
        ResourceResolver resourceResolver = new ResourceResolver(baseUri, retriever);
        byte[] bytes = resourceResolver.retrieveBytesFromResource(fileName);
        Assert.assertNotNull(bytes);
        Assert.assertEquals(((DefaultResourceRetriever) retriever).getResourceSizeByteLimit(), bytes.length);
    }

    // Retrieve input stream tests block

    @Test
    public void attemptToReadBytesFromLimitedInputStreamTest() throws IOException {
        junitExpectedException.expect(ReadingByteLimitException.class);
        String fileName = "retrieveStyleSheetTest.css.dat";
        // retrieveStyleSheetTest.css.dat size is 89 bytes
        IResourceRetriever retriever = new DefaultResourceRetriever().setResourceSizeByteLimit(40);
        ResourceResolver resourceResolver = new ResourceResolver(baseUri, retriever);
        InputStream stream = resourceResolver.retrieveResourceAsInputStream(fileName);
        for (int i = 0; i < 41; i++) {
            stream.read();
        }
    }

    @Test
    public void retrieveResourceAsInputStreamBase64Test() {
        ResourceResolver resourceResolver = new ResourceResolver(baseUri);
        InputStream stream = resourceResolver.retrieveResourceAsInputStream(bLogo);
        Assert.assertNotNull(stream);
    }

    @Test
    public void retrieveStyleSheetTest() throws IOException {
        String fileName = "retrieveStyleSheetTest.css";
        InputStream expected = new FileInputStream(baseUri + fileName);
        ResourceResolver resourceResolver = new ResourceResolver(baseUri);
        InputStream stream = resourceResolver.retrieveStyleSheet(fileName);
        Assert.assertNotNull(stream);
        Assert.assertEquals(expected.read(), stream.read());
    }

    @Test
    public void retrieveResourceAsInputStreamStyleSheetTest() throws IOException {
        String fileName = "retrieveStyleSheetTest.css";
        InputStream expected = new FileInputStream(baseUri + fileName);
        ResourceResolver resourceResolver = new ResourceResolver(baseUri);
        InputStream stream = resourceResolver.retrieveResourceAsInputStream(fileName);
        Assert.assertNotNull(stream);
        Assert.assertEquals(expected.read(), stream.read());
    }

    @Test
    @LogMessages(messages = @LogMessage(messageTemplate = LogMessageConstant.RESOURCE_WITH_GIVEN_URL_WAS_FILTERED_OUT, logLevel = LogLevelConstants.WARN))
    public void attemptToRetrieveInputStreamWithFilterRetrieverTest() {
        String fileName = "retrieveStyleSheetTest.css";
        ResourceResolver resourceResolver = new ResourceResolver(baseUri);
        resourceResolver.setRetriever(new FilterResourceRetriever());
        InputStream stream = resourceResolver.retrieveResourceAsInputStream(fileName);
        Assert.assertNull(stream);
    }

    private static class FilterResourceRetriever extends DefaultResourceRetriever {
        @Override
        protected boolean urlFilter(URL url) {
            return url.getPath().startsWith("/MyFolderWithUniqName");
        }
    }

    @Test
    public void retrieveInputStreamWithRetryRetrieverTest() throws IOException {
        String fileName = "!invalid! StyleSheetName.css";
        ResourceResolver resourceResolver = new ResourceResolver(baseUri, new RetryResourceRetriever(baseUri));
        InputStream expected = new FileInputStream(baseUri + "retrieveStyleSheetTest.css");
        InputStream stream = resourceResolver.retrieveResourceAsInputStream(fileName);
        Assert.assertNotNull(stream);
        Assert.assertEquals(expected.read(), stream.read());
    }

    private static class RetryResourceRetriever extends DefaultResourceRetriever {
        private String baseUri;

        public RetryResourceRetriever(String baseUri) {
            this.baseUri = baseUri;
        }

        @Override
        public InputStream getInputStreamByUrl(URL url) throws IOException {
            InputStream stream = null;
            try {
                stream = super.getInputStreamByUrl(url);
            } catch (Exception ignored) {
            }

            if (stream == null) {
                URL newUrl = new UriResolver(this.baseUri).resolveAgainstBaseUri("retrieveStyleSheetTest.css");
                stream = super.getInputStreamByUrl(newUrl);
            }
            return stream;
        }
    }

    // Absolute path tests block

    @Test
    public void retrieveStyleSheetAbsolutePathTest() throws IOException {
        String fileName = "retrieveStyleSheetTest.css";
        String absolutePath = Paths.get(baseUri, fileName).toFile().getAbsolutePath();

        ResourceResolver resourceResolver = new ResourceResolver(baseUri);
        try (InputStream stream = resourceResolver.retrieveStyleSheet(absolutePath);
                InputStream expected = new FileInputStream(absolutePath);) {
            Assert.assertNotNull(stream);
            Assert.assertEquals(expected.read(), stream.read());
        }
    }

    @Test
    public void retrieveResourceAsInputStreamAbsolutePathTest() throws IOException {
        String fileName = "retrieveStyleSheetTest.css";
        String absolutePath = Paths.get(baseUri, fileName).toFile().getAbsolutePath();

        ResourceResolver resourceResolver = new ResourceResolver(baseUri);
        try (InputStream stream = resourceResolver.retrieveResourceAsInputStream(absolutePath);
                InputStream expected = new FileInputStream(absolutePath);) {
            Assert.assertNotNull(stream);
            Assert.assertEquals(expected.read(), stream.read());
        }
    }

    @Test
    public void retrieveStyleSheetFileUrlTest() throws IOException {
        String fileName = "retrieveStyleSheetTest.css";
        URL url = Paths.get(baseUri, fileName).toUri().toURL();
        String fileUrlString = url.toExternalForm();

        ResourceResolver resourceResolver = new ResourceResolver(baseUri);
        try (InputStream stream = resourceResolver.retrieveStyleSheet(fileUrlString);
                InputStream expected = url.openStream()) {
            Assert.assertNotNull(stream);
            Assert.assertEquals(expected.read(), stream.read());
        }
    }

    @Test
    public void retrieveResourceAsInputStreamFileUrlTest() throws IOException {
        String fileName = "retrieveStyleSheetTest.css";
        URL url = Paths.get(baseUri, fileName).toUri().toURL();
        String fileUrlString = url.toExternalForm();

        ResourceResolver resourceResolver = new ResourceResolver(baseUri);
        try (InputStream stream = resourceResolver.retrieveResourceAsInputStream(fileUrlString);
                InputStream expected = url.openStream()) {
            Assert.assertNotNull(stream);
            Assert.assertEquals(expected.read(), stream.read());
        }
    }
}
