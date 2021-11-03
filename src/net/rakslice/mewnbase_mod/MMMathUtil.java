package net.rakslice.mewnbase_mod;

import com.badlogic.gdx.math.Vector2;
import com.cairn4.moonbase.tiles.Tile;

public class MMMathUtil {
	public static float deltaAngle(float target, float initial) {
		return floatmod(target - initial + 180.0f, 360.0f) - 180.0f;
	}

	public static Vector2 tileCenterPos(Vec2Int tileCoords) {
		return new Vector2(tileCoords.x * Tile.GRID_SIZE / 256.0F + 0.25f, tileCoords.y * Tile.GRID_SIZE / 256.0F + 0.25f);
	}
	
	public static final int intmod(int a, int b) {
		int out = a % b;
		if (out < 0) {
			out += b;
		}
		return out;
	}
	
	public static final float floatmod(float a, float b) {
		float out = a % b;
		if (out < 0) {
			out += b;
		}
		return out;
	}
	
	public static final Vec2Int octant2vec(int octant) {
		Vec2Int point = new Vec2Int();
		octant = intmod(octant, 8);
    	switch (octant) {
    	case 0:
    		point.x = 1; point.y = 0;
    		break;
    	case 1:
    		point.x = 1; point.y = 1;
    		break;
    	case 2:
    		point.x = 0; point.y = 1;
    		break;
    	case 3:
    		point.x = -1; point.y = 1;
    		break;
    	case 4:
    		point.x = -1; point.y = 0;
    		break;
    	case 5:
    		point.x = -1; point.y = -1;
    		break;
    	case 6:
    		point.x = 0; point.y = -1;
    		break;
    	case 7:
    		point.x = 1; point.y = -1;
    		break;
    	}		
    	return point;
	}
}
