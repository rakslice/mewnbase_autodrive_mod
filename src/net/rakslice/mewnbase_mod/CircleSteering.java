package net.rakslice.mewnbase_mod;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

public class CircleSteering extends Steering {

	public static void baseLog(String msg) {
		System.out.println("CircleSteering: " + msg);
	}

	public static void log(String msg) {
		baseLog(msg);
	}
	
	@Override
	public SteeringResult steer(SteeringParams params) {		
		// correct the us position a bit for the steering tires location
		
		float frontCorrectDistance = 0.20f;  
		Vector2 facingUnit = new Vector2(MathUtils.cosDeg(params.rot), MathUtils.sinDeg(params.rot));
		params.us.add(facingUnit.cpy().scl(frontCorrectDistance));
		
		// imagine a path along a circle that goes through us, 
		// and through the steer point going the eventual steering direction (so the eventual steering direction is the
		// tangent of the circle)
		
		// the angle between the tangent at the steer point and the vector to us from the steer point (a chord of the circle)
		// is the same as the angle between the tangent and chord where we are, by symmetry 
		
		Vector2 chordDirection = params.steerPointWorld.cpy().sub(params.us);
		
		float tangentAngleAtSteerPoint = new Vector2(params.eventualDirectionAtSteerPoint.x, params.eventualDirectionAtSteerPoint.y).angle();
		float chordAngleMinusTangentAngle = MMMathUtil.deltaAngle(chordDirection.angle(), tangentAngleAtSteerPoint);
				
		float steerAngle = chordDirection.angle() + chordAngleMinusTangentAngle;

		// if chord and steering direction are on opposite sides of facing direction, limit turning
		
		float circleWrongTurningLimit = 5f;
		
		float steerAngleDifference = MMMathUtil.deltaAngle(params.rot, steerAngle); 
		if ((steerAngleDifference < 0f) ^ (MMMathUtil.deltaAngle(params.rot, chordDirection.angle()) < 0f)) {
			if (steerAngleDifference < -circleWrongTurningLimit)  { 
				steerAngle = MMMathUtil.floatmod(params.rot - circleWrongTurningLimit, 360f);
			} else if (steerAngleDifference > circleWrongTurningLimit) {
				steerAngle = MMMathUtil.floatmod(params.rot + circleWrongTurningLimit, 360f);
			}
		}
		
		StringBuilder sb = new StringBuilder("   --> tangent ");
		sb.append(tangentAngleAtSteerPoint);
		sb.append(" chord ");
		sb.append(chordDirection.angle());
		sb.append(" delta ");
		sb.append(chordAngleMinusTangentAngle);
		log(sb.toString());
		
		SteeringResult result = new SteeringResult();
		result.steeringAngle = steerAngle;
		return result;
	}

}
