/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

public class HTMLPalette extends StringPalette {
    public HTMLPalette() {
        super(new Object[] {
                "\u00b0" /* degree */,
                new Object[][] {
                    {"&lt;", "<"},
                    {"&gt;", ">"},
                    {"&amp;", "&"},
                    {"<br>\n", "<html><span style=\"font-size: 65%;\">line<br>break</span>"},
                    {"&nbsp;", "Hard space"},
                }
            });
    }
}
