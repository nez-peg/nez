package nez.ast;

import java.util.TreeMap;

import nez.NezOption;
import nez.util.FileBuilder;
import nez.util.StringUtils;

public class CommonTreeWriter extends FileBuilder {
	
	boolean sourceOption = false;
	
	public CommonTreeWriter() {
		super(null);
	}

	public CommonTreeWriter(String path) {
		super(path);
	}

	public CommonTreeWriter(NezOption option, String path) {
		super(path);
	}

	public CommonTreeWriter(String path, String dir, String ext) {
		this(StringUtils.toFileName(path, dir, ext));
	}

	public CommonTreeWriter(NezOption option, String path, String dir, String ext) {
		this(StringUtils.toFileName(path, dir, ext));
	}
		
	public final <T extends AbstractTree<T>> void writeTree(AbstractTree<T> node) {
		if(node == null) {
			this.writeIndent("null");
			return;
		}
		this.writeIndent("#" + node.getTag().toString() + "["); 
		if(node.size() == 0) {
			this.write(StringUtils.quoteString('\'', node.getText(), '\''));
			this.write("]");
		}
		else {
			this.incIndent();
			for(int i = 0; i < node.size(); i++) {
				this.writeTree(node.get(i));
			}
			this.decIndent();
			this.writeIndent("]"); 
		}
	}
		
	public final <T extends AbstractTree<T>> void writeXML(AbstractTree<T> node) {
		String tag = node.getTag().toString();
		this.writeIndent("<" + tag); 
		if(node.size() == 0) {
			String s = node.getText();
			if(s.equals("")) {
				this.write("/>");
			}
			else {
				this.write(">");
				this.write(node.getText());
				this.write("</" + tag + ">");
			}
		}
		else {
			for(int i = 0; i < node.size(); i++) {
				AbstractTree<T> sub = node.get(i);
				String stag = sub.getTag().toString();
				if(stag.startsWith("@")) {
					this.write(" ");
					this.write(stag.substring(1));
					this.write("=");
					this.write(StringUtils.quoteString('"', node.getText(), '"'));
				}
				this.writeXML(node.get(i));
			}
			this.write(">");
			this.incIndent();
			for(int i = 0; i < node.size(); i++) {
				AbstractTree<T> sub = node.get(i);
				String stag = sub.getTag().toString();
				if(!stag.startsWith("@")) {
					this.writeXML(node.get(i));
				}
			}
			this.decIndent();
			this.write("</" + tag + ">");
		}
	}
	
	public <T extends AbstractTree<T>> void writeTag(AbstractTree<T> po) {
		TreeMap<String,Integer> m = new TreeMap<String,Integer>();
		this.countTag(po, m);
		for(String k : m.keySet()) {
			this.write("#" + k + ":" + m.get(k));
		}
		this.writeNewLine();
	}

	private <T extends AbstractTree<T>> void countTag(AbstractTree<T> po, TreeMap<String,Integer> m) {
		for(int i = 0; i < po.size(); i++) {
			countTag(po.get(i), m);
		}
		String key = po.getTag().toString();
		Integer n = m.get(key);
		if(n == null) {
			m.put(key, 1);
		}
		else {
			m.put(key, n+1);
		}
	}

}
