/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Enumeration;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

/** GUI for updating a LinearRuler. */

public class RulerDialog extends JDialog {
    private static final long serialVersionUID = -2793746548149999804L;

    abstract class Action extends AbstractAction {
        private static final long serialVersionUID = -6218060076708914216L;

        Action(String name) {
            super(name);
        }
    }

    class LabelAnchorAction extends AbstractAction {
        private static final long serialVersionUID = -3652297861974269074L;

        LinearRuler.LabelAnchor labelAnchor;

        LabelAnchorAction(String name, LinearRuler.LabelAnchor anchor) {
            super(name);
            this.labelAnchor = anchor;
        }

        @Override public void actionPerformed(ActionEvent e) {
            RulerDialog.this.labelAnchor = labelAnchor;
        }
    }

    class LabelAnchorButton extends JRadioButton {
        private static final long serialVersionUID = 7463710339083034076L;

        LabelAnchorButton(String caption, LinearRuler.LabelAnchor anchor) {
            super(new LabelAnchorAction(caption, anchor));
            setSelected(anchor == RulerDialog.this.labelAnchor);
            labelAnchorGroup.add(this);
        }
    }

    void setLabelAnchor(LinearRuler.LabelAnchor anchor) {
        for (Enumeration<AbstractButton> e = labelAnchorGroup.getElements();
             e.hasMoreElements();) {
            LabelAnchorButton lab = (LabelAnchorButton) e.nextElement();
            LabelAnchorAction act = (LabelAnchorAction) lab.getAction();
            lab.setSelected(act.labelAnchor == anchor);
        }
        labelAnchor = anchor; 
    }

    JCheckBox tickLeft = new JCheckBox("Left-side tick marks");
    JCheckBox tickRight = new JCheckBox("Right-side tick marks");
    JCheckBox tickTypeV = new JCheckBox("V-shaped tick marks");

    JCheckBox startArrow = new JCheckBox("Start arrow");
    JCheckBox endArrow = new JCheckBox("End arrow");

    JCheckBox suppressStartTick = new JCheckBox("Suppress start tick");
    JCheckBox suppressStartLabel = new JCheckBox("Suppress start label");
    JCheckBox suppressEndTick = new JCheckBox("Suppress end tick");
    JCheckBox suppressEndLabel = new JCheckBox("Suppress end label");
    JCheckBox suppressSmallTicks = new JCheckBox("Suppress small ticks");

    JCheckBox showPercentages = new JCheckBox("Use percentages in labels");
    JCheckBox displayLog10 = new JCheckBox
        ("<html>Display 10<sup><var>x</var></sup> in labels");
    JTextField tickPadding = new JTextField("0", 10);
    JTextField textAngle = new JTextField("0", 10);

    JTextField bigTickDelta = new JTextField("", 10);
    JTextField tickDelta = new JTextField("", 10);
    JTextField tickStart = new JTextField("", 10);
    JTextField tickEnd = new JTextField("", 10);
    VariableSelector variable = new VariableSelector();

    boolean pressedOK = false;
    LinearRuler.LabelAnchor labelAnchor = null;
    ButtonGroup labelAnchorGroup = new ButtonGroup();
    JButton okButton = new JButton(new Action("OK") {
            private static final long serialVersionUID = 54912002834702747L;

            @Override public void actionPerformed(ActionEvent e) {
                pressedOK = true;
                setVisible(false);
            }
        });

    protected String helpFile = "rulerhelp.html";
    JButton helpButton = new JButton(new Action("Help") {
            private static final long serialVersionUID = -5968712403650275353L;

            @Override public void actionPerformed(ActionEvent e) {
                ShowHTML.show(helpFile, (JFrame) getOwner());
            }
        });
    LabelAnchorButton leftAnchorButton = new LabelAnchorButton
        ("On left", LinearRuler.LabelAnchor.LEFT);
    LabelAnchorButton rightAnchorButton = new LabelAnchorButton
        ("On right", LinearRuler.LabelAnchor.RIGHT);

