package net.rakslice.mewnbase_mod;

import org.lwjgl.system.MathUtil;

import com.badlogic.gdx.math.Vector2;

public class StraightToDestSteering extends Steering {
	
	protected static void log(String msg) {
		System.out.println("STDS: " + msg);
	}

	@Override
	public SteeringResult steer(SteeringParams params) {
		
		// if we're more or less on the path already, just steer toward the dest
		
		Vector2 pathStart = params.curChunkWorld;
		Vector2 pathDirection = params.steerPointWorld.cpy().sub(pathStart);

		// find side component of our distance to path
		Vector2 distanceToPathOrigin = pathStart.cpy().sub(params.us);
		Vector2 pathPerp = new Vector2(-pathDirection.y, pathDirection.x).nor();
		

		Vector2 sideComponent = pathPerp.cpy().scl(distanceToPathOrigin.dot(pathPerp));
		float sideDistance = sideComponent.len();
		
		Vector2 straightLineToSteerPoint = params.steerPointWorld.cpy().sub(params.us);
		float straightCourseToSteerPoint = straightLineToSteerPoint.angle();
		float pointAngleDifference = MMMathUtil.deltaAngle(straightCourseToSteerPoint, params.rot);
		float allowedAngleForStraightShot = 10.0f;
		
		//if ((sideDistance < 0.25f) && (pointAngleDifference > -allowedAngleForStraightShot) && (pointAngleDifference < allowedAngleForStraightShot)) {
		
		
		SteeringResult result = new SteeringResult();
		
		

		if ((sideDistance < 0.0f)
				) {
			result.steeringAngle = straightCourseToSteerPoint;
			log("*** SIDE DISTANCE LOW " + Float.toString(sideDistance));
		} else {
			log("*** SIDE DISTANCE HIGH " + Float.toString(sideDistance));
			log("pathDirection " + pathDirection + " distanceToPathOrigin " + distanceToPathOrigin + " sideComponent " + sideComponent);
			
			Vector2 forward = pathDirection.cpy().nor();
			Vector2 side = sideComponent.cpy().nor();
			float forwardFrac = 2.0f - sideDistance;
			if (forwardFrac < 0f) forwardFrac = 0f;
			float sideFrac = sideDistance;
			result.steeringAngle = forward.scl(forwardFrac).add(side.scl(sideFrac)).angle();
			result.pathAngle = pathDirection.angle();
			
			
			//result.angle = 45f;
		}
		
		return result;
	}

}
