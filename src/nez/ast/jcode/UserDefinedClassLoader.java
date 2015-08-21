package nez.ast.jcode;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * used for user defined class loading. not thread safe.
 * 
 * @author skgchxngsxyz-osx
 *
 */
public class UserDefinedClassLoader extends ClassLoader {
	/**
	 * if true, dump byte code.
	 */
	private boolean enableDump = false;

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
	public UserDefinedClassLoader(String packageName) {
		super();
		this.allowedPackageName = toBinaryName(packageName);
		this.byteCodeMap = new HashMap<>();
	}

	public UserDefinedClassLoader() {
		super();
		this.allowedPackageName = null;
		this.byteCodeMap = new HashMap<>();
	}

	/**
	 * used for child class loader creation.
	 * 
	 * @param classLoader
	 */
	protected UserDefinedClassLoader(UserDefinedClassLoader classLoader) {
		super(classLoader);
		this.allowedPackageName = classLoader.allowedPackageName;
		this.byteCodeMap = new HashMap<>();
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		byte[] byteCode = this.byteCodeMap.remove(name);
		if(byteCode == null) {
			throw new ClassNotFoundException("not found class: " + name);
		}
		return this.defineClass(name, byteCode, 0, byteCode.length);
	}

	@Override
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		if(this.allowedPackageName == null) { // allow all package name
			return super.loadClass(name, resolve);
		}

		Class<?> foundClass = this.findLoadedClass(name);
		if(foundClass == null) {
			ClassLoader parent = this.getParent();
			if((parent instanceof UserDefinedClassLoader) || !name.startsWith(this.allowedPackageName)) {
				try {
					foundClass = parent.loadClass(name);
				} catch (ClassNotFoundException e) {
				}
			}
		}
		if(foundClass == null) {
			foundClass = this.findClass(name);
		}
		if(resolve) {
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
		if(this.byteCodeMap.put(binaryName, byteCode) != null) {
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
	public UserDefinedClassLoader createChild() {
		UserDefinedClassLoader loader = new UserDefinedClassLoader(this);
		loader.setDump(this.enableDump);
		return loader;
	}

	/**
	 * for debug purpose.
	 */
	private void dump(String binaryClassName, byte[] byteCode) {
		if(!this.enableDump) {
			return;
		}
		int index = binaryClassName.lastIndexOf('.');
		String classFileName = binaryClassName.substring(index + 1) + ".class";
		System.err.println("@@@@ Dump ByteCode: " + classFileName + " @@@@");
		try(FileOutputStream stream = new FileOutputStream(classFileName)) {
			stream.write(byteCode);
			stream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void setDump(boolean enableByteCodeDump) {
		enableDump = enableByteCodeDump;
	}

	private final static String toBinaryName(String className) {
		return className.replace('/', '.');
	}
}
