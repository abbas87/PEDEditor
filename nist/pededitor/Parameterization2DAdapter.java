package gov.nist.pededitor;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/** Parameterize a Bezier curve. */
public abstract class Parameterization2DAdapter
    implements BoundedParam2D {
     /* Allowed t values belong to [t0, t1]. */
    double t0;
    double t1;
    /** Start (position at t=0) */
    Point2D.Double p0;
    /** End (position at t=1) */
    Point2D.Double pEnd;

    public Parameterization2DAdapter
        (Point2D p0, Point2D pEnd, double t0, double t1) {
        this.t0 = t0;
        this.t1 = t1;
        this.p0 = new Point2D.Double(p0.getX(), p0.getY());
        this.pEnd = new Point2D.Double(pEnd.getX(), pEnd.getY());
    }
    
    @Override public double getMinT() { return t0; }
    @Override public double getMaxT() { return t1; }

    @Override public double getLastVertex(double t) { return 0; }
    @Override public double getNextVertex(double t) { return 1; }

    @Override public Point2D.Double getStart() {
        return getLocation(getMinT());
    }

    @Override public Point2D.Double getEnd() {
        return getLocation(getMaxT());
    }

    @Override public CurveDistance distance(Point2D p, double t) {
        Point2D.Double pt = getLocation(t);
        return new CurveDistance(t, pt, pt.distance(p));
    }

    /** If your distance() method is exact, you should override this
        method to point to that one instead. */
    @Override public CurveDistanceRange distance
        (Point2D p, double maxError, int maxSteps) {
        return BoundedParam2Ds.distance(this, p, maxError, maxSteps);
    }

    @Override public BoundedParam2D[] subdivide() {
        return BoundedParam2Ds.subdivide(this);
    }

    public boolean inDomain(double t) { return t >= t0 && t <= t1; }

    @Override abstract public BoundedParam2D derivative();
    @Override abstract public CurveDistanceRange distance(Point2D p);
    @Override abstract public Rectangle2D.Double getBounds();
    @Override abstract public Point2D.Double getDerivative(double t);
    @Override abstract public Point2D.Double getLocation(double t);
}
