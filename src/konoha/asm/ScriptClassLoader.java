package konoha.asm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.HashMap;
import java.util.Map;

import konoha.script.TypeSystem;
import nez.main.Verbose;
import nez.util.ConsoleUtils;

/**
 * used for user defined class loading. not thread safe.
 * 
 * @author skgchxngsxyz-osx
 *
 */
public class ScriptClassLoader extends ClassLoader {
	/**
	 * if true, dump byte code.
	 */
	private TypeSystem typeSystem;

	/**
	 * must be fully qualified binary name(contains . ).
	 */
	private final String allowedPackageName;

	/**
	 * contains byte code(require java class specification). key is fully
	 * qualified binary class name(contains . ).
	 */
	private final Map<String, byte[]> byteCodeMap;

	/**
	 * 
	 * @param packageName
	 *            not null
	 */
	// public ScriptClassLoader(String packageName) {
	// super();
	// this.allowedPackageName = toBinaryName(packageName);
	// this.byteCodeMap = new HashMap<>();
	// }

	/**
	 * used for child class loader creation.
	 * 
	 * @param classLoader
	 */
	protected ScriptClassLoader(ScriptClassLoader classLoader) {
		super(classLoader);
		this.allowedPackageName = classLoader.allowedPackageName;
		this.byteCodeMap = new HashMap<>();
		this.typeSystem = classLoader.typeSystem;
		this.dumpDirectory = classLoader.dumpDirectory;
	}

	public ScriptClassLoader(TypeSystem typeSystem) {
		super();
		this.allowedPackageName = null;
		this.byteCodeMap = new HashMap<>();
		this.typeSystem = typeSystem;
		this.dumpDirectory = System.getenv("DUMPDIR");
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		byte[] byteCode = this.byteCodeMap.remove(name);
		if (byteCode == null) {
			throw new ClassNotFoundException("class is not found: " + name);
		}
		return this.defineClass(name, byteCode, 0, byteCode.length);
	}

	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		if (this.allowedPackageName == null) { // allow all package name
			return super.loadClass(name, resolve);
		}

		Class<?> foundClass = this.findLoadedClass(name);
		if (foundClass == null) {
			ClassLoader parent = this.getParent();
			if ((parent instanceof ScriptClassLoader) || !name.startsWith(this.allowedPackageName)) {
				try {
					foundClass = parent.loadClass(name);
				} catch (ClassNotFoundException e) {
				}
			}
		}
		if (foundClass == null) {
			foundClass = this.findClass(name);
		}
		if (resolve) {
			this.resolveClass(foundClass);
		}
		return foundClass;
	}

	/**
	 * set byte code and class name. before class loading, must call it.
	 * 
	 * @param className
	 *            - must be fully qualified class name.
	 * @param byteCode
	 *            require java class specification.
	 */

	public void addByteCode(String className, byte[] byteCode) {
		String binaryName = toBinaryName(className);
		if (this.byteCodeMap.put(binaryName, byteCode) != null) {
			throw new RuntimeException("already defined class: " + className);
		}
		this.dump(binaryName, byteCode);
	}

	/**
	 * 
	 * @param className
	 *            - must be fully qualified class name.
	 * @param byteCode
	 * @return - if class loading failed, call System.exit(1).
	 */

	public Class<?> definedAndLoadClass(String className, byte[] byteCode) {
		String binaryName = toBinaryName(className);
		this.addByteCode(binaryName, byteCode);
		try {
			return this.loadClass(binaryName);
		} catch (Throwable e) {
			e.printStackTrace();
			System.err.println("class loading failed: " + binaryName);
			System.exit(1);
		}
		return null;
	}

	/**
	 * create child class loader.
	 * 
	 * @return
	 */
	public ScriptClassLoader createChild() {
		ScriptClassLoader loader = new ScriptClassLoader(this);
		return loader;
	}

	public static boolean enabledDump = false;
	private String dumpDirectory = null;

	private void dump(String binaryClassName, byte[] byteCode) {
		if (enabledDump || this.typeSystem.isVerboseMode() || dumpDirectory != null) {
			int index = binaryClassName.lastIndexOf('.');
			String classFileName = binaryClassName.substring(index + 1) + ".class";
			if (dumpDirectory != null) {
				classFileName = dumpDirectory + "/" + classFileName;
			}
			try (FileOutputStream stream = new FileOutputStream(classFileName)) {
				stream.write(byteCode);
				stream.close();
				ConsoleUtils.println("[Generated] " + classFileName);
				if (dumpDirectory == null) {
					new File(classFileName).deleteOnExit();
				}
				ProcessBuilder pb = new ProcessBuilder("javap", "-c", classFileName);
				pb.redirectOutput(Redirect.INHERIT);
				Process p = pb.start();
				p.waitFor();
				p.destroy();
			} catch (IOException e) {
				ConsoleUtils.println("cannot dump " + classFileName + " caused by " + e);
			} catch (InterruptedException e) {
				Verbose.traceException(e);
			}
		}
	}

	private final static String toBinaryName(String className) {
		return className.replace('/', '.');
	}
}
