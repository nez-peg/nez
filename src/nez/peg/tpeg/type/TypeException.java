package nez.peg.tpeg.type;

/**
 * Created by skgchxngsxyz-osx on 15/09/03.
 */
public class TypeException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3927572996889146061L;

	public TypeException(String message) {
		super(message);
	}

	public static void typeError(String message) throws TypeException {
		throw new TypeException(message);
	}
}
