package nez.lang;

public class Bytes {

	// Utils
	public final static boolean[] newMap(boolean initValue) {
		boolean[] b = new boolean[257];
		if (initValue) {
			for (int i = 0; i < b.length; i++) {
				b[i] = initValue;
			}
		}
		return b;
	}

	public final static boolean[] parseByteClass(String octet) {
		boolean[] b = Bytes.newMap(true);
		while (octet.length() < 8) {
			octet = "0" + octet;
		}
		for (int i = 0; i < 8; i++) {
			int position = 0x80 >> i;
			switch (octet.charAt(i)) {
			case '0':
				for (int j = 0; j < 256; j++) {
					if ((j & position) == 0) {
						continue;
					}
					b[j] = false;
				}
				break;
			case '1':
				for (int j = 0; j < 256; j++) {
					if ((j & position) != 0) {
						continue;
					}
					b[j] = false;
				}
				break;
			case 'x':
			default:
				break;
			}
		}
		b[256] = false;
		return b;
	}

	public final static void clear(boolean[] byteMap) {
		for (int c = 0; c < byteMap.length; c++) {
			byteMap[c] = false;
		}
	}

	public final static void appendRange(boolean[] b, int beginChar, int endChar) {
		for (int c = beginChar; c <= endChar; c++) {
			b[c] = true;
		}
	}

	public final static void appendBitMap(boolean[] dst, boolean[] src) {
		for (int i = 0; i < 256; i++) {
			if (src[i]) {
				dst[i] = true;
			}
		}
	}

	public final static void reverse(boolean[] byteMap, boolean isBinary) {
		for (int i = 0; i < 256; i++) {
			byteMap[i] = !byteMap[i];
		}
		if (!isBinary) {
			byteMap[0] = false;
		}
	}

}
