# VaeronTools

This is a **Spigot** plugin providing various tools for building Minecraft maps.
The plugin was originally developed for the [Vaeron build team](https://twitter.com/teamvaeron) but now made available for everyone including the source code.
Feel free to improve existing features or implement new ones via Pull requests.

## Requirements

This plugin requires [FAWE](https://www.spigotmc.org/resources/fast-async-worldedit-voxelsniper.13932/) and currently doesn't work with WorldEdit.

## Current commands

This is only a short summary, see [here](https://docs.google.com/document/d/12E4FCDBK3RkA1Tj0hDZlaY1BTzYPzINYfONOY03zFX8) for an in-depth documentation on command usage on publication.

### //vaesweep
This command works like the WorldEdit [//curve](http://wiki.sk89q.com/wiki/WorldEdit/Reference) command 
but instead of tracing the curve with a specific block or pattern it uses a previously made copy of a region 
that is pasted along the curve.

### //vaeschematic
This command works similarly to **//vaesweep** but instead of using a copy of a region this command uses all
schematics with a specific prefix and pastes them with random 90° rotations along the curve.

## Building the project

To build the plugin you need **Git** and **Maven**.
1. Clone the repository using `git clone https://github.com/Rafessor/VaeronTools.git`.
2. Compile the project with `mvn clean install`.
3. The jar file will be at `target/VaeronTools-x.x.x.jar`.
