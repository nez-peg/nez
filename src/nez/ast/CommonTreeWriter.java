package nez.ast;

import java.util.TreeMap;

import nez.util.FileBuilder;
import nez.util.StringUtils;

public class CommonTreeWriter {
	public void transform(String path, CommonTree node) {
		FileBuilder fb = new FileBuilder(path);
		this.writeCommonTree(fb, node);
		fb.writeNewLine();
		fb.flush();
	}
	private void writeCommonTree(FileBuilder fb, CommonTree node) {
		if(node == null) {
			fb.writeIndent("null");
			return;
		}
		fb.writeIndent("(#" + node.getTag().toString()); 
		if(node.size() == 0) {
			fb.write(" "); 
			fb.write(StringUtils.quoteString('\'', node.getText(), '\''));
			fb.write(")");
		}
		else {
			fb.incIndent();
			for(int i = 0; i < node.size(); i++) {
				this.writeCommonTree(fb, node.get(i));
			}
			fb.decIndent();
			fb.writeIndent(")"); 
		}
	}
	
	public void writeTag(FileBuilder fb, CommonTree po) {
		TreeMap<String,Integer> m = new TreeMap<String,Integer>();
		this.tagCount(po, m);
		for(String k : m.keySet()) {
			fb.write("#" + k + ":" + m.get(k));
		}
		fb.writeNewLine();
	}

	private void tagCount(CommonTree po, TreeMap<String,Integer> m) {
		for(int i = 0; i < po.size(); i++) {
			tagCount(po.get(i), m);
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
