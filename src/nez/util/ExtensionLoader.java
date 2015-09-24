package nez.util;

import nez.main.Verbose;

public class ExtensionLoader {
	public final static Object newInstance(String loadPoint, String ext) {
		try {
			Class<?> c = Class.forName(loadPoint + ext);
			return c.newInstance();
		} catch (ClassNotFoundException e) {

		} catch (InstantiationException e) {
			Verbose.traceException(e);
		} catch (IllegalAccessException e) {
			Verbose.traceException(e);
		}
		return null;
	}
}
