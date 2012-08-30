package gov.nist.pededitor;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;

public class TieLine implements Decorated {
    /** Number of tie lines. Lines are painted only on the interior of
        the tie line region; the angle at which
        outerEdge(ot1)innerEdge(it1) and outerEdge(ot2)innerEdge(it2)
        meet is sectioned into lineCnt+1 equal parts. */
    public int lineCnt;
    public StandardStroke stroke;
    public double lineWidth = 1.0;
    protected Color color = null;

    /** Each tie line starts at innerEdge somewhere along [it1, it2]
        (the two values may be equal for triangular tie line regions)
        and ends somewhere along outerEdge along [ot1, ot2]. */

    @JsonProperty("innerT1") public double it1 = -1.0;
    @JsonProperty("innerT2") public double it2 = -1.0;
    @JsonIgnore public CuspFigure innerEdge;

    /** Used only during JSON deserialization. Later, use getInnerID()
        instead. */
    int innerId = -1;

    @JsonProperty("outerT1") public double ot1 = -1.0;
    @JsonProperty("outerT2") public double ot2 = -1.0;
    @JsonIgnore public CuspFigure outerEdge;

    /** Used only during JSON deserialization. Later, use getOuterID()
        instead. */
    int outerId = -1;

    public TieLine(int lineCnt, StandardStroke stroke) {
        this.lineCnt = lineCnt;
        this.stroke = stroke;
    }

    @JsonCreator
    TieLine(@JsonProperty("lineCnt") int lineCnt,
            @JsonProperty("lineStyle") StandardStroke stroke,
            @JsonProperty("innerId") int innerId,
            @JsonProperty("innerT1") double it1,
            @JsonProperty("innerT2") double it2,
            @JsonProperty("outerId") int outerId,
            @JsonProperty("outerT1") double ot1,
            @JsonProperty("outerT2") double ot2) {
        this.lineCnt = lineCnt;
        this.stroke = stroke;

        this.innerId = innerId;
        this.it1 = it1;
        this.it2 = it2;

        this.outerId = outerId;
        this.ot1 = ot1;
        this.ot2 = ot2;
    }

    /** @return null unless this polyline has been assigned a
        color. */
    public Color getColor() {
        return color;
    }

    /** Set the color. Use null to indicate that the color should be
        the same as whatever was last chosen for the graphics
        context. */
    public void setColor(Color color) {
        this.color = color;
    }

    /** Used during JSON serialization. */
    @JsonProperty int getInnerId() {
        return (innerEdge == null) ? -1 : innerEdge.getJSONId();
    }

    /** Used during JSON serialization. */
    @JsonProperty int getOuterId() {
        return (outerEdge == null) ? -1 : outerEdge.getJSONId();
    }

    public Point2D.Double getInnerEdge(double t) {
        return innerEdge.getLocation(t);
    }

    public Point2D.Double getOuterEdge(double t) {
        return outerEdge.getLocation(t);
    }

    /** Return the location of endpoint #1 of the inner edge. */
    @JsonIgnore public Point2D.Double getInner1() {
        return getInnerEdge(it1);
    }

    /** Return the location of endpoint #2 of the inner edge. */
    @JsonIgnore public Point2D.Double getInner2() {
        return getInnerEdge(it2);
    }

    /** Return the location of endpoint #1 of the outer edge. */
    @JsonIgnore public Point2D.Double getOuter1() {
        return getOuterEdge(ot1);
    }

    /** Return the location of endpoint #2 of the outer edge. */
    @JsonIgnore public Point2D.Double getOuter2() {
        return getOuterEdge(ot2);
    }

    @JsonIgnore
    public Line2D.Double getEdge1() {
        return new Line2D.Double(getInner1(), getOuter1());
    }

    @JsonIgnore
    public Line2D.Double getEdge2() {
        return new Line2D.Double(getInner2(), getOuter2());
    }

    /** Return the point at which all tie lines converge. */
    public Point2D.Double convergencePoint() {
        return Duh.lineIntersection(getOuter1(), getInner1(),
                                    getOuter2(), getInner2());
    }

    /** Return true if the inner and outer edges are oriented in
        opposite directions. If left alone, convergencePoint() will
        lie somewhere between the two edges instead of somewhere
        beyond them, which is almost certainly not the intent. */
    boolean isTwisted() {
        Point2D.Double i1 = getInner1();
        Point2D.Double i2 = getInner2();
        Point2D.Double o1 = getOuter1();
        Point2D.Double o2 = getOuter2();

        double dot = (i2.x - i1.x) * (o2.x - o1.x)
            + (i2.y - i1.y) * (o2.y - o1.y);

        return (dot < 0);
    }

