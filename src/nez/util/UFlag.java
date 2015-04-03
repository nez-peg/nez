package nez.util;

public class UFlag {
	public final static boolean is(int flag, int flag2) {
		return ((flag & flag2) == flag2);
	}
	public final static int setFlag(int flag, int flag2) {
		return (flag | flag2);
	}
	public final static int unsetFlag(int flag, int flag2) {
		return (flag & (~flag2));
	}
}
