package com.hpspells.core.spell;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.material.Door;

import com.hpspells.core.HPS;
import com.hpspells.core.SpellTargeter.SpellHitEvent;
import com.hpspells.core.spell.Spell.SpellInfo;
import com.hpspells.core.util.BlockUtils;
import com.hpspells.core.util.ParticleEffect;

@SpellInfo(
        name = "Colloportus",
        description = "descColloportus",
        range = 50,
        goThroughWalls = false,
        cooldown = 60
)

public class Colloportus extends Spell {
    
    private static Map<Integer, Block> doorMap = new HashMap<>();
    private static int idCounter = Integer.MIN_VALUE;
    private static List<Material> doorTypes = new ArrayList<>(Arrays.asList(
            Material.WOODEN_DOOR,
            Material.IRON_DOOR_BLOCK,
            //1.8 Doors
            Material.ACACIA_DOOR,
            Material.BIRCH_DOOR,
            Material.DARK_OAK_DOOR,
            Material.JUNGLE_DOOR,
            Material.SPRUCE_DOOR
    ));
    private static List<Material> padTypes = new ArrayList<>(Arrays.asList(
            Material.WOOD_PLATE,
            Material.STONE_PLATE,
            Material.IRON_PLATE,
            Material.GOLD_PLATE
    ));
    

    public Colloportus(HPS instance) {
        super(instance);
    }

    @Override
    public boolean cast(Player p) {
        HPS.SpellTargeter.register(p, new SpellHitEvent() {

            @Override
            public void hitBlock(Block block) {
//                HPS.PM.broadcastMessage("Block type: " + block.getType());
                if (doorTypes.contains(block.getType())) {
                    if (doorMap.containsValue(block)) {
                    	HPS.PM.warn(p, "That door is already locked.");
                    	return;
                    }
                    
                    lockDoor(block);
                    
                    Block otherDoorBlock = getDoubleDoor(block);
                    if (otherDoorBlock != null) {
                    	lockDoor(otherDoorBlock);
                    }
                } else {
                    HPS.PM.warn(p, "You may only use this spell on doors.");
                }
            }

            @Override
            public void hitEntity(LivingEntity entity) {
                HPS.PM.warn(p, HPS.Localisation.getTranslation("spellBlockOnly"));
            }
            
        }, 1f, ParticleEffect.BARRIER);
        return true;
    }
    
    private void lockDoor(Block block) {
    	BlockState blockState = block.getState();
        // The way minecraft works, top door block doesnt have correct state.
        if (((Door) blockState.getData()).isTopHalf()) {
            blockState = block.getRelative(BlockFace.DOWN).getState();
        }
        Door door = (Door) blockState.getData();
        door.setOpen(false);
        blockState.setData(door);
        blockState.update();
        
        Block otherDoorBlock = ((Door) block.getState().getData()).isTopHalf() ? block.getRelative(BlockFace.DOWN) : block.getRelative(BlockFace.UP);
        final int blockId = assignId(), otherBlockId = assignId();
        doorMap.put(blockId, block);
        doorMap.put(otherBlockId, otherDoorBlock);
        HPS.PM.debug("Added door id:" + blockId);
        HPS.PM.debug("Added door id:" + otherBlockId);
        
        Bukkit.getScheduler().scheduleSyncDelayedTask(HPS, new Runnable() {
            @Override
            public void run() {
            	doorMap.remove(blockId);
            	doorMap.remove(otherBlockId);
            	HPS.PM.debug("Removed door id:" + blockId);
            	HPS.PM.debug("Removed door id:" + otherBlockId);
            }
        }, ((Integer)getConfig("lock-time", 30) * 20));
    }

    /**
     * Unlocks a Colloportus locked door. Unlocks both doors if door is a double door.
     * 
     * @param block Source block
     * @return true if door unlocks
     */
    public static boolean unlockDoor(Block block) {
    	if (doorTypes.contains(block.getType())) {
            if (doorMap.containsValue(block)) {
	            Block otherDoorBlock = ((Door) block.getState().getData()).isTopHalf() ? block.getRelative(BlockFace.DOWN) : block.getRelative(BlockFace.UP);
	        	doorMap.values().remove(block);
	        	doorMap.values().remove(otherDoorBlock);
	        	
	        	Block door2Block = getDoubleDoor(block);
	        	if (door2Block != null && doorMap.containsValue(door2Block)) {
	        		Block otherDoor2Block = ((Door) door2Block.getState().getData()).isTopHalf() ? door2Block.getRelative(BlockFace.DOWN) : door2Block.getRelative(BlockFace.UP);
	        		doorMap.values().remove(door2Block);
		        	doorMap.values().remove(otherDoor2Block);
	        	}
	        	return true;
            }
        }
    	return false;
    }
    
    public static boolean isLockedDoor(Block block) {
        if (doorMap.containsValue(block))
            return true;
        return false;
    }
    
    public static List<Material> getDoorTypes() {
        return doorTypes;
    }
    
    public static List<Material> getPadTypes() {
        return padTypes;
    }

    private Integer assignId() {
        if (idCounter == Integer.MAX_VALUE) {
            idCounter = Integer.MIN_VALUE;
        } else {
            idCounter++;
        }
        return idCounter;
    }
    
    /**
     * Get the double door for the given block.
     *
     * @param block Source block
     * @return Gets the double door block or return null.
     */
    private static Block getDoubleDoor(Block block) {
        Block found = doorTypes.contains(block.getType()) ? BlockUtils.findAdjacentBlock(block, block.getType()) : null;
        return found;
    }
}
