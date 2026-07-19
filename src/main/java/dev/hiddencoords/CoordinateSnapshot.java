package dev.hiddencoords;

/** Immutable data copied from the Minecraft thread before it is sent to Swing. */
public record CoordinateSnapshot(int x, int y, int z, String dimension, String biome) { }
