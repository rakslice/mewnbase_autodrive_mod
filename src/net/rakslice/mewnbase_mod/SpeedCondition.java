package net.rakslice.mewnbase_mod;

public class SpeedCondition {
	
	public float speed = 0f;
	
	protected int tilesAheadThreshold = 0;
	protected float angleDifferenceThreshold = 0f;
	
	public SpeedCondition(float speed, int tilesAheadThreshold, float angleDifferenceThreshold) {
		this.speed = speed;
		this.tilesAheadThreshold = tilesAheadThreshold;
		
		if (angleDifferenceThreshold < 0f) {
			angleDifferenceThreshold = -angleDifferenceThreshold;
		}
		this.angleDifferenceThreshold = angleDifferenceThreshold;
	}
	
	public boolean check(int tilesAhead, float angleDifference) {
		return (tilesAhead >= tilesAheadThreshold) && (angleDifference <= angleDifferenceThreshold) && (angleDifference >= -angleDifferenceThreshold);
	}


}
