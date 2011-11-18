package gov.nist.pededitor;

import java.awt.geom.*;
import java.util.*;
import java.text.*;

/** Class to hold information about the y coordinate of an affine
    transformation of the principal coordinates. */
public class AffineYAxisInfo extends AxisInfo {
    public AffineTransform at;
    Point2D.Double dummy1 = new Point2D.Double();
    Point2D.Double dummy2 = new Point2D.Double();

    public AffineYAxisInfo(AffineTransform at, NumberFormat format) {
        super(format);
        this.at = at;
    }

    public AffineYAxisInfo(AffineTransform at) {
        this(at, new DecimalFormat("##0.0"));
    }

    public double value(double px, double py) {
        dummy1.setLocation(px, py);
        at.transform(dummy1, dummy2);
        return dummy2.y;
    }
}