    RulerDialog(Frame owner, String title) {
        super(owner, "Edit Ruler", true);
        JPanel contentPane = (JPanel) getContentPane();
        GridBagUtil cpgb = new GridBagUtil(contentPane);

        cpgb.addWest(new JLabel("Tick labels:"));

        {
            JPanel panel = new JPanel();
            GridBagUtil gb = new GridBagUtil(panel);
            gb.addWest
                (new LabelAnchorButton
                 ("None", LinearRuler.LabelAnchor.NONE));
            gb.addWest(leftAnchorButton);
            gb.endRowWith(rightAnchorButton);
            cpgb.endRowWith(panel);
        }

        {
            JLabel variableLabel = new JLabel("Display variable/component:");
            variableLabel.setLabelFor(variable);
            cpgb.addWest(variableLabel);
            cpgb.endRowWith(variable);
        }

        JLabel textAngleLabel = new JLabel
            ("Label angle with respect to axis:");
        textAngleLabel.setLabelFor(textAngle);
        cpgb.addWest(textAngleLabel);
        cpgb.endRowWith(textAngle);
        textAngle.setToolTipText("(degrees counterclockwise from axis)");

        JLabel tickPaddingLabel = new JLabel
            ("<html>Minimum white space between tick<br>"
             + "labels, in multiples of letter height:");
        tickPaddingLabel.setLabelFor(tickPadding);
        cpgb.addWest(tickPaddingLabel);
        cpgb.endRowWith(tickPadding);

        cpgb.addWest(tickLeft);
        cpgb.endRowWith(tickRight);
        cpgb.endRowWith(tickTypeV);
        cpgb.addWest(showPercentages);
        cpgb.endRowWith(displayLog10);

        cpgb.addWest(startArrow);
        cpgb.endRowWith(endArrow);
        cpgb.addWest(suppressStartTick);
        cpgb.endRowWith(suppressEndTick);
        cpgb.addWest(suppressStartLabel);
        cpgb.endRowWith(suppressEndLabel);
        cpgb.endRowWith(suppressSmallTicks);

        {
            JLabel label = new JLabel("Big tick step size (optional):");
            label.setLabelFor(bigTickDelta);
            cpgb.addWest(label);
            cpgb.endRowWith(bigTickDelta);
        }

        {
            JLabel label = new JLabel("Small tick step size (optional):");
            label.setLabelFor(tickDelta);
            cpgb.addWest(label);
            cpgb.endRowWith(tickDelta);
        }

        {
            JLabel label = new JLabel("Minimum tick value (optional):");
            label.setLabelFor(tickStart);
            cpgb.addWest(label);
            cpgb.endRowWith(tickStart);
        }

        {
            JLabel label = new JLabel("Maximum tick value (optional):");
            label.setLabelFor(tickEnd);
            cpgb.addWest(label);
            cpgb.endRowWith(tickEnd);
        }

        cpgb.addEast(okButton);
        cpgb.endRowWith(helpButton);
        getRootPane().setDefaultButton(okButton);
    }

    RulerDialog(Frame owner, String title, LinearRuler ruler,
                ArrayList<LinearAxis> axes) {
        this(owner, title);
        set(ruler, axes);
    }

    /** Change the captions to reflect the actual appearance of the
        ruler, so the left half-plane of the vector from startPoint to
        endPoint may be best described as "left", "right", "up", or
        "down" depending on the angle.

     @param angle: the angle (counterclockwise from east in radians) of the vector
     ruler.startPoint -&gt; ruler.endPoint. */
    void updateLeftRightLabels(double angle) {
        int quadrant = (int) Math.floor((angle + Math.PI/4)/(Math.PI/2));
        quadrant = ((quadrant % 4) + 4) % 4;
        String left, right;
        String start, end;;
        if (quadrant == 0) { // rightish
            left = "Top";
            right = "Bottom";
            start = "Left";
            end = "Right";
        } else if (quadrant == 1) { // upish
            left = "Left";
            right = "Right";
            start = "Bottom";
            end = "Top";
        } else if (quadrant == 2) { // leftish
            left = "Bottom";
            right = "Top";
            start = "Right";
            end = "Left";
        } else { // downish
            left = "Right";
            right = "Left";
            start = "Top";
            end = "Bottom";
        }
        tickLeft.setText(left + "-side tick marks");
        tickRight.setText(right + "-side tick marks");
        leftAnchorButton.setText("On " + left.toLowerCase());
        rightAnchorButton.setText("On " + right.toLowerCase());
        startArrow.setText(start + " arrow");
        endArrow.setText(end + " arrow");
        suppressStartTick.setText("Suppress " + start.toLowerCase() + " tick");
        suppressEndTick.setText("Suppress " + end.toLowerCase() + " tick");
        suppressStartLabel.setText("Suppress " + start.toLowerCase() + " label");
        suppressEndLabel.setText("Suppress " + end.toLowerCase() + " label");
    }

