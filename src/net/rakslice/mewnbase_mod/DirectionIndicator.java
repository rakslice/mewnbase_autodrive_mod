package net.rakslice.mewnbase_mod;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.cairn4.moonbase.AdditiveImage;
import com.cairn4.moonbase.World;
import com.cairn4.moonbase.entities.Vehicle;
import com.cairn4.moonbase.tiles.Tile;

public class DirectionIndicator {
	
	protected AdditiveImage image;
	protected Vehicle vehicle;
	protected World world;
	protected int pos;
	
	protected void createAdditiveImage(Color color) {
		image = new AdditiveImage(world.gameScreen.skin.getDrawable("nav-arrow"));
		image.setSize(48,12);
		image.setOrigin(1);
		image.setTouchable(Touchable.disabled);
		image.setColor(color);
		vehicle.group.addActor((Actor) image);
		
	}

	public DirectionIndicator(World world, Vehicle vehicle, Color color, int pos) {
		this.world = world;
		this.vehicle = vehicle;
		this.pos = pos;
		
		createAdditiveImage(color);
		
		setAngle(0f);
	}
	
	public void setVisible(boolean visible) {
		image.setVisible(visible);
	}
	
	public void setAngle(float angle) {

    	float rot = MMMathUtil.floatmod(vehicle.group.getRotation(), 360.0f);
		image.setRotation(angle-rot);
		//this.headLightSprite.setRotation(steerAngle-rot);

		float rotIndicatorAngle = rot-angle+90f;
		Vector2 rotIndicatorVect = new Vector2(MathUtils.sinDeg(rotIndicatorAngle), MathUtils.cosDeg(rotIndicatorAngle));
		rotIndicatorVect.scl(Tile.TILE_SIZE*0.7f*pos);
		
		image.setPosition(rotIndicatorVect.x, rotIndicatorVect.y);
		
	}

}
