/*

    This file is part of the iText (R) project.
    Copyright (c) 1998-2020 iText Group NV
    Authors: Bruno Lowagie, Paulo Soares, et al.

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
package com.itextpdf.kernel.pdf.annot;

import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDictionary;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfObject;
import com.itextpdf.kernel.pdf.action.PdfAction;

public class PdfScreenAnnotation extends PdfAnnotation {

    private static final long serialVersionUID = 1334399136151450493L;

	public PdfScreenAnnotation(Rectangle rect) {
        super(rect);
    }

    /**
     * Instantiates a new {@link PdfScreenAnnotation} instance based on {@link PdfDictionary}
     * instance, that represents existing annotation object in the document.
     *
     * @param pdfObject the {@link PdfDictionary} representing annotation object
     * @see PdfAnnotation#makeAnnotation(PdfObject)
     */
    protected PdfScreenAnnotation(PdfDictionary pdfObject) {
        super(pdfObject);
    }

    @Override
    public PdfName getSubtype() {
        return PdfName.Screen;
    }

    /**
     * An {@link PdfAction} to perform, such as launching an application, playing a sound,
     * changing an annotation’s appearance state etc, when the annotation is activated.
     * @return {@link PdfDictionary} which defines the characteristics and behaviour of an action.
     */
    public PdfDictionary getAction() {
        return getPdfObject().getAsDictionary(PdfName.A);
    }

    /**
     * Sets a {@link PdfAction} to this annotation which will be performed when the annotation is activated.
     * @param action {@link PdfAction} to set to this annotation.
     * @return this {@link PdfScreenAnnotation} instance.
     */
    public PdfScreenAnnotation setAction(PdfAction action) {
        return (PdfScreenAnnotation) put(PdfName.A, action.getPdfObject());
    }

    /**
     * An additional actions dictionary that extends the set of events that can trigger the execution of an action.
     * See ISO-320001 12.6.3 Trigger Events.
     * @return an additional actions {@link PdfDictionary}.
     * @see #getAction()
     */
    public PdfDictionary getAdditionalAction() {
        return getPdfObject().getAsDictionary(PdfName.AA);
    }

    /**
     * Sets an additional {@link PdfAction} to this annotation which will be performed in response to
     * the specific trigger event defined by {@code key}. See ISO-320001 12.6.3, "Trigger Events".
     * @param key a {@link PdfName} that denotes a type of the additional action to set.
     * @param action {@link PdfAction} to set as additional to this annotation.
     * @return this {@link PdfScreenAnnotation} instance.
     */
    public PdfScreenAnnotation setAdditionalAction(PdfName key, PdfAction action) {
        PdfAction.setAdditionalAction(this, key, action);
        return this;
    }

    /**
     * An appearance characteristics dictionary containing additional information for constructing the
     * annotation’s appearance stream. See ISO-320001, Table 189.
     *
     * @return an appearance characteristics dictionary or null if it isn't specified.
     */
    public PdfDictionary getAppearanceCharacteristics() {
        return getPdfObject().getAsDictionary(PdfName.MK);
    }

    /**
     * Sets an appearance characteristics dictionary containing additional information for constructing the
     * annotation’s appearance stream. See ISO-320001, Table 189.
     *
     * @param characteristics the {@link PdfDictionary} with additional information for appearance stream.
     * @return this {@link PdfScreenAnnotation} instance.
     */
    public PdfScreenAnnotation setAppearanceCharacteristics(PdfDictionary characteristics) {
        return (PdfScreenAnnotation) put(PdfName.MK, characteristics);
    }
}
