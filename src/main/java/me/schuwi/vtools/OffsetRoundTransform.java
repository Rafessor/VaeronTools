package me.schuwi.vtools;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.math.transform.AffineTransform;

/**
 * @author Schuwi
 * @version 1.0
 */
public class OffsetRoundTransform extends AffineTransform {

    private Vector preOffset;

    OffsetRoundTransform(double[] coeffs) {
        super(coeffs);
    }

    public OffsetRoundTransform preOffset(Vector preOffset) {
        this.preOffset = preOffset;
        return this;
    }

    @Override
    public Vector apply(Vector vector) {
        return super.apply(vector.subtract(preOffset)).round();
    }
}
