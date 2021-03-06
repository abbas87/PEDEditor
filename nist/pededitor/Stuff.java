/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.Component;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import javax.swing.JOptionPane;

/** Miscellaneous minor utilities. */
public class Stuff {
    /** If the last section of the filename contains a dot, then
        return everything after that dot, converted to lower case.
        Otherwise, return null. */
    public static String getExtension(String s) {
        String separator = System.getProperty("file.separator");
        int lastSeparatorIndex = s.lastIndexOf(separator);
        int extensionIndex = s.lastIndexOf(".");
        return (extensionIndex <= lastSeparatorIndex + 1) ? null
            : s.substring(extensionIndex + 1).toLowerCase();
    }

    /** If the base filename contains a dot, then remove the last dot
        and everything after it. Otherwise, return the entire string.
        Modified from coobird's suggestion on Stack Overflow. */
    public static String removeExtension(String s) {
        String separator = System.getProperty("file.separator");
        int lastSeparatorIndex = s.lastIndexOf(separator);
        int extensionIndex = s.lastIndexOf(".");
        return (extensionIndex <= lastSeparatorIndex + 1) ? s
            : s.substring(0, extensionIndex);
    }

    /** If you display a long ASCII paragraph in a showMessageDialog,
        you just end up with a very long line. Unless mess starts with
        <html>, add your own <html> tag and word wrap. */
    public static String htmlify(String mess) {
        return mess.startsWith("<html>") ? mess
            : ("<html><div width=\"250 px\"><p>" + mess);
    }

    /** Show error message mess with word wrap and the given error
        dialog title. */
    static void showError(Component parent, String mess, String title) {
        JOptionPane.showMessageDialog
            (parent, htmlify(mess), title, JOptionPane.ERROR_MESSAGE);
    }

    static String fakeClipboard = null;

    /**
     * Read the clipboard contents. If the clipboard cannot be read,
     * but the fake clipboard that stores the result of failed
     * attempts to write to the clipboard is not null, return that
     * instead. */
    static String getClipboardString() {
        try {
            return (String) Toolkit.getDefaultToolkit().getSystemClipboard()
                .getData(DataFlavor.stringFlavor);
        } catch (HeadlessException e) {
            if (fakeClipboard != null)
                return fakeClipboard;
            throw new IllegalArgumentException(
                    "Can't call coordinatesToClipboard() in a headless environment:"
                    + e);
        } catch (UnsupportedFlavorException e) {
            throw new IllegalStateException
                ("StringFlavor unsupported (internal:" + e);
        } catch (IOException e) {
            if (fakeClipboard != null)
                return fakeClipboard;
            throw new IllegalStateException
                ("While getting clipboard: " + e);
        }
    }

    /** Write str to the clipboard. If it fails and ignoreFailure
        equals false, throw IllegalArgumentException. Also, on
        failure, write to the fake clipboard so cut/paste can still
        work. */
    public static void setClipboardString(String str, boolean ignoreFailure) {
        try {
            StringSelection sel = new StringSelection(str);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents
                (sel, sel);
        } catch (HeadlessException e) {
            fakeClipboard = str;
            if (!ignoreFailure) {
                throw new IllegalArgumentException(
                        "Can't call coordinatesToClipboard() in a headless environment:" + e);
            }
        }
    }

    public static boolean isOSX() {
        return System.getProperty("os.name").contains("OS X");
    }

    static boolean fileAssociationBroken = false;

    public static void setFileAssociationBroken(boolean b) {
        fileAssociationBroken = b;
    }

    /** Return true if file associations aren't even supposed to work for this OS. */
    public static boolean isFileAssociationBroken() {
        return isOSX() || fileAssociationBroken;
    }
}
