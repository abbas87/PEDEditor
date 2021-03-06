/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/** A class for pairing a CuspInterp2D with its color, stroke, fill,
    and/or line width. */
public class ArcDecoration extends DecorationHasInterp2D
    implements CurveCloseable, Fillable {

    public ArcDecoration() {}

    public ArcDecoration(ArcInterp2D curve) {
        super(curve);
    }

    @JsonProperty("curve") public ArcInterp2D getArcInterp2D() {
        return (ArcInterp2D) curve;
    }

    @JsonProperty("curve") public void setCurve(ArcInterp2D curve) {
        this.curve = curve;
    }

    @Override public boolean isDegenerate() {
        return getCurve().size() < 2;
    }

    @Override public String typeName() {
        return "arc";
    }

    @Override public ArcDecoration clone() {
        ArcDecoration res = new ArcDecoration();
        res.copyFrom(this);
        return res;
    }

    @Override public DecorationHandle[] getHandles(DecorationHandle.Type type) {
        ArrayList<DecorationHandle> res = new ArrayList<>();
        res.add(new ArcCenterHandle(this));
        if (type == DecorationHandle.Type.SELECTION) {
            if (!isClosed()) {
                res.add(createHandle(0));
                res.add(createHandle(getCurve().size() - 1));
            }
        } else {
            int size = getCurve().size();
            for (int j = 0; j < size; ++j) {
                res.add(createHandle(j));
            }
        }
        return res.toArray(new DecorationHandle[0]);
    }

    /** For testing purposes only; could be safely deleted. */
    public static void main(String[] args) {
        String filename = "/eb/arc-test.json";

        Point2D[] points1 = new Point2D[]
            { new Point2D.Double(3.1, 5.7),
              new Point2D.Double(0.0, 0.1) };
        Point2D[] points2 = new Point2D[]
            { points1[0], points1[1],
              new Point2D.Double(4.5, 1.2),
              new Point2D.Double(9.1, 10.1) };

        ArcInterp2D pol = new ArcInterp2D(Arrays.asList(points2), false);
        ArcDecoration o = new ArcDecoration(pol);
        o.setLineWidth(1.3);

        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            mapper.writeValue(new File(filename), o);
            ArcDecoration o2 = mapper.readValue(new File(filename),
                                               ArcDecoration.class);
            System.out.println(o2);
            System.out.println(Diagram.getObjectMapper().writeValueAsString(o2));
        } catch (JsonGenerationException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
