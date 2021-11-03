package net.rakslice.mewnbase_mod;

import java.util.HashMap;
import java.util.Map;

import com.cairn4.moonbase.GameLoader;
import com.cairn4.moonbase.World;
import com.cairn4.moonbase.worlddata.EntityData;
import com.cairn4.moonbase.worlddata.GameSaveData;

public class AltGameLoader extends GameLoader {

	Map<String, String> entityClassReplacements;
	
	public AltGameLoader() {
		entityClassReplacements = new HashMap<>();
		entityClassReplacements.put("com.cairn4.moonbase.entities.Buggie", "com.cairn4.moonbase.entities.AltBuggie");
		entityClassReplacements.put("com.cairn4.moonbase.entities.Train", "com.cairn4.moonbase.entities.AltBuggie");
	}
	
	@Override
	protected void loadEntities(World world, GameSaveData gsd) {
		for (EntityData ed: gsd.entityDataList) {
			String replacementClassName = entityClassReplacements.get(ed.className);
			if (replacementClassName != null) {
				ed.className = replacementClassName;
			}
		}
		super.loadEntities(world, gsd);
	}

}
