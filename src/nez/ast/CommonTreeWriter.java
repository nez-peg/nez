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
			this.write(StringUtils.quoteString('\'', node.toText(), '\''));
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
		if(node.size() == 2 && node.getTag() == Tag.MetaTag) {
			writeXML(node.get(0).toText(), node.get(1));
		}
		else {
			String tag = node.getTag().toString();
			writeXML(tag, node);
		}
	}

	public final <T extends AbstractTree<T>> void writeXML(String tag, AbstractTree<T> node) {
		this.writeIndent("<" + tag); 
		if(node.size() == 0) {
			String s = node.toText();
			if(s.equals("")) {
				this.write("/>");
			}
			else {
				this.write(">");
				this.write(node.toText());
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
					this.write(StringUtils.quoteString('"', sub.toText(), '"'));
				}
			}
			this.write(">");
			this.incIndent();
			for(int i = 0; i < node.size(); i++) {
				AbstractTree<T> sub = node.get(i);
				String stag = sub.getTag().toString();
				if(!stag.startsWith("@")) {
					this.writeXML(sub);
				}
			}
			this.decIndent();
			this.writeIndent("</" + tag + ">");
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
