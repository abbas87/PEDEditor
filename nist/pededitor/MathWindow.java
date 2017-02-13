/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.geom.Point2D;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.codehaus.jackson.annotate.JsonIgnore;

/** GUI to display information about a tangency point in the
    diagram. */
public class MathWindow extends JDialog {
    private static final long serialVersionUID = 1686051698640332170L;

    protected JTextField angle = new JTextField(9);
    protected JTextField slope = new JTextField(12);

    protected JLabel length = new JLabel("0.0000000");
    protected JLabel lengthLabel = new JLabel("Arc length:");

    protected JLabel totLength = new JLabel("0.0000000");
    protected JLabel totLengthLabel = new JLabel("Total length:");

    protected JLabel area = new JLabel("0.0000000");
    protected JLabel areaLabel = new JLabel("\u222BdY/dX:");

    protected JLabel totArea = new JLabel("0.0000000");
    protected JLabel totAreaLabel = new JLabel("Total \u222B:");
    {
        lengthLabel.setToolTipText("Arc length from left curve endpoint to here");
        totLengthLabel.setToolTipText("Perimeter or total curve length");
        areaLabel.setToolTipText("Curve integral from left endpoint to here");
        totAreaLabel.setToolTipText("Area or integral under whole curve");
        length.setToolTipText(lengthLabel.getToolTipText());
        area.setToolTipText(areaLabel.getToolTipText());
        totLength.setToolTipText(totLengthLabel.getToolTipText());
        totArea.setToolTipText(totAreaLabel.getToolTipText());
    }

    /** These values provide greater precision than angle.getText()
     * and slope.getText() do. */

    protected double angled = 0;
    protected double sloped = 0;
    protected double lineWidthd = 0;
    protected BasicEditor parentEditor = null;

    public boolean selfModifying = false;
    JLabel slopeLabel = new JLabel("d####.../d####....");
    public JLabel lineWidth = new JLabel("0.00000");

    public MathWindow(BasicEditor parentEditor) {
        super(parentEditor.editFrame, "Math", false);
        this.parentEditor = parentEditor;
        setAngleDegrees(0);

        slope.getDocument().addDocumentListener
            (new DocumentListener() {
                    @Override public void changedUpdate(DocumentEvent e) {
                        if (selfModifying) {
                            // This event was presumably generated by
                            // another event.
                            return;
                        }
                        try {
                            setSlope
                                (ContinuedFraction.parseDouble(slope.getText()),
                                 false);
                        } catch (NumberFormatException ex) {
                            return;
                        }
                    }

                    @Override public void insertUpdate(DocumentEvent e) {
                        changedUpdate(e);
                    }

                    @Override public void removeUpdate(DocumentEvent e) {
                        changedUpdate(e);
                    }
                });

        angle.getDocument().addDocumentListener
            (new DocumentListener() {
                    @Override public void changedUpdate(DocumentEvent e) {
                        if (selfModifying) {
                            // This event was presumably generated by
                            // another event.
                            return;
                        }
                        try {
                            setAngleDegrees
                                (ContinuedFraction.parseDouble(angle.getText()),
                                 false);
                        } catch (NumberFormatException ex) {
                            return;
                        }
                    }

                    @Override public void insertUpdate(DocumentEvent e) {
                        changedUpdate(e);
                    }

                    @Override public void removeUpdate(DocumentEvent e) {
                        changedUpdate(e);
                    }
                });

        GridBagUtil gb = new GridBagUtil(getContentPane());

        gb.addEast(new JLabel("Angle:"));
        gb.addWest(angle);
        gb.endRowWith(new JLabel("\u00B0") /* degree symbol */);

        gb.addEast(slopeLabel);
        gb.endRowWith(slope);

        gb.addEast(lengthLabel);
        gb.endRowWith(length);

        gb.addEast(totLengthLabel);
        gb.endRowWith(totLength);

        gb.addEast(areaLabel);
        gb.endRowWith(area);

        gb.addEast(totAreaLabel);
        gb.endRowWith(totArea);

        gb.addEast(new JLabel("Line width:"));
        gb.endRowWith(lineWidth);
        pack();
        setDerivative(null);
        setLineWidth(0.0);
        setResizable(false);
    }

