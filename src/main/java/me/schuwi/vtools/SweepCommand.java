package me.schuwi.vtools;

import com.boydti.fawe.config.BBC;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.worldedit.*;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.interpolation.Interpolation;
import com.sk89q.worldedit.math.interpolation.Node;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.regions.ConvexPolyhedralRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.util.command.parametric.Optional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Schuwi
 * @version 1.0
 */
public class SweepCommand {

    private static final double tension = 0D;
    private static final double bias = 0D;
    private static final double continuity = 0D;
    private static final double quality = 10D;

    private static final Vector2D direction = new Vector2D(1, 0);
    //private static final double scaleConst = Math.sqrt(2) - 1;
    //private static final Vector direction = new Vector(1, 0, 0);
    //private static final Vector up = new Vector(0, 1, 0);

    @com.sk89q.minecraft.util.commands.Command(
            aliases = { "vaesweep", "/vaesweep" },
            usage = "[copies=-1]",
            desc = "Sweep your clipboard content along a curve",
            help = "Sweeps your clipboard content along a curve.\n" +
                    "Define a curve by selecting the individual points with a convex selection.\n" +
                    "Set [copies] to a value > 0 if you want to have your selection pasted a limited amount of times equally spaced on the curve\n" +
                    "Tip: You can switch to convex selection mode with //sel convex.",
            max = 1
    )
    @CommandPermissions("vaeron.sweep")
    public void sweepCommand(Player player, LocalSession session, EditSession editSession, @Optional("-1") int copies) {
        try {
            ConvexPolyhedralRegion region = (ConvexPolyhedralRegion) session.getSelection(player.getWorld());

            Interpolation interpol = new KochanekBartelsInterpolation();
            List<Node> nodes = new ArrayList<>(region.getVertices()).stream().map(v -> {
                Node n = new Node(v);
                n.setTension(tension);
                n.setBias(bias);
                n.setContinuity(continuity);
                return n;
            }).collect(Collectors.toList());
            interpol.setNodes(nodes);

            ClipboardHolder holder = session.getClipboard();
            Clipboard clipboard = holder.getClipboard();
            Transform baseTransform = holder.getTransform();
            Vector baseOrigin = clipboard.getOrigin();

            int affected = 0;
            try {
                Vector center = clipboard.getRegion().getCenter();

                Vector offset = center.subtract(center.round());
                center = center.subtract(offset);

                clipboard.setOrigin(center);

                if (copies == 1) {
                    affected += pastePosition(editSession, interpol, holder, baseTransform, 0, offset);
                } else if (copies > 1) {
                    for (double pos = 0; pos <= 1; pos += 1D / (copies - 1)) {
                        affected += pastePosition(editSession, interpol, holder, baseTransform, pos, offset);
                    }
                } else {
                    double splineLength = interpol.arcLength(0, 1);
                    for (double pos = 0; pos <= 1; pos += 1D / splineLength / quality) {
                        affected += pastePosition(editSession, interpol, holder, baseTransform, pos, offset);
                    }
                }
            } catch (MaxChangedBlocksException e) {
                player.print("Maximum number of block changes reached");
            }

            // Reset clipboard
            holder.setTransform(baseTransform);
            clipboard.setOrigin(baseOrigin);
            BBC.OPERATION.send(player, affected);
        } catch (IncompleteRegionException | EmptyClipboardException | ClassCastException ignored) {
            player.print("You have to make a convex polyhedral selection and copy a region before using this command");
        }
    }

    private int pastePosition(EditSession editSession, Interpolation interpol, ClipboardHolder holder, Transform baseTransform, double position, Vector preOffset) throws MaxChangedBlocksException {
        Vector target = interpol.getPosition(position);

        Vector offset = target.subtract(target.round());
        target = target.subtract(offset);

        Vector deriv = interpol.get1stDerivative(position);
        Vector2D deriv2D = new Vector2D(deriv.getX(), deriv.getZ()).normalize();
        double angle = Math.toDegrees(
                Math.atan2(direction.getZ(), direction.getX()) - Math.atan2(deriv2D.getZ(), deriv2D.getX())
        );

        AffineTransform transform = new OffsetRoundTransform(
                new AffineTransform()
                        .translate(offset)
                        .rotateY(angle)
                        //.scale(direction.getZ() != 0 ? scale : 1, 1, direction.getX() != 0 ? scale : 1)
                        .coefficients())
                .preOffset(preOffset);

        holder.setTransform(baseTransform.combine(transform));

        Operation operation = holder
                .createPaste(editSession, editSession.getWorld().getWorldData())
                .to(target)
                .ignoreAirBlocks(true)
                .build();
        Operations.completeLegacy(operation);
        return ((ForwardExtentCopy)operation).getAffected();
    }
}
