package com.cairn4.moonbase.tiles.behaviors;

import com.cairn4.moonbase.ItemData;
import com.cairn4.moonbase.entities.AltBuggie;
import com.cairn4.moonbase.tiles.Tile;

public class AltGarageCrafter extends GarageCrafter {
	ItemData lastFinishedItem;
	String lastFinishedItemActualID;

	public static void baseLog(String msg) {
		System.out.println("AltGarageCrafter: " + msg);
	}

	public static void log(String msg) {
		baseLog(msg);
	}

	public AltGarageCrafter() {
		log("created");
	}
	
	@Override
	public void finishedBuilding() {

		ItemData finishedItem = getCurrentBuildItem();

		if (finishedItem.id.equals("buggie")) {
			log("building altbuggie");

			// this deals with the shenanigans necessary to have this work with the parent class
			// to do everything we need other than spawning the new entity at the right pos
			doFinishedItemBookkeeping();

			float spawnX = (this.chunk.chunkX * 10 + this.localX) * Tile.TILE_SIZE + Tile.TILE_SIZE / 2.0F;
			float spawnY = (this.chunk.chunkY * 10 + this.localY) * Tile.TILE_SIZE + Tile.TILE_SIZE / 2.0F;

			AltBuggie altBuggie = new AltBuggie(this.chunk.world, spawnX, spawnY, -90.0F);

			altBuggie.spawnAnim();
		} else {
			super.finishedBuilding();
		}
	}
	
	// in the 0.52.2 GarageCrafter, if the item id was not recognized by 
	// finishedBuilding, it will still do all the bookkeeping,
	// so we can just change the id and call it to avoid re-implementing that,

	static String placeholderItemID = "invalid_placeholder_value";

	// however we need the original item id back by time observers are notified

	@Override
	protected synchronized void setChanged() {
		restoreOriginalItemID();
		super.setChanged();
	}
	
	protected void doFinishedItemBookkeeping() {
		ItemData finishedItem = getCurrentBuildItem();

		if (lastFinishedItem != null) {
			throw new Error("finished item while finishing item was already in progress");
		}
		lastFinishedItem = finishedItem;
		lastFinishedItemActualID = finishedItem.id;
		
		finishedItem.id = placeholderItemID;
		super.finishedBuilding();
		
		// and then we can just do the parts it didn't do for the unknown vehicle id case
	}
	
	protected void restoreOriginalItemID() {
		if (lastFinishedItem != null) {
			if (lastFinishedItem.id.equals(placeholderItemID)) {
				lastFinishedItem.id = lastFinishedItemActualID;
				lastFinishedItem = null;
			}
		}
	}
	
}
