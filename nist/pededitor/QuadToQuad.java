/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.geom.*;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Class that transforms the boundary of an arbitrary convex
    quadrilateral into the boundary of another arbitrary convex
    quadrilateral. The transformation can be a bit slow and may not be
    the most robust one available; even transforming a quadrilateral
    into itself, while it will behave as an identity transformation as
    expected inside the quadrilateral, may return an
    UnsolvableException for points sufficiently far outside of it.
    (The transformation will behave sensibly for all inputs if the
    input and output quadrilaterals are parallelograms, however.) The
    implementation is as follows: the input quadrilateral is
    transformed into a unit square with QuadToRect, and the unit
    square is transformed into the output quadrilateral with
    RectToQuad.
 */
public class QuadToQuad
    extends PolygonTransformAdapter
    implements QuadrilateralTransform {
    protected QuadToRect q2r = new QuadToRect();
    protected RectToQuad r2q = new RectToQuad();

    public void setInputVertices(Point2D.Double[] vertices) {
        q2r.setVertices(vertices);
    }

    public void setOutputVertices(Point2D.Double[] vertices) {
        r2q.setVertices(vertices);
    }

    public QuadToQuad() {
    }

    public QuadToQuad(@JsonProperty("input") Point2D.Double[] ins,
                      @JsonProperty("output") Point2D.Double[] outs) {
        q2r.setVertices(ins); 
        r2q.setVertices(outs); 
    }

    @Override
	public Transform2D createInverse() {
        return new QuadToQuad(getOutputVertices(), getInputVertices());
    }

    @Override
	public Point2D.Double[] getInputVertices() {
        return q2r.getInputVertices();
    }

    @Override
	public Point2D.Double[] getOutputVertices() {
        return r2q.getOutputVertices();
    }

    @Override
	public void preConcatenate(Transform2D other) {
        // This is a little sketchy, in that there is no guarantee
        // that the resulting transform will behave identically to
        // transforming by this and then transforming by other.
        r2q.preConcatenate(other);
    }
    @Override
	public void concatenate(Transform2D other) {
        q2r.concatenate(other);
    }

    @Override
	public Transform2D squareToDomain() {
        return q2r.createInverse();
    }

    @Override
	public QuadToQuad clone() {
        return new QuadToQuad(getInputVertices(), getOutputVertices());
    }

    @Override
	public Point2D.Double transform(double x, double y)
        throws UnsolvableException {
        return r2q.transform(q2r.transform(x,y));
    }

    @Override
	public void transform(double[] srcPts, int srcOff,
                          double[] dstPts, int dstOff, int numPts)
        throws UnsolvableException {
        q2r.transform(srcPts, srcOff, dstPts, dstOff, numPts);
        r2q.transform(srcPts, srcOff, dstPts, dstOff, numPts);
    }
}
