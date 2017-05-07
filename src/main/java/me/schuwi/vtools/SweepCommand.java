package me.schuwi.vtools;

import com.sk89q.worldedit.*;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.bukkit.BukkitUtil;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.interpolation.Interpolation;
import com.sk89q.worldedit.math.interpolation.KochanekBartelsInterpolation;
import com.sk89q.worldedit.math.interpolation.Node;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.regions.ConvexPolyhedralRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Schuwi
 * @version 1.0
 */
public class SweepCommand implements CommandExecutor {

    private static final double tension = 0D;
    private static final double bias = 0D;
    private static final double continuity = 0D;
    private static final double quality = 10D;

    private static final Vector2D direction = new Vector2D(1, 0);
    private static final double scaleConst = Math.sqrt(2) - 1;
    //private static final Vector direction = new Vector(1, 0, 0);
    //private static final Vector up = new Vector(0, 1, 0);

    private final WorldEditPlugin wePlugin;

    public SweepCommand() {
        wePlugin = (WorldEditPlugin) Bukkit.getServer().getPluginManager().getPlugin("WorldEdit");
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            BukkitPlayer wePlayer = wePlugin.wrapPlayer(player);

            LocalSession session = wePlugin.getSession(player);
            EditSession editSession = null;

            try {
                ConvexPolyhedralRegion region = (ConvexPolyhedralRegion) session.getSelection(BukkitUtil.getLocalWorld(player.getWorld()));

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

                // >> From WorldEdit
                editSession = session.createEditSession(wePlayer);
                editSession.enableQueue();
                // <<

                try {
                    Vector center = clipboard.getRegion().getCenter();
                    clipboard.setOrigin(center);

                    double splineLength = interpol.arcLength(0, 1);
                    double lastAngle = Double.NaN;
                    for (double pos = 0; pos <= 1; pos += 1D / splineLength / quality) {
                        Vector target = interpol.getPosition(pos);
                        //target = target.setY(Math.ceil(target.getY()));
                        Vector minor = target.subtract(target.round());
                        target = target.subtract(minor);

                        //Vector origin = center.add(minor);
                        //clipboard.setOrigin(origin);

                        Vector deriv = interpol.get1stDerivative(pos);
                        Vector2D deriv2D = new Vector2D(deriv.getX(), deriv.getZ()).normalize();
                        double angle = Math.toDegrees(
                                Math.atan2(direction.getZ(), direction.getX()) - Math.atan2(deriv2D.getZ(), deriv2D.getX())
                        );

                        double scale = 1 + Math.abs(Math.sin(Math.acos(deriv2D.dot(direction)) * 2)) * scaleConst;
                        if (angle != lastAngle || pos == 0) {
                            System.out.println(angle);
                            lastAngle = angle;
                        }
                        /*Vector upDeriv;
                        if (deriv.getX() == 0 && deriv.getZ() == 0) { // deriv is straight up
                            upDeriv = direction.multiply(-1);
                        } else if (deriv.getY() == 0) { // deriv is parallel to ground plane
                            upDeriv = up;
                        } else {
                            Vector perpDeriv;
                            if (deriv.getX() == 0) {
                                perpDeriv = new Vector(1, 0, 0);
                            } else if (deriv.getZ() == 0) {
                                perpDeriv = new Vector(0, 0, 1);
                            } else {
                                perpDeriv = new Vector(1, 0, -deriv.getX()/deriv.getZ()).normalize();
                            }
                            upDeriv = deriv.cross(perpDeriv); //no normalize because angle is 90deg so magnitude = 1
                            if (upDeriv.getY() < 0) upDeriv = perpDeriv.cross(deriv);
                        }

                        Vector derivCross = deriv.cross(upDeriv); //again no normalize, same reason
                        Matrix derivMatrix = new Matrix(new double[][]{
                                {deriv.getX(), upDeriv.getX(), derivCross.getX()},
                                {deriv.getY(), upDeriv.getY(), derivCross.getY()},
                                {deriv.getZ(), upDeriv.getZ(), derivCross.getZ()}
                        });

                        Vector directionCross = direction.cross(up);
                        Matrix directionMatrix = new Matrix(new double[][]{
                                {direction.getX(), up.getX(), directionCross.getX()},
                                {direction.getY(), up.getY(), directionCross.getY()},
                                {direction.getZ(), up.getZ(), directionCross.getZ()}
                        }).inverse();

                        Matrix rotMatrix = derivMatrix.times(directionMatrix);





                        /*Vector rotAxis = direction.cross( deriv );
                        if (rotAxis.lengthSq() != 0) rotAxis = rotAxis.normalize();
                        double rotAngle = Math.acos( deriv.dot( direction ) );

                        Matrix skewAxis = new Matrix( new double[][]{
                                { 0D, -rotAxis.getZ(), rotAxis.getY() },
                                { rotAxis.getZ(), 0D, -rotAxis.getX() },
                                { -rotAxis.getY(), rotAxis.getX(), 0D }
                        } );

                        double sin = Math.sin( rotAngle );
                        double tCos = 1 - Math.cos(rotAngle);

                        Matrix rotMatrix = Matrix.identity( 3, 3 );
                        rotMatrix.plusEquals( skewAxis.times( sin ) );
                        rotMatrix.plusEquals( skewAxis.times( skewAxis ).times( tCos ) );*/

                        //RoundingTransform transform = new RoundingTransform( rotMatrix.getRowPackedCopy() );

                        RoundingTransform transform = new RoundingTransform()
                                .scale(direction.getZ() != 0 ? scale : 1, 1, direction.getX() != 0 ? scale : 1)
                                .rotateY(angle)
                                .translate(minor);
                        
                        /*transform.rotateX(Math.toDegrees(
                                Math.atan2(deriv.getZ(), deriv.getY()) - Math.atan2(direction.getZ(), direction.getY())
                        ));
                        transform.rotateY(Math.toDegrees(
                                Math.atan2(deriv.getX(), deriv.getZ()) - Math.atan2(direction.getX(), direction.getZ())
                        ));
                        transform.rotateZ(Math.toDegrees(
                                Math.atan2(deriv.getY(), deriv.getX()) - Math.atan2(direction.getY(), direction.getX())
                        ));*/

                        holder.setTransform(baseTransform.combine(transform));

                        Operation operation = holder
                                .createPaste(editSession, editSession.getWorld().getWorldData())
                                .to(target)
                                .ignoreAirBlocks(true)
                                .build();
                        Operations.completeLegacy(operation);
                        break;
                    }
                } catch (MaxChangedBlocksException e) {
                    sender.sendMessage("Maximum number of block changes reached");
                }

                // Reset clipboard
                holder.setTransform(baseTransform);
                clipboard.setOrigin(baseOrigin);
                sender.sendMessage("Performed " + editSession.getBlockChangeCount() + " block changes in total");

                return true;
            } catch (IncompleteRegionException | EmptyClipboardException | ClassCastException e) {
            } finally {
                // >> From WorldEdit
                if (editSession != null) {
                    session.remember(editSession);
                    editSession.flushQueue();
                    wePlugin.getWorldEdit().flushBlockBag(wePlayer, editSession);
                }
                // <<
            }
            sender.sendMessage("You have to make a convex polyhedral selection and copy a region before using this command");
        } else {
            sender.sendMessage("You can only execute this command as a player");
        }

        return true;
    }
}
