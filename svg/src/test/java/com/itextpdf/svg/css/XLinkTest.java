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
package com.itextpdf.svg.css;

import com.itextpdf.styledxmlparser.LogMessageConstant;
import com.itextpdf.styledxmlparser.jsoup.nodes.Attribute;
import com.itextpdf.styledxmlparser.jsoup.nodes.Attributes;
import com.itextpdf.styledxmlparser.jsoup.nodes.Element;
import com.itextpdf.styledxmlparser.jsoup.parser.Tag;
import com.itextpdf.styledxmlparser.node.impl.jsoup.node.JsoupElementNode;
import com.itextpdf.svg.SvgConstants;
import com.itextpdf.svg.css.impl.SvgStyleResolver;
import com.itextpdf.svg.processors.impl.SvgConverterProperties;
import com.itextpdf.svg.processors.impl.SvgProcessorContext;
import com.itextpdf.test.ExtendedITextTest;
import com.itextpdf.test.annotations.LogMessage;
import com.itextpdf.test.annotations.LogMessages;
import com.itextpdf.test.annotations.type.UnitTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Map;

@Category(UnitTest.class)
public class XLinkTest extends ExtendedITextTest {

    @Test
    @LogMessages(messages = @LogMessage(messageTemplate = LogMessageConstant.UNABLE_TO_RESOLVE_IMAGE_URL))
    public void svgCssResolveMalformedXlinkTest() {
        Element jsoupImage = new Element(Tag.valueOf("image"), "");
        Attributes imageAttributes = jsoupImage.attributes();

        String value = "http://are::";
        imageAttributes.put(new Attribute("xlink:href", value));
        JsoupElementNode node = new JsoupElementNode(jsoupImage);

        SvgStyleResolver sr = new SvgStyleResolver(new SvgProcessorContext(new SvgConverterProperties()));
        Map<String, String> attr = sr.resolveStyles(node, new SvgCssContext());
        Assert.assertEquals(value, attr.get("xlink:href"));
    }

    @Test
    public void svgCssResolveDataXlinkTest() {
        Element jsoupImage = new Element(Tag.valueOf(SvgConstants.Tags.IMAGE), "");
        Attributes imageAttributes = jsoupImage.attributes();
        JsoupElementNode node = new JsoupElementNode(jsoupImage);

        String value1 = "data:image/png;base64,iVBORw0KGgoAAAANSU";
        imageAttributes.put(new Attribute("xlink:href", value1));

        SvgStyleResolver sr = new SvgStyleResolver(new SvgProcessorContext(new SvgConverterProperties()));
        Map<String, String> attr = sr.resolveStyles(node, new SvgCssContext());
        Assert.assertEquals(value1, attr.get("xlink:href"));

        String value2 = "data:...,.";
        imageAttributes.put(new Attribute("xlink:href", value2));

        sr = new SvgStyleResolver(new SvgProcessorContext(new SvgConverterProperties()));
        attr = sr.resolveStyles(node, new SvgCssContext());
        Assert.assertEquals(value2, attr.get("xlink:href"));

        String value3 = "dAtA:...,.";
        imageAttributes.put(new Attribute("xlink:href", value3));

        sr = new SvgStyleResolver(new SvgProcessorContext(new SvgConverterProperties()));
        attr = sr.resolveStyles(node, new SvgCssContext());
        Assert.assertEquals(value3, attr.get("xlink:href"));
    }
}
