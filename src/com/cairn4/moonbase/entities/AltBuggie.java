package com.cairn4.moonbase.entities;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.ai.msg.MessageManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.cairn4.moonbase.Chunk;
import com.cairn4.moonbase.World;
import com.cairn4.moonbase.tiles.PavementStripedFloor;
import com.cairn4.moonbase.tiles.Tile;
import com.cairn4.moonbase.ui.HudNotificationData;

import net.rakslice.mewnbase_mod.DirectionIndicator;
import net.rakslice.mewnbase_mod.MMMathUtil;
import net.rakslice.mewnbase_mod.SpeedCondition;
import net.rakslice.mewnbase_mod.Steering;
import net.rakslice.mewnbase_mod.SteeringParams;
import net.rakslice.mewnbase_mod.SteeringResult;
import net.rakslice.mewnbase_mod.StraightToDestSteering;
import net.rakslice.mewnbase_mod.Vec2Int;

public class AltBuggie extends Buggie {

	// --- Instance variables
	
	protected transient Steering steeringImpl;
	
	protected transient DirectionIndicator steeringDirectionIndicator;
	protected transient DirectionIndicator pathDirectionIndicator;
	protected transient DirectionIndicator thirdDirectionIndicator;
	
	protected transient List<DirectionIndicator> directionIndicators;
	
	public boolean autoDrive = false;
	
	// --- Static utility functions
	
	public static void baseLog(String msg) {
		System.out.println("AltBuggie: " + msg);
	}

	public static void log(String msg) {
		baseLog(msg);
	}
	
	// --- Constructors
	
	public AltBuggie(World world, float worldX, float worldY, float rotation) {
		super(world, worldX, worldY, rotation);
		if (directionIndicators == null) {
			directionIndicators = new ArrayList<DirectionIndicator>();
		}
		steeringImpl = new StraightToDestSteering();
		
		
	}
	
	@Override
	public void createDrawable(String sprite) {
		super.createDrawable(sprite);

		Color greenIndicator = new Color(0.5f, 2f, 0.5f, 1f);
		Color redIndicator = new Color(2f, 0.5f, 0.5f, 1f);
		Color goldIndicator = new Color(2f, 2f, 0.5f, 1f);
		this.thirdDirectionIndicator = createDirectionIndicator(goldIndicator);
		this.steeringDirectionIndicator = createDirectionIndicator(greenIndicator);
		this.pathDirectionIndicator = createDirectionIndicator(redIndicator);	
	}

	protected DirectionIndicator createDirectionIndicator(Color color) {
		if (directionIndicators == null) {
			directionIndicators = new ArrayList<DirectionIndicator>();
		}
		int pos = directionIndicators.size();
		
		
		DirectionIndicator cur = new DirectionIndicator(world, this, color, pos);
		cur.setVisible(autoDrive);
		
		directionIndicators.add(cur);
		
		return cur;
	}
	
	// Auto-drive replaces the previous function of the special ability button,
	// which makes the default buggy's handbrake inaccessible
	
	// TODO: rework auto-drive control so that it's not the special ability,
	// but is separate, with a new keyboard toggle in the UI 
	protected boolean prevSpecialAbility = false;

	public void setSpecialAbility(boolean b) {
	    super.setSpecialAbility(b);
		
		if (!prevSpecialAbility && b) {
			// single button press
			
			// toggle autodrive mode
			setAutoDrive(!autoDrive);
		}
		
		prevSpecialAbility = b;
	}
	
	protected void setAutoDrive(boolean newAutoDrive) {
		autoDrive = newAutoDrive;
		log("changed autodrive to " + (autoDrive? "on":"off"));
		showAutoDriveStatus();
		if (autoDrive) {
			onAutoDriveEnable();
		} else {
			onAutoDriveDisable();
		}
	}
	
	protected void showAutoDriveStatus() {
        HudNotificationData msg = new HudNotificationData();
        msg.icon = "health-particle-red";
        msg.message = "Auto-Drive: " + (autoDrive? "on":"off");
        MessageManager.getInstance().dispatchMessage(3, msg);
        
        for (DirectionIndicator cur: directionIndicators) {
            cur.setVisible(autoDrive);
        }
	}
	
	protected void onAutoDriveEnable() {
		updateAutoDriveDirection();
	}
	
	protected void onAutoDriveDisable() {
		
	}
	
	protected Vehicle.steering autoDriveSteeringHint = steering.none;

	public void setSteering(Vehicle.steering s) {

		//if (s != oldSteering)
		//	log("setSteering " + s.toString());
		
		if (autoDrive) {
			// manual control is disabled; auto-drive control updates will be controlled by update loop
			autoDriveSteeringHint = s;
		} else {
			autoDriveSteeringHint = steering.none;
			super.setSteering(s);
		}
	}
	
