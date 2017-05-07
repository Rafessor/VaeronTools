package me.schuwi.vtools;

import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.math.transform.AffineTransform;

/**
 * @author Schuwi
 * @version 1.0
 */
public class RoundingTransform extends AffineTransform {

    RoundingTransform(double[] coeffs) {
        super(coeffs);
    }

    @Override
    public Vector apply(Vector vector) {
        return super.apply(vector).round();
    }
}
