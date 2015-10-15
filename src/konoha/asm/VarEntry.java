package konoha.asm;

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

	private final String varName;

	/**
	 * 
	 * @param varIndex
	 * @param varClass
	 *            null, if this entry represents return address entry.
	 * @param varName
	 */
	VarEntry(int varIndex, String varName, Class<?> varClass) {
		this.varIndex = varIndex;
		this.varClass = varClass;
		this.varName = varName;
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

	public String getVarName() {
		return this.varName;
	}
}