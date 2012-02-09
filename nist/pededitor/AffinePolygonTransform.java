package gov.nist.pededitor;

import java.awt.geom.*;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@JsonIgnoreProperties
    ({"scaleX", "scaleY", "shearX", "shearY", "translateX", "translateY",
      "identity", "determinant", "type", "flatMatrix" })
public abstract class AffinePolygonTransform extends Affine
                                              implements PolygonTransform {
        
	private static final long serialVersionUID = 7329000844756906085L;
	/** The polygon's input vertices. */
    public abstract Point2D.Double[] getInputVertices();
    /** The polygon's output vertices. */
    public abstract Point2D.Double[] getOutputVertices();
    public abstract Rectangle2D.Double inputBounds();
    public abstract Rectangle2D.Double outputBounds();
    public abstract AffinePolygonTransform clone();
}
