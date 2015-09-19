package nez.peg.tpeg;

public class LongRange {
	public final long pos;
	public final long len;

	public LongRange(long pos, long len) {
		this.pos = pos;
		this.len = len;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof LongRange && this.pos == ((LongRange) obj).pos && this.len == ((LongRange) obj).len;
	}

	@Override
	public String toString() {
		return "(pos=" + this.pos + ", len=" + this.len + ")";
	}
}