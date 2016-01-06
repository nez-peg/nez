package nez.parser.moz;

public class ByteReader {
	byte[] source;
	int pos;

	public ByteReader(byte[] b) {
		this.source = b;
		this.pos = 0;
	}

	public final int read_i8() {
		pos++;
		return this.source[pos - 1];
	}

	public final int read_u8() {
		pos++;
		return this.source[pos - 1] & 0xff;
	}

	public final int read_u16() {
		int n0 = read_u8();
		int n1 = read_u8();
		return n0 * 256 + n1;
	}

	public final int read_u24() {
		int n0 = read_u8();
		int n1 = read_u8();
		int n2 = read_u8();
		return n0 * 256 * 256 + n1 * 256 + n2;
	}

	public final int read_u32() {
		int n0 = read_u8();
		int n1 = read_u8();
		int n2 = read_u8();
		int n3 = read_u8();
		return n0 * (256 * 256 * 256) + n1 * 256 * 256 + n2 * 256 + n3;
	}

	public final boolean read_b() {
		pos++;
		return this.source[pos - 1] != 0;
	}

}
