/*
 * $Id$
 * ============================================================================
 *                    The Apache Software License, Version 1.1
 * ============================================================================
 *
 * Copyright (C) 1999-2003 The Apache Software Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modifica-
 * tion, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. The end-user documentation included with the redistribution, if any, must
 *    include the following acknowledgment: "This product includes software
 *    developed by the Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself, if
 *    and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "FOP" and "Apache Software Foundation" must not be used to
 *    endorse or promote products derived from this software without prior
 *    written permission. For written permission, please contact
 *    apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache", nor may
 *    "Apache" appear in their name, without prior written permission of the
 *    Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * APACHE SOFTWARE FOUNDATION OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLU-
 * DING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * ============================================================================
 *
 * This software consists of voluntary contributions made by many individuals
 * on behalf of the Apache Software Foundation and was originally created by
 * James Tauber <jtauber@jtauber.com>. For more information on the Apache
 * Software Foundation, please see <http://www.apache.org/>.
 */
package org.apache.fop.render.ps;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
/* java.awt.Font is not imported to avoid confusion with
   org.apache.fop.fonts.Font */

import java.text.AttributedCharacterIterator;
import java.awt.font.TextAttribute;
import java.awt.Shape;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.Color;
import java.util.List;
import java.util.Iterator;

import org.apache.batik.gvt.text.Mark;
import org.apache.batik.gvt.TextPainter;
import org.apache.batik.gvt.TextNode;
import org.apache.batik.gvt.text.GVTAttributedCharacterIterator;
import org.apache.batik.gvt.text.TextPaintInfo;
import org.apache.batik.gvt.font.GVTFontFamily;
import org.apache.batik.bridge.SVGFontFamily;
import org.apache.batik.gvt.renderer.StrokingTextPainter;

import org.apache.fop.fonts.FontMetrics;
import org.apache.fop.fonts.Font;
import org.apache.fop.apps.Document;

/**
 * Renders the attributed character iterator of a <tt>TextNode</tt>.
 * This class draws the text directly into the PSGraphics2D so that
 * the text is not drawn using shapes which makes the PS files larger.
 * If the text is simple enough to draw then it sets the font and calls
 * drawString. If the text is complex or the cannot be translated
 * into a simple drawString the StrokingTextPainter is used instead.
 *
 * (todo) handle underline, overline and strikethrough
 * (todo) use drawString(AttributedCharacterIterator iterator...) for some
 *
 * @author <a href="mailto:keiron@aftexsw.com">Keiron Liddle</a>
 * @version $Id: PSTextPainter.java,v 1.15 2003/01/08 14:03:55 jeremias Exp $
 */
public class PSTextPainter implements TextPainter {
    private Document fontInfo;

    /**
     * Use the stroking text painter to get the bounds and shape.
     * Also used as a fallback to draw the string with strokes.
     */
    protected static final TextPainter PROXY_PAINTER =
        StrokingTextPainter.getInstance();

    /**
     * Create a new PS text painter with the given font information.
     * @param fi the fint info
     */
    public PSTextPainter(Document fi) {
        fontInfo = fi;
    }