    public void set(LinearRuler ruler, ArrayList<LinearAxis> axes) {
        setLabelAnchor(ruler.labelAnchor); 
        variable.setAxes(axes);
        variable.setSelected(ruler.axis);

        tickLeft.setSelected(ruler.tickLeft);
        tickRight.setSelected(ruler.tickRight);
        tickTypeV.setSelected(ruler.tickType == LinearRuler.TickType.V);
        startArrow.setSelected(ruler.startArrow);
        endArrow.setSelected(ruler.endArrow);
        suppressStartTick.setSelected(ruler.suppressStartTick);
        suppressStartLabel.setSelected(ruler.suppressStartLabel);
        suppressEndTick.setSelected(ruler.suppressEndTick);
        suppressEndLabel.setSelected(ruler.suppressEndLabel);
        displayLog10.setSelected(ruler.displayLog10);

        boolean showPct;

        if (ruler.multiplier == 1.0) {
            showPct = false;
        } else if (ruler.multiplier == 100.0) {
            showPct = true;
        } else {
            // Multiplier values other than 1 or 100 are not supported
            // by this dialog.
            throw new IllegalStateException("Multiplier = " + ruler.multiplier);
        }
        showPercentages.setSelected(showPct);

        double td;
        td = ruler.bigTickDelta;
        bigTickDelta.setText((Double.isNaN(td) || (td == 0)) ? "" :
                             ContinuedFraction.toString
                             (ruler.bigTickDelta / ruler.multiplier, showPct));
        td = ruler.tickDelta;
        tickDelta.setText((Double.isNaN(td) || td == 0) ? "" :
                          ContinuedFraction.toString
                          (ruler.tickDelta / ruler.multiplier, showPct));
        tickStart.setText((ruler.tickStartD == null) ? "" :
                          ContinuedFraction.toString
                          (ruler.tickStartD / ruler.multiplier, showPct));
        tickEnd.setText((ruler.tickEndD == null) ? "" :
                          ContinuedFraction.toString
                        (ruler.tickEndD / ruler.multiplier, showPct));

        suppressSmallTicks.setSelected(ruler.tickDelta == 0);
        textAngle.setText(String.format("%7.3f", ruler.textAngle * (-180 / Math.PI)));
        tickPadding.setText(ContinuedFraction.toString(ruler.tickPadding,
                                                       false));
    }

    /** Show the dialog as document-modal. If the dialog is closed
        normally, then update "dest" with the new values and return
        true. Otherwise, make no changes to "dest" and return false. */
    public boolean showModal(LinearRuler dest, ArrayList<LinearAxis> axes,
                             AffineTransform toPage) {
        setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);
        set(dest, axes);
        Point2D.Double v = Geom.aMinusB(dest.endPoint, dest.startPoint);
        toPage.deltaTransform(v, v);
        updateLeftRightLabels(Math.atan2(-v.y, v.x));
        pack();
        setVisible(true);
        if (!pressedOK) {
            return false;
        }

        dest.tickLeft = tickLeft.isSelected();
        dest.tickRight = tickRight.isSelected();
        dest.startArrow = startArrow.isSelected();
        dest.endArrow = endArrow.isSelected();
        dest.suppressStartTick = suppressStartTick.isSelected();
        dest.suppressStartLabel = suppressStartLabel.isSelected();
        dest.suppressEndTick = suppressEndTick.isSelected();
        dest.suppressEndLabel = suppressEndLabel.isSelected();
        dest.displayLog10 = displayLog10.isSelected();
        dest.multiplier = showPercentages.isSelected() ? 100.0 : 1.0;
        dest.labelAnchor = labelAnchor;
        dest.tickType = tickTypeV.isSelected() ? LinearRuler.TickType.V
            : LinearRuler.TickType.NORMAL;

        String str;

        str = tickPadding.getText();
        try {
            dest.tickPadding = ContinuedFraction.parseDouble(str);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog
                (getOwner(),
                 "Warning: could not parse tick padding value '"
                 + str + "'");
        }

        str = tickStart.getText().trim();
        if (!str.equals("")) {
            try {
                dest.tickStartD = ContinuedFraction.parseDouble(str)
                    * dest.multiplier;
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog
                    (getOwner(),
                     "Warning: could not parse tick start value '"
                     + str + "'");
            }
        } else {
            dest.tickStartD = null;
        }

        str = tickEnd.getText().trim();
        if (!str.equals("")) {
            try {
                dest.tickEndD = ContinuedFraction.parseDouble(str)
                    * dest.multiplier;
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog
                    (getOwner(),
                     "Warning: could not parse tick end value '"
                     + str + "'");
            }
        } else {
            dest.tickEndD = null;
        }

        str = bigTickDelta.getText().trim();
        if (!str.equals("")) {
            try {
                dest.bigTickDelta = ContinuedFraction.parseDouble(str)
                    * dest.multiplier;
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog
                    (getOwner(),
                     "Warning: could not parse big tick delta value '"
                     + str + "'");
            }
        } else {
            dest.bigTickDelta = Double.NaN;
        }

        str = tickDelta.getText().trim();
        if (!str.equals("")) {
            try {
                dest.tickDelta = ContinuedFraction.parseDouble(str)
                    * dest.multiplier;
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog
                    (getOwner(),
                     "Warning: could not parse small tick delta value '"
                     + str + "'");
            }
        } else {
            dest.tickDelta = Double.NaN;
        }

        String angleStr = textAngle.getText();
        try {
            double degangle = ContinuedFraction.parseDouble(angleStr);
            double odegangle = -dest.textAngle * 180 / Math.PI;
            if (Math.abs(degangle - odegangle) > 1e-3) {
                dest.textAngle = -degangle * Math.PI / 180;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog
                (getOwner(),
                 "Warning: could not parse text angle value '"
                 + angleStr + "'");
        }

        dest.axis = variable.getSelected(axes);
        if (suppressSmallTicks.isSelected()) {
            dest.tickDelta = 0;
        } else if (dest.tickDelta == 0) {
            dest.tickDelta = Double.NaN;
        }
        return true;
    }
}
