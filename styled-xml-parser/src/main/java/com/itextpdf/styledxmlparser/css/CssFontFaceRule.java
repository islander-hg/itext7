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
package com.itextpdf.styledxmlparser.css;

import com.itextpdf.layout.font.Range;
import com.itextpdf.styledxmlparser.css.util.CssUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to store a CSS font face At rule.
 */
public class CssFontFaceRule extends CssNestedAtRule {

    /**
     * Properties in the form of a list of CSS declarations.
     */
    private List<CssDeclaration> properties;

    /**
     * Instantiates a new CSS font face rule.
     */
    public CssFontFaceRule() {
        this("");
    }

    /**
     * Instantiates a new CSS font face rule.
     *
     * @param ruleParameters the rule parameters
     * @deprecated Will be removed in 7.2. Use {@link #CssFontFaceRule()} instead
     */
    @Deprecated
    public CssFontFaceRule(String ruleParameters) {
        super(CssRuleName.FONT_FACE, ruleParameters);
    }

    /**
     * Gets the properties.
     *
     * @return the properties
     */
    public List<CssDeclaration> getProperties() {
        return new ArrayList<>(properties) ;
    }

    /* (non-Javadoc)
     * @see com.itextpdf.styledxmlparser.css.CssNestedAtRule#addBodyCssDeclarations(java.util.List)
     */
    @Override
    public void addBodyCssDeclarations(List<CssDeclaration> cssDeclarations) {
        properties = new ArrayList<>(cssDeclarations);
    }

    /* (non-Javadoc)
     * @see com.itextpdf.styledxmlparser.css.CssNestedAtRule#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("@").append(getRuleName()).append(" {").append("\n");
        for (CssDeclaration declaration : properties) {
            sb.append("    ");
            sb.append(declaration);
            sb.append(";\n");
        }
        sb.append("}");
        return sb.toString();
    }

    public Range resolveUnicodeRange() {
        Range range = null;
        for (CssDeclaration descriptor : getProperties()) {
            if ("unicode-range".equals(descriptor.getProperty())) {
                range = CssUtils.parseUnicodeRange(descriptor.getExpression());
            }
        }
        return range;
    }
}
