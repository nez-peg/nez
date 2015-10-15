package konoha.asm;

import java.util.ArrayList;

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

	private LocalVarScope parent;

	private ArrayList<VarEntry> localVars;

	protected LocalVarScope(int localVarBaseIndex) {
		this.localVarBaseIndex = localVarBaseIndex;
		this.currentLocalVarIndex = this.localVarBaseIndex;
		this.localVars = new ArrayList<VarEntry>();
	}

	protected LocalVarScope(int localVarBaseIndex, LocalVarScope parent) {
		this(localVarBaseIndex);
		this.parent = parent;
	}

	public VarEntry newVarEntry(String varName, Class<?> clazz) {
		int valueSize = Type.getType(clazz).getSize();
		return this.newVarEntry(valueSize, varName, clazz);
	}

	public VarEntry newRetAddressEntry() {
		return this.newVarEntry(1, "", null);
	}

	public VarEntry getLocalVar(String varName) {
		for (VarEntry entry : this.localVars) {
			if (entry.getVarName().equals(varName)) {
				return entry;
			}
		}
		LocalVarScope parent = this.parent;
		VarEntry entry = null;
		while (parent != null && entry == null) {
			entry = parent.getLocalVar(varName);
			parent = parent.getParent();
		}
		return entry;
	}

	/**
	 * 
	 * @param valueSize
	 *            size of variable, long, double is 2, otherwise 1
	 * @param varClass
	 * @param varName
	 * @return
	 */
	private VarEntry newVarEntry(int valueSize, String varName, Class<?> varClass) {
		assert valueSize > 0;
		int index = this.currentLocalVarIndex;
		VarEntry entry = new VarEntry(index, varName, varClass);
		this.currentLocalVarIndex += valueSize;
		this.localVars.add(entry);
		return entry;
	}

	public int getEndIndex() {
		return this.currentLocalVarIndex;
	}

	public LocalVarScope getParent() {
		return this.parent;
	}
}