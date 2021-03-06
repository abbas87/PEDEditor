/* Eric Boesch, NIST Materials Measurement Laboratory, 2017. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

import javax.imageio.ImageIO;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import gov.nist.pededitor.DecorationHandle.Type;

/** Main driver class for Phase Equilibria Diagram digitization and creation. */
public class SourceImage implements Decoration {
    protected PolygonTransform transform = null;
    protected double alpha = 0.0;
    protected String filename;
    protected byte[] bytes;
    protected Rectangle2D pageBounds;

    /**
     * For use by EditorState to check whether bytes[] has changed
     * without actually storing the whole thing. Yeah, it's not 100%
     * reliable, but 99.99% is good enough. */
    transient int bytesHashCode = 0;

    @JsonProperty void setBytesHashCode(int code) {
        bytesHashCode = code;
    }

    int bytesHashCode() {
        return (bytes == null) ? -491983163 : Arrays.hashCode(bytes);
    }


    /**
     * Transform from original coordinates to principal coordinates. Original
     * coordinates are (x,y) positions within a scanned image. Principal
     * coordinates are either the natural (x,y) coordinates of a Cartesian graph
     * or binary diagram (for example, y may equal a temperature while x equals
     * the atomic fraction of the second diagram component), or the fraction of
     * the right and top components respectively for a ternary diagram.
     */
    protected transient Transform2D inverseTransform = null;

    protected transient double oldAlpha = 0.0;
    protected transient BufferedImage image = null;
    protected transient boolean triedToLoad = false;

    public SourceImage() {
    }

    @Override public boolean equals(Object other) {
        return equalsExceptBytes(other)
                && bytesHashCode() == ((SourceImage) other).bytesHashCode();
    }

    /**
     * Compare this to other, but compare bytes hash codes instead of
     * the actual bytes arrays.
     */
    boolean equalsByBytesHashCode(Object other) {
        return equalsExceptBytes(other)
                && bytesHashCode == ((SourceImage) other).bytesHashCode;
    }

    boolean equalsExceptBytes(Object other0) {
        if (this == other0)
            return true;
        if (other0 == null || getClass() != other0.getClass())
            return false;
        SourceImage other = (SourceImage) other0;

        return (alpha == other.alpha)
            && (filename == other.filename || (filename != null && filename.equals(other.filename)))
            && (transform == other.transform || (transform != null && transform.equals(other.transform)))
            && (pageBounds == other.pageBounds
                    || (pageBounds != null && pageBounds.equals(other.pageBounds)));
    }

    @Override
    public int hashCode() {
        return Double.hashCode(alpha)
            + (filename == null ? 2983 : filename.hashCode())
            + (transform == null ? 4790832 : transform.hashCode())
            + (pageBounds == null ? 2982575 : pageBounds.hashCode())
            + (bytes == null ? 3729835 : Arrays.hashCode(bytes));
    }

    @Override
    public SourceImage clone() {
        SourceImage res = new SourceImage();
        res.alpha = alpha;
        res.filename = filename;
        res.bytes = bytes;
        res.triedToLoad = false;
        res.transform = transform.clone();
        res.transformedImages = transformedImages;
        return res;
    }

    /**
     * Because rescaling an image is slow, keep a cache of locations and sizes
     * that have been rescaled. All of these images have had oldTransform
     * applied to them; if a new transform is attempted, the cache gets emptied.
     */
    protected transient ArrayList<SoftReference<CroppedTransformedImage>> transformedImages = new ArrayList<>();

