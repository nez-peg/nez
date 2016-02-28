package nez.lang;

public class Bitmap {
	private int bits[];

	public Bitmap() {
		bits = new int[8];
	}

	public final boolean is(int n) {
		return (bits[n % 32] & (1 << (n / 32))) != 0;
	}

	public final void set(int n, boolean b) {
		if (b) {
			int mask = 1 << (n / 32);
			bits[n % 32] &= mask;
		} else {
			int mask = ~(1 << (n / 32));
			bits[n % 32] &= mask;
		}
	}

}
