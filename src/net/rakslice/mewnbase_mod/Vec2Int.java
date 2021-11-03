package net.rakslice.mewnbase_mod;

/** Integer-valued 2 element vector */
public final class Vec2Int {

	public int x = 0;
	public int y = 0;
	
	public Vec2Int() {
		
	}
	
	public Vec2Int(int x, int y) {
		this.x = x;
		this.y = y;
	}
	
	@Override
	public String toString() {
		return Integer.toString(x) + "," + Integer.toString(y);
	}

	public Vec2Int plus(Vec2Int other) {
		return new Vec2Int(this.x + other.x, this.y + other.y);
	}

}
