/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.geom.*;

import com.fasterxml.jackson.annotation.JsonIgnore;

/** Abstract class containing elements common to RectToQuad and
    QuadToRect. */
abstract public class RectToQuadCommon
    extends PolygonTransformAdapter
    implements QuadrilateralTransform {
    @Override
    abstract public Transform2D createInverse();
    @Override
    abstract public void preConcatenate(Transform2D other);
    @Override
    abstract public void concatenate(Transform2D other);
    @Override
    abstract public Transform2D squareToDomain();
    @Override
    abstract public RectToQuadCommon clone();

    @Override
    public Point2D.Double transform(double x, double y)
        throws UnsolvableException {
        return xform.transform(x,y);
    }

    @Override
    public void transform(double[] srcPts, int srcOff,
                          double[] dstPts, int dstOff, int numPts)
        throws UnsolvableException {
        xform.transform(srcPts, srcOff, dstPts, dstOff, numPts);
    }

    /** X offset of rectangle */
    protected double x = 0;
    /** Y offset of rectangle */
    protected double y = 0;
    /** Width of rectangle */
    protected double w = 1;
    /** Height of rectangle */
    protected double h = 1;

    protected AffineXYCommon xform = null;

    /** X offset of rectangle */
    public double getX() { return x; }
    /** Y offset of rectangle */
    public double getY() { return y; }
    /** Width of rectangle */
    public double getW() { return w; }
    /** Height of rectangle */
    public double getH() { return h; }

    // These attributes define the output (for RectToQuad) or input
    // (for QuadToRect) quadrilateral. llx is the x value of the
    // "lower left" vertex, ury is the y value of the "upper right"
    // vertex (whichever vertex is diagonal from the "lower left"
    // vertex), and so on.

    protected double llx, lly, ulx, uly, urx, ury, lrx, lry;

    @JsonIgnore public double getLlx() { return llx; }
    @JsonIgnore public double getLly() { return lly; }
    @JsonIgnore public double getUlx() { return ulx; }
    @JsonIgnore public double getUly() { return uly; }
    @JsonIgnore public double getUrx() { return urx; }
    @JsonIgnore public double getUry() { return ury; }
    @JsonIgnore public double getLrx() { return lrx; }
    @JsonIgnore public double getLry() { return lry; }

    @JsonIgnore public void setLlx(double v) { llx = v; update(); }
    @JsonIgnore public void setLly(double v) { lly = v; update(); }
    @JsonIgnore public void setUlx(double v) { ulx = v; update(); }
    @JsonIgnore public void setUly(double v) { uly = v; update(); }
    @JsonIgnore public void setUrx(double v) { urx = v; update(); }
    @JsonIgnore public void setUry(double v) { ury = v; update(); }
    @JsonIgnore public void setLrx(double v) { lrx = v; update(); }
    @JsonIgnore public void setLry(double v) { lry = v; update(); }
    @JsonIgnore public void setW(double v) { w = v; update(); }
    @JsonIgnore public void setH(double v) { h = v; update(); }
    @JsonIgnore public void setHeight(double v) { setH(v); }
    @JsonIgnore public void setWidth(double v) { setW(v); }
    @JsonIgnore public void setX(double v) { x = v; update(); }
    @JsonIgnore public void setY(double v) { y = v; update(); }


    @JsonIgnore public void setRectangle(Rectangle2D rect) {
        x = rect.getX();
        y = rect.getY();
        w = rect.getWidth();
        h = rect.getHeight();
        update();
    }


    @JsonIgnore public Rectangle2D.Double getRectangle() {
        return new Rectangle2D.Double(x, y, w, h);
    }


    @JsonIgnore public void setVertices(Point2D.Double[] vertices) {
        llx = vertices[0].x;
        lly = vertices[0].y;
        ulx = vertices[1].x;
        uly = vertices[1].y;
        urx = vertices[2].x;
        ury = vertices[2].y;
        lrx = vertices[3].x;
        lry = vertices[3].y;
        update();
    }


    protected void copyFieldsFrom(RectToQuadCommon src) {
        llx = src.llx;
        lly = src.lly;
        ulx = src.ulx;
        uly = src.uly;
        urx = src.urx;
        ury = src.ury;
        lrx = src.lrx;
        lry = src.lry;
        x = src.x;
        y = src.y;
        w = src.w;
        h = src.h;
        update();
    }


    /** @return the four vertices of the quadrilateral (LL, UL, UR,
     * LR). */
    protected Point2D.Double[] quadVertices() {
        Point2D.Double[] output =
            {new Point2D.Double(llx,lly),
             new Point2D.Double(ulx,uly),
             new Point2D.Double(urx,ury),
             new Point2D.Double(lrx,lry)};
        return output;
    }


    /** @return the four vertices of the rectangle (LL, UL, UR,
     * LR). */
    protected Point2D.Double[] rectVertices() {
        Point2D.Double[] output =
            {new Point2D.Double(x,y),
             new Point2D.Double(x,y+h),
             new Point2D.Double(x+w,y+h),
             new Point2D.Double(x+w,y)};
        return output;
    }


    /** Update xform's parameters to reflect changes made here. */
    void update() {

        // The transformation equals

        // LL(1 - (x - @x)/@w)(1 - (y - @y)/@h) +
        // UL(1 - (x - @x)/@w)(y - @y)/@h +
        // LR(1 - (y - @y)/@h)(x - @x)/@w
        // UR((x - @x)/@w)(y - @y)/@h +

        // Let XW = 1 + @x/@w
        // Let YH = 1 + @y/@h
        //
        // | -  | 1          | x      | y      | xy    |
        // | LL | XW YH      | - YH/w | - XW/h | 1/wh  |
        // | UL | XW (-@y/h) | @y/hw  | XW/h   | -1/wh |
        // | LR | YH (-@x/w) | YH/w   | @x/hw  | -1/wh |
        // | UR | (@x@y/wh)  | -@y/wh | -@x/wh | 1/wh  |

        double xw = 1 + x/w;
        double yh = 1 + y/h;

        double xk = llx*xw*yh - ulx*xw*y/h - lrx*yh*x/w + urx*x*y/w/h;
        double xkx = -llx*yh/w + ulx*y/w/h + lrx*yh/w - urx*y/w/h;
        double xky = -llx*xw/h + lrx*x/w/h + ulx*xw/h - urx*x/w/h;
        double xkxy = (llx+urx-ulx-lrx)/w/h;

        double yk = lly*xw*yh - uly*xw*y/h - lry*yh*x/w + ury*x*y/w/h;
        double ykx = -lly*yh/w + uly*y/w/h + lry*yh/w - ury*y/w/h;
        double yky = -lly*xw/h + lry*x/w/h + uly*xw/h - ury*x/w/h;
        double ykxy = (lly+ury-uly-lry)/w/h;

        xform.set(xk, xkx, xky, xkxy, yk, ykx, yky, ykxy);
    }

    /** Apply the given transformation to the quadrilateral's vertices,
        with the effect of changing the transformation that this object
        represents. */
    public void transformQuad(Transform2D other) {
        try {
            Point2D.Double[] points = quadVertices();
            for (int i = 0; i < points.length; ++i) {
                points[i] = other.transform(points[i]);
            }
            setVertices(points);
        } catch (UnsolvableException e) {
            throw new RuntimeException(e);
        }
    }

    /** Apply the given transformation to the rectangle, with the
        effect of changing the transformation that this object
        represents. If the rectangle is no longer just a rectangle
        after transformation, though, then that is not supported. */
    public void transformRect(Transform2D other) {
        if (other instanceof Affine) {
            Affine oa = (Affine) other;
            if (oa.getShearX() == 0 && oa.getShearY() == 0) {
                double xs = oa.getScaleX();
                double ys = oa.getScaleY();
                double tx = oa.getTranslateX();
                double ty = oa.getTranslateY();
                setRectangle(new Rectangle2D.Double(x * xs + tx, y * ys + ty,
                                                        w * xs, h * ys));
                return;
            }
        }
        throw new IllegalArgumentException
            ("implemented only for Affine transformations\n"
             + "with no shear, not for\n"
             + other);
    }

    @Override public boolean equals(Object other0) {
        if (this == other0) return true;
        if (other0 == null || getClass() != other0.getClass()) return false;
        RectToQuadCommon other = (RectToQuadCommon) other0;
        return x == other.x && y == other.y && w == other.w && h == other.h
            && llx == other.llx && lly == other.lly
            && ulx == other.ulx && uly == other.uly
            && lrx == other.lrx && lry == other.lry
            && urx == other.urx && ury == other.ury;
    }
}
