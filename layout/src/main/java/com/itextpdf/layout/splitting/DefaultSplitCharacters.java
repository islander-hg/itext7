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
package com.itextpdf.layout.splitting;

import com.itextpdf.io.font.otf.GlyphLine;

/**
 * The default implementation of {@link ISplitCharacters} interface.
 */
public class DefaultSplitCharacters implements ISplitCharacters {

    @Override
    public boolean isSplitCharacter(GlyphLine text, int glyphPos) {
        if (!text.get(glyphPos).hasValidUnicode()) {
            return false;
        }
        int charCode = text.get(glyphPos).getUnicode();
        //Check if a hyphen proceeds a digit to denote negative value
        // TODO: DEVSIX-4863 why is glyphPos == 0? negative value could be preceded by a whitespace!
        if ((glyphPos == 0) && (charCode == '-') && (text.size() - 1 > glyphPos) && (isADigitChar(text, glyphPos + 1))) {
            return false;
        }
        return (charCode <= ' ' || charCode == '-' || charCode == '\u2010'
                // block of whitespaces
                || (charCode >= 0x2002 && charCode <= 0x200b)
                || (charCode >= 0x2e80 && charCode < 0xd7a0)
                || (charCode >= 0xf900 && charCode < 0xfb00)
                || (charCode >= 0xfe30 && charCode < 0xfe50)
                || (charCode >= 0xff61 && charCode < 0xffa0));
    }

    private boolean isADigitChar(GlyphLine text, int glyphPos) {
       return Character.isDigit(text.get(glyphPos).getChars()[0]);
    }
}
