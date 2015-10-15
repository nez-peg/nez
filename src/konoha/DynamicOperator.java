package konoha;

import java.util.Comparator;

import konoha.script.ScriptRuntimeException;
import konoha.script.TypeSystem;

public class DynamicOperator {

	public final static Object opPlus(Object a) {
		if (a instanceof Double || a instanceof Float) {
			return +((Number) a).doubleValue();
		}
		if (a instanceof Long) {
			return +((Number) a).longValue();
		}
		if (a instanceof Number) {
			return +((Number) a).intValue();
		}
		throw new ScriptRuntimeException("unsupproted operation + %s", c(a));
	}

	public final static Object opMinus(Object a) {
		if (a instanceof Number) {
			if (a instanceof Double || a instanceof Float) {
				return -((Number) a).doubleValue();
			}
			if (a instanceof Long) {
				return -((Number) a).longValue();
			}
			return -((Number) a).intValue();
		}
		throw new ScriptRuntimeException("unsupproted operation - %s", c(a));
	}

	private static String c(Object o) {
		return TypeSystem.name(o.getClass());
	}

	// @Deprecated
	// public final static Object opAdd(String a, Object b) {
	// return a + b;
	// }

	public final static Object opAdd(Object a, Object b) {
		if (a instanceof Number && b instanceof Number) {
			if (a instanceof Double || a instanceof Double || a instanceof Float || b instanceof Float) {
				return ((Number) a).doubleValue() + ((Number) b).doubleValue();
			}
			if (a instanceof Long || a instanceof Long) {
				return ((Number) a).longValue() + ((Number) b).longValue();
			}
			return ((Number) a).intValue() + ((Number) b).intValue();
		}
		throw new ScriptRuntimeException("unsupproted operation %s + %s", c(a), c(b));
	}

	public final static Object opSub(Object a, Object b) {
		if (a instanceof Number && b instanceof Number) {
			if (a instanceof Double || a instanceof Double || a instanceof Float || b instanceof Float) {
				return ((Number) a).doubleValue() - ((Number) b).doubleValue();
			}
			if (a instanceof Long || a instanceof Long) {
				return ((Number) a).longValue() - ((Number) b).longValue();
			}
			return ((Number) a).intValue() - ((Number) b).intValue();
		}
		throw new ScriptRuntimeException("unsupproted operation %s - %s", c(a), c(b));
	}

	public final static Object opMul(Object a, Object b) {
		if (a instanceof Number && b instanceof Number) {
			if (a instanceof Double || a instanceof Double || a instanceof Float || b instanceof Float) {
				return ((Number) a).doubleValue() * ((Number) b).doubleValue();
			}
			if (a instanceof Long || a instanceof Long) {
				return ((Number) a).longValue() * ((Number) b).longValue();
			}
			return ((Number) a).intValue() * ((Number) b).intValue();
		}
		throw new ScriptRuntimeException("unsupproted operation %s * %s", c(a), c(b));
	}

	public final static Object opDiv(Object a, Object b) {
		if (a instanceof Number && b instanceof Number) {
			if (a instanceof Double || a instanceof Double || a instanceof Float || b instanceof Float) {
				return ((Number) a).doubleValue() / ((Number) b).doubleValue();
			}
			if (a instanceof Long || a instanceof Long) {
				return ((Number) a).longValue() / ((Number) b).longValue();
			}
			return ((Number) a).intValue() / ((Number) b).intValue();
		}
		throw new ScriptRuntimeException("unsupproted operation %s / %s", c(a), c(b));
	}

	public final static Object opMod(Object a, Object b) {
		if (a instanceof Number && b instanceof Number) {
			if (a instanceof Double || a instanceof Double || a instanceof Float || b instanceof Float) {
				return ((Number) a).doubleValue() % ((Number) b).doubleValue();
			}
			if (a instanceof Long || a instanceof Long) {
				return ((Number) a).longValue() % ((Number) b).longValue();
			}
			return ((Number) a).intValue() % ((Number) b).intValue();
		}
		throw new ScriptRuntimeException("unsupproted operation %s % %s", c(a), c(b));
	}

