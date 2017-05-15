package me.schuwi.vtools.sweep;

import com.boydti.fawe.config.BBC;
import com.boydti.fawe.config.Settings;
import com.boydti.fawe.object.schematic.Schematic;
import com.sk89q.minecraft.util.commands.CommandPermissions;
import com.sk89q.worldedit.*;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.math.interpolation.Interpolation;
import com.sk89q.worldedit.math.interpolation.Node;
import com.sk89q.worldedit.regions.ConvexPolyhedralRegion;
import com.sk89q.worldedit.util.command.parametric.Optional;
import com.sk89q.worldedit.util.io.file.FilenameException;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Schuwi
 * @version 1.0
 */
public class SchematicCommand {

    private static final double tension = 0D;
    private static final double bias = 0D;
    private static final double continuity = 0D;

    @com.sk89q.minecraft.util.commands.Command(
            aliases = { "vaeschem", "/vaeschem" },
            usage = "<prefix> <copies> [offset-x] [offset-y] [offset-z] [format]",
            desc = "Load all schematics with a prefix and paste randomly with random rotation along a spline",
            help = "Loads all schematics with the given prefix. Pastes a <copies> copies of it with random rotations along a curve. \n" +
                    "Define a curve by selecting the individual points with a convex selection.\n" +
                    "You can optionally define an offset added to the center of the schematics.\n" +
                    "All rotations are a multiple of 90 degree.\n" +
                    "Tip: You can switch to convex selection mode with //sel convex.",
            min = 2,
            max = 6
    )
    @CommandPermissions("vaeron.schematic")
    public void schematicCommand(Player player, LocalSession session, EditSession editSession,
                                 String prefix, int copies,
                                 @Optional("0") double offsetX, @Optional("0") double offsetY, @Optional("0") double offsetZ,
                                 @Optional("schematic") String formatName) {
        ConvexPolyhedralRegion region;
        try {
            region = (ConvexPolyhedralRegion) session.getSelection(player.getWorld());
        } catch (IncompleteRegionException | ClassCastException ignored) {
            player.print("You have to make a convex polyhedral selection before using this command.");
            player.print("Tip: You can switch to convex selection mode with //sel convex.");
            return;
        }

        ClipboardFormat format = ClipboardFormat.findByAlias(formatName);
        if (format == null) {
            BBC.CLIPBOARD_INVALID_FORMAT.send(player, formatName);
            return;
        }

        Schematic[] schematics = loadSchematics(player, format, prefix.toLowerCase());

        if (schematics == null || schematics.length == 0) {
            player.print("Couldn't find any schematics with matching prefix.");
            return;
        }

        Interpolation interpol = new KochanekBartelsInterpolation();
        List<Node> nodes = new ArrayList<>(region.getVertices()).stream().map(v -> {
            Node n = new Node(v);
            n.setTension(tension);
            n.setBias(bias);
            n.setContinuity(continuity);
            return n;
        }).collect(Collectors.toList());
        interpol.setNodes(nodes);

        SchematicSpline spline = new SchematicSpline(editSession, interpol, nodes.size());
        spline.setAlign(false);
        spline.setCenterOffset(new Vector(offsetX, offsetY, offsetZ));

        Random rand = new Random();
        try {
            if (copies == 1) {
                Schematic schematic = schematics[rand.nextInt(schematics.length)];
                int rotation = rand.nextInt(4);

                spline.setSchematic(schematic);
                spline.rotate(rotation * 90D);
                spline.pastePosition(0D);
            } else if (copies > 1) {
                for (double pos = 0D; pos <= 1D; pos += 1D / (copies - 1)) {
                    Schematic schematic = schematics[rand.nextInt(schematics.length)];
                    int rotation = rand.nextInt(4);

                    spline.setSchematic(schematic);
                    spline.rotate(rotation * 90D);
                    spline.pastePosition(pos);
                }
            }
        } catch (MaxChangedBlocksException e) {
            player.print("Maximum number of block changes reached.");
        }

        BBC.OPERATION.send(player, 0);
    }

    private Schematic[] loadSchematics(Player player, ClipboardFormat format, final String prefix) {
        WorldEdit worldEdit = WorldEdit.getInstance();

        File working = worldEdit.getWorkingDirectoryFile(worldEdit.getConfiguration().saveDir);

        Set<Schematic> schematics = new HashSet<>();
        if (Settings.IMP.PATHS.PER_PLAYER_SCHEMATICS) {
            File dir = new File(working, player.getUniqueId().toString());
            schematics.addAll(loadSchematicsFolder(dir, prefix, player, format));
        }
        schematics.addAll(loadSchematicsFolder(working, prefix, player, format));
        return schematics.toArray(new Schematic[schematics.size()]);
    }

    private Set<Schematic> loadSchematicsFolder(File dir, String prefix, Player player, ClipboardFormat format) {
        WorldEdit worldEdit = WorldEdit.getInstance();

        if (dir.isDirectory()) {
            String[] files = dir.list((f, n) -> n.toLowerCase().startsWith(prefix) && n.toLowerCase().endsWith("." + format.getExtension()));
            if (files != null && files.length != 0) {
                Set<Schematic> schematics = new HashSet<>(files.length);

                int failed = 0;
                for (String filename : files) {
                    try {
                        File file = worldEdit.getSafeSaveFile(player, dir, filename, format.getExtension());
                        if (file.exists()) {
                            schematics.add(format.load(file));
                        }
                    } catch (FilenameException | IOException ex) {
                        Bukkit.getLogger().warning("Error while loading schematic file '" + filename + "': " + ex.getMessage());
                        failed++;
                    }
                }

                if (failed > 0) {
                    player.print("Warning: Failed to load " + failed + " file(s).");
                }

                return schematics;
            }
        }
        return Collections.EMPTY_SET;
    }
}
