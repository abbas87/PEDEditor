/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

/** Exception type for inverse transforms that have no solutions */
public class ZeroSolutionsException extends UnsolvableException {
    private static final long serialVersionUID = 6225956804542984176L;

    ZeroSolutionsException() {}
    ZeroSolutionsException(String message) {
        super(message);
    }
}