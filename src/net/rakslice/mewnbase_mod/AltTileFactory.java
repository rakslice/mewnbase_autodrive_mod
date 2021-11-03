package net.rakslice.mewnbase_mod;

import java.util.TreeMap;

import com.cairn4.moonbase.Chunk;
import com.cairn4.moonbase.tiles.Garage;
import com.cairn4.moonbase.tiles.GroundTile;
import com.cairn4.moonbase.tiles.Tile;
import com.cairn4.moonbase.tiles.TileFactory;
import com.cairn4.moonbase.worlddata.GroundTileData;
import com.cairn4.moonbase.worlddata.TileData;

public class AltTileFactory extends TileFactory {
	
	protected String lastLog = null;
	protected int lastCount = 0;
	protected TreeMap<String, String> altTileMap;

	public AltTileFactory() {
		altTileMap = new TreeMap<String, String>();
		
		altTileMap.put(Garage.class.getName(), AltGarage.class.getName());
	}

	protected void showReps() {
		baseLog("Repeated " + Integer.toString(lastCount) + " times.");
	}
	
	public static void baseLog(String msg) {
		System.out.println("AltTileFactory: " + msg);
	}
	
	protected void log(String msg) {
		if (!msg.equals(lastLog)) {
			if (lastLog != null) showReps();
			lastLog = msg;
			lastCount = 1;
			baseLog(msg);
		} else {
			lastCount += 1;
			if (lastCount % 256 == 0) {
				showReps();
			}
		}
		
	}
	
	@Override
	public Tile createTile(Chunk chunk, TileData tileData) throws NoSuchFieldException, IllegalAccessException {
		log("AltTileFactory creating tile " + tileData.name);
		String altName = altTileMap.get(tileData.name);
		if (altName == null) {
			// not remapped
			return super.createTile(chunk, tileData);
		} else {
			String origName = tileData.name;
			tileData.name = altName;
			try {
				return super.createTile(chunk, tileData);				
			} finally {
				tileData.name = origName;
			}
		}
	}
	
	@Override
	public GroundTile createGroundTile(Chunk chunk, GroundTileData groundTileData) {
		//log("AltTileFactory creating ground tile w/ biome " + groundTileData.biome);
		return super.createGroundTile(chunk, groundTileData);
	}
	
}
