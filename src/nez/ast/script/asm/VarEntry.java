package nez.ast.script.asm;

/**
 * contains var index
 * 
 * @author skgchxngsxyz-osx
 *
 */
public class VarEntry {
	/**
	 * represents jvm local variable table's index.
	 */
	private final int varIndex;

	private final Class<?> varClass;

	/**
	 * 
	 * @param varIndex
	 * @param varClass
	 *            null, if this entry represents return address entry.
	 */
	VarEntry(int varIndex, Class<?> varClass) {
		this.varIndex = varIndex;
		this.varClass = varClass;
	}

	int getVarIndex() {
		return this.varIndex;
	}

	/**
	 * get class of variable
	 * 
	 * @return return null if this entry represents return address
	 */
	public Class<?> getVarClass() {
		return this.varClass;
	}
}