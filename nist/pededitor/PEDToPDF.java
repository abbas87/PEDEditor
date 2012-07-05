package gov.nist.pededitor;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JOptionPane;

import com.itextpdf.text.Document;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.BadPdfFormatException;
import com.itextpdf.text.pdf.PdfCopy;
import com.itextpdf.text.pdf.PdfReader;

/** Wrapper class for conversion of PED files to PDF files. */
public class PEDToPDF {
    public final static String PED_DIR = "/eb/ped";

    /** If the base filename contains a dot, then remove the last dot
        and everything after it. Otherwise, return the entire string.
        Modified from coobird's suggestion on Stack Overflow. */
    public static String removeExtension(String s) {
        String separator = System.getProperty("file.separator");
        int lastSeparatorIndex = s.lastIndexOf(separator);
        int extensionIndex = s.lastIndexOf(".");
        return (extensionIndex <= lastSeparatorIndex) ? s
            : s.substring(0, extensionIndex);
    }

    public static Diagram loadAndCrop(String filename) throws IOException {
        Diagram d = Diagram.loadFrom(new File(filename));
        Rectangle2D bounds = new Rectangle2D.Double(-0.5, -0.5, 2.0, 2.0);
        if (d.crop(bounds)) {
            System.err.println(filename + " did not fit in the normal page bounds.");
            d.computeMargins();
        }
        return d;
    }

    /** Return an unsorted list of all PED files in the given directory. */
    static ArrayList<String> getInputFilenames(String dir)
        throws DirectoryIteratorException, IOException {
        PathMatcher m = FileSystems.getDefault().getPathMatcher
            ("glob:**.ped");
        ArrayList<String> res = new ArrayList<>();

        try  (DirectoryStream<Path> stream
              = Files.newDirectoryStream(Paths.get(dir))) {
                for (Path file: stream) {
                    if (m.matches(file)) {
                        res.add(file.toString());
                    }
                }
            } catch (IOException | DirectoryIteratorException x) {
            // IOException can never be thrown by the iteration.
            // In this snippet, it can only be thrown by newDirectoryStream.
            System.err.println(x);
        }
        return res;
    }

    public void combinePEDs(List<String> peds) {
        combinePEDs(peds, 0);
    }

    public static void combinePEDs(List<String> peds, int diagramsPerDocument) {
        Document doc = null;
        PdfCopy copy = null;
        int inFileNo = -1;
        int outFileCnt = 0;
        String ofn = null;

        for (String filename: peds) {
            System.out.println("Reading " + filename);
            ++inFileNo;
            if (inFileNo == 0
                || (diagramsPerDocument > 0
                    && inFileNo % diagramsPerDocument == 0)) {
                ++outFileCnt;
                if (doc != null) {
                    doc.close();
                }
                ofn = (diagramsPerDocument == 0) ? "/eb/pdf/combined.pdf"
                    : String.format("/eb/pdf/combined%04d.pdf", outFileCnt);
                System.out.println("Starting " + ofn);
                doc = new Document(PageSize.LETTER);
                try {
                    copy = new PdfCopy(doc, new FileOutputStream(ofn));
                } catch (Exception e) {
                    System.err.println(e);
                    return;
                }
                doc.open();
            }

            try {
                Diagram d = loadAndCrop(filename);
                copy.addPage(copy.getImportedPage
                             (new PdfReader(d.toPDFByteArray()),
                              1));
                System.out.println(filename + " -> " + ofn);
            } catch (IOException | BadPdfFormatException x) {
                System.err.println(filename + ": " + x);
            }
        }
        doc.close();
    }

    public static void main(String[] args) {
        if (args.length == 2) {
            String ifn = args[0];
            String ofn = args[1];
            try {
                loadAndCrop(ifn).saveAsPDF(new File(ofn));
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, e.toString());
            }
        } else if (args.length == 0) {
            try {
                ArrayList<String> peds = getInputFilenames(PED_DIR);
                Collections.sort(peds, new MixedIntegerStringComparator());
                combinePEDs(peds, 100);
            } catch (IOException | DirectoryIteratorException x) {
                // IOException can never be thrown by the iteration.
                // In this snippet, it can only be thrown by newDirectoryStream.
                System.err.println(x);
                return;
            }
        } else {
            System.err.println("Expected 0 or 2 arguments");
        }
    }
}
