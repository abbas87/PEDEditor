/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

public class PedPalette extends StringPalette {
    public PedPalette() {
        super
            (new Object[]
                { 
                    // Plotting symbols
                    "\u25cb" /* white circle */, "\u25cf" /* black circle */,
                    "\u25d3" /* lower-half-filled circle */,
                    "\u25c9" /* fisheye */,
                    "\u29b5" /* white circle with horizontal bar */,
                    "\u2715" /* multiplication x? */,
                    "\u25a1" /* white square */, "\u25a0" /* black square */, 
                    "\u25b3" /* white triangle */, "\u25b2" /* black triangle */,
                    "\u25bd" /* white upside-down triangle */,
                    "\u25bc" /* black upside-down triangle */,
                    "\u25c7" /* white diamond */, "\u25c6" /* black diamond */,
                    "\u2721" /* white Star of David */, "\u2736" /* black star */,
                    null,

                    // Miscellaneous
                    new Object[][] {
                        { "\u207a", "C\u207a" } /* superscript plus */,
                        {"\u207b", "C\u207b" } /* superscript minus */,
                        {"\u00b7",  "C\u00b7C"} /* dot */,
                    },
                    "\u221A" /* square root/radical */,
                    "\u2192" /* right arrow */, "\u2190" /* left arrow */,
                    "\u00b1" /* plus-minus */, "\u2248" /* almost equal to */,
                    "\u2264" /* <= */, "\u2265" /* >= */, "\u2260" /* != */,
                    "\u2261" /* congruent */,
                    "\u221E" /* infinity */, "\u21cc" /* right-over-left harpoon */,
                    "\u2207" /* nabla / gradient symbol */, "\u222b" /* integral */,
                    null,

                    // Greek lowercase
                    "\u03b1", "\u03b2", "\u03b3",  "\u03b4", "\u03b5", 
                    "\u03b6", "\u03b7", "\u03b8",  "\u03b9", "\u03ba", 
                    "\u03bb", "\u03bc", "\u03bd",  "\u03be", "\u03bf", 
                    "\u03c0", "\u03c1", "\u03c3", "\u03c4", 
                    "\u03c5", "\u03c6", "\u03c7",  "\u03c8", "\u03c9", 
                    null,

                    // Greek uppercase
                    "\u0391", "\u0392", "\u0393",  "\u0394", "\u0395", 
                    "\u0396", "\u0397", "\u0398",  "\u0399", "\u039a", 
                    "\u039b", "\u039c", "\u039d",  "\u039e", "\u039f", 
                    "\u03a0", "\u03a1",  "\u03a3", "\u03a4", 
                    "\u03a5", "\u03a6", "\u03a7",  "\u03a8", "\u03a9", 
                    null,

                    // Roman numerals
                    "\u2160", "\u2161", "\u2162", "\u2163", "\u2164",
                    "\u2169", "\u216c", "\u216d",
                    null,
                });
    }
}
