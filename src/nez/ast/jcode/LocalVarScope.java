package nez.ast.jcode;

import org.objectweb.asm.Type;

class LocalVarScope {
	/**
	 * represent start index of local variable in this scope.
	 */
	private final int localVarBaseIndex;

	/**
	 * represent local variable index. after adding new local variable,
	 * increment this index by value size.
	 */
	private int currentLocalVarIndex;

	protected LocalVarScope(int localVarBaseIndex) {
		this.localVarBaseIndex = localVarBaseIndex;
		this.currentLocalVarIndex = this.localVarBaseIndex;
	}

	public VarEntry newVarEntry(Class<?> clazz) {
		int valueSize = Type.getType(clazz).getSize();
		return this.newVarEntry(valueSize, clazz);
	}

	public VarEntry newRetAddressEntry() {
		return this.newVarEntry(1, null);
	}

	/**
	 * 
	 * @param valueSize
	 *            size of variable, long, double is 2, otherwise 1
	 * @param varClass
	 * @return
	 */
	private VarEntry newVarEntry(int valueSize, Class<?> varClass) {
		assert valueSize > 0;
		int index = this.currentLocalVarIndex;
		VarEntry entry = new VarEntry(index, varClass);
		this.currentLocalVarIndex += valueSize;
		return entry;
	}

	public int getEndIndex() {
		return this.currentLocalVarIndex;
	}
}