    /**
     * Paints the specified attributed character iterator using the
     * specified Graphics2D and context and font context.
     * @param node the TextNode to paint
     * @param g2d the Graphics2D to use
     */
    public void paint(TextNode node, Graphics2D g2d) {
        // System.out.println("PSText paint");
        String txt = node.getText();
        Point2D loc = node.getLocation();

        AttributedCharacterIterator aci =
          node.getAttributedCharacterIterator();
        // reset position to start of char iterator
        if (aci.getBeginIndex() == aci.getEndIndex()) {
            return;
        }
        char ch = aci.first();
        if (ch == AttributedCharacterIterator.DONE) {
            return;
        }

        TextPaintInfo tpi = (TextPaintInfo) aci.getAttribute(
            GVTAttributedCharacterIterator.TextAttribute.PAINT_INFO);
        
        if (tpi == null) {
            return;
        }        

        TextNode.Anchor anchor;
        anchor = (TextNode.Anchor) aci.getAttribute(
                      GVTAttributedCharacterIterator.TextAttribute.ANCHOR_TYPE);

        List gvtFonts;
        gvtFonts = (List) aci.getAttribute(
                      GVTAttributedCharacterIterator.TextAttribute.GVT_FONT_FAMILIES);
        Paint forg = (Paint) aci.getAttribute(TextAttribute.FOREGROUND);
        Paint strokePaint = tpi.strokePaint;
        Float size = (Float) aci.getAttribute(TextAttribute.SIZE);
        if (size == null) {
            return;
        }
        Stroke stroke = tpi.strokeStroke;
        /*
        Float xpos = (Float) aci.getAttribute(
                       GVTAttributedCharacterIterator.TextAttribute.X);
        Float ypos = (Float) aci.getAttribute(
                       GVTAttributedCharacterIterator.TextAttribute.Y);
        */

        Float posture = (Float) aci.getAttribute(TextAttribute.POSTURE);
        Float taWeight = (Float) aci.getAttribute(TextAttribute.WEIGHT);

        boolean useStrokePainter = false;

        if (forg instanceof Color) {
            Color col = (Color) forg;
            if (col.getAlpha() != 255) {
                useStrokePainter = true;
            }
            g2d.setColor(col);
        }
        g2d.setPaint(forg);
        g2d.setStroke(stroke);

        if (strokePaint != null) {
            // need to draw using AttributedCharacterIterator
            useStrokePainter = true;
        }

        if (hasUnsupportedAttributes(aci)) {
            useStrokePainter = true;
        }

        // text contains unsupported information
        if (useStrokePainter) {
            PROXY_PAINTER.paint(node, g2d);
            return;
        }

        String style = ((posture != null) && (posture.floatValue() > 0.0))
                       ? "italic" : "normal";
        int weight = ((taWeight != null)
                       &&  (taWeight.floatValue() > 1.0)) ? Font.BOLD
                       : Font.NORMAL;

        Font fontState = null;
        Document fi = fontInfo;
        boolean found = false;
        String fontFamily = null;
        if (gvtFonts != null) {
            Iterator i = gvtFonts.iterator();
            while (i.hasNext()) {
                GVTFontFamily fam = (GVTFontFamily) i.next();
                if (fam instanceof SVGFontFamily) {
                    PROXY_PAINTER.paint(node, g2d);
                    return;
                }
                fontFamily = fam.getFamilyName();
                if (fi.hasFont(fontFamily, style, weight)) {
                    String fname = fontInfo.fontLookup(fontFamily, style,
                                                       weight);
                    FontMetrics metrics = fontInfo.getMetricsFor(fname);
                    int fsize = (int)(size.floatValue() * 1000);
                    fontState = new Font(fname, metrics, fsize);
                    found = true;
                    break;
                }
            }
        }
        if (!found) {
            String fname =
              fontInfo.fontLookup("any", style, Font.NORMAL);
            FontMetrics metrics = fontInfo.getMetricsFor(fname);
            int fsize = (int)(size.floatValue() * 1000);
            fontState = new Font(fname, metrics, fsize);
        } else {
            if (g2d instanceof PSGraphics2D) {
                ((PSGraphics2D) g2d).setOverrideFontState(fontState);
            }
        }
        int fStyle = java.awt.Font.PLAIN;
        if (weight == Font.BOLD) {
            if (style.equals("italic")) {
                fStyle = java.awt.Font.BOLD | java.awt.Font.ITALIC;
            } else {
                fStyle = java.awt.Font.BOLD;
            }
        } else {
            if (style.equals("italic")) {
                fStyle = java.awt.Font.ITALIC;
            } else {
                fStyle = java.awt.Font.PLAIN;
            }
        }
        java.awt.Font font = new java.awt.Font(fontFamily, fStyle,
                             (int)(fontState.getFontSize() / 1000));

        g2d.setFont(font);

        float advance = getStringWidth(txt, fontState);
        float tx = 0;
        if (anchor != null) {
            switch (anchor.getType()) {
                case TextNode.Anchor.ANCHOR_MIDDLE:
                    tx = -advance / 2;
                    break;
                case TextNode.Anchor.ANCHOR_END:
                    tx = -advance;
            }
        }
        g2d.drawString(txt, (float)(loc.getX() + tx), (float)(loc.getY()));
    }

