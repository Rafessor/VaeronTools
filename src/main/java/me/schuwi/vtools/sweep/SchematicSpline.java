package me.schuwi.vtools.sweep;

import com.boydti.fawe.object.schematic.Schematic;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.interpolation.Interpolation;
import com.sk89q.worldedit.math.transform.AffineTransform;

/**
 * An implementation of a {@link Spline} using a Schematic as source for the structure.
 * @author Schuwi
 * @version 1.0
 */
public class SchematicSpline extends Spline {

    private Schematic schematic = null;
    private boolean align = true;

    private boolean rotate = false;
    private double rotation = 0D;

    private Vector customCenterOffset;

    private Vector center;
    private Vector centerOffset;
    private Vector originalOrigin;

    /**
     * Constructor without position-correction. Use this constructor for an interpolation implementation which does not need position-correction.
     * <p>
     * Be advised that currently subsequent changes to the interpolation parameters may not be supported.
     * @param editSession     The EditSession which will be used when pasting the clipboard content
     * @param interpolation   An implementation of the interpolation algorithm used to calculate the curve
     */
    public SchematicSpline(EditSession editSession, Interpolation interpolation) {
        this(editSession, interpolation, -1);
    }

    /**
     * Constructor with position-correction. Use this constructor for an interpolation implementation that needs position-correction.
     * <p>
     * Some interpolation implementations calculate the position on the curve (used by {@link #pastePosition(double)})
     * based on an equidistant distribution of the nodes on the curve. For example: on a spline with 5 nodes position 0.0 would refer
     * to the first node, 0.25 to the second, 0.5 to the third, ... .<br>
     * By providing this method with the amount of nodes used by the interpolation implementation the distribution of the
     * nodes is converted to a proportional distribution based on the length between two adjacent nodes calculated by {@link Interpolation#arcLength(double, double)}.<br>
     * This means that the distance between two positions used to paste the clipboard (e.g. 0.75 - 0.5 = 0.25) on the curve
     * will always amount to that part of the length (e.g. 40 units) of the curve. In this example it would amount to
     * 0.25 * 40 = 10 units of curve length between these two positions.
     * <p>
     * Be advised that currently subsequent changes to the interpolation parameters may not be supported.
     * @param editSession     The EditSession which will be used when pasting the clipboard content
     * @param interpolation   An implementation of the interpolation algorithm used to calculate the curve
     * @param nodeCount       The number of nodes provided to the interpolation object
     */
    public SchematicSpline(EditSession editSession, Interpolation interpolation, int nodeCount) {
        super(editSession, interpolation, nodeCount);
    }

    public void setSchematic(Schematic schematic) {
        this.schematic = schematic;
        calculateCenter();
    }

    public Schematic getSchematic() {
        return this.schematic;
    }

    public void setAlign(boolean align) {
        this.align = align;
    }

    public boolean getAlign() {
        return this.align;
    }

    public void rotate(double rotation) {
        this.rotate = true;
        this.rotation = rotation;
    }

    public void noRotate() {
        this.rotate = false;
    }

    public void setCenterOffset(Vector offset) {
        this.customCenterOffset = offset;
        calculateCenter();
    }

    public Vector getCenterOffset() {
        return this.customCenterOffset;
    }

    private void calculateCenter() {
        if (schematic == null) return;

        Clipboard clipboard = schematic.getClipboard();
        this.originalOrigin = clipboard.getOrigin();

        center = clipboard.getRegion().getCenter();
        if (customCenterOffset != null) center = center.add(customCenterOffset);

        this.centerOffset = center.subtract(center.round());
        this.center = center.subtract(centerOffset);
    }

    @Override
    protected int pasteBlocks(Vector target, Vector offset, double angle) throws MaxChangedBlocksException {
        if (schematic == null) return 0;

        AffineTransform transform = new AffineTransform();
        //transform = transform.translate(offset);
        if (rotate || align) transform = transform.rotateY((align ? angle : 0D) + (rotate ? rotation : 0D));
        transform = new OffsetRoundTransform(transform.coefficients()).preOffset(centerOffset);

        // Pasting

        Clipboard clipboard = schematic.getClipboard();
        clipboard.setOrigin(center);

        schematic.paste(editSession, editSession.getWorldData(), target, false, transform);

        // Cleanup
        clipboard.setOrigin(originalOrigin);

        return 0;
    }
}
