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
package com.itextpdf.kernel.pdf.layer;

import com.itextpdf.kernel.pdf.PdfArray;
import com.itextpdf.kernel.pdf.PdfDictionary;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfObject;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.test.ExtendedITextTest;
import com.itextpdf.test.annotations.type.UnitTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Category(UnitTest.class)
public class PdfVisibilityExpressionTest extends ExtendedITextTest {

    @Test
    public void expressionByArrayTest() {
        PdfDocument tempDoc = new PdfDocument(new PdfWriter(new ByteArrayOutputStream()));

        PdfArray array = new PdfArray();

        // add the AND operator as the first parameter of the expression
        array.add(PdfName.And);

        // add two empty dictionaries as the other parameters
        array.add(new PdfLayer((PdfDictionary) new PdfDictionary().makeIndirect(tempDoc)).getPdfObject());
        array.add(new PdfLayer((PdfDictionary) new PdfDictionary().makeIndirect(tempDoc)).getPdfObject());

        // create visibility expression
        PdfVisibilityExpression expression = new PdfVisibilityExpression(array);

        PdfObject expressionObject = expression.getPdfObject();
        Assert.assertTrue(expressionObject instanceof PdfArray);
        Assert.assertEquals(3, ((PdfArray) expressionObject).size());
        Assert.assertEquals(PdfName.And, ((PdfArray) expressionObject).getAsName(0));
    }

    @Test
    public void andExpressionTest() {
        PdfDocument tempDoc = new PdfDocument(new PdfWriter(new ByteArrayOutputStream()));

        // create expression with the AND operator as the first parameter
        PdfVisibilityExpression expression = new PdfVisibilityExpression(PdfName.And);

        // add two empty dictionaries as the other parameters
        expression.addOperand(new PdfLayer((PdfDictionary) new PdfDictionary().makeIndirect(tempDoc)));
        expression.addOperand(new PdfLayer((PdfDictionary) new PdfDictionary().makeIndirect(tempDoc)));

        PdfObject expressionObject = expression.getPdfObject();
        Assert.assertTrue(expressionObject instanceof PdfArray);
        Assert.assertEquals(3, ((PdfArray) expressionObject).size());
        Assert.assertEquals(PdfName.And, ((PdfArray) expressionObject).getAsName(0));
    }

    @Test
    public void nestedExpressionTest() {
        PdfDocument tempDoc = new PdfDocument(new PdfWriter(new ByteArrayOutputStream()));

        // create expression with the OR operator as the first parameter
        PdfVisibilityExpression expression = new PdfVisibilityExpression(PdfName.Or);

        // add an empty dictionary as the second parameter
        expression.addOperand(new PdfLayer((PdfDictionary) new PdfDictionary().makeIndirect(tempDoc)));

        // create a nested expression with the AND operator and two empty dictionaries as parameters
        PdfVisibilityExpression nestedExpression = new PdfVisibilityExpression(PdfName.And);
        nestedExpression.addOperand(new PdfLayer((PdfDictionary) new PdfDictionary().makeIndirect(tempDoc)));
        nestedExpression.addOperand(new PdfLayer((PdfDictionary) new PdfDictionary().makeIndirect(tempDoc)));

        // add another expression as the third parameter
        expression.addOperand(nestedExpression);

        PdfObject expressionObject = expression.getPdfObject();
        Assert.assertTrue(expressionObject instanceof PdfArray);
        Assert.assertEquals(3, ((PdfArray) expressionObject).size());
        Assert.assertEquals(PdfName.Or, ((PdfArray) expressionObject).getAsName(0));

        PdfObject child = ((PdfArray) expressionObject).get(2);
        Assert.assertTrue(child instanceof PdfArray);
        Assert.assertEquals(3, ((PdfArray) child).size());
        Assert.assertEquals(PdfName.And, ((PdfArray) child).get(0));
    }

}