    private boolean hasUnsupportedAttributes(AttributedCharacterIterator aci) {
        boolean hasunsupported = false;
        Object letSpace = aci.getAttribute(
                            GVTAttributedCharacterIterator.TextAttribute.LETTER_SPACING);
        if (letSpace != null) {
            hasunsupported = true;
        }

        Object wordSpace = aci.getAttribute(
                             GVTAttributedCharacterIterator.TextAttribute.WORD_SPACING);
        if (wordSpace != null) {
            hasunsupported = true;
        }

        AttributedCharacterIterator.Attribute key;
        key = GVTAttributedCharacterIterator.TextAttribute.WRITING_MODE;
        Object writeMod = aci.getAttribute(key);
        if (!GVTAttributedCharacterIterator.TextAttribute.WRITING_MODE_LTR.equals(
                  writeMod)) {
            hasunsupported = true;
        }

        Object vertOr = aci.getAttribute(
                          GVTAttributedCharacterIterator.TextAttribute.VERTICAL_ORIENTATION);
        if (GVTAttributedCharacterIterator.TextAttribute.ORIENTATION_ANGLE.equals(
                  vertOr)) {
            hasunsupported = true;
        }
        return hasunsupported;
    }

    private float getStringWidth(String str, Font fontState) {
        float wordWidth = 0;
        float whitespaceWidth = fontState.getWidth(fontState.mapChar(' '));

        for (int i = 0; i < str.length(); i++) {
            float charWidth;
            char c = str.charAt(i);
            if (!((c == ' ') || (c == '\n') || (c == '\r') || (c == '\t'))) {
                charWidth = fontState.getWidth(fontState.mapChar(c));
                if (charWidth <= 0) {
                    charWidth = whitespaceWidth;
                }
            } else {
                charWidth = whitespaceWidth;
            }
            wordWidth += charWidth;
        }
        return wordWidth / 1000f;
    }

    /**
     * Get the outline shape of the text characters.
     * This uses the StrokingTextPainter to get the outline
     * shape since in theory it should be the same.
     *
     * @param node the text node
     * @return the outline shape of the text characters
     */
    public Shape getOutline(TextNode node) {
        return PROXY_PAINTER.getOutline(node);
    }

    /**
     * Get the bounds.
     * This uses the StrokingTextPainter to get the bounds
     * since in theory it should be the same.
     *
     * @param node the text node
     * @return the bounds of the text
     */
    public Rectangle2D getBounds2D(TextNode node) {
        return PROXY_PAINTER.getBounds2D(node);
    }

    /**
     * Get the geometry bounds.
     * This uses the StrokingTextPainter to get the bounds
     * since in theory it should be the same.
     * @param node the text node
     * @return the bounds of the text
     */
    public Rectangle2D getGeometryBounds(TextNode node) {
        return PROXY_PAINTER.getGeometryBounds(node);
    }

    // Methods that have no purpose for PS

    /**
     * Get the mark.
     * This does nothing since the output is pdf and not interactive.
     * @param node the text node
     * @param pos the position
     * @param all select all
     * @return null
     */
    public Mark getMark(TextNode node, int pos, boolean all) {
        System.out.println("PSText getMark");
        return null;
    }

    /**
     * Select at.
     * This does nothing since the output is pdf and not interactive.
     * @param x the x position
     * @param y the y position
     * @param node the text node
     * @return null
     */
    public Mark selectAt(double x, double y, TextNode node) {
        System.out.println("PSText selectAt");
        return null;
    }

    /**
     * Select to.
     * This does nothing since the output is pdf and not interactive.
     * @param x the x position
     * @param y the y position
     * @param beginMark the start mark
     * @return null
     */
    public Mark selectTo(double x, double y, Mark beginMark) {
        System.out.println("PSText selectTo");
        return null;
    }

    /**
     * Selec first.
     * This does nothing since the output is pdf and not interactive.
     * @param node the text node
     * @return null
     */
    public Mark selectFirst(TextNode node) {
        System.out.println("PSText selectFirst");
        return null;
    }

    /**
     * Select last.
     * This does nothing since the output is pdf and not interactive.
     * @param node the text node
     * @return null
     */
    public Mark selectLast(TextNode node) {
        System.out.println("PSText selectLast");
        return null;
    }

    /**
     * Get selected.
     * This does nothing since the output is pdf and not interactive.
     * @param start the start mark
     * @param finish the finish mark
     * @return null
     */
    public int[] getSelected(Mark start, Mark finish) {
        System.out.println("PSText getSelected");
        return null;
    }

    /**
     * Get the highlighted shape.
     * This does nothing since the output is pdf and not interactive.
     * @param beginMark the start mark
     * @param endMark the end mark
     * @return null
     */
    public Shape getHighlightShape(Mark beginMark, Mark endMark) {
        System.out.println("PSText getHighlightShape");
        return null;
    }

}