	@SuppressWarnings("unchecked")
	public final static boolean opEquals(Object a, Object b) {
		if (a instanceof Number && b instanceof Number) {
			if (a instanceof Double || a instanceof Double || a instanceof Float || b instanceof Float) {
				return ((Number) a).doubleValue() == ((Number) b).doubleValue();
			}
			if (a instanceof Long || a instanceof Long) {
				return ((Number) a).longValue() == ((Number) b).longValue();
			}
			return ((Number) a).intValue() == ((Number) b).intValue();
		}
		if (a instanceof Comparator<?> && a.getClass() == b.getClass()) {
			return ((Comparator<Object>) a).compare(a, b) == 0;
		}
		return a == b;
	}

	@SuppressWarnings("unchecked")
	public final static boolean opNotEquals(Object a, Object b) {
		if (a instanceof Number && b instanceof Number) {
			if (a instanceof Double || a instanceof Double || a instanceof Float || b instanceof Float) {
				return ((Number) a).doubleValue() != ((Number) b).doubleValue();
			}
			if (a instanceof Long || a instanceof Long) {
				return ((Number) a).longValue() != ((Number) b).longValue();
			}
			return ((Number) a).intValue() != ((Number) b).intValue();
		}
		if (a instanceof Comparator<?> && a.getClass() == b.getClass()) {
			return ((Comparator<Object>) a).compare(a, b) == 0;
		}
		return a != b;
	}

	@SuppressWarnings("unchecked")
	public final static boolean opLessThan(Object a, Object b) {
		if (a instanceof Number && b instanceof Number) {
			if (a instanceof Double || a instanceof Double || a instanceof Float || b instanceof Float) {
				return ((Number) a).doubleValue() < ((Number) b).doubleValue();
			}
			if (a instanceof Long || a instanceof Long) {
				return ((Number) a).longValue() < ((Number) b).longValue();
			}
			return ((Number) a).intValue() < ((Number) b).intValue();
		}
		if (a instanceof Comparator<?> && a.getClass() == b.getClass()) {
			return ((Comparator<Object>) a).compare(a, b) < 0;
		}
		throw new ScriptRuntimeException("unsupproted operation %s < %s", c(a), c(b));
	}

	@SuppressWarnings("unchecked")
	public final static boolean opGreaterThan(Object a, Object b) {
		if (a instanceof Number && b instanceof Number) {
			if (a instanceof Double || a instanceof Double || a instanceof Float || b instanceof Float) {
				return ((Number) a).doubleValue() > ((Number) b).doubleValue();
			}
			if (a instanceof Long || a instanceof Long) {
				return ((Number) a).longValue() > ((Number) b).longValue();
			}
			return ((Number) a).intValue() > ((Number) b).intValue();
		}
		if (a instanceof Comparator<?> && a.getClass() == b.getClass()) {
			return ((Comparator<Object>) a).compare(a, b) > 0;
		}
		throw new ScriptRuntimeException("unsupproted operation %s > %s", c(a), c(b));
	}

	@SuppressWarnings("unchecked")
	public final static boolean opLessThanEquals(Object a, Object b) {
		if (a instanceof Number && b instanceof Number) {
			if (a instanceof Double || a instanceof Double || a instanceof Float || b instanceof Float) {
				return ((Number) a).doubleValue() <= ((Number) b).doubleValue();
			}
			if (a instanceof Long || a instanceof Long) {
				return ((Number) a).longValue() <= ((Number) b).longValue();
			}
			return ((Number) a).intValue() <= ((Number) b).intValue();
		}
		if (a instanceof Comparator<?> && a.getClass() == b.getClass()) {
			return ((Comparator<Object>) a).compare(a, b) <= 0;
		}
		throw new ScriptRuntimeException("unsupproted operation %s <= %s", c(a), c(b));
	}

	@SuppressWarnings("unchecked")
	public final static boolean opGreaterThanEquals(Object a, Object b) {
		if (a instanceof Number && b instanceof Number) {
			if (a instanceof Double || a instanceof Double || a instanceof Float || b instanceof Float) {
				return ((Number) a).doubleValue() >= ((Number) b).doubleValue();
			}
			if (a instanceof Long || a instanceof Long) {
				return ((Number) a).longValue() >= ((Number) b).longValue();
			}
			return ((Number) a).intValue() >= ((Number) b).intValue();
		}
		if (a instanceof Comparator<?> && a.getClass() == b.getClass()) {
			return ((Comparator<Object>) a).compare(a, b) >= 0;
		}
		throw new ScriptRuntimeException("unsupproted operation %s >= %s", c(a), c(b));
	}

