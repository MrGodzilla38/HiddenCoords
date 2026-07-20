package dev.hiddencoords;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CoordOverlayMod implements ModInitializer {
    public static final String MOD_ID = "hidden_coords";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Hidden Coords initialized (client-side only)");
    }
}
