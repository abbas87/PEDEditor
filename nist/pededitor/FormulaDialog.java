/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** GUI for entering a chemical formula. */
public class FormulaDialog extends JDialog {
    private static final long serialVersionUID = 3114984690946697109L;
    protected JLabel descr = new JLabel
        ("The string you enter will be placed in the clipboard.\n");
    protected JTextField formula = new JTextField(30);
    protected JTextField wtFormula = new JTextField(30);
    protected JLabel hillFormula = new JLabel();
    protected JLabel weight = new JLabel();
    protected transient boolean pressedOK = false;
    protected transient boolean selfModifying = false;
    @SuppressWarnings("serial")
    protected JButton okButton =  new JButton
        (new AbstractAction("Locate compound in diagram") {
                @Override public void actionPerformed(ActionEvent e) {
                    normalExit();
                }
            });
    { okButton.setFocusable(false); }

    public FormulaDialog(JFrame parent) {
        super(parent, "Enter Chemical Formula", false);
        
        formula.getDocument().addDocumentListener
            (new DocumentListener() {
                    @Override public void changedUpdate(DocumentEvent e) {
                        if (selfModifying) {
                            // This event was presumably generated by
                            // another event.
                            return;
                        }
                        updateFormula(true);
                    }

                    @Override public void insertUpdate(DocumentEvent e) {
                        changedUpdate(e);
                    }

                    @Override public void removeUpdate(DocumentEvent e) {
                        changedUpdate(e);
                    }
                });
        
        wtFormula.getDocument().addDocumentListener
            (new DocumentListener() {
                    @Override public void changedUpdate(DocumentEvent e) {
                        if (selfModifying) {
                            // This event was presumably generated by
                            // another event.
                            return;
                        }
                        updateWtFormula();
                    }

                    @Override public void insertUpdate(DocumentEvent e) {
                        changedUpdate(e);
                    }

                    @Override public void removeUpdate(DocumentEvent e) {
                        changedUpdate(e);
                    }
                });
        
        GridBagUtil gb = new GridBagUtil(getContentPane());

        gb.centerAndEndRow(descr);
        gb.addEast(new JLabel("Formula:"));
        gb.endRowWith(formula);
        gb.addEast(new JLabel("Wt. Form.:"));
        gb.endRowWith(wtFormula);
        JLabel hillLabel = new JLabel("Hill system:");
        {
            String tt = "A simple standard chemical formula notation";
            hillLabel.setToolTipText(tt);
            hillFormula.setToolTipText(tt);
        }
        gb.addEast(hillLabel);
        gb.endRowWith(hillFormula);
        gb.addEast(new JLabel("Weight:"));
        gb.endRowWith(weight);
        gb.centerAndEndRow(okButton);
        getRootPane().setDefaultButton(okButton);
        setResizable(false);
    }

    public void normalExit() {
        pressedOK = true;
        setVisible(false);
    }

    public void setFormula(String text) {
        formula.setText(text);
    }

    public void setWeightFormula(String text) {
        wtFormula.setText(text);
    }

    public class StringAndMatch {
        String string;
        ChemicalString.Match match;

        StringAndMatch(String s, ChemicalString.Match m) {
            string = s;
            match = m;
        }
    }

    StringAndMatch parseFormula(String formula, boolean allowFractions) {
        String res = "<html><div style=\"width:350px;\"><p><font color=\"red\">Invalid formula</font>";
        do {
            formula = formula.trim();
            if ("".equals(formula)) {
                res ="";
                break;
            }
            
            // Attempt to convert the input from HTML to regular text.
            // This should be mostly harmless even if it was regular
            // text to begin with, since normal chemical formulas
            // don't include <, >, or & anyhow.
            formula = HtmlToText.htmlToText(formula).trim();
            if (formula.isEmpty()) {
                break;
            }
        
            ChemicalString.Match m = ChemicalString.maybeQuotedComposition(formula);
            if (m != null && m.isWholeStringMatch()) {
                res = "<html>"
                    + ChemicalString.autoSubscript(m.toString(allowFractions));
                return new StringAndMatch(res, m);
            } else {
                int endIndex = (m == null) ? 0 : m.endIndex;
                res += "<p>Parse error after <font color=\"red\">&lt;&lt;HERE</font> in '"
                    + formula.substring(0, endIndex)
                    + "<font color=\"red\">&lt;&lt;HERE</font>" + formula.substring(endIndex) + "'";
            }
        } while (false);
        return new StringAndMatch(res, null);
    }

    /** Process a change to formula.getText() */
    void updateFormula(boolean allowFractions) {
        weight.setText("");
        StringAndMatch sam = parseFormula(getFormula(), allowFractions);
        String wtForm = "";
        if (sam.match != null) {
            double wt = sam.match.getWeight();
            if (!Double.isNaN(wt)) {
                weight.setText(String.format("%.4f", wt));
                wtForm = sam.match.toWeight().toString();
            }
        }
        hillFormula.setText(sam.string);
        if (!selfModifying) {
            try {
                selfModifying = true;
                setWeightFormula(wtForm);
            } finally {
                selfModifying = false;
            }
        }
        pack();
    }

    /** Process a change to wtFormula.getText() */
    void updateWtFormula() {
        try {
            selfModifying = true;
            weight.setText("");
            StringAndMatch sam = parseFormula(getWeightFormula(), false);
            ChemicalString.Match m2 = null;
            if (sam.match != null && (m2 = sam.match.fromWeight()) != null) {
                formula.setText(m2.toString(false));
                updateFormula(false);
            } else {
                hillFormula.setText(sam.string);
                formula.setText("");
            }
        } finally {
            selfModifying = false;
        }
        pack();
    }

    /** Return the text that the user entered into the formula box. */
    public String getFormula() {
        return formula.getText();
    }

    /** Return the text that the user entered into the weight formula
     * box. */
    public String getWeightFormula() {
        return wtFormula.getText();
    }

    /** Return the text that the user entered into the formula box,
        trimmed and with HTML stripped out. */
    public String getPlainFormula() {
        return HtmlToText.htmlToText(getFormula()).trim();
    }

    public String getHillFormula() {
        ChemicalString.Match m = ChemicalString.maybeQuotedComposition(getFormula());
        if (m != null && m.isWholeStringMatch()) {
            return m.toString();
        }
        return null;
    }

    public Map<String,Double> getComposition() {
        ChemicalString.Match m = ChemicalString.maybeQuotedComposition(getFormula());
        if (m != null && m.isWholeStringMatch()) {
            return m.composition;
        }
        return null;
    }

    /** Show the dialog as document-modal. Return null if the user did
        not press "OK" but closed the window or pressed the exit
        button, otherwise return getPlainFormula().

        @param strict If true, return null unless the formula can be parsed.
    */
    public String showModal(boolean strict) {
        pack();
        setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);
        pressedOK = false;
        setVisible(true);
        if (!pressedOK || (strict && getHillFormula() == null)) {
            return null;
        }

        // Can't set the clipboard? Don't worry about it.
        Stuff.setClipboardString(getFormula().trim(), true);
        return getPlainFormula();
    }

    public static void main(String[] args) {
        String formula = (new FormulaDialog(null)).showModal(true);
        System.out.println("You selected " + formula);
    }
}