	public final static Object opLeftShift(Object a, Object b) {
		if (a instanceof Number && b instanceof Number) {
			if (a instanceof Long || a instanceof Long) {
				return ((Number) a).longValue() << ((Number) b).longValue();
			}
			return ((Number) a).intValue() << ((Number) b).intValue();
		}
		throw new ScriptRuntimeException("unsupproted operation %s << %s", c(a), c(b));
	}

	public final static Object opRightShift(Object a, Object b) {
		if (a instanceof Number && b instanceof Number) {
			if (a instanceof Long || a instanceof Long) {
				return ((Number) a).longValue() >> ((Number) b).longValue();
			}
			return ((Number) a).intValue() >> ((Number) b).intValue();
		}
		throw new ScriptRuntimeException("unsupproted operation %s >> %s", c(a), c(b));
	}

	public final static Object opLogicalRightShift(Object a, Object b) {
		if (a instanceof Number && b instanceof Number) {
			if (a instanceof Long || a instanceof Long) {
				return ((Number) a).longValue() >>> ((Number) b).longValue();
			}
			return ((Number) a).intValue() >>> ((Number) b).intValue();
		}
		throw new ScriptRuntimeException("unsupproted operation %s >>> %s", c(a), c(b));
	}

	public final static Object opBitwiseAnd(Object a, Object b) {
		if (a instanceof Number && b instanceof Number) {
			if (a instanceof Long || a instanceof Long) {
				return ((Number) a).longValue() & ((Number) b).longValue();
			}
			return ((Number) a).intValue() & ((Number) b).intValue();
		}
		throw new ScriptRuntimeException("unsupproted operation %s & %s", c(a), c(b));
	}

	public final static Object opBitwiseOr(Object a, Object b) {
		if (a instanceof Number && b instanceof Number) {
			if (a instanceof Long || a instanceof Long) {
				return ((Number) a).longValue() | ((Number) b).longValue();
			}
			return ((Number) a).intValue() | ((Number) b).intValue();
		}
		throw new ScriptRuntimeException("unsupproted operation %s | %s", c(a), c(b));
	}

	public final static Object opBitwiseXor(Object a, Object b) {
		if (a instanceof Number && b instanceof Number) {
			if (a instanceof Long || a instanceof Long) {
				return ((Number) a).longValue() | ((Number) b).longValue();
			}
			return ((Number) a).intValue() | ((Number) b).intValue();
		}
		throw new ScriptRuntimeException("unsupproted operation %s ^ %s", c(a), c(b));
	}

	public final static Object opCompl(Object a) {
		if (a instanceof Long) {
			return ~((Number) a).longValue();
		}
		if (a instanceof Number) {
			return ~((Number) a).intValue();
		}
		throw new ScriptRuntimeException("unsupproted operation ~ %s", c(a));
	}

	/** converter */

	public final static Object to_Object(boolean a) {
		return a;
	}

	public final static Object to_Object(byte a) {
		return a;
	}

	public final static Object to_Object(short a) {
		return (int) a;
	}

	public final static Object to_Object(int a) {
		return a;
	}

	public final static Object to_Object(long a) {
		return a;
	}

	public final static Object to_Object(float a) {
		return (double) a;
	}

	public final static Object to_Object(double a) {
		return a;
	}

	/** downcast */

	public final static boolean to_boolean(Object a) {
		return (Boolean) a;
	}

	public final static byte to_byte(Object a) {
		return ((Number) a).byteValue();
	}

	public final static short to_short(Object a) {
		return ((Number) a).shortValue();
	}

	public final static int to_int(Object a) {
		return ((Number) a).intValue();
	}

	public final static long to_long(Object a) {
		return ((Number) a).longValue();
	}

	public final static float to_float(Object a) {
		return ((Number) a).floatValue();
	}

	public final static double to_double(Object a) {
		return ((Number) a).doubleValue();
	}

}
