package net.rakslice.mewnbase_mod;

import java.lang.reflect.Field;

import com.cairn4.moonbase.Chunk;
import com.cairn4.moonbase.World;
import com.cairn4.moonbase.tiles.Garage;
import com.cairn4.moonbase.tiles.behaviors.AltGarageCrafter;
import com.cairn4.moonbase.tiles.behaviors.GarageCrafter;

public class AltGarage extends Garage {

	public static void baseLog(String msg) {
		System.out.println("AltGarage: " + msg);
	}
	
	public static void log(String msg) {
		baseLog(msg);
	}
	
	protected void replaceGarageCrafter() {
		log("replacing garage crafter");

		FieldPatchHelper<Garage, GarageCrafter> helper = new FieldPatchHelper<Garage, GarageCrafter>("AltGarage"); 
		Field garageCrafterField = helper.getField(Garage.class, "garageCrafter");
		
		GarageCrafter oldGarageCrafter = helper.getValueFromField(garageCrafterField, this);
		
		behaviorList.remove(oldGarageCrafter);
		
		GarageCrafter newGarageCrafter = new AltGarageCrafter();
		helper.setValueToField(garageCrafterField, this, newGarageCrafter);
		
		newGarageCrafter.setSpawnTile(this.chunk, this.x, this.y);
		newGarageCrafter.buildQueueSizeLimit = 1;
		newGarageCrafter.requirePowerToCraft = true;
		newGarageCrafter.baseModule = this;
		newGarageCrafter.setupBuildables(this.builderId);		

		behaviorList.add(newGarageCrafter);		
	}

	public AltGarage(World world, Chunk chunk, int x, int y) {
		super(world, chunk, x, y);
		log("created");
		
		replaceGarageCrafter();		
	}

}
