package nez.runtime;

import nez.expr.Expression;

public final class MemoPoint {
	public final int id;
	public final String label;
	public final Expression e;
	final boolean contextSensitive;

	int  memoHit = 0;
	int  memoFailHit = 0;
	long hitLength = 0;
	int  maxLength = 0;
	int  memoMiss = 0;
	
	MemoPoint(int id, String label, Expression e, boolean contextSensitive) {
		this.id = id;
		this.label = label;
		this.e = e;
		this.contextSensitive = contextSensitive;
	}
	
	void memoHit(int consumed) {
		this.memoHit += 1;
		this.hitLength += consumed;
		if(this.maxLength < consumed) {
			this.maxLength = consumed;
		}
	}

	void failHit() {
		this.memoFailHit += 1;
	}

	void miss() {
		this.memoMiss ++;
	}
	
	public final double hitRatio() {
		if(this.memoMiss == 0) return 0.0;
		return (double)this.memoHit / this.memoMiss;
	}

	public final double failHitRatio() {
		if(this.memoMiss == 0) return 0.0;
		return (double)this.memoFailHit / this.memoMiss;
	}

	public final double meanLength() {
		if(this.memoHit == 0) return 0.0;
		return (double)this.hitLength / this.memoHit;
	}

	public final int count() {
		return this.memoMiss + this.memoFailHit + this.memoHit;
	}

	protected final boolean checkUseless() {
		if(this.memoMiss == 32) {
			if(this.memoHit < 2) {          
				return true;
			}
		}
		if(this.memoMiss % 64 == 0) {
			if(this.memoHit == 0) {
				return true;
			}
//			if(this.hitLength < this.memoHit) {
//				enableMemo = false;
//				disabledMemo();
//				return;
//			}
			if(this.memoMiss / this.memoHit > 10) {
				return true;
			}
		}
		return false;
	}

}