    public BasicEditor getParentEditor() { return parentEditor; }

    /** Like setDerivative(), but the derivative is expressed in
     standard page coordinates, not principal coordinates. */
    public void setScreenDerivative(Point2D p) {
        Affine af = getParentEditor().standardPageToPrincipal;
        if (af == null) {
            setDerivative(p);
        } else {
            setDerivative(af.deltaTransform(p, new Point2D.Double()));
        }
    }

    public void setDerivative(Point2D point) {
        double x = (point == null) ? 0 : point.getX();
        double y = (point == null) ? 0 : point.getY();
        if (x == 0 && y == 0) {
            angle.setText("");
            slope.setText("");
            angled = Double.NaN;
            sloped = Double.NaN;
            return;
        }

        if (x == 0) {
            sloped = (y > 0) ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
            slope.setText("");
        } else {
            setSlope(y/x);
        }

        Point2D.Double p = new Point2D.Double();
        getParentEditor().principalToStandardPage.deltaTransform(point, p);
        setAngle(Math.atan2(p.y, p.x));
    }

    static double thetaToDegrees(double theta) {
        return normalizeDegrees180(-theta * 180 / Math.PI);
    }

    /** Turn values almost exactly equal to 90 degrees into -90
        degrees so it's not a coin flip whether a nearly-vertical line
        ends up being displayed as pointing upwards (90 degrees) or
        downwards (-90). Take angles that point leftwards and rotate
        them 180 degrees so they point rightwards. */
    static double normalizeDegrees180(double deg) {
        if (deg < -90 - 1e-10) {
            deg += 180;
        } else if (deg > 90 - 1e-10) {
            deg -= 180;
        }
        return deg;
    }

    /** Like normalizeDegrees180 but for radians.  */
    static double normalizeRadians180(double theta) {
        if (theta > Math.PI/2 + 1e-12) {
            theta -= Math.PI;
        } else if (theta < -Math.PI/2 + 1e-12) {
            theta += Math.PI;
        }
        return theta;
    }

    static public double degreesToTheta(double deg) {
        return -deg * Math.PI / 180;
    }

    /** Return true if v1 and v2 are so close to parallel that they
        approach the limits of double precision floating point
        numbers. */
    static boolean nearlyParallel(Point2D.Double v1, Point2D.Double v2) {
        return 1e13 * Math.abs(v1.x * v2.y - v1.y * v2.x)
            < Math.abs(v1.x * v2.x + v1.y * v2.y);
    }

    public double thetaToSlope(double theta) {
        Point2D.Double p = new Point2D.Double
            (Math.cos(theta), Math.sin(theta));
        Affine af = getParentEditor().standardPageToPrincipal;
        if (af == null) {
            return Double.NaN;
        }

        // Don't show values like -5.7e16 when the slope is +/-
        // infinity to the limits of precision.
        Affine afInverse = getParentEditor().principalToStandardPage;
        Point2D.Double vert = new Point2D.Double(0, 1);
        afInverse.deltaTransform(vert, vert);
        if (nearlyParallel(p, vert)) { // Nearly vertical
            return Double.NaN;
        }
        // Don't show values like -5.7e-16 when the slope is zero to
        // the limits of precision.
        Point2D.Double horiz = new Point2D.Double(1, 0);
        afInverse.deltaTransform(horiz, horiz);
        if (nearlyParallel(p, vert)) { // Nearly horizontal
            return 0;
        }

        af.deltaTransform(p, p);
        return p.y/p.x;
    }

    public double slopeToTheta(double s) {
        Point2D.Double p = new Point2D.Double(1.0, s);
        Affine af = getParentEditor().principalToStandardPage;
        if (af != null) {
            af.deltaTransform(p, p);
        }
        return Math.atan2(p.y, p.x);
    }

    public void setAngle(double theta) {
        setAngleDegrees(thetaToDegrees(theta));
    }