    public void setAcceleration(Vehicle.acceleration a) {
		//Vehicle.acceleration oldAccel = currentAccelration;
		
		//if (currentAccelration != oldAccel)
		//	log("setAcceleration " + a.toString());
		
		if (autoDrive) {
			// innerSetAcceleration(a); // for testing steering
			// manual control is disabled; auto-drive control updates will be controlled by update loop
		} else {
			super.setAcceleration(a);
		}
	}

    @Override
    public void update(float delta) {
    	if (autoDrive) {
    		updateAutoDriveDirection();
    	}

    	super.update(delta);
    }
	
	public void updateAutoDriveDirection() {
	    double xd = Math.floor(((this.body.getTransform().getPosition()).x * 256.0F / Tile.GRID_SIZE));
	    double yd = Math.floor(((this.body.getTransform().getPosition()).y * 256.0F / Tile.GRID_SIZE));
	    int worldX = Math.round((float)xd);
	    int worldY = Math.round((float)yd);
	    
	    int range = 6;
	    
	    boolean grid[][] = new boolean[2*range+1][2*range+1];
	    
	    boolean showGrid = false;
	    
	    if (showGrid)
	    	log("Current near tiles:");
    	for (int y = worldY + range; y >= worldY - range; y--) {
    	    for (int x = worldX - range; x <= worldX + range; x++) {
	    		Tile cur = world.getFloorTile(x, y); //.getTile(x, y);
	    		boolean val = cur instanceof PavementStripedFloor;
	    		grid[x - (worldX-range)][y - (worldY - range)] = val;
	    	    if (showGrid) {
	    	    	if (x == worldX && y == worldY) {
		    	    	System.out.print(">" + Boolean.toString(val) + "<");
	    	    	} else {
		    	    	System.out.print(" " + Boolean.toString(val) + " ");
	    	    	}
	    	    	
	    	    }
	    	}
    	    if (showGrid)
    	    	System.out.println();
	    }
    	
    	//setAutoDrive(false);
    	
    	Vec2Int worldPos = new Vec2Int(worldX, worldY);
    	int worldOffsetX = 0;
    	int worldOffsetY = 0;
    	
    	if (!grid[range][range]) { // not currently on a road tile
    		// check sides
    		int count = 0;
    		Vec2Int[] sides = new Vec2Int[] {
    			new Vec2Int(worldX+1, worldY),	
    			new Vec2Int(worldX-1, worldY),	
    			new Vec2Int(worldX, worldY+1),	
    			new Vec2Int(worldX, worldY-1)	
    		};
    		for (Vec2Int cur: sides) {
    			if (grid[range+cur.x-worldX][range+cur.y-worldY]) {
    				worldPos = cur;
    				count++;
    			}
    		}
    		if (count == 0) {

        		log("current tile isn't near road, stopping");
        		setAutoDrive(false);
        		return;
    		}

    		worldOffsetX = worldPos.x - worldX;
    		worldOffsetY = worldPos.y - worldY;
    	}
    	 
    	
    	/*
	    log("Current near ground tiles:");
    	for (int y = worldY + 2; y >= worldY - 2; y--) {
    	    for (int x = worldX - 2; x <= worldX + 2; x++) {
	    		GroundTile cur = world.getGroundTile(x, y);

	    		if (cur == null) {
	    			System.out.print("null ");	    			
	    		} else {
	    			System.out.print(cur.getSprite() + " ");
	    		}
	    	}
    	    System.out.println();
	    	
	    }
	    */
    	
    	/**
    	 * Possible decision outcomes for navigation
    	 * 1. divide the world into 8 octant directions
    	 * 2. determine what is ahead based on where we are facing
    	 *  
    	 * - there is a road ahead
    	 *    ** steer as necessary to put us on the center of the road
    	 * - there is no road ahead, but a road to one side, up to 90 degrees
    	 *    ** steer along a turn for the new road direction
    	 *    ** slow down as appropriate
    	 * - there are multiple roads ahead
    	 *    ** use control inputs to pick a road (no input -- closest to direction; left and right choose the next road of that)
    	 * - else we are coming to the end of the road
    	 *    ** stop
    	 * 
    	 */
    	
    	float rot = MMMathUtil.floatmod(this.group.getRotation(), 360.0f);
    	
    	log("Current group rotation:" + Float.toString(rot));
    	
    	int octant = ((int) ((rot + 22.5f)/45.0f)) % 8;
    	log("Current octant:" + Integer.toString(octant));
   	
    	float speedFactor = 0.0f;
    	
    	boolean testMode = false;
    	
    	float speedTestFactor = testMode? 0.2f : 1.0f;

		float autosteerDeadZoneDeg = 0.5f; // 5.0f;

    	SpeedCondition thresholds[] = new SpeedCondition[] {
    			new SpeedCondition(1.1f, 5, 14f),
    			new SpeedCondition(0.8f, 5, 25f),
    			new SpeedCondition(0.65f, 3, 35f),
    			new SpeedCondition(0.5f, 2, 45f),
    			new SpeedCondition(0.3f, 0, 360f)
    	};
    	
    	//float gentleTurnSpeed = 0.225f * 0.4f;
    	//float sharpTurnSpeed = 0.225f * 0.2f; // 0.06125f * 0.2f;
    	
    	int pathGoodFor = 0;
    	
    	int steerTilesAhead = 5;
    	
    	// look ahead and follow the path

    	boolean showTilePlanning = false; 
    	 
    	Vec2Int autoDriveCurDest = new Vec2Int(worldOffsetX, worldOffsetY);
    	Vec2Int steerPoint = autoDriveCurDest;
    	Vec2Int eventualDirectionAtSteerPoint = null;
    	
    	for (int lookAheadSpaces = 0; lookAheadSpaces < range-1; lookAheadSpaces++) {
        	Vec2Int point = MMMathUtil.octant2vec(octant);
        	Vec2Int aheadOffset = autoDriveCurDest.plus(point);
    		
        	if (showTilePlanning)
        		log("point ahead #" + Integer.toString(lookAheadSpaces) + " " + aheadOffset.toString());
    		
        	if (grid[range+aheadOffset.x][range+aheadOffset.y]) { // there is road ahead
	    		autoDriveCurDest = aheadOffset;
	    		pathGoodFor++;
	    		if (pathGoodFor <= steerTilesAhead) {
	    			steerPoint = autoDriveCurDest;
	    			eventualDirectionAtSteerPoint = point;
	    		}
	    		// we will keep going this direction
	    	} else {
	    		// find a turn
	    		int turnOctants[] = new int[] {octant-2, octant-1, octant+1, octant+2};
	    		
	    		Vec2Int[] turnDirections = new Vec2Int[turnOctants.length];
	    		for (int i = 0; i < turnOctants.length; i++)  {
	    			turnDirections[i] = MMMathUtil.octant2vec(turnOctants[i]);
	    		}
	    		boolean turnsHaveNextRoad[] = new boolean[turnOctants.length];
	    		int count = 0;
	    		{
	        		int i = 0;
	        		for (Vec2Int curTurnDirection : turnDirections) {
	        			Vec2Int turnDestOffset = autoDriveCurDest.plus(curTurnDirection);
	        			boolean curResult = grid[range+turnDestOffset.x][range+turnDestOffset.y];
	        			turnsHaveNextRoad[i] = curResult;
	        			if (curResult) count++;
	        			i++;
	        		}
	    		}
	    		
	    		if (count == 0) {
	            	if (showTilePlanning)
	            		log("point ahead #" + Integer.toString(lookAheadSpaces) + " no good turns");
	    			// stop path here.
	    			break;
	    		} else if (count == 1) {
	            	if (showTilePlanning)
	            		log("point ahead #" + Integer.toString(lookAheadSpaces) + " one turn");
	        		pathGoodFor++;
	    			// take the one turn
	    			for (int i = 0; i < 4; i++) {
	    				if (turnsHaveNextRoad[i]) {
	    					pathGoodFor++;
	    					autoDriveCurDest = autoDriveCurDest.plus(turnDirections[i]);

	    		    		if (pathGoodFor <= steerTilesAhead) {
	    		    			steerPoint = autoDriveCurDest;
	    		    			eventualDirectionAtSteerPoint = turnDirections[i];
	    		    		}

	    					octant = turnOctants[i]; // the resulting direction from the turn will be the new forward direction
	    					break;
	    				}
	    			}
	    		} else if ((turnsHaveNextRoad[0] == turnsHaveNextRoad[1]) && (turnsHaveNextRoad[2] == turnsHaveNextRoad[3]) && (turnsHaveNextRoad[0] ^ turnsHaveNextRoad[2])) {
	    			log("two turns on one side; take the shallow one");
	    			pathGoodFor++;
	    			int takeI = turnsHaveNextRoad[1]? 1:2;
	    			Vec2Int turnDirectionTaken = turnDirections[takeI];
	    			autoDriveCurDest = autoDriveCurDest.plus(turnDirectionTaken);
	    			if (pathGoodFor <= steerTilesAhead) {
	    				steerPoint = autoDriveCurDest;
	    				eventualDirectionAtSteerPoint = turnDirectionTaken;
	    			}
	    			octant = turnOctants[takeI];
	    		} else {
	        		log("something else unknown stop");
	        		log("turns with roads:");
	        		for (boolean cur: turnsHaveNextRoad) {
	        			System.out.print(cur + " ");
	        		}
	        		System.out.println();
	    			// something else we didn't consider.
	    			// stop path here.
	    			break;
	    		}
	    	}
        	
    	}
    	
    	if (pathGoodFor == 0) {
			speedFactor = 0.0f;
			setAutoDrive(false);
			super.setAcceleration(acceleration.none);
			return;
    	}
    	   	
		Vector2 us = this.body.getTransform().getPosition();

		// steering logic
		
		/*
		
		
		if (false) {
			steerAngle = straightCourseToSteerPoint;
			log("--> MODE: STRAIGHT COURSE");
		} else {
			log("--> MODE: CIRCLE ROUTE");


		}
		*/

		Vector2 steerPointWorld = MMMathUtil.tileCenterPos(worldPos.plus(steerPoint));
		
		Vector2 curTilePoint = MMMathUtil.tileCenterPos(worldPos.plus(new Vec2Int(worldOffsetX, worldOffsetY)));
		
		float steerAngle;
		float pathAngle;
		{
			SteeringParams params = new SteeringParams();
			params.rot = rot;
			params.us = us;
			params.steerPointWorld = steerPointWorld;
			params.eventualDirectionAtSteerPoint = eventualDirectionAtSteerPoint;
			params.curChunkWorld = curTilePoint;
			params.physicsSteeringAngle = this.steeringAngle;
			SteeringResult result = steeringImpl.steer(params);
			steerAngle = result.steeringAngle;
			pathAngle = result.pathAngle;
		}
		
		steeringDirectionIndicator.setAngle(steerAngle);
		
		// FIXME temporary for testing
		
		float effectiveSteeringAngle = this.steeringAngle + rot;
		pathDirectionIndicator.setAngle(effectiveSteeringAngle);
		
					
		//log("   --> steer direction " + steerDirection.toString());
		//float steerAngle = steerDirection.angle();		
		
		
		float angularVelocity = this.body.getAngularVelocity();
		float slowingTime = 0.4f;
		float slowingRot = angularVelocity * slowingTime * 10f;
		float eventualRot = rot + slowingRot;
		eventualRot = effectiveSteeringAngle + slowingRot; 
		//float eventualRot = this.body.getLinearVelocity().angle();
		thirdDirectionIndicator.setAngle(eventualRot);
		float angleDifference = MMMathUtil.deltaAngle(steerAngle, eventualRot);
		log("steerAngle " + steerAngle + " rot " + rot 
			+ " difference " + angleDifference + " angular_vel " + angularVelocity + " slowingRot " + slowingRot
			+ " eventualRot " + eventualRot);
		
		Vehicle.steering newSteering;
		if ((-autosteerDeadZoneDeg < angleDifference) && (angleDifference < autosteerDeadZoneDeg)) {
			// we'll call this on-path
			newSteering = steering.none;
		} else if (angleDifference < 0.0f) {
			newSteering = steering.right;
		} else {
			newSteering = steering.left;
		}
		log("NEW STEERING " + newSteering);
		super.setSteering(newSteering);
		
		for (SpeedCondition cur: thresholds) {
			if (cur.check(pathGoodFor, angleDifference)) {
				speedFactor = cur.speed;
				break;
			}
		}
    	
    	log("--> TARGET SPEED FACTOR " + speedFactor + " which is speed " + vd.maxSpeed * speedFactor);
    	log("    cur speed " + this.getSpeedKMH() + " local vel " + this.getLocalVelocity().len());

    	speedFactor *= speedTestFactor;
    	
    	//Train: --> TARGET SPEED FACTOR 0.5 which is speed 4.0
    	//Train:     cur speed 7.9242973 local vel 2.2011666
    	
    	float localSpeedPerKMHSpeed = 2.2011666f/7.9242973f;  
    	
    	{
    		float targetSpeed2 = vd.maxSpeed * speedFactor * localSpeedPerKMHSpeed;
    		targetSpeed2 *= targetSpeed2;
    		float curSpeed2 = getLocalVelocity().len2();
    		if (curSpeed2 < targetSpeed2 * 0.9f) {
    			super.setAcceleration(acceleration.acceleration);    			
    		} else if (curSpeed2 > targetSpeed2 * 1.1f) {
    			super.setAcceleration(acceleration.brake);
    		} else {
    			super.setAcceleration(acceleration.none);
    		}
    	}
        	
	}	
	
	
}
