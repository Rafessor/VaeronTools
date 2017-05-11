package me.schuwi.vtools.sweep;

import com.boydti.fawe.config.BBC;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.worldedit.*;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.math.interpolation.Interpolation;
import com.sk89q.worldedit.math.interpolation.Node;
import com.sk89q.worldedit.regions.ConvexPolyhedralRegion;
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

            Spline spline = new Spline(editSession, session.getClipboard(), interpol, nodes.size());

            int affected = 0;
            try {
                if (copies == 1) {
                    affected += spline.pastePosition(0D);
                } else if (copies > 1) {
                    for (double pos = 0D; pos <= 1D; pos += 1D / (copies - 1)) {
                        affected += spline.pastePosition(pos);
                    }
                } else {
                    double splineLength = interpol.arcLength(0D, 1D);
                    for (double pos = 0D; pos <= 1D; pos += 1D / splineLength / quality) {
                        affected += spline.pastePosition(pos);
                    }
                }
            } catch (MaxChangedBlocksException e) {
                player.print("Maximum number of block changes reached");
            }

            BBC.OPERATION.send(player, affected);
        } catch (IncompleteRegionException | EmptyClipboardException | ClassCastException ignored) {
            player.print("You have to make a convex polyhedral selection and copy a region before using this command");
        }
    }
}
