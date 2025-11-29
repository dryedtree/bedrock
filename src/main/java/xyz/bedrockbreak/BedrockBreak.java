package xyz.bedrockbreak;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class BedrockBreak extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("BedrockBreak for Eaglercraft 1.12.2 enabled.");
    }

    @EventHandler
    public void onPiston(BlockPistonExtendEvent e) {
        for (Block b : e.getBlocks()) {
            Block front = b.getRelative(e.getDirection());
            if(front.getType() == Material.BEDROCK) {
                front.setType(Material.AIR);
            }
        }
    }
}