    @JsonIgnore
    public BufferedImage getImage() {
        if (triedToLoad || image != null)
            return image;
        triedToLoad = true;
        try {
            if (bytes == null) {
                if (filename == null) {
                    return null;
                }
                bytes = Files.readAllBytes(Paths.get(filename));
            }

            image = ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (IOException x) {
            SourceImage.readFailures++;
            x.printStackTrace();
            // No better option than to live with it.
            bytes = null;
        }
        return image;
    }

    /**
     * There's no reason why the image page bounds shouldn't be saveable, but
     * currently you can't do that.
     */
    @JsonIgnore
    public void setPageBounds(Rectangle2D pageBounds) {
        this.pageBounds = (Rectangle2D) pageBounds.clone();
    }

    public double getAlpha() {
        return alpha;
    }

    public void setAlpha(double alpha) {
        if (alpha != this.alpha) {
            oldAlpha = this.alpha;
            this.alpha = alpha;
        }
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
        bytes = null;
        image = null;
        transformedImages = new ArrayList<>();
        triedToLoad = false;
    }

    public void setTransform(PolygonTransform xform) {
        this.transform = xform.clone();
        inverseTransform = null;
    }

    public PolygonTransform getTransform() {
        return transform.clone();
    }

    /** @return the inverse transform of p. */
    public Point2D.Double inverseTransform(Point2D p) throws UnsolvableException {
        if (inverseTransform == null) {
            if (transform == null)
                return null;
            try {
                inverseTransform = transform.createInverse();
            } catch (NoninvertibleTransformException e) {
                System.err.println("This transform is not invertible");
                System.exit(2);
            }
        }
        return inverseTransform.transform(p);
    }

    /*
     * Like setAlpha, but trying to set the value to what it already is causes
     * it to change to oldAlpha. If oldAlpha also equals its current value, it
     * changes to invisible (0) if it didn't used to be, and to 1.0 (opaque) if
     * it used to be invisible. This allows control-H to switch between the
     * image being hidden or not..
     */
    public double toggleAlpha(double value) {
        double oa = alpha;
        if (value == alpha)
            value = oldAlpha;
        if (value == alpha) {
            if (value > 0) {
                value = 0;
            } else {
                value = 1.0;
            }
        }
        oldAlpha = oa;
        alpha = value;
        return alpha;
    }

    static void draw(Graphics2D g, BufferedImage im, float alpha) {
        draw(g, im, alpha, 0, 0);
    }

    public static void draw(Graphics2D g, BufferedImage im, float alpha, int x, int y) {
        Composite oldComposite = g.getComposite();
        try {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g.drawImage(im, x, y, null);
        } finally {
            g.setComposite(oldComposite);
        }
    }

    void emptyCache() {
        transformedImages = new ArrayList<>();
    }

    @Override
    public void draw(Graphics2D g, AffineTransform xform, double scale) {
        if (alpha == 0)
            return;
        Rectangle bounds = (g.getClip() == null) ? null : g.getClip().getBounds();
        PolygonTransform xform0 = transform.clone();
        xform0.preConcatenate(new Affine(xform));
        xform0.preConcatenate(new Affine(AffineTransform.getScaleInstance(scale, scale)));
        CroppedTransformedImage im = getCroppedTransformedImage(getImage(), transformedImages, xform0, bounds,
                toScaledRectangle(pageBounds, scale));
        if (im == null)
            return;
        draw(g, im.croppedImage, (float) alpha, im.cropBounds.x, im.cropBounds.y);
    }

    static Rectangle toScaledRectangle(Rectangle2D rect, double scale) {
        rect = Geom.createScaled(rect, scale);
        int x = (int) Math.floor(rect.getX());
        int x2 = (int) Math.ceil(rect.getX() + rect.getWidth());
        int y = (int) Math.floor(rect.getY());
        int y2 = (int) Math.ceil(rect.getY() + rect.getHeight());
        return new Rectangle(x, y, x2 - x, y2 - y);
    }

    /** @param imageBounds the rectangle to crop the image into
        in unscaled coordinates, independent of the view bounds.

        @param viewBounds The region that is actually visible right
        now, in scaled coordinates. This will typically correspond to
        the clipping region of the Graphics2D object.

        imageBounds and viewBounds are considered separately for
        caching purposes. Scaling is expensive, and viewBounds can
        change rapidly (as when you move the scrollbar), so caching a
        version of the image that is larger than viewBounds but not
        larger than imageBounds, and clipping it to the view region,
        can be faster than recomputing from scratch each time. */
    static CroppedTransformedImage getCroppedTransformedImage(
            BufferedImage input,
            ArrayList<SoftReference<CroppedTransformedImage>> transformedImages2,
            PolygonTransform xform,
            Rectangle viewBounds, Rectangle imageBounds) {
        if (viewBounds == null) {
            return null;
            // TODO Most but not all users prefer not to have the
            // diagram margins expanded to cover the whole scanned
            // image, and sometimes even trying can cause a heap
            // error. The only way to satisfy everyone would be to
            // ask.
        }
        Rectangle imageViewBounds = imageBounds.intersection(viewBounds);

        // Attempt to work around a bug where Rectangle#intersection
        // returns negative widths or heights.
        if (imageViewBounds.width <= 0 || imageViewBounds.height <= 0) {
            return null;
        }

        int totalMemoryUsage = 0;
        int maxScoreIndex = -1;
        int maxScore = 0;

        int cnt = transformedImages2.size();

        for (int i = cnt - 1; i>=0; --i) {
            CroppedTransformedImage im = transformedImages2.get(i).get();
            if (im == null) {
                transformedImages2.remove(i);
                continue;
            }
            if (xform.nearlyEquals(im.transform, 1e-6)
                    && im.cropBounds.contains(imageViewBounds)) {
                // Found a match.

                // Promote this image to the front of the LRU queue (last
                // position in the ArrayList).
                transformedImages2.remove(i);
                transformedImages2.add(new SoftReference<>(im));
                return im;
            }

            // Lower scores are better. Penalties are given for memory
            // usage and distance back in the queue (implying the
            // image has not been used recently).

            // Memory usage is 4 bytes per pixel (ARGB).
            int mu = im.getMemoryUsage() * 4;
            totalMemoryUsage += mu;

            int thisScore = mu * (cnt - i);
            if (thisScore > maxScore) {
                maxScore = thisScore;
                maxScoreIndex = i;
            }
        }

        // Save memory if we're at the limit.

        int totalMemoryLimit = 100_000_000;
        int totalImageCntLimit = 50;
        if (totalMemoryUsage > totalMemoryLimit) {
            transformedImages2.remove(maxScoreIndex);
        } else if (cnt >= totalImageCntLimit) {
            // Remove the oldest image.
            transformedImages2.remove(0);
        }

        // Create a new CroppedTransformedImage that is big enough to hold
        // all of a medium-sized scaled image and that is also at
        // least several times the viewport size if the scaled image
        // is big enough to need to be cropped.

        // Creating a cropped image that is double the viewport size
        // in both dimensions is near optimal in the sense that for a
        // double-sized cropped image, if the user drags the mouse in
        // a fixed direction, the frequency with which the scaled
        // image has to be updated times the approximate cost of each
        // update is minimized.

        Dimension maxCropSize = new Dimension
            (Math.max(2000, viewBounds.width * 2),
             Math.max(1500, viewBounds.height * 2));

        Rectangle cropBounds = new Rectangle();

        if (imageBounds.width * 3 <= maxCropSize.width * 4) {
            // If allowing a little extra space beyond the normal
            // maximum can make cropping unnecessary, then do it.
            cropBounds.x = 0;
            cropBounds.width = imageBounds.width;
        } else {
            int margin1 = (maxCropSize.width - imageViewBounds.width) / 2;
            int margin2 = margin1;

            int ivmin = imageViewBounds.x;
            int ivmax = ivmin + imageViewBounds.width;
            int immax = imageBounds.x + imageBounds.width;

            int extra = margin1 - ivmin;
            if (extra > 0) {
                // We don't need so much of a margin on this side, so
                // we can have extra on the other side.
                margin2 += extra;
                margin1 -= extra;
            }

            extra = margin2 - (immax - ivmax);
            if (extra > 0) {
                // We don't need so much of a margin on this side, so
                // we can have extra on the other side.
                margin2 -= extra;
                margin1 += extra;
            }

            cropBounds.x = imageViewBounds.x - margin1;
            cropBounds.width = imageViewBounds.width + margin1 + margin2;
        }

        if (imageBounds.height * 3 <= maxCropSize.height  * 4) {
            // If allowing a little extra space beyond the normal
            // maximum can make cropping unnecessary, then do it.
            cropBounds.y = 0;
            cropBounds.height = imageBounds.height;
        } else {
            int margin1 = (maxCropSize.height - imageViewBounds.height) / 2;
            int margin2 = margin1;

            int ivmin = imageViewBounds.y;
            int ivmax = ivmin + imageViewBounds.height;
            int immax = imageBounds.y + imageBounds.height;

            int extra = margin1 - ivmin;
            if (extra > 0) {
                // We don't need so much of a margin on this side, so
                // we can have extra on the other side.
                margin2 += extra;
                margin1 -= extra;
            }

            extra = margin2- (immax - ivmax);
            if (extra > 0) {
                // We don't need so much of a margin on this side, so
                // we can have extra on the other side.
                margin2 -= extra;
                margin1 += extra;
            }

            cropBounds.y = imageViewBounds.y - margin1;
            cropBounds.height = imageViewBounds.height + margin1 + margin2;
        }

        CroppedTransformedImage im = new CroppedTransformedImage();
        im.transform = xform;
        im.cropBounds = cropBounds;
        ImageTransform.DithererType dither
            = (cropBounds.getWidth() * cropBounds.getHeight() > 3000000)
            ? ImageTransform.DithererType.FAST
            : ImageTransform.DithererType.GOOD;

        im.croppedImage = transform(input, cropBounds, xform, dither, 1.0);
        transformedImages2.add(new SoftReference<>(im));
        return im;
    }

    /**
     * @return the original binary content of the image file. Changing the array
     *         contents is not safe.
     */
    @JsonProperty("bytes")
    protected byte[] getBytesUnsafe() throws IOException {
        return bytes;
    }

    /**
     * Set the binary content of the image file. Changing the array contents is
     * not safe.
     */
    @JsonProperty("bytes")
    protected void setBytesUnsafe(byte[] bytes) {
        this.bytes = bytes;
        image = null;
        transformedImages = new ArrayList<>();
        triedToLoad = false;
    }

    /**
     * Apply transform to the image, then apply principalToScaledPage, then
     * translate the upper-left corner of cropRect to position (0,0). Return the
     * portion of the image that intersects cropRect with its alpha value
     * multiplied by alpha. Any part of the returned image not covered by the
     * translated input image is assigned an alpha of 0. Return null if the
     * image could not be generated or it would be completely transparent.
     */
    synchronized BufferedImage transform(Rectangle cropRect, AffineTransform principalToScaledPage,
            ImageTransform.DithererType dither, double alpha) throws IOException {
        PolygonTransform xform = transform.clone();
        xform.preConcatenate(new Affine(principalToScaledPage));
        return transform(getImage(), cropRect, xform, dither, alpha);
    }

    /**
     * Apply transform to the image, then apply principalToScaledPage, then
     * translate the upper-left corner of cropRect to position (0,0). Return the
     * portion of the image that intersects cropRect with its alpha value
     * multiplied by alpha. Any part of the returned image not covered by the
     * translated input image is assigned an alpha of 0. Return null if the
     * image could not be generated or it * would be completely transparent.
     */
    public static BufferedImage transform(BufferedImage input, Rectangle cropRect, PolygonTransform xform,
            ImageTransform.DithererType dither, double alpha) {
        if (input == null || alpha == 0)
            return null;

        PolygonTransform toCrop = xform.clone();

        // Shift the transform so that location (cropRect.x,
        // cropRect.y) is mapped to location (0,0).
        toCrop.preConcatenate(new Affine(AffineTransform.getTranslateInstance(-cropRect.x, -cropRect.y)));

        System.out.println("Resizing original image (" + dither + ")...");
        BufferedImage img = ImageTransform.run(toCrop, input, null, cropRect.getSize(), dither,
                BufferedImage.TYPE_INT_ARGB);
        if (alpha == 1) {
            return img;
        }

        BufferedImage res = new BufferedImage(cropRect.width, cropRect.height, BufferedImage.TYPE_INT_ARGB);
        draw(res.createGraphics(), img, (float) alpha);
        return res;
    }

    @Override
    public void draw(Graphics2D g, double scale) {
        draw(g, new AffineTransform(), scale);
    }

    @Override
    @JsonIgnore
    public Color getColor() {
        return null;
    }

    @Override
    public void setColor(Color color) {
    }

    @Override
    @JsonIgnore
    public DecorationHandle[] getHandles(Type type) {
        // TODO Auto-generated method stub
        return new DecorationHandle[0];
    }

    @Override
    public void transform(AffineTransform xform) {
        PolygonTransform xform2 = getTransform();
        xform2.preConcatenate(new Affine(xform));
        setTransform(xform2);
    }

    @Override
    public String typeName() {
        return "image";
    }

    @Override
    public SourceImage createTransformed(AffineTransform xform) {
        SourceImage res = clone();
        res.transform(xform);
        return res;
    }

    @Override
    @JsonIgnore
    public double getLineWidth() {
        return 0;
    }

    @Override
    public void transform(SlopeTransform2D xform) throws UnsolvableException {
        // TODO Auto-generated method stub

    }

    static int readFailures = 0;
}
