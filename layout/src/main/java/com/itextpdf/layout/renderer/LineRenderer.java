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
package com.itextpdf.layout.renderer;

import com.itextpdf.io.LogMessageConstant;
import com.itextpdf.io.font.otf.Glyph;
import com.itextpdf.io.font.otf.GlyphLine;
import com.itextpdf.io.util.ArrayUtil;
import com.itextpdf.io.util.MessageFormatUtil;
import com.itextpdf.io.util.TextUtil;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.layout.element.TabStop;
import com.itextpdf.layout.layout.LayoutArea;
import com.itextpdf.layout.layout.LayoutContext;
import com.itextpdf.layout.layout.LayoutResult;
import com.itextpdf.layout.layout.LineLayoutContext;
import com.itextpdf.layout.layout.LineLayoutResult;
import com.itextpdf.layout.layout.MinMaxWidthLayoutResult;
import com.itextpdf.layout.layout.TextLayoutResult;
import com.itextpdf.layout.minmaxwidth.MinMaxWidth;
import com.itextpdf.layout.minmaxwidth.MinMaxWidthUtils;
import com.itextpdf.layout.property.BaseDirection;
import com.itextpdf.layout.property.FloatPropertyValue;
import com.itextpdf.layout.property.Leading;
import com.itextpdf.layout.property.OverflowPropertyValue;
import com.itextpdf.layout.property.Property;
import com.itextpdf.layout.property.RenderingMode;
import com.itextpdf.layout.property.TabAlignment;
import com.itextpdf.layout.property.UnitValue;
import com.itextpdf.layout.renderer.TextSequenceWordWrapping.LastFittingChildRendererData;
import com.itextpdf.layout.renderer.TextSequenceWordWrapping.MinMaxWidthOfTextRendererSequenceHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LineRenderer extends AbstractRenderer {

    // AbstractRenderer.EPS is not enough here
    private static final float MIN_MAX_WIDTH_CORRECTION_EPS = 0.001f;

    private static final Logger logger = LoggerFactory.getLogger(LineRenderer.class);

    protected float maxAscent;
    protected float maxDescent;

    // bidi levels
    protected byte[] levels;

    float maxTextAscent;
    float maxTextDescent;
    private float maxBlockAscent;
    private float maxBlockDescent;

    @Override
    public LayoutResult layout(LayoutContext layoutContext) {
        boolean textSequenceOverflowXProcessing = false;
        int firstChildToRelayout = -1;

        Rectangle layoutBox = layoutContext.getArea().getBBox().clone();
        boolean wasParentsHeightClipped = layoutContext.isClippedHeight();
        List<Rectangle> floatRendererAreas = layoutContext.getFloatRendererAreas();

        OverflowPropertyValue oldXOverflow = null;
        boolean wasXOverflowChanged = false;
        boolean floatsPlacedBeforeLine = false;

        if (floatRendererAreas != null) {
            float layoutWidth = layoutBox.getWidth();
            float layoutHeight = layoutBox.getHeight();
            // consider returning some value to check if layoutBox has been changed due to floats,
            // than reuse on non-float layout: kind of not first piece of content on the line
            FloatingHelper.adjustLineAreaAccordingToFloats(floatRendererAreas, layoutBox);
            if (layoutWidth > layoutBox.getWidth() || layoutHeight > layoutBox.getHeight()) {
                floatsPlacedBeforeLine = true;
                oldXOverflow = this.<OverflowPropertyValue>getProperty(Property.OVERFLOW_X);
                wasXOverflowChanged = true;
                setProperty(Property.OVERFLOW_X, OverflowPropertyValue.FIT);
            }
        }

        boolean noSoftWrap = Boolean.TRUE.equals(this.<Boolean>getOwnProperty(Property.NO_SOFT_WRAP_INLINE));

        LineLayoutContext lineLayoutContext = layoutContext instanceof LineLayoutContext ? (LineLayoutContext) layoutContext : new LineLayoutContext(layoutContext);
        if (lineLayoutContext.getTextIndent() != 0) {
            layoutBox
                    .moveRight(lineLayoutContext.getTextIndent())
                    .setWidth(layoutBox.getWidth() - lineLayoutContext.getTextIndent());
        }

        occupiedArea = new LayoutArea(layoutContext.getArea().getPageNumber(), layoutBox.clone().moveUp(layoutBox.getHeight()).setHeight(0).setWidth(0));

        updateChildrenParent();

        TargetCounterHandler.addPageByID(this);

        float curWidth = 0;
        if (RenderingMode.HTML_MODE.equals(this.<RenderingMode>getProperty(Property.RENDERING_MODE))
                && hasChildRendererInHtmlMode()) {
            float[] ascenderDescender = LineHeightHelper.getActualAscenderDescender(this);
            maxAscent = ascenderDescender[0];
            maxDescent = ascenderDescender[1];
        } else {
            maxAscent = 0;
            maxDescent = 0;
        }
        maxTextAscent = 0;
        maxTextDescent = 0;
        maxBlockAscent = -1e20f;
        maxBlockDescent = 1e20f;

        int childPos = 0;

        MinMaxWidth minMaxWidth = new MinMaxWidth();
        AbstractWidthHandler widthHandler;
        if (noSoftWrap) {
            widthHandler = new SumSumWidthHandler(minMaxWidth);
        } else {
            widthHandler = new MaxSumWidthHandler(minMaxWidth);
        }

        resolveChildrenFonts();

        int totalNumberOfTrimmedGlyphs = trimFirst();

        BaseDirection baseDirection = applyOtf();

        updateBidiLevels(totalNumberOfTrimmedGlyphs, baseDirection);

        boolean anythingPlaced = false;
        TabStop hangingTabStop = null;
        LineLayoutResult result = null;

        boolean floatsPlacedInLine = false;
        Map<Integer, IRenderer> floatsToNextPageSplitRenderers = new LinkedHashMap<>();
        List<IRenderer> floatsToNextPageOverflowRenderers = new ArrayList<>();
        List<IRenderer> floatsOverflowedToNextLine = new ArrayList<>();
        int lastTabIndex = 0;

        Map<Integer, LayoutResult> specialScriptLayoutResults = new HashMap<>();
        Map<Integer, LayoutResult> textRendererLayoutResults = new HashMap<>();

        Map<Integer, float[]> textRendererSequenceAscentDescent = new HashMap<>();
        LineAscentDescentState lineAscentDescentStateBeforeTextRendererSequence = null;

        MinMaxWidthOfTextRendererSequenceHelper minMaxWidthOfTextRendererSequenceHelper = null;

        while (childPos < childRenderers.size()) {
            IRenderer childRenderer = childRenderers.get(childPos);
            LayoutResult childResult = null;
            Rectangle bbox = new Rectangle(layoutBox.getX() + curWidth, layoutBox.getY(), layoutBox.getWidth() - curWidth, layoutBox.getHeight());

            RenderingMode childRenderingMode = childRenderer.<RenderingMode>getProperty(Property.RENDERING_MODE);

            if (TextSequenceWordWrapping.isTextRendererAndRequiresSpecialScriptPreLayoutProcessing(childRenderer)
                    && TypographyUtils.isPdfCalligraphAvailable()) {
                TextSequenceWordWrapping.processSpecialScriptPreLayout(this, childPos);
            }
            TextSequenceWordWrapping.resetTextSequenceIfItEnded(
                    specialScriptLayoutResults, true, childRenderer, childPos,
                    minMaxWidthOfTextRendererSequenceHelper, noSoftWrap, widthHandler);
            TextSequenceWordWrapping.resetTextSequenceIfItEnded(
                    textRendererLayoutResults, false, childRenderer, childPos,
                    minMaxWidthOfTextRendererSequenceHelper, noSoftWrap, widthHandler);

            if (childRenderer instanceof TextRenderer) {
                // Delete these properties in case of relayout. We might have applied them during justify().
                childRenderer.deleteOwnProperty(Property.CHARACTER_SPACING);
                childRenderer.deleteOwnProperty(Property.WORD_SPACING);
            } else if (childRenderer instanceof TabRenderer) {
                if (hangingTabStop != null) {
                    IRenderer tabRenderer = childRenderers.get(childPos - 1);
                    tabRenderer.layout(new LayoutContext(new LayoutArea(layoutContext.getArea().getPageNumber(), bbox), wasParentsHeightClipped));
                    curWidth += tabRenderer.getOccupiedArea().getBBox().getWidth();
                    widthHandler.updateMaxChildWidth(tabRenderer.getOccupiedArea().getBBox().getWidth());
                }
                hangingTabStop = calculateTab(childRenderer, curWidth, layoutBox.getWidth());
                if (childPos == childRenderers.size() - 1)
                    hangingTabStop = null;
                if (hangingTabStop != null) {
                    lastTabIndex = childPos;
                    ++childPos;
                    continue;
                }
            }

            if (hangingTabStop != null && hangingTabStop.getTabAlignment() == TabAlignment.ANCHOR
                    && childRenderer instanceof TextRenderer) {
                childRenderer.setProperty(Property.TAB_ANCHOR, hangingTabStop.getTabAnchor());
            }

            // Normalize child width
            Object childWidth = childRenderer.<Object>getProperty(Property.WIDTH);
            boolean childWidthWasReplaced = false;
            boolean childRendererHasOwnWidthProperty = childRenderer.hasOwnProperty(Property.WIDTH);
            if (childWidth instanceof UnitValue && ((UnitValue) childWidth).isPercentValue()) {
                float normalizedChildWidth = ((UnitValue) childWidth).getValue() / 100 * layoutContext.getArea().getBBox().getWidth();
                normalizedChildWidth = decreaseRelativeWidthByChildAdditionalWidth(childRenderer, normalizedChildWidth);

                if (normalizedChildWidth > 0) {
                    childRenderer.setProperty(Property.WIDTH, UnitValue.createPointValue(normalizedChildWidth));
                    childWidthWasReplaced = true;
                }
            }

            FloatPropertyValue kidFloatPropertyVal = childRenderer.<FloatPropertyValue>getProperty(Property.FLOAT);
            boolean isChildFloating = childRenderer instanceof AbstractRenderer && FloatingHelper.isRendererFloating(childRenderer, kidFloatPropertyVal);
            if (isChildFloating) {
                childResult = null;
                MinMaxWidth kidMinMaxWidth = FloatingHelper.calculateMinMaxWidthForFloat((AbstractRenderer) childRenderer, kidFloatPropertyVal);
                float floatingBoxFullWidth = kidMinMaxWidth.getMaxWidth();
                // TODO width will be recalculated on float layout;
                // also not taking it into account (i.e. not setting it on child renderer) results in differences with html
                // when floating span is split on other line;
                // TODO DEVSIX-1730: may be process floating spans as inline blocks always?

                if (!wasXOverflowChanged && childPos > 0) {
                    oldXOverflow = this.<OverflowPropertyValue>getProperty(Property.OVERFLOW_X);
                    wasXOverflowChanged = true;
                    setProperty(Property.OVERFLOW_X, OverflowPropertyValue.FIT);
                }
                if (!lineLayoutContext.isFloatOverflowedToNextPageWithNothing() && floatsOverflowedToNextLine.isEmpty()
                        && (!anythingPlaced || floatingBoxFullWidth <= bbox.getWidth())) {
                    childResult = childRenderer.layout(new LayoutContext(new LayoutArea(layoutContext.getArea().getPageNumber(), layoutContext.getArea().getBBox().clone()), null, floatRendererAreas, wasParentsHeightClipped));
                }
                // Get back child width so that it's not lost
                if (childWidthWasReplaced) {
                    if (childRendererHasOwnWidthProperty) {
                        childRenderer.setProperty(Property.WIDTH, childWidth);
                    } else {
                        childRenderer.deleteOwnProperty(Property.WIDTH);
                    }
                }

                float minChildWidth = 0;
                float maxChildWidth = 0;
                if (childResult instanceof MinMaxWidthLayoutResult) {
                    if (!childWidthWasReplaced) {
                        minChildWidth = ((MinMaxWidthLayoutResult) childResult).getMinMaxWidth().getMinWidth();
                    }
                    // TODO if percents width was used, max width might be huge
                    maxChildWidth = ((MinMaxWidthLayoutResult) childResult).getMinMaxWidth().getMaxWidth();
                    widthHandler.updateMinChildWidth(minChildWidth + AbstractRenderer.EPS);
                    widthHandler.updateMaxChildWidth(maxChildWidth + AbstractRenderer.EPS);
                } else {
                    widthHandler.updateMinChildWidth(kidMinMaxWidth.getMinWidth() + AbstractRenderer.EPS);
                    widthHandler.updateMaxChildWidth(kidMinMaxWidth.getMaxWidth() + AbstractRenderer.EPS);
                }

                if (childResult == null && !lineLayoutContext.isFloatOverflowedToNextPageWithNothing()) {
                    floatsOverflowedToNextLine.add(childRenderer);
                } else if (lineLayoutContext.isFloatOverflowedToNextPageWithNothing() || childResult.getStatus() == LayoutResult.NOTHING) {
                    floatsToNextPageSplitRenderers.put(childPos, null);
                    floatsToNextPageOverflowRenderers.add(childRenderer);
                    lineLayoutContext.setFloatOverflowedToNextPageWithNothing(true);
                } else if (childResult.getStatus() == LayoutResult.PARTIAL) {
                    floatsPlacedInLine = true;

                    if (childRenderer instanceof TextRenderer) {
                        // This code is specifically for floating inline text elements:
                        // inline elements cannot have fixed width, also they progress horizontally, which means
                        // that if they don't fit in one line, they will definitely be moved onto the new line (and also
                        // under all floats). Specifying the whole width of layout area is required to avoid possible normal
                        // content wrapping around floating text in case floating text gets wrapped onto the next line
                        // not evenly.
                        LineRenderer[] split = splitNotFittingFloat(childPos, childResult);
                        IRenderer splitRenderer = childResult.getSplitRenderer();
                        if (splitRenderer instanceof TextRenderer) {
                            ((TextRenderer) splitRenderer).trimFirst();
                            ((TextRenderer) splitRenderer).trimLast();
                        }
                        // ensure no other thing (like text wrapping the float) will occupy the line
                        splitRenderer.getOccupiedArea().getBBox().setWidth(layoutContext.getArea().getBBox().getWidth());
                        result = new LineLayoutResult(LayoutResult.PARTIAL, occupiedArea, split[0], split[1], null);
                        break;
                    } else {
                        floatsToNextPageSplitRenderers.put(childPos, childResult.getSplitRenderer());
                        floatsToNextPageOverflowRenderers.add(childResult.getOverflowRenderer());
                        adjustLineOnFloatPlaced(layoutBox, childPos, kidFloatPropertyVal, childResult.getSplitRenderer().getOccupiedArea().getBBox());
                    }
                } else {
                    floatsPlacedInLine = true;

                    if (childRenderer instanceof TextRenderer) {
                        ((TextRenderer) childRenderer).trimFirst();
                        ((TextRenderer) childRenderer).trimLast();
                    }

                    adjustLineOnFloatPlaced(layoutBox, childPos, kidFloatPropertyVal, childRenderer.getOccupiedArea().getBBox());
                }

                childPos++;
                if (!anythingPlaced && childResult != null && childResult.getStatus() == LayoutResult.NOTHING && floatRendererAreas.isEmpty()) {
                    if (isFirstOnRootArea()) {
                        // Current line is empty, kid returns nothing and neither floats nor content
                        // were met on root area (e.g. page area) - return NOTHING, don't layout other line content,
                        // expect FORCED_PLACEMENT to be set.
                        break;
                    }
                }
                continue;
            }

            MinMaxWidth childBlockMinMaxWidth = null;
            boolean isInlineBlockChild = isInlineBlockChild(childRenderer);
            if (!childWidthWasReplaced) {
                if (isInlineBlockChild && childRenderer instanceof AbstractRenderer) {
                    childBlockMinMaxWidth = ((AbstractRenderer) childRenderer).getMinMaxWidth();
                    float childMaxWidth = childBlockMinMaxWidth.getMaxWidth();
                    float lineFullAvailableWidth = layoutContext.getArea().getBBox().getWidth() - lineLayoutContext.getTextIndent();
                    if (!noSoftWrap && childMaxWidth > bbox.getWidth() + MIN_MAX_WIDTH_CORRECTION_EPS && bbox.getWidth() != lineFullAvailableWidth) {
                        childResult = new LineLayoutResult(LayoutResult.NOTHING, null, null, childRenderer, childRenderer);
                    } else {
                        childMaxWidth += MIN_MAX_WIDTH_CORRECTION_EPS;
                        float inlineBlockWidth = Math.min(childMaxWidth, lineFullAvailableWidth);

                        if (!isOverflowFit(this.<OverflowPropertyValue>getProperty(Property.OVERFLOW_X))) {
                            float childMinWidth = childBlockMinMaxWidth.getMinWidth() + MIN_MAX_WIDTH_CORRECTION_EPS;
                            inlineBlockWidth = Math.max(childMinWidth, inlineBlockWidth);
                        }
                        bbox.setWidth(inlineBlockWidth);

                        if (childBlockMinMaxWidth.getMinWidth() > bbox.getWidth()) {
                            if (logger.isWarnEnabled()) {
                                logger.warn(LogMessageConstant.INLINE_BLOCK_ELEMENT_WILL_BE_CLIPPED);
                            }
                            childRenderer.setProperty(Property.FORCED_PLACEMENT, true);
                        }
                    }
                    childBlockMinMaxWidth.setChildrenMaxWidth(childBlockMinMaxWidth.getChildrenMaxWidth() + MIN_MAX_WIDTH_CORRECTION_EPS);
                    childBlockMinMaxWidth.setChildrenMinWidth(childBlockMinMaxWidth.getChildrenMinWidth() + MIN_MAX_WIDTH_CORRECTION_EPS);
                }
            }


            boolean shouldBreakLayouting = false;

            if (childResult == null) {
                boolean setOverflowFitCausedBySpecialScripts = childRenderer instanceof TextRenderer
                        && ((TextRenderer) childRenderer).textContainsSpecialScriptGlyphs(true);

                boolean setOverflowFitCausedByTextRendererInHtmlMode = RenderingMode.HTML_MODE == childRenderingMode
                        && childRenderer instanceof TextRenderer
                        && !((TextRenderer) childRenderer).textContainsSpecialScriptGlyphs(true);

                if (!wasXOverflowChanged
                        && (childPos > 0 || setOverflowFitCausedBySpecialScripts || setOverflowFitCausedByTextRendererInHtmlMode)
                        && !textSequenceOverflowXProcessing) {
                    oldXOverflow = this.<OverflowPropertyValue>getProperty(Property.OVERFLOW_X);
                    wasXOverflowChanged = true;
                    setProperty(Property.OVERFLOW_X, OverflowPropertyValue.FIT);
                }

                TextSequenceWordWrapping.preprocessTextSequenceOverflowX(this, textSequenceOverflowXProcessing,
                        childRenderer, wasXOverflowChanged, oldXOverflow);

                childResult = childRenderer.layout(new LayoutContext(new LayoutArea(layoutContext.getArea().getPageNumber(), bbox), wasParentsHeightClipped));

                shouldBreakLayouting = TextSequenceWordWrapping.postprocessTextSequenceOverflowX(
                        this, textSequenceOverflowXProcessing,
                        childPos, childRenderer, childResult, wasXOverflowChanged);

                TextSequenceWordWrapping.updateTextSequenceLayoutResults(
                        textRendererLayoutResults, false, childRenderer, childPos, childResult);
                TextSequenceWordWrapping.updateTextSequenceLayoutResults(
                        specialScriptLayoutResults, true, childRenderer, childPos, childResult);

                // it means that we've already increased layout area by MIN_MAX_WIDTH_CORRECTION_EPS
                if (childResult instanceof MinMaxWidthLayoutResult && null != childBlockMinMaxWidth) {
                    MinMaxWidth childResultMinMaxWidth = ((MinMaxWidthLayoutResult) childResult).getMinMaxWidth();
                    childResultMinMaxWidth.setChildrenMaxWidth(childResultMinMaxWidth.getChildrenMaxWidth() + MIN_MAX_WIDTH_CORRECTION_EPS);
                    childResultMinMaxWidth.setChildrenMinWidth(childResultMinMaxWidth.getChildrenMinWidth() + MIN_MAX_WIDTH_CORRECTION_EPS);
                }
            }

            // Get back child width so that it's not lost
            if (childWidthWasReplaced) {
                if (childRendererHasOwnWidthProperty) {
                    childRenderer.setProperty(Property.WIDTH, childWidth);
                } else {
                    childRenderer.deleteOwnProperty(Property.WIDTH);
                }
            }

            float minChildWidth = 0;
            float maxChildWidth = 0;
            if (childResult instanceof MinMaxWidthLayoutResult) {
                if (!childWidthWasReplaced) {
                    minChildWidth = ((MinMaxWidthLayoutResult) childResult).getMinMaxWidth().getMinWidth();
                }
                maxChildWidth = ((MinMaxWidthLayoutResult) childResult).getMinMaxWidth().getMaxWidth();
            } else if (childBlockMinMaxWidth != null) {
                minChildWidth = childBlockMinMaxWidth.getMinWidth();
                maxChildWidth = childBlockMinMaxWidth.getMaxWidth();
            }

            float[] childAscentDescent = getAscentDescentOfLayoutedChildRenderer(childRenderer, childResult,
                    childRenderingMode, isInlineBlockChild);

            lineAscentDescentStateBeforeTextRendererSequence =
                    TextSequenceWordWrapping.updateTextRendererSequenceAscentDescent(
                            this, textRendererSequenceAscentDescent, childPos, childAscentDescent,
                            lineAscentDescentStateBeforeTextRendererSequence);

            minMaxWidthOfTextRendererSequenceHelper =
                    TextSequenceWordWrapping.updateTextRendererSequenceMinMaxWidth(
                            this, widthHandler, childPos,
                            minMaxWidthOfTextRendererSequenceHelper, anythingPlaced, textRendererLayoutResults,
                            specialScriptLayoutResults, lineLayoutContext.getTextIndent());

            boolean newLineOccurred = (childResult instanceof TextLayoutResult && ((TextLayoutResult) childResult).isSplitForcedByNewline());
            if (!shouldBreakLayouting) {
                shouldBreakLayouting = childResult.getStatus() != LayoutResult.FULL || newLineOccurred;
            }

            boolean shouldBreakLayoutingOnTextRenderer = shouldBreakLayouting
                    && childResult instanceof TextLayoutResult;
            boolean forceOverflowForTextRendererPartialResult = false;

            if (shouldBreakLayoutingOnTextRenderer) {
                boolean isWordHasBeenSplitLayoutRenderingMode = ((TextLayoutResult) childResult).isWordHasBeenSplit()
                        && RenderingMode.HTML_MODE != childRenderingMode
                        && !((TextRenderer) childRenderer).textContainsSpecialScriptGlyphs(true);
                boolean enableSpecialScriptsWrapping = ((TextRenderer) childRenderers.get(childPos))
                        .textContainsSpecialScriptGlyphs(true)
                        && !textSequenceOverflowXProcessing && !newLineOccurred;
                boolean enableTextSequenceWrapping = RenderingMode.HTML_MODE == childRenderingMode && !newLineOccurred
                        && !textSequenceOverflowXProcessing;

                if (isWordHasBeenSplitLayoutRenderingMode) {
                    forceOverflowForTextRendererPartialResult = isForceOverflowForTextRendererPartialResult(
                            childRenderer, wasXOverflowChanged, oldXOverflow, layoutContext, layoutBox,
                            wasParentsHeightClipped);
                } else if (enableSpecialScriptsWrapping) {
                    boolean isOverflowFit = wasXOverflowChanged
                            ? (oldXOverflow == OverflowPropertyValue.FIT)
                            : isOverflowFit(this.<OverflowPropertyValue>getProperty(Property.OVERFLOW_X));
                    LastFittingChildRendererData lastFittingChildRendererData =
                            TextSequenceWordWrapping.getIndexAndLayoutResultOfTheLastTextRendererContainingSpecialScripts(
                                    this, childPos,
                                    specialScriptLayoutResults, wasParentsHeightClipped,
                                    isOverflowFit);

                    if (lastFittingChildRendererData == null) {
                        textSequenceOverflowXProcessing = true;
                        shouldBreakLayouting = false;
                        firstChildToRelayout = childPos;
                    } else {
                        curWidth -= TextSequenceWordWrapping.getCurWidthRelayoutedTextSequenceDecrement(childPos,
                                lastFittingChildRendererData.childIndex, specialScriptLayoutResults);
                        childPos = lastFittingChildRendererData.childIndex;
                        childResult = lastFittingChildRendererData.childLayoutResult;
                        specialScriptLayoutResults.put(childPos, childResult);

                        MinMaxWidth textSequenceElemminMaxWidth = ((MinMaxWidthLayoutResult) childResult).getMinMaxWidth();
                        minChildWidth = textSequenceElemminMaxWidth.getMinWidth();
                        maxChildWidth = textSequenceElemminMaxWidth.getMaxWidth();
                    }
                } else if (enableTextSequenceWrapping) {
                    boolean isOverflowFit = wasXOverflowChanged
                            ? (oldXOverflow == OverflowPropertyValue.FIT)
                            : isOverflowFit(this.<OverflowPropertyValue>getProperty(Property.OVERFLOW_X));
                    LastFittingChildRendererData lastFittingChildRendererData =
                            TextSequenceWordWrapping.getIndexAndLayoutResultOfTheLastTextRendererWithNoSpecialScripts(
                                    this, childPos,
                                    textRendererLayoutResults, wasParentsHeightClipped,
                                    isOverflowFit, floatsPlacedInLine || floatsPlacedBeforeLine);
                    if (lastFittingChildRendererData == null) {
                        textSequenceOverflowXProcessing = true;
                        shouldBreakLayouting = false;
                        firstChildToRelayout = childPos;
                    } else {
                        curWidth -= TextSequenceWordWrapping.getCurWidthRelayoutedTextSequenceDecrement(childPos,
                                lastFittingChildRendererData.childIndex, textRendererLayoutResults);
                        childAscentDescent =
                                updateAscentDescentAfterTextRendererSequenceProcessing(
                                        (lastFittingChildRendererData.childLayoutResult.getStatus()
                                                == LayoutResult.NOTHING)
                                                ? (lastFittingChildRendererData.childIndex - 1)
                                                : lastFittingChildRendererData.childIndex,
                                        lineAscentDescentStateBeforeTextRendererSequence,
                                        textRendererSequenceAscentDescent);

                        childPos = lastFittingChildRendererData.childIndex;
                        childResult = lastFittingChildRendererData.childLayoutResult;
                        textRendererLayoutResults.put(childPos, childResult);

                        MinMaxWidth textSequenceElemminMaxWidth = ((MinMaxWidthLayoutResult) childResult).getMinMaxWidth();
                        minChildWidth = textSequenceElemminMaxWidth.getMinWidth();
                        maxChildWidth = textSequenceElemminMaxWidth.getMaxWidth();
                    }
                }
            }

            if (childPos != firstChildToRelayout) {
                if (!forceOverflowForTextRendererPartialResult) {
                    updateAscentDescentAfterChildLayout(childAscentDescent, childRenderer, isChildFloating);
                }
                float maxHeight = maxAscent - maxDescent;

                float currChildTextIndent = anythingPlaced ? 0 : lineLayoutContext.getTextIndent();
                if (hangingTabStop != null
                        && (TabAlignment.LEFT == hangingTabStop.getTabAlignment() || shouldBreakLayouting || childRenderers.size() - 1 == childPos || childRenderers.get(childPos + 1) instanceof TabRenderer)) {
                    IRenderer tabRenderer = childRenderers.get(lastTabIndex);
                    List<IRenderer> affectedRenderers = new ArrayList<>();
                    affectedRenderers.addAll(childRenderers.subList(lastTabIndex + 1, childPos + 1));
                    float tabWidth = calculateTab(layoutBox, curWidth, hangingTabStop, affectedRenderers, tabRenderer);

                    tabRenderer.layout(new LayoutContext(new LayoutArea(layoutContext.getArea().getPageNumber(), bbox), wasParentsHeightClipped));
                    float sumOfAffectedRendererWidths = 0;
                    for (IRenderer renderer : affectedRenderers) {
                        renderer.move(tabWidth + sumOfAffectedRendererWidths, 0);
                        sumOfAffectedRendererWidths += renderer.getOccupiedArea().getBBox().getWidth();
                    }
                    if (childResult.getSplitRenderer() != null) {
                        childResult.getSplitRenderer().move(tabWidth + sumOfAffectedRendererWidths - childResult.getSplitRenderer().getOccupiedArea().getBBox().getWidth(), 0);
                    }
                    float tabAndNextElemWidth = tabWidth + childResult.getOccupiedArea().getBBox().getWidth();
                    if (hangingTabStop.getTabAlignment() == TabAlignment.RIGHT && curWidth + tabAndNextElemWidth < hangingTabStop.getTabPosition()) {
                        curWidth = hangingTabStop.getTabPosition();
                    } else {
                        curWidth += tabAndNextElemWidth;
                    }
                    widthHandler.updateMinChildWidth(minChildWidth + currChildTextIndent);
                    widthHandler.updateMaxChildWidth(tabWidth + maxChildWidth + currChildTextIndent);
                    hangingTabStop = null;
                } else if (null == hangingTabStop) {
                    if (childResult.getOccupiedArea() != null && childResult.getOccupiedArea().getBBox() != null) {
                        curWidth += childResult.getOccupiedArea().getBBox().getWidth();
                    }
                    widthHandler.updateMinChildWidth(minChildWidth + currChildTextIndent);
                    widthHandler.updateMaxChildWidth(maxChildWidth + currChildTextIndent);
                }
                if (!forceOverflowForTextRendererPartialResult) {
                    occupiedArea.setBBox(new Rectangle(layoutBox.getX(), layoutBox.getY() + layoutBox.getHeight() - maxHeight, curWidth, maxHeight));
                }
            }

            if (shouldBreakLayouting) {
                LineRenderer[] split = split();
                split[0].childRenderers = new ArrayList<>(childRenderers.subList(0, childPos));

                if (forceOverflowForTextRendererPartialResult) {
                    split[1].childRenderers.add(childRenderer);
                    split[1].childRenderers.addAll(childRenderers.subList(childPos + 1, childRenderers.size()));
                } else {
                    boolean forcePlacement = Boolean.TRUE.equals(getPropertyAsBoolean(Property.FORCED_PLACEMENT));
                    boolean isInlineBlockAndFirstOnRootArea = isInlineBlockChild && isFirstOnRootArea();
                    if (childResult.getStatus() == LayoutResult.PARTIAL && (!isInlineBlockChild || forcePlacement || isInlineBlockAndFirstOnRootArea) || childResult.getStatus() == LayoutResult.FULL) {
                        split[0].addChild(childResult.getSplitRenderer());
                        anythingPlaced = true;
                    }

                    if (null != childResult.getOverflowRenderer()) {
                        if (isInlineBlockChild && !forcePlacement && !isInlineBlockAndFirstOnRootArea) {
                            split[1].childRenderers.add(childRenderer);
                        } else {
                            if (isInlineBlockChild && childResult.getOverflowRenderer().getChildRenderers().size() == 0
                                    && childResult.getStatus() == LayoutResult.PARTIAL) {
                                if (logger.isWarnEnabled()) {
                                    logger.warn(LogMessageConstant.INLINE_BLOCK_ELEMENT_WILL_BE_CLIPPED);
                                }
                            } else {
                                split[1].childRenderers.add(childResult.getOverflowRenderer());
                            }
                        }
                    }
                    split[1].childRenderers.addAll(childRenderers.subList(childPos + 1, childRenderers.size()));
                }

                replaceSplitRendererKidFloats(floatsToNextPageSplitRenderers, split[0]);
                split[0].childRenderers.removeAll(floatsOverflowedToNextLine);
                split[1].childRenderers.addAll(0, floatsOverflowedToNextLine);

                // no sense to process empty renderer
                if (split[1].childRenderers.size() == 0 && floatsToNextPageOverflowRenderers.isEmpty()) {
                    split[1] = null;
                }

                IRenderer causeOfNothing = childResult.getStatus() == LayoutResult.NOTHING ? childResult.getCauseOfNothing() : childRenderers.get(childPos);
                if (split[1] == null) {
                    result = new LineLayoutResult(LayoutResult.FULL, occupiedArea, split[0], split[1], causeOfNothing);
                } else {
                    if (anythingPlaced || floatsPlacedInLine) {
                        result = new LineLayoutResult(LayoutResult.PARTIAL, occupiedArea, split[0], split[1], causeOfNothing);
                    } else {
                        result = new LineLayoutResult(LayoutResult.NOTHING, null, split[0], split[1], null);
                    }
                    result.setFloatsOverflowedToNextPage(floatsToNextPageOverflowRenderers);
                }
                if (newLineOccurred) {
                    result.setSplitForcedByNewline(true);
                }

                break;
            } else {
                if (childPos == firstChildToRelayout) {
                    firstChildToRelayout = -1;
                } else {
                    anythingPlaced = true;
                    childPos++;
                }
            }
        }

        TextSequenceWordWrapping.resetTextSequenceIfItEnded(specialScriptLayoutResults, true, null, childPos,
                minMaxWidthOfTextRendererSequenceHelper, noSoftWrap, widthHandler);
        TextSequenceWordWrapping.resetTextSequenceIfItEnded(textRendererLayoutResults, false, null, childPos,
                minMaxWidthOfTextRendererSequenceHelper, noSoftWrap, widthHandler);

        if (result == null) {
            boolean noOverflowedFloats = floatsOverflowedToNextLine.isEmpty() && floatsToNextPageOverflowRenderers.isEmpty();
            if ((anythingPlaced || floatsPlacedInLine) && noOverflowedFloats || 0 == childRenderers.size()) {
                result = new LineLayoutResult(LayoutResult.FULL, occupiedArea, null, null);
            } else {
                if (noOverflowedFloats) {
                    // all kids were some non-image and non-text kids (tab-stops?),
                    // but in this case, it should be okay to return FULL, as there is nothing to be placed
                    result = new LineLayoutResult(LayoutResult.FULL, occupiedArea, null, null);
                } else if (anythingPlaced || floatsPlacedInLine) {
                    LineRenderer[] split = split();
                    split[0].childRenderers.addAll(childRenderers.subList(0, childPos));
                    replaceSplitRendererKidFloats(floatsToNextPageSplitRenderers, split[0]);
                    split[0].childRenderers.removeAll(floatsOverflowedToNextLine);

                    // If `result` variable is null up until now but not everything was placed - there is no
                    // content overflow, only floats are overflowing.
                    // The floatsOverflowedToNextLine might be empty, while the only overflowing floats are
                    // in floatsToNextPageOverflowRenderers. This situation is handled in ParagraphRenderer separately.
                    split[1].childRenderers.addAll(floatsOverflowedToNextLine);
                    result = new LineLayoutResult(LayoutResult.PARTIAL, occupiedArea, split[0], split[1], null);
                    result.setFloatsOverflowedToNextPage(floatsToNextPageOverflowRenderers);
                } else {
                    IRenderer causeOfNothing = floatsOverflowedToNextLine.isEmpty() ? floatsToNextPageOverflowRenderers.get(0) : floatsOverflowedToNextLine.get(0);
                    result = new LineLayoutResult(LayoutResult.NOTHING, null, null, this, causeOfNothing);
                }
            }
        }

        if (baseDirection != null && baseDirection != BaseDirection.NO_BIDI) {
            List<IRenderer> children = null;
            if (result.getStatus() == LayoutResult.PARTIAL) {
                children = result.getSplitRenderer().getChildRenderers();
            } else if (result.getStatus() == LayoutResult.FULL) {
                children = getChildRenderers();
            }

            if (children != null) {
                boolean newLineFound = false;
                List<RendererGlyph> lineGlyphs = new ArrayList<>();

                // We shouldn't forget about images, float, inline-blocks that has to be inserted somewhere.
                // TODO determine correct place to insert this content. Probably consider inline floats separately.
                Map<TextRenderer, List<IRenderer>> insertAfter = new HashMap<>();
                List<IRenderer> starterNonTextRenderers = new ArrayList<>();
                TextRenderer lastTextRenderer = null;

                for (IRenderer child : children) {
                    if (newLineFound) {
                        break;
                    }
                    if (child instanceof TextRenderer) {
                        GlyphLine childLine = ((TextRenderer) child).line;
                        for (int i = childLine.start; i < childLine.end; i++) {
                            if (TextUtil.isNewLine(childLine.get(i))) {
                                newLineFound = true;
                                break;
                            }
                            lineGlyphs.add(new RendererGlyph(childLine.get(i), (TextRenderer) child));
                        }
                        lastTextRenderer = (TextRenderer) child;
                    } else if (lastTextRenderer != null) {
                        if (!insertAfter.containsKey(lastTextRenderer)) {
                            insertAfter.put(lastTextRenderer, new ArrayList<IRenderer>());
                        }
                        insertAfter.get(lastTextRenderer).add(child);
                    } else {
                        starterNonTextRenderers.add(child);
                    }
                }
                byte[] lineLevels = new byte[lineGlyphs.size()];
                if (levels != null) {
                    System.arraycopy(levels, 0, lineLevels, 0, lineGlyphs.size());
                }

                int[] reorder = TypographyUtils.reorderLine(lineGlyphs, lineLevels, levels);

                if (reorder != null) {
                    children.clear();
                    int pos = 0;
                    int initialPos = 0;
                    boolean reversed = false;
                    int offset = 0;

                    // Insert non-text renderers
                    for (IRenderer child : starterNonTextRenderers) {
                        children.add(child);
                    }

                    while (pos < lineGlyphs.size()) {
                        IRenderer renderer = lineGlyphs.get(pos).renderer;
                        TextRenderer newRenderer = new TextRenderer((TextRenderer) renderer).removeReversedRanges();
                        children.add(newRenderer);

                        // Insert non-text renderers
                        if (insertAfter.containsKey((TextRenderer) renderer)) {
                            children.addAll(insertAfter.get((TextRenderer) renderer));
                            insertAfter.remove((TextRenderer) renderer);
                        }

                        newRenderer.line = new GlyphLine(newRenderer.line);
                        List<Glyph> replacementGlyphs = new ArrayList<>();
                        while (pos < lineGlyphs.size() && lineGlyphs.get(pos).renderer == renderer) {
                            if (pos + 1 < lineGlyphs.size()) {
                                if (reorder[pos] == reorder[pos + 1] + 1 &&
                                        !TextUtil.isSpaceOrWhitespace(lineGlyphs.get(pos + 1).glyph) && !TextUtil.isSpaceOrWhitespace(lineGlyphs.get(pos).glyph)) {
                                    reversed = true;
                                } else {
                                    if (reversed) {
                                        List<int[]> reversedRange = newRenderer.initReversedRanges();
                                        reversedRange.add(new int[]{initialPos - offset, pos - offset});
                                        reversed = false;
                                    }
                                    initialPos = pos + 1;
                                }
                            }

                            replacementGlyphs.add(lineGlyphs.get(pos).glyph);
                            pos++;
                        }

                        if (reversed) {
                            List<int[]> reversedRange = newRenderer.initReversedRanges();
                            reversedRange.add(new int[]{initialPos - offset, pos - 1 - offset});
                            reversed = false;
                            initialPos = pos;
                        }
                        offset = initialPos;
                        newRenderer.line.setGlyphs(replacementGlyphs);
                    }

                    adjustChildPositionsAfterReordering(children, occupiedArea.getBBox().getLeft());
                }

                if (result.getStatus() == LayoutResult.PARTIAL) {
                    LineRenderer overflow = (LineRenderer) result.getOverflowRenderer();
                    if (levels != null) {
                        overflow.levels = new byte[levels.length - lineLevels.length];
                        System.arraycopy(levels, lineLevels.length, overflow.levels, 0, overflow.levels.length);
                        if (overflow.levels.length == 0) {
                            overflow.levels = null;
                        }
                    }
                }
            }
        }
        LineRenderer processed = result.getStatus() == LayoutResult.FULL ? this : (LineRenderer) result.getSplitRenderer();
        if (anythingPlaced || floatsPlacedInLine) {
            processed.adjustChildrenYLine().trimLast();
            result.setMinMaxWidth(minMaxWidth);
        }

        if (wasXOverflowChanged) {
            setProperty(Property.OVERFLOW_X, oldXOverflow);
            if (null != result.getSplitRenderer()) {
                result.getSplitRenderer().setProperty(Property.OVERFLOW_X, oldXOverflow);
            }
            if (null != result.getOverflowRenderer()) {
                result.getOverflowRenderer().setProperty(Property.OVERFLOW_X, oldXOverflow);
            }
        }
        return result;
    }

    public float getMaxAscent() {
        return maxAscent;
    }

    public float getMaxDescent() {
        return maxDescent;
    }

    public float getYLine() {
        return occupiedArea.getBBox().getY() - maxDescent;
    }

    public float getLeadingValue(Leading leading) {
        switch (leading.getType()) {
            case Leading.FIXED:
                return Math.max(leading.getValue(), maxBlockAscent - maxBlockDescent);
            case Leading.MULTIPLIED:
                return getTopLeadingIndent(leading) + getBottomLeadingIndent(leading);
            default:
                throw new IllegalStateException();
        }
    }

    @Override
    public IRenderer getNextRenderer() {
        return new LineRenderer();
    }

    @Override
    protected Float getFirstYLineRecursively() {
        return getYLine();
    }

    @Override
    protected Float getLastYLineRecursively() {
        return getYLine();
    }

    public void justify(float width) {
        float ratio = (float) this.getPropertyAsFloat(Property.SPACING_RATIO);
        IRenderer lastChildRenderer = getLastNonFloatChildRenderer();
        if (lastChildRenderer == null) {
            return;
        }
        float freeWidth = occupiedArea.getBBox().getX() + width -
                lastChildRenderer.getOccupiedArea().getBBox().getX() - lastChildRenderer.getOccupiedArea().getBBox().getWidth();
        int numberOfSpaces = getNumberOfSpaces();
        int baseCharsCount = baseCharactersCount();
        float baseFactor = freeWidth / (ratio * numberOfSpaces + (1 - ratio) * (baseCharsCount - 1));

        //Prevent a NaN when trying to justify a single word with spacing_ratio == 1.0
        if (Float.isInfinite(baseFactor)) {
            baseFactor = 0;
        }
        float wordSpacing = ratio * baseFactor;
        float characterSpacing = (1 - ratio) * baseFactor;

        float lastRightPos = occupiedArea.getBBox().getX();
        for (IRenderer child : childRenderers) {
            if (FloatingHelper.isRendererFloating(child)) {
                continue;
            }
            float childX = child.getOccupiedArea().getBBox().getX();
            child.move(lastRightPos - childX, 0);
            childX = lastRightPos;
            if (child instanceof TextRenderer) {
                float childHSCale = (float) ((TextRenderer) child).getPropertyAsFloat(Property.HORIZONTAL_SCALING, 1f);
                Float oldCharacterSpacing = ((TextRenderer) child).getPropertyAsFloat(Property.CHARACTER_SPACING);
                Float oldWordSpacing = ((TextRenderer) child).getPropertyAsFloat(Property.WORD_SPACING);
                child.setProperty(Property.CHARACTER_SPACING, (null == oldCharacterSpacing ? 0 : (float) oldCharacterSpacing) + characterSpacing / childHSCale);
                child.setProperty(Property.WORD_SPACING, (null == oldWordSpacing ? 0 : (float) oldWordSpacing) + wordSpacing / childHSCale);
                boolean isLastTextRenderer = child == lastChildRenderer;
                float widthAddition = (isLastTextRenderer ? (((TextRenderer) child).lineLength() - 1) : ((TextRenderer) child).lineLength()) * characterSpacing +
                        wordSpacing * ((TextRenderer) child).getNumberOfSpaces();
                child.getOccupiedArea().getBBox().setWidth(child.getOccupiedArea().getBBox().getWidth() + widthAddition);
            }
            lastRightPos = childX + child.getOccupiedArea().getBBox().getWidth();
        }

        getOccupiedArea().getBBox().setWidth(width);
    }

    protected int getNumberOfSpaces() {
        int spaces = 0;
        for (IRenderer child : childRenderers) {
            if (child instanceof TextRenderer && !FloatingHelper.isRendererFloating(child)) {
                spaces += ((TextRenderer) child).getNumberOfSpaces();
            }
        }
        return spaces;
    }

    /**
     * Gets the total lengths of characters in this line. Other elements (images, tables) are not taken
     * into account.
     * @return the total lengths of characters in this line.
     */
    protected int length() {
        int length = 0;
        for (IRenderer child : childRenderers) {
            if (child instanceof TextRenderer && !FloatingHelper.isRendererFloating(child)) {
                length += ((TextRenderer) child).lineLength();
            }
        }
        return length;
    }

    /**
     * Returns the number of base characters, i.e. non-mark characters
     * @return the number of base non-mark characters
     */
    protected int baseCharactersCount() {
        int count = 0;
        for (IRenderer child : childRenderers) {
            if (child instanceof TextRenderer && !FloatingHelper.isRendererFloating(child)) {
                count += ((TextRenderer) child).baseCharactersCount();
            }
        }
        return count;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (IRenderer renderer : childRenderers) {
            sb.append(renderer.toString());
        }
        return sb.toString();
    }

    protected LineRenderer createSplitRenderer() {
        return (LineRenderer) getNextRenderer();
    }

    protected LineRenderer createOverflowRenderer() {
        return (LineRenderer) getNextRenderer();
    }

    protected LineRenderer[] split() {
        LineRenderer splitRenderer = createSplitRenderer();
        splitRenderer.occupiedArea = occupiedArea.clone();
        splitRenderer.parent = parent;
        splitRenderer.maxAscent = maxAscent;
        splitRenderer.maxDescent = maxDescent;
        splitRenderer.maxTextAscent = maxTextAscent;
        splitRenderer.maxTextDescent = maxTextDescent;
        splitRenderer.maxBlockAscent = maxBlockAscent;
        splitRenderer.maxBlockDescent = maxBlockDescent;
        splitRenderer.levels = levels;
        splitRenderer.addAllProperties(getOwnProperties());

        LineRenderer overflowRenderer = createOverflowRenderer();
        overflowRenderer.parent = parent;
        overflowRenderer.addAllProperties(getOwnProperties());

        return new LineRenderer[]{splitRenderer, overflowRenderer};
    }

    protected LineRenderer adjustChildrenYLine() {
        float actualYLine = occupiedArea.getBBox().getY() + occupiedArea.getBBox().getHeight() - maxAscent;
        for (IRenderer renderer : childRenderers) {
            if (FloatingHelper.isRendererFloating(renderer)) {
                continue;
            }
            if (renderer instanceof ILeafElementRenderer) {
                float descent = ((ILeafElementRenderer) renderer).getDescent();
                renderer.move(0, actualYLine - renderer.getOccupiedArea().getBBox().getBottom() + descent);
            } else {
                Float yLine = isInlineBlockChild(renderer) && renderer instanceof AbstractRenderer ? ((AbstractRenderer) renderer).getLastYLineRecursively() : null;
                renderer.move(0, actualYLine - (yLine == null ? renderer.getOccupiedArea().getBBox().getBottom() : (float) yLine));
            }
        }
        return this;
    }

    protected void applyLeading(float deltaY) {
        occupiedArea.getBBox().moveUp(deltaY);
        occupiedArea.getBBox().decreaseHeight(deltaY);
        for (IRenderer child : childRenderers) {
            if (!FloatingHelper.isRendererFloating(child)) {
                child.move(0, deltaY);
            }
        }
    }

    protected LineRenderer trimLast() {
        int lastIndex = childRenderers.size();
        IRenderer lastRenderer = null;
        while (--lastIndex >= 0) {
            lastRenderer = childRenderers.get(lastIndex);
            if (!FloatingHelper.isRendererFloating(lastRenderer)) {
                break;
            }
        }
        if (lastRenderer instanceof TextRenderer && lastIndex >= 0) {
            float trimmedSpace = ((TextRenderer) lastRenderer).trimLast();
            occupiedArea.getBBox().setWidth(occupiedArea.getBBox().getWidth() - trimmedSpace);
        }
        return this;
    }

    public boolean containsImage() {
        for (IRenderer renderer : childRenderers) {
            if (renderer instanceof ImageRenderer) {
                return true;
            }
        }
        return false;
    }

    @Override
    public MinMaxWidth getMinMaxWidth() {
        LineLayoutResult result = (LineLayoutResult) layout(new LayoutContext(new LayoutArea(1, new Rectangle(MinMaxWidthUtils.getInfWidth(), AbstractRenderer.INF))));
        return result.getMinMaxWidth();
    }

    boolean hasChildRendererInHtmlMode() {
        for (IRenderer childRenderer : childRenderers) {
            if (RenderingMode.HTML_MODE.equals(childRenderer.<RenderingMode>getProperty(Property.RENDERING_MODE))) {
                return true;
            }
        }
        return false;
    }

    float getTopLeadingIndent(Leading leading) {
        switch (leading.getType()) {
            case Leading.FIXED:
                return (Math.max(leading.getValue(), maxBlockAscent - maxBlockDescent) - occupiedArea.getBBox().getHeight()) / 2;
            case Leading.MULTIPLIED:
                UnitValue fontSize = this.<UnitValue>getProperty(Property.FONT_SIZE, UnitValue.createPointValue(0f));
                if (!fontSize.isPointValue()) {
                    logger.error(MessageFormatUtil.format(LogMessageConstant.PROPERTY_IN_PERCENTS_NOT_SUPPORTED, Property.FONT_SIZE));
                }
                // In HTML, depending on whether <!DOCTYPE html> is present or not, and if present then depending on the version,
                // the behavior id different. In one case, bottom leading indent is added for images, in the other it is not added.
                // This is why !containsImage() is present below. Depending on the presence of this !containsImage() condition, the behavior changes
                // between the two possible scenarios in HTML.
                float textAscent = maxTextAscent == 0 && maxTextDescent == 0 && Math.abs(maxAscent) + Math.abs(maxDescent) != 0 && !containsImage() ? fontSize.getValue() * 0.8f : maxTextAscent;
                float textDescent = maxTextAscent == 0 && maxTextDescent == 0 && Math.abs(maxAscent) + Math.abs(maxDescent) != 0 && !containsImage() ? -fontSize.getValue() * 0.2f : maxTextDescent;
                return Math.max(textAscent + ((textAscent - textDescent) * (leading.getValue() - 1)) / 2, maxBlockAscent) - maxAscent;
            default:
                throw new IllegalStateException();
        }
    }

    float getBottomLeadingIndent(Leading leading) {
        switch (leading.getType()) {
            case Leading.FIXED:
                return (Math.max(leading.getValue(), maxBlockAscent - maxBlockDescent) - occupiedArea.getBBox().getHeight()) / 2;
            case Leading.MULTIPLIED:
                UnitValue fontSize = this.<UnitValue>getProperty(Property.FONT_SIZE, UnitValue.createPointValue(0f));
                if (!fontSize.isPointValue()) {
                    logger.error(MessageFormatUtil.format(LogMessageConstant.PROPERTY_IN_PERCENTS_NOT_SUPPORTED, Property.FONT_SIZE));
                }
                // In HTML, depending on whether <!DOCTYPE html> is present or not, and if present then depending on the version,
                // the behavior id different. In one case, bottom leading indent is added for images, in the other it is not added.
                // This is why !containsImage() is present below. Depending on the presence of this !containsImage() condition, the behavior changes
                // between the two possible scenarios in HTML.
                float textAscent = maxTextAscent == 0 && maxTextDescent == 0 && !containsImage() ? fontSize.getValue() * 0.8f : maxTextAscent;
                float textDescent = maxTextAscent == 0 && maxTextDescent == 0 && !containsImage() ? -fontSize.getValue() * 0.2f : maxTextDescent;
                return Math.max(-textDescent + ((textAscent - textDescent) * (leading.getValue() - 1)) / 2, -maxBlockDescent) + maxDescent;
            default:
                throw new IllegalStateException();
        }
    }

    static void adjustChildPositionsAfterReordering(List<IRenderer> children, float initialXPos) {
        float currentXPos = initialXPos;
        for (IRenderer child : children) {
            if (!FloatingHelper.isRendererFloating(child)) {
                float currentWidth;
                if (child instanceof TextRenderer) {
                    currentWidth = ((TextRenderer) child).calculateLineWidth();
                    UnitValue[] margins = ((TextRenderer) child).getMargins();
                    if (!margins[1].isPointValue() && logger.isErrorEnabled()) {
                        logger.error(MessageFormatUtil.format(LogMessageConstant.PROPERTY_IN_PERCENTS_NOT_SUPPORTED, "right margin"));
                    }
                    if (!margins[3].isPointValue() && logger.isErrorEnabled()) {
                        logger.error(MessageFormatUtil.format(LogMessageConstant.PROPERTY_IN_PERCENTS_NOT_SUPPORTED, "left margin"));
                    }
                    UnitValue[] paddings = ((TextRenderer) child).getPaddings();
                    if (!paddings[1].isPointValue() && logger.isErrorEnabled()) {
                        logger.error(MessageFormatUtil.format(LogMessageConstant.PROPERTY_IN_PERCENTS_NOT_SUPPORTED, "right padding"));
                    }
                    if (!paddings[3].isPointValue() && logger.isErrorEnabled()) {
                        logger.error(MessageFormatUtil.format(LogMessageConstant.PROPERTY_IN_PERCENTS_NOT_SUPPORTED, "left padding"));
                    }
                    currentWidth += margins[1].getValue() + margins[3].getValue() + paddings[1].getValue() + paddings[3].getValue();
                    ((TextRenderer) child).occupiedArea.getBBox().setX(currentXPos).setWidth(currentWidth);
                } else {
                    currentWidth = child.getOccupiedArea().getBBox().getWidth();
                    child.move(currentXPos - child.getOccupiedArea().getBBox().getX(), 0);
                }
                currentXPos += currentWidth;
            }
        }
    }

    private LineRenderer[] splitNotFittingFloat(int childPos, LayoutResult childResult) {
        LineRenderer[] split = split();
        split[0].childRenderers.addAll(childRenderers.subList(0, childPos));
        split[0].childRenderers.add(childResult.getSplitRenderer());
        split[1].childRenderers.add(childResult.getOverflowRenderer());
        split[1].childRenderers.addAll(childRenderers.subList(childPos + 1, childRenderers.size()));

        return split;
    }

    private void adjustLineOnFloatPlaced(Rectangle layoutBox, int childPos, FloatPropertyValue kidFloatPropertyVal, Rectangle justPlacedFloatBox) {
        if (justPlacedFloatBox.getBottom() >= layoutBox.getTop() || justPlacedFloatBox.getTop() < layoutBox.getTop()) {
            return;
        }

        float floatWidth = justPlacedFloatBox.getWidth();
        if (kidFloatPropertyVal.equals(FloatPropertyValue.LEFT)) {
            layoutBox.setWidth(layoutBox.getWidth() - floatWidth).moveRight(floatWidth);
            occupiedArea.getBBox().moveRight(floatWidth);
                for (int i = 0; i < childPos; ++i) {
                    IRenderer prevChild = childRenderers.get(i);
                    if (!FloatingHelper.isRendererFloating(prevChild)) {
                        prevChild.move(floatWidth, 0);
                    }
                }

        } else {
            layoutBox.setWidth(layoutBox.getWidth() - floatWidth);
        }
    }

    private void replaceSplitRendererKidFloats(Map<Integer, IRenderer> floatsToNextPageSplitRenderers, LineRenderer splitRenderer) {
        for (Map.Entry<Integer, IRenderer> splitFloat : floatsToNextPageSplitRenderers.entrySet()) {
            if (splitFloat.getValue() != null) {
                splitRenderer.childRenderers.set(splitFloat.getKey(), splitFloat.getValue());
            } else {
                splitRenderer.childRenderers.set(splitFloat.getKey(), null);
            }
        }
        for (int i = splitRenderer.getChildRenderers().size() - 1; i >= 0; --i) {
            if (splitRenderer.getChildRenderers().get(i) == null) {
                splitRenderer.getChildRenderers().remove(i);
            }
        }
    }

    private IRenderer getLastNonFloatChildRenderer() {
        for (int i = childRenderers.size() - 1; i >= 0; --i) {
            if (FloatingHelper.isRendererFloating(childRenderers.get(i))) {
                continue;
            }
            return childRenderers.get(i);
        }
        return null;
    }

    private TabStop getNextTabStop(float curWidth) {
        NavigableMap<Float, TabStop> tabStops = this.<NavigableMap<Float, TabStop>>getProperty(Property.TAB_STOPS);

        Map.Entry<Float, TabStop> nextTabStopEntry = null;
        TabStop nextTabStop = null;

        if (tabStops != null)
            nextTabStopEntry = tabStops.higherEntry(curWidth);
        if (nextTabStopEntry != null) {
            nextTabStop = ((Map.Entry<Float, TabStop>) nextTabStopEntry).getValue();
        }

        return nextTabStop;
    }

    /**
     * Calculates and sets encountered tab size.
     * Returns null, if processing is finished and layout can be performed for the tab renderer;
     * otherwise, in case when the tab should be processed after the next element in the line, this method returns corresponding tab stop.
     */
    private TabStop calculateTab(IRenderer childRenderer, float curWidth, float lineWidth) {
        TabStop nextTabStop = getNextTabStop(curWidth);

        if (nextTabStop == null) {
            processDefaultTab(childRenderer, curWidth, lineWidth);
            return null;
        }

        childRenderer.setProperty(Property.TAB_LEADER, nextTabStop.getTabLeader());
        childRenderer.setProperty(Property.WIDTH, UnitValue.createPointValue(nextTabStop.getTabPosition() - curWidth));
        childRenderer.setProperty(Property.MIN_HEIGHT, UnitValue.createPointValue(maxAscent - maxDescent));

        if (nextTabStop.getTabAlignment() == TabAlignment.LEFT) {
            return null;
        }

        return nextTabStop;
    }

    /**
     * Calculates and sets tab size with the account of the element that is next in the line after the tab.
     * Returns resulting width of the tab.
     */
    private float calculateTab(Rectangle layoutBox, float curWidth, TabStop tabStop, List<IRenderer> affectedRenderers, IRenderer tabRenderer) {
        float sumOfAffectedRendererWidths = 0;
        for (IRenderer renderer : affectedRenderers) {
            sumOfAffectedRendererWidths += renderer.getOccupiedArea().getBBox().getWidth();
        }
        float tabWidth = 0;
        switch (tabStop.getTabAlignment()) {
            case RIGHT:
                tabWidth = tabStop.getTabPosition() - curWidth - sumOfAffectedRendererWidths;
                break;
            case CENTER:
                tabWidth = tabStop.getTabPosition() - curWidth - sumOfAffectedRendererWidths / 2;
                break;
            case ANCHOR:
                float anchorPosition = -1;
                float processedRenderersWidth = 0;
                for (IRenderer renderer : affectedRenderers) {
                    anchorPosition = ((TextRenderer) renderer).getTabAnchorCharacterPosition();
                    if (-1 != anchorPosition) {
                        break;
                    } else {
                        processedRenderersWidth += renderer.getOccupiedArea().getBBox().getWidth();
                    }
                }
                if (anchorPosition == -1) {
                    anchorPosition = 0;
                }
                tabWidth = tabStop.getTabPosition() - curWidth - anchorPosition - processedRenderersWidth;
                break;
        }
        if (tabWidth < 0) {
            tabWidth = 0;
        }
        if (curWidth + tabWidth + sumOfAffectedRendererWidths > layoutBox.getWidth()) {
            tabWidth -= (curWidth + sumOfAffectedRendererWidths + tabWidth) - layoutBox.getWidth();
        }

        tabRenderer.setProperty(Property.WIDTH, UnitValue.createPointValue(tabWidth));
        tabRenderer.setProperty(Property.MIN_HEIGHT, UnitValue.createPointValue(maxAscent - maxDescent));

        return tabWidth;
    }

    private void processDefaultTab(IRenderer tabRenderer, float curWidth, float lineWidth) {
        Float tabDefault = this.getPropertyAsFloat(Property.TAB_DEFAULT);
        Float tabWidth = tabDefault - curWidth % tabDefault;
        if (curWidth + tabWidth > lineWidth)
            tabWidth = lineWidth - curWidth;
        tabRenderer.setProperty(Property.WIDTH, UnitValue.createPointValue((float) tabWidth));
        tabRenderer.setProperty(Property.MIN_HEIGHT, UnitValue.createPointValue(maxAscent - maxDescent));
    }

    private void updateChildrenParent() {
        for (IRenderer renderer : childRenderers) {
            renderer.setParent(this);
        }
    }

    /**
     * Trim first child text renderers.
     *
     * @return total number of trimmed glyphs.
     */
    int trimFirst() {
        int totalNumberOfTrimmedGlyphs = 0;
        for (IRenderer renderer : childRenderers) {
            if (FloatingHelper.isRendererFloating(renderer)) {
                continue;
            }
            if (renderer instanceof TextRenderer) {
                TextRenderer textRenderer = (TextRenderer) renderer;
                GlyphLine currentText = textRenderer.getText();
                if (currentText != null) {
                    int prevTextStart = currentText.start;
                    textRenderer.trimFirst();
                    int numOfTrimmedGlyphs = textRenderer.getText().start - prevTextStart;
                    totalNumberOfTrimmedGlyphs += numOfTrimmedGlyphs;
                }
                if (textRenderer.length() > 0) {
                    break;
                }
            } else {
                break;
            }
        }
        return totalNumberOfTrimmedGlyphs;
    }

    /**
     * Apply OTF features and return the last(!) base direction of child renderer
     *
     * @return the last(!) base direction of child renderer.
     */
    private BaseDirection applyOtf() {
        BaseDirection baseDirection = this.<BaseDirection>getProperty(Property.BASE_DIRECTION);
        for (IRenderer renderer : childRenderers) {
            if (renderer instanceof TextRenderer) {
                ((TextRenderer) renderer).applyOtf();
                if (baseDirection == null || baseDirection == BaseDirection.NO_BIDI) {
                    baseDirection = renderer.<BaseDirection>getOwnProperty(Property.BASE_DIRECTION);
                }
            }
        }
        return baseDirection;
    }

    static boolean isChildFloating(IRenderer childRenderer) {
        FloatPropertyValue kidFloatPropertyVal = childRenderer.<FloatPropertyValue>getProperty(Property.FLOAT);
        return childRenderer instanceof AbstractRenderer
                && FloatingHelper.isRendererFloating(childRenderer, kidFloatPropertyVal);
    }

    static boolean isInlineBlockChild(IRenderer child) {
        return child instanceof BlockRenderer || child instanceof TableRenderer;
    }

    /**
     * Checks if the word that's been split when has been layouted on this line can fit the next line without splitting.
     *
     * @param childRenderer the childRenderer containing the split word
     * @param wasXOverflowChanged true if {@link Property#OVERFLOW_X} has been changed
     *                            during layouting of {@link LineRenderer}
     * @param oldXOverflow the value of {@link Property#OVERFLOW_X} before it's been changed
     *                     during layouting of {@link LineRenderer}
     *                     or null if {@link Property#OVERFLOW_X} hasn't been changed
     * @param layoutContext {@link LayoutContext}
     * @param layoutBox current layoutBox
     * @param wasParentsHeightClipped true if layoutBox's height has been clipped
     * @return true if the split word can fit the next line without splitting
     */
    boolean isForceOverflowForTextRendererPartialResult(IRenderer childRenderer, boolean wasXOverflowChanged,
                                                        OverflowPropertyValue oldXOverflow, LayoutContext layoutContext,
                                                        Rectangle layoutBox, boolean wasParentsHeightClipped) {
        if (wasXOverflowChanged) {
            setProperty(Property.OVERFLOW_X, oldXOverflow);
        }
        LayoutResult newLayoutResult = childRenderer.layout(
                new LayoutContext(new LayoutArea(layoutContext.getArea().getPageNumber(), layoutBox),
                        wasParentsHeightClipped));
        if (wasXOverflowChanged) {
            setProperty(Property.OVERFLOW_X, OverflowPropertyValue.FIT);
        }
        return newLayoutResult instanceof TextLayoutResult
                && !((TextLayoutResult) newLayoutResult).isWordHasBeenSplit();
    }

    /**
     * Extracts ascender and descender of an already layouted {@link IRenderer childRenderer}.
     *
     * @param childRenderer an already layouted child who's ascender and descender are to be extracted
     * @param childResult {@link LayoutResult} of the childRenderer based on which ascender and descender are defined
     * @param childRenderingMode {@link RenderingMode rendering mode}
     * @param isInlineBlockChild true if childRenderer {@link #isInlineBlockChild(IRenderer)}
     * @return a two-element float array where first element is ascender value and second element is descender value
     */
    float[] getAscentDescentOfLayoutedChildRenderer(IRenderer childRenderer, LayoutResult childResult,
                                                    RenderingMode childRenderingMode, boolean isInlineBlockChild) {
        float childAscent = 0;
        float childDescent = 0;
        if (childRenderer instanceof ILeafElementRenderer
                && childResult.getStatus() != LayoutResult.NOTHING) {
            if (RenderingMode.HTML_MODE == childRenderingMode && childRenderer instanceof TextRenderer) {
                return LineHeightHelper.getActualAscenderDescender((TextRenderer) childRenderer);
            } else {
                childAscent = ((ILeafElementRenderer) childRenderer).getAscent();
                childDescent = ((ILeafElementRenderer) childRenderer).getDescent();
            }
        } else if (isInlineBlockChild && childResult.getStatus() != LayoutResult.NOTHING) {
            if (childRenderer instanceof AbstractRenderer) {
                Float yLine = ((AbstractRenderer) childRenderer).getLastYLineRecursively();
                if (yLine == null) {
                    childAscent = childRenderer.getOccupiedArea().getBBox().getHeight();
                } else {
                    childAscent = childRenderer.getOccupiedArea().getBBox().getTop() - (float) yLine;
                    childDescent = -((float) yLine - childRenderer.getOccupiedArea().getBBox().getBottom());
                }
            } else {
                childAscent = childRenderer.getOccupiedArea().getBBox().getHeight();
            }
        }

        return new float[] {childAscent, childDescent};
    }

    /**
     * Updates {@link LineRenderer#maxAscent}, {@link LineRenderer#maxDescent}, {@link LineRenderer#maxTextAscent} and
     * {@link LineRenderer#maxTextDescent} after a {@link TextRenderer} sequence has been fully processed.
     *
     * @param newChildPos                                      position of the last {@link TextRenderer} child of the
     *                                                         sequence to remain on the line
     * @param lineAscentDescentStateBeforeTextRendererSequence a {@link LineAscentDescentState} containing
     *                                                         {@link LineRenderer}'s maxAscent, maxDescent,
     *                                                         maxTextAscent, maxTextDescent before
     *                                                         {@link TextRenderer} sequence start
     * @param textRendererSequenceAscentDescent                a {@link Map} with {@link TextRenderer} children's
     *                                                         positions as keys
     *                                                         and float arrays consisting of maxAscent, maxDescent,
     *                                                         maxTextAscent,
     *                                                         maxTextDescent of the corresponding {@link TextRenderer}
     *                                                         children.
     * @return a two-element float array where first element is a new {@link LineRenderer}'s ascender
     *         and second element is a new {@link LineRenderer}'s descender
     */
    float[] updateAscentDescentAfterTextRendererSequenceProcessing(
            int newChildPos, LineAscentDescentState lineAscentDescentStateBeforeTextRendererSequence,
            Map<Integer, float[]> textRendererSequenceAscentDescent) {
        float maxAscentUpdated = lineAscentDescentStateBeforeTextRendererSequence.maxAscent;
        float maxDescentUpdated = lineAscentDescentStateBeforeTextRendererSequence.maxDescent;
        float maxTextAscentUpdated = lineAscentDescentStateBeforeTextRendererSequence.maxTextAscent;
        float maxTextDescentUpdated = lineAscentDescentStateBeforeTextRendererSequence.maxTextDescent;
        for (Map.Entry<Integer, float[]> childAscentDescent : textRendererSequenceAscentDescent.entrySet()) {
            if (childAscentDescent.getKey() <= newChildPos) {
                maxAscentUpdated = Math.max(maxAscentUpdated, childAscentDescent.getValue()[0]);
                maxDescentUpdated = Math.min(maxDescentUpdated, childAscentDescent.getValue()[1]);
                maxTextAscentUpdated = Math.max(maxTextAscentUpdated, childAscentDescent.getValue()[0]);
                maxTextDescentUpdated = Math.min(maxTextDescentUpdated, childAscentDescent.getValue()[1]);
            }
        }

        this.maxAscent = maxAscentUpdated;
        this.maxDescent = maxDescentUpdated;
        this.maxTextAscent = maxTextAscentUpdated;
        this.maxTextDescent = maxTextDescentUpdated;

        return new float[] {this.maxAscent, this.maxDescent};
    }

    /**
     * Update {@link LineRenderer#maxAscent}, {@link LineRenderer#maxDescent}, {@link LineRenderer#maxTextAscent},
     * {@link LineRenderer#maxTextDescent}, {@link LineRenderer#maxBlockAscent} and {@link LineRenderer#maxBlockDescent}
     * after child's layout.
     *
     * @param childAscentDescent a two-element float array where first element is ascender of a layouted child
     *                           and second element is descender of a layouted child
     * @param childRenderer the layouted {@link IRenderer childRenderer} of current {@link LineRenderer}
     * @param isChildFloating true if {@link #isChildFloating(IRenderer)}
     */
    void updateAscentDescentAfterChildLayout(float[] childAscentDescent, IRenderer childRenderer,
                                             boolean isChildFloating) {
        float childAscent = childAscentDescent[0];
        float childDescent = childAscentDescent[1];
        this.maxAscent = Math.max(this.maxAscent, childAscent);
        if (childRenderer instanceof TextRenderer) {
            this.maxTextAscent = Math.max(this.maxTextAscent, childAscent);
        } else if (!isChildFloating) {
            this.maxBlockAscent = Math.max(this.maxBlockAscent, childAscent);
        }
        this.maxDescent = Math.min(this.maxDescent, childDescent);
        if (childRenderer instanceof TextRenderer) {
            this.maxTextDescent = Math.min(this.maxTextDescent, childDescent);
        } else if (!isChildFloating) {
            this.maxBlockDescent = Math.min(this.maxBlockDescent, childDescent);
        }
    }

    private void updateBidiLevels(int totalNumberOfTrimmedGlyphs, BaseDirection baseDirection) {
        if (totalNumberOfTrimmedGlyphs != 0 && levels != null) {
            levels = Arrays.copyOfRange(levels, totalNumberOfTrimmedGlyphs, levels.length);
        }

        List<Integer> unicodeIdsReorderingList = null;
        if (levels == null && baseDirection != null && baseDirection != BaseDirection.NO_BIDI) {
            unicodeIdsReorderingList = new ArrayList<>();
            boolean newLineFound = false;
            for (IRenderer child : childRenderers) {
                if (newLineFound) {
                    break;
                }
                if (child instanceof TextRenderer) {
                    GlyphLine text = ((TextRenderer) child).getText();
                    for (int i = text.start; i < text.end; i++) {
                        Glyph glyph = text.get(i);
                        if (TextUtil.isNewLine(glyph)) {
                            newLineFound = true;
                            break;
                        }
                        // we assume all the chars will have the same bidi group
                        // we also assume pairing symbols won't get merged with other ones
                        int unicode = glyph.hasValidUnicode() ? glyph.getUnicode() : glyph.getUnicodeChars()[0];
                        unicodeIdsReorderingList.add(unicode);
                    }
                }
            }
            levels = unicodeIdsReorderingList.size() > 0 ? TypographyUtils.getBidiLevels(baseDirection, ArrayUtil.toIntArray(unicodeIdsReorderingList)) : null;
        }
    }

    /**
     * While resolving TextRenderer may split into several ones with different fonts.
     */
    private void resolveChildrenFonts() {
        List<IRenderer> newChildRenderers = new ArrayList<>(childRenderers.size());
        boolean updateChildRenderers = false;
        for (IRenderer child : childRenderers) {
            if (child instanceof TextRenderer) {
                if (((TextRenderer) child).resolveFonts(newChildRenderers)) {
                    updateChildRenderers = true;
                }
            } else {
                newChildRenderers.add(child);
            }
        }

        // this mean, that some TextRenderer has been replaced.
        if (updateChildRenderers) {
            childRenderers = newChildRenderers;
        }
    }

    private float decreaseRelativeWidthByChildAdditionalWidth(IRenderer childRenderer, float normalizedChildWidth) {
        // Decrease the calculated width by margins, paddings and borders so that even for 100% width the content definitely fits.
        // TODO Actually, from html/css point of view - this is wrong, however we still do it, in order to avoid NOTHING due to
        // horizontal overflow. Probably remove this when overflow-x is supported.
        if (childRenderer instanceof AbstractRenderer) {
            Rectangle dummyRect = new Rectangle(normalizedChildWidth, 0);
            ((AbstractRenderer) childRenderer).applyMargins(dummyRect, false);
            if (!isBorderBoxSizing(childRenderer)) {
                ((AbstractRenderer) childRenderer).applyBorderBox(dummyRect, false);
                ((AbstractRenderer) childRenderer).applyPaddings(dummyRect, false);
            }
            normalizedChildWidth = dummyRect.getWidth();
        }
        return normalizedChildWidth;
    }

    static class RendererGlyph {
        public Glyph glyph;
        public TextRenderer renderer;

        public RendererGlyph(Glyph glyph, TextRenderer textRenderer) {
            this.glyph = glyph;
            this.renderer = textRenderer;
        }
    }

    static class LineAscentDescentState {
        float maxAscent;
        float maxDescent;
        float maxTextAscent;
        float maxTextDescent;

        LineAscentDescentState(float maxAscent, float maxDescent, float maxTextAscent, float maxTextDescent) {
            this.maxAscent = maxAscent;
            this.maxDescent = maxDescent;
            this.maxTextAscent = maxTextAscent;
            this.maxTextDescent = maxTextDescent;
        }
    }

}