    public void setAngleDegrees(double deg) {
        setAngleDegrees(deg, true);
    }

    protected void setAngleDegrees(double deg, boolean changeDegreeText) {
        try {
            selfModifying = true;
            basicSetAngleDegrees(deg, changeDegreeText);
            basicSetSlope(thetaToSlope(degreesToTheta(deg)), true);
            repaint();
        } finally {
            selfModifying = false;
        }
    }

    protected void basicSetAngleDegrees(double deg, boolean changeText) {
        angled = deg;
        if (changeText) {
            angle.setText(ContinuedFraction.fixMinusZero(String.format("%.2f", deg)));
        }
    }

    @JsonIgnore public double getAngleDegrees() {
        return angled;
    }

    /** Return the angle in radians, where 0 is straight right and
        values increase clockwise. */
    public double getAngle() {
        return Compass.degreesToTheta(angled);
    }

    public void setSlope(double m) {
        setSlope(m, true);
    }

    protected void setSlope(double m, boolean changeSlopeText) {
        try {
            selfModifying = true;
            basicSetSlope(m, changeSlopeText);
            basicSetAngleDegrees(thetaToDegrees(slopeToTheta(m)), true);
            repaint();
        } finally {
            selfModifying = false;
        }
    }

    protected void basicSetSlope(double m, boolean changeText) {
        sloped = m;
        if (changeText) {
            if (Double.isNaN(m)) {
                slope.setText("");
            } else {
                slope.setText(ContinuedFraction.toDecimalString(sloped, 4));
            }
        }
    }

    public void setSlopeLabel(String label) {
        slopeLabel.setText(label + ":");
        slopeLabel.repaint();
        areaLabel.setText("\u222B" + label + ":");
        areaLabel.repaint();
    }

    public void setTotAreaLabel(String label) {
        totAreaLabel.setText(label + ":");
    }

    public void setTotLengthLabel(String label) {
        totLengthLabel.setText(label + ":");
    }

    static String truncatedName(LinearAxis ax, String defaultName) {
        String s = defaultName;
        if (ax != null && ax.name != null) {
            s = (String) ax.name;
        }
        if (s.length() >= 6) {
            return s.substring(0,4) + "...";
        } else {
            return s;
        }
    }

    /** Set the slope label automatically from the parent BasicEditor's
        settings. */
    void setSlopeLabel() {
        BasicEditor e = getParentEditor();
        String label = "dY/dX";
        if (e.haveDiagram()) {
            label = "d" + truncatedName(e.getYAxis(), "y")
                + "/d" + truncatedName(e.getXAxis(), "x");
        }
        setSlopeLabel(label);
    }

    /** Set the slope label automatically from the parent BasicEditor's
        settings. */
    public void refresh() {
        setSlopeLabel();
        setAngle(0);
        setLength(0);
        setArea(0);
        setTotLength(0);
        setTotArea(0);
        boolean b = !getParentEditor().isTernary();
        setLengthVisible(b);
        setAreaVisible(b);
    }

    public void setLength(double v) {
        length.setText(ContinuedFraction.toDecimalString(v, 4));
    }

    public void setTotLength(double v) {
        totLength.setText(ContinuedFraction.toDecimalString(v, 4));
    }

    public void setArea(double v) {
        area.setText(ContinuedFraction.toDecimalString(v, 4));
    }

    public void setTotArea(double v) {
        totArea.setText(ContinuedFraction.toDecimalString(v, 4));
    }

    public double getSlope() {
        return sloped;
    }

    public void setAreaVisible(boolean b) {
        area.setVisible(b);
        areaLabel.setVisible(b);
        totArea.setVisible(b);
        totAreaLabel.setVisible(b);
    }

    public void setLengthVisible(boolean b) {
        length.setVisible(b);
        lengthLabel.setVisible(b);
        totLength.setVisible(b);
        totLengthLabel.setVisible(b);
    }

    public void setLineWidth(double w) {
        lineWidthd = w;
        lineWidth.setText(String.format("%.5f", w));
        repaint();
    }

    public double getLineWidth() {
        return lineWidthd;
    }
}