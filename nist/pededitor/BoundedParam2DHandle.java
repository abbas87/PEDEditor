/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

/** Interface for a DecorationHandle that is associated with a point
    on a BoundedParam2D. */
public interface BoundedParam2DHandle extends DecorationHandle, BoundedParameterizable2D {
    @Override BoundedParam2D getParameterization();
    double getT();
}