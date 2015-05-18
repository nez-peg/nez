package nez.fsharp;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;

import nez.ast.CommonTree;
import nez.main.Command;
import nez.util.UMap;

public abstract class ParsingWriter {
	protected String fileName = null;
	protected PrintWriter out = null;
	public void writeTo(String fileName, CommonTree po) throws IOException {
		this.fileName = fileName;
		if(fileName != null) {
			this.out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(fileName),"UTF-8"));
			this.write(po);
			this.out.flush();
			this.out.close();
		}
		else {
			this.out = new PrintWriter(System.out);
			this.write(po);
			this.out.flush();
		}
		this.out = null;
	}
	protected abstract void write(CommonTree po);
	
	// data
	private final static UMap<Class<?>> extClassMap = new UMap<Class<?>>();
	public final static void registerExtension(String ext, Class<?> c) {
		if(ParsingWriter.class.isAssignableFrom(c));
		extClassMap.put(ext, c);
	}
	
	public final static ParsingWriter newInstance(String fileName, Class<?> defClass) {
		if(fileName != null) {
			int loc = fileName.lastIndexOf(".");
			if(loc != -1) {
				String ext = fileName.substring(loc+1);
				Class<?> c = extClassMap.get(ext);
				if(c != null) {
					defClass = c;
				}
			}
		}
		if(defClass != null) {
			try {
				return (ParsingWriter)defClass.newInstance();
			} catch (InstantiationException e) {
				//Command.reportException(e);
			} catch (IllegalAccessException e) {
				//Command.reportException(e);
			}
		}
		return null; // default 
	}

	public final static void writeAs( Class<?> defClass, String fileName, CommonTree po) {
		ParsingWriter w = newInstance(fileName, defClass);
		if(w != null) {
			try {
				w.writeTo(fileName, po);
			} catch (IOException e) {
				//Command.reportException(e);
			}
		}
	}

}
