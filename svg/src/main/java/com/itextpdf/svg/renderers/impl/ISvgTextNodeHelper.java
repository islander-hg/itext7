/*
    This file is part of the iText (R) project.
    Copyright (c) 1998-2020 iText Group NV
    Authors: iText Software.

    This program is offered under a commercial and under the AGPL license.
    For commercial licensing, contact us at https://itextpdf.com/sales.  For AGPL licensing, see below.

    AGPL licensing:
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.itextpdf.svg.renderers.impl;

import com.itextpdf.kernel.geom.Point;
import com.itextpdf.svg.renderers.SvgDrawContext;
import com.itextpdf.svg.utils.TextRectangle;


/**
 * An interface containing a method to simplify working with SVG text elements.
 * Must be removed in update 7.3 as the methods of this interface will be moved to {@link ISvgTextNodeRenderer}
 */
@Deprecated
public interface ISvgTextNodeHelper {
    /**
     * Return the bounding rectangle of the text element.
     *
     * @param context current {@link SvgDrawContext}
     * @param basePoint end point of previous text element
     * @return created instance of {@link TextRectangle}
     */
    // TODO DEVSIX-3814 This method should be moved to ISvgTextNodeRenderer in 7.2 and this class should be removed
    TextRectangle getTextRectangle(SvgDrawContext context, Point basePoint);
}
