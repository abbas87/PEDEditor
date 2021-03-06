/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.*;

/** Utility class that provides some standard BasicStroke styles. */
public class BasicStrokes {
    /** @return a copy of "stroke" with its line width and dash
        pattern lengths scaled by a factor of "scaled". */
    public static BasicStroke scaledStroke(BasicStroke stroke, double scaled) {
        if (scaled == 1.0) {
            return stroke;
        }

        float scale = (float) scaled;
        float[] dashes = stroke.getDashArray();

        if (dashes != null) {
            dashes = (float[]) dashes.clone();
            for (int i = 0; i < dashes.length; ++i) {
                dashes[i] *= scale;
            }
        }

        if (scale == 0) {
            throw new IllegalStateException("Zero scale");
        }

        return new BasicStroke(stroke.getLineWidth() * scale,
                               stroke.getEndCap(), stroke.getLineJoin(),
                               stroke.getMiterLimit(), dashes,
                               stroke.getDashPhase() * scale);
    }

    /** @return a copy of "stroke" with its line width and dash
        pattern lengths scaled by a factor of "scaled". Also, the cap
        and join settings are rounded if "round" is true or square
        otherwise
.
        That's only approximately true. If round is true, then the cap
        and join settings are not changed at all -- but most
        StandardStrokes use rounded settings to begin with. If round
        is false, then a cap setting of ROUND is changed to BUTT, and
        a join setting of ROUND is changed to MITER. */
    public static BasicStroke scaledStroke(BasicStroke stroke, double scaled,
                                           boolean round) {
        float scale = (float) scaled;
        float[] dashes = stroke.getDashArray();

        if (dashes != null) {
            dashes = (float[]) dashes.clone();
            for (int i = 0; i < dashes.length; ++i) {
                dashes[i] *= scale;
            }
        }

        if (scale == 0) {
            throw new IllegalStateException("Zero scale");
        }

        int cap = stroke.getEndCap();
        int join = stroke.getLineJoin();
        if (!round) {
            if (join == BasicStroke.JOIN_ROUND)
                join = BasicStroke.JOIN_MITER;
            if (cap == BasicStroke.CAP_ROUND)
                cap = BasicStroke.CAP_SQUARE;
        }

        return new BasicStroke(stroke.getLineWidth() * scale,
                               cap, join, stroke.getMiterLimit(),
                               dashes, stroke.getDashPhase() * scale);
    }

    public static BasicStroke getSolidLine() {
        return new BasicStroke
            (1.0f,
             BasicStroke.CAP_ROUND,
             BasicStroke.JOIN_ROUND);
    }

    public static BasicStroke getDottedLine() {
        return getDottedLine(5.4f);
    }

    public static BasicStroke getDottedLine(double dashPeriod) {
        return new BasicStroke
            (1.8f,
             BasicStroke.CAP_ROUND,
             BasicStroke.JOIN_ROUND,
             3.0f,
             new float[] { 0, (float) dashPeriod },
             0.0f);
    }

    /** Return a dashed line that is about 64% filled, accounting for
        the leading and trailing diameter-1 semicircles */
    public static BasicStroke getDashedLine(double dashPeriod) {
        double density = 0.642822018f;
        double on = dashPeriod * density - Math.PI / 4;
        return getDashedLine(on, dashPeriod - on);
    }

    public static BasicStroke getDashedLine(double on, double off) {
        return new BasicStroke
            (1.0f,
             BasicStroke.CAP_ROUND,
             BasicStroke.JOIN_ROUND,
             3.0f,
             new float[] { (float) on, (float) off },
             0.0f);
    }

    public static BasicStroke getBlankFirstDashedLine() {
        return new BasicStroke
            (1.0f,
             BasicStroke.CAP_ROUND,
             BasicStroke.JOIN_ROUND,
             3.0f,
             new float[] { 5, 4 },
             7.0f);
    }

    /** Plot nothing. */
    public static BasicStroke getInvisibleLine() {
        return new BasicStroke
            (1.0f,
             BasicStroke.CAP_ROUND,
             BasicStroke.JOIN_ROUND,
             3.0f,
             new float[] { 0, 1e6f },
             2.0f);
    }
}
