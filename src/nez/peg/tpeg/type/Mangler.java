package nez.peg.tpeg.type;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;

public class Mangler {
	private Mangler() {
	}

	public static String mangleBasicType(String name) {
		return "B" + name.length() + name;
	}

	/**
	 *
	 * @param types
	 *            contains at least 2 elements
	 * @return
	 * @throws IllegalArgumentException
	 */
	public static String mangleTupleType(LType[] types) throws IllegalArgumentException {
		if (types.length < 2) {
			throw new IllegalArgumentException("need at least 2 elements");
		}

		for (LType t : types) {
			if (t.isVoid()) {
				throw new IllegalArgumentException("element type must not be void type");
			}
		}

		return mangleComposedType("Tuple", types);
	}

	/**
	 *
	 * @param types
	 *            not contains duplicated element contains at least 2 elements.
	 * @return
	 * @throws IllegalArgumentException
	 */
	public static String mangleUnionType(LType[] types) throws IllegalArgumentException {
		return mangleUnionTypeUnsafe(flattenUnionElements(types));
	}

	static String mangleUnionTypeUnsafe(LType[] types) {
		return mangleComposedType("Union", types);
	}

	/**
	 *
	 * @param types
	 * @return
	 * @throws IllegalArgumentException
	 */
	static LType[] flattenUnionElements(LType[] types) throws IllegalArgumentException {
		Set<LType> typeSet = new TreeSet<>();

		LinkedList<LType> queue = new LinkedList<>();
		queue.addAll(Arrays.asList(types));
		while (!queue.isEmpty()) {
			LType type = queue.removeFirst();
			if (type instanceof LType.UnionType) {
				queue.addAll(((LType.UnionType) type).getElementTypes());
			} else if (type.isVoid()) {
				throw new IllegalArgumentException("element type must not be void type");
			} else {
				typeSet.add(type);
			}
		}

		final int size = typeSet.size();
		if (size < 2) {
			throw new IllegalArgumentException("need at least 2 element");
		}

		types = new LType[size];
		int index = 0;
		for (LType t : typeSet) {
			types[index++] = t;
		}
		return types;
	}

	/**
	 *
	 * @param type
	 * @return
	 * @throws IllegalArgumentException
	 */
	public static String mangleArrayType(LType type) throws IllegalArgumentException {
		if (type.equals(LType.voidType)) {
			throw new IllegalArgumentException("element type must not be void type");
		}
		return mangleComposedType("Array", new LType[] { type });
	}

	/**
	 *
	 * @param type
	 * @return
	 * @throws IllegalArgumentException
	 */
	public static String mangleOptionalType(LType type) throws IllegalArgumentException {
		if (type.equals(LType.voidType)) {
			throw new IllegalArgumentException("element type must not be void type");
		}
		return mangleComposedType("Optional", new LType[] { type });
	}

	private static String mangleComposedType(String baseName, LType[] types) {
		StringBuilder sBuilder = new StringBuilder().append("C").append(baseName.length()).append(baseName);

		sBuilder.append("E").append(types.length);
		for (LType type : types) {
			sBuilder.append(type.getUniqueName());
		}
		return sBuilder.toString();
	}

	public static String demangle(String mangledName) {
		StringBuilder sBuilder = new StringBuilder();
		demangle(mangledName, 0, sBuilder);
		return sBuilder.toString();
	}

	private static int demangle(final String mangledName, int index, final StringBuilder sb) {
		assert index < mangledName.length();
		char ch = mangledName.charAt(index);
		switch (ch) {
		case 'B': { // basic type
			int[] len = new int[1];
			index = demangleNumber(mangledName, ++index, len);
			sb.append(mangledName, index, index += len[0]);
			return index;
		}
		case 'C': { // composed type
			int[] len = new int[1];
			index = demangleNumber(mangledName, ++index, len);
			sb.append(mangledName, index, index += len[0]);
			sb.append("<");

			index = demangleNumber(mangledName, ++index, len); // skip 'E'
			for (int i = 0; i < len[0]; i++) {
				if (i > 0) {
					sb.append(",");
				}
				index = demangle(mangledName, index, sb);
			}
			sb.append(">");
			return index;
		}
		default:
			break;
		}
		throw new IllegalArgumentException("illegal char: " + ch + " at " + mangledName);
	}

	/**
	 *
	 * @param mangledName
	 * @param index
	 * @param result
	 *            for demangled result. must be 1 length array.
	 * @return index of next character
	 */
	private static int demangleNumber(final String mangledName, int index, final int[] result) {
		final int size = mangledName.length();
		StringBuilder sb = new StringBuilder();
		for (; index < size; index++) {
			char ch = mangledName.charAt(index);
			if (Character.isDigit(ch)) {
				sb.append(ch);
			} else {
				break;
			}
		}
		result[0] = Integer.parseInt(sb.toString());
		return index;
	}
}