    @JsonIgnore public Path2D.Double getPath() {
        Path2D.Double output = new Path2D.Double();
        BoundedParam2D innerParam = innerEdge.getParameterization();
        BoundedParam2D outerParam = outerEdge.getParameterization();

        if (isTwisted()) {
            // Swap ot1 <=> ot2 to untwist.
            double tmp = ot1;
            ot1 = ot2;
            ot2 = tmp;
        }

        Point2D.Double i1 = getInner1();
        Point2D.Double i2 = getInner2();
        Point2D.Double o1 = getOuter1();
        Point2D.Double o2 = getOuter2();
        Point2D.Double v = convergencePoint();

        if (v == null) {
            return output;
        }

        // For triangular tie-line regions, i1.equals(i2) or
        // o1.equals(o2), but the midpoint of i1o1 never equals the
        // midpoint of i2o2.

        Point2D.Double mid1 = new Point2D.Double
            ((i1.x + o1.x) / 2, (i1.y + o1.y) / 2);
        Point2D.Double mid2 = new Point2D.Double
            ((i2.x + o2.x) / 2, (i2.y + o2.y) / 2);
        double theta1 = Math.atan2(mid1.y - v.y, mid1.x - v.x);
        double theta2 = Math.atan2(mid2.y - v.y, mid2.x - v.x);

        // Determine whether to proceed clockwise or counterclockwise
        // from theta1 to theta2. We'll do this by taking the midpoint
        // of the segment from the middle t value of the inner edge to
        // the middle t value of the outer edge.
        Point2D.Double iMid = getInnerEdge((it1 + it2) / 2);
        Point2D.Double oMid = getOuterEdge((ot1 + ot2) / 2);
        Point2D.Double midMid = new Point2D.Double
            ((iMid.x + oMid.x) / 2, (iMid.y + oMid.y) / 2);

        double thetaMid = Math.atan2(midMid.y - v.y, midMid.x - v.x);

        // The correct direction of rotation from theta1 to theta2
        // passes through thetaMid; the incorrect direction does not.
        final double twoPi = 2 * Math.PI;

        // Force theta2 >= theta1.
        double theta2Minus1 = theta2 - theta1;
        theta2Minus1 -= twoPi * Math.floor(theta2Minus1 / twoPi);
        theta2 = theta1 + theta2Minus1;

        double thetaMidMinus1 = thetaMid - theta1;
        thetaMidMinus1 -= twoPi * Math.floor(thetaMidMinus1 / twoPi);

        double oldit = it1;
        double oldot = ot1;

        /* oneToTwo is true if the tie lines should be starting next
           to inner1outer1 and ending next to inner2outer2, and false
           if the tie lines should be drawn in the reverse order. */
        boolean oneToTwo = theta2Minus1 >= thetaMidMinus1;

        if (!oneToTwo) {
            // Theta1 and theta2 are out of order. Swap them.
            double tmp = theta1;
            theta1 = theta2;
            theta2 = tmp;

            oldit = it2;
            oldot = ot2;

            // Force theta2 >= theta1.
            theta2Minus1 = theta2 - theta1;
            theta2Minus1 -= twoPi * Math.floor(theta2Minus1 / twoPi);
            theta2 = theta1 + theta2Minus1;
        }

        double deltaTheta = (theta2 - theta1) / (lineCnt + 1);
        double theta = theta1;

        for (int i = 0; i < lineCnt; ++i) {
            theta += deltaTheta;
            Line2D.Double line = new Line2D.Double
                (v.x, v.y, v.x + Math.cos(theta), v.y + Math.sin(theta));

            Point2D.Double innerPoint = i1;
            if (!i1.equals(i2)) {
                double minDeltaT = -1;
                double newit = 0;

                for (double t: innerParam.lineIntersections(line)) {
                    if ((it1 < t) == (t < it2) && t != oldit
                        && ((t > oldit) == (it2 > it1)) == oneToTwo) {
                        // Look for the t value closest to the previous t value.
                        double deltaT = Math.abs(t - oldit);
                        if (minDeltaT < 0 || deltaT < minDeltaT) {
                            innerPoint = innerParam.getLocation(t);
                            minDeltaT = deltaT;
                            newit = t;
                        }
                    }
                }
                oldit = newit;
            }

            Point2D.Double outerPoint = o1;
            if (!o1.equals(o2)) {
                double minDeltaT = -1;
                double newot = 0;

                for (double t: outerParam.lineIntersections(line)) {
                    if ((ot1 < t) == (t < ot2) && t != oldot
                        && ((t > oldot) == (ot2 > ot1)) == oneToTwo) {
                        // Look for the t value closest to the previous t value.
                        double deltaT = Math.abs(t - oldot);
                        if (minDeltaT < 0 || deltaT < minDeltaT) {
                            outerPoint = outerParam.getLocation(t);
                            minDeltaT = deltaT;
                            newot = t;
                        }
                    }
                }
                oldot = newot;
            }

            output.moveTo(innerPoint.x, innerPoint.y);
            output.lineTo(outerPoint.x, outerPoint.y);
        }

        return output;
    }

    /** @return a new TieLines that is like this one, but xform has
        been applied to its control points. Note that the smooth of
        the transform is generally not the same as the transform of
        the smoothing. */
    public TieLine createTransformed(AffineTransform xform) {
        TieLine res = new TieLine(lineCnt, stroke);
        res.it1 = it1;
        res.it2 = it2;
        res.ot1 = ot1;
        res.ot2 = ot2;
        res.lineWidth = lineWidth;
        res.innerEdge = innerEdge.createTransformed(xform);
        res.outerEdge = outerEdge.createTransformed(xform);
        res.setColor(getColor());
        return res;
    }

    /** Draw the path of this TieLine. The coordinates for
        this path should be defined in the "Original" coordinate
        system, but line widths are defined with respect to the
        "SquarePixel" coordinate system. Also, the output is scaled by
        "scale" before being drawn.
    */
    public void draw(Graphics2D g,
                     AffineTransform originalToSquarePixel,
                     double scale) {
        AffineTransform xform = AffineTransform.getScaleInstance(scale, scale);
        xform.concatenate(originalToSquarePixel);
        createTransformed(xform).draw(g, scale);
    }

    public void draw(Graphics2D g, double scale) {
        stroke.getStroke().draw(g, getPath(), scale * lineWidth);
    }

    @Override
	public String toString() {
        return "TieLines[lineCnt=" + lineCnt + ", stroke = " + stroke
            + ", lineWidth = " + lineWidth
            + ", inner = " + innerEdge + ",  outer = " + outerEdge
            + ", ot1 = " + ot1 + ", ot2 = " + ot2
            + ", it1 = " + it1 + ", it2 = " + it2 + "]";
    }
}
