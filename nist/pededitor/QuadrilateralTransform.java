/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

interface QuadrilateralTransform extends PolygonTransform {
   /** The transformation from a unit square centered at (0.5,0.5)
    * into the input quadrilateral. The usefulness of this lies in
    * enabling easy identification of reasonably regularly spaced
    * points in the input and output quadrilaterals: start with
    * regularly spaced points in the unit square, and transform
    * them. */
   Transform2D squareToDomain();
}
