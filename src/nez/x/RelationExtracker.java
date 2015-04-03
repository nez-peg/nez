package nez.x;

import java.util.AbstractList;
import java.util.Arrays;

import nez.ast.Source;
import nez.ast.SourcePosition;
import nez.ast.Tag;
import nez.main.Recorder;
import nez.util.FileBuilder;
import nez.util.StringUtils;
import nez.util.UList;
import nez.util.UMap;

public class RelationExtracker {
	Schema inferedSchema = null;
	long lastExtracted = 0;
	RNode[] buffer;
	Schema[] schemaBuffer;
	UList<String> workingValueList;
	long    index = 0;
	double thr = 0.75;
	WordCount keyCount = new WordCount();
	FileBuilder file;
	
	RelationExtracker(int w, double thr, String fileName) {
		this.workingValueList = new UList<String>(new String[64]);
		this.buffer = new RNode[w];
		this.schemaBuffer = new Schema[w];
		this.index = 0;
		this.thr = thr;
		this.file = new FileBuilder(fileName);
	}
	
//	public SyntaxTree newNode() {
//		return new RNode(this);
//	}

	void recieve(RNode t) {
		keyCount.count(t.getTag().getName());
		if(t.subNodes() < 3) {
			return; // Too small
		}
		if(extractRelation(t)) {
			return;
		}
		appendBuffer(t);
		//System.out.println("index="+index + ", last+len=" + (lastExtracted + buffer.length));
		if(index == lastExtracted + buffer.length) {
			inferSchema(buffer.length);
			lastExtracted = index;
		}
	}
	
	boolean extractRelation(RNode t) {
		if(inferedSchema != null) {
			Schema s = extractSchema(t);
			double sim = inferedSchema.sim(s);
			if(sim > thr) {
				this.inferedSchema.addRelation(t.getSourcePosition(), sim, s, this.workingValueList);
				lastExtracted = index;
				if(inferedSchema.count > 4096) {
					this.flushRelation();
				}
				return true;
			}
		}
		return false;
	}
	
	void appendBuffer(RNode t) {
		int n = (int)(index % buffer.length);
		RNode old = this.buffer[n];
		if(old != null) {
			old.subTree = null; // GC
		}
		this.buffer[n] = t;
		index++;	
	}
		
	void inferSchema(int bufsiz) {
		sort(bufsiz);
		Arrays.fill(this.schemaBuffer, null);
		for(int i = 0; i < bufsiz; i++) {
			Schema s = getSchemaInBuffer(i);
			s = findSchema(s, i+1, bufsiz);
			if(s != null) {
				detectedNewSchema(s, bufsiz);
				return;
			}
		}
	}
	
	public void cleanUp() {
		if(index < buffer.length) {
			inferSchema((int)index);
		}
		this.flushRelation();
	}
	
	void sort(int e) {
		for(int i = 0; i < e -1; i++) {
			if(this.buffer[i] == null) {
				e = i;
				continue;
			}
			for(int j = i+1; j < e; j++) {
				if(this.buffer[i].subNodes() < this.buffer[j].subNodes()) {
					RNode o = this.buffer[i];
					this.buffer[i] = this.buffer[j];
					this.buffer[j] = o;
				}
			}
		}
	}
	
	Schema getSchemaInBuffer(int index) {
		if(this.schemaBuffer[index] == null) {
			this.schemaBuffer[index] = extractSchema(this.buffer[index]);
		}
		return this.schemaBuffer[index];
	}

	Schema findSchema(Schema s1, int s, int e) {
		Schema[] candidates = new Schema[e - s + 1];
		candidates[0] = s1;
		int size = 1;
		for(int i = s; i < e; i++) {
			Schema s2 = getSchemaInBuffer(i);
			double sim = s1.sim(s2);
			if(sim > thr) {
				candidates[size] = s2;
				size++;
			}
			if(sim < 0.2) {
				break;
			}
		}
		//System.out.println("candidate: " + size + " " + s1);
		if(size < 4) {
			return null;
		}
		Schema view = candidates[0];
		boolean needsUnion = false;
		for(int i = 1; i < size; i++) {
			Schema v = candidates[i];
			//System.out.println("candidate: @" + i + " " + v);
			if(v.contains(view)) {
				view = v;
				continue;
			}
			needsUnion = true;
		}
		//System.out.println("enlarge: " + size + " " + view);
//		if(needsUnion) {
//			for(int i = 1; i < size; i++) {
//				Schema v = candidates[i];
//				if(view.contains(v)) {
//					continue;
//				}
//				view = view.union(v);
//			}
//		}
//		System.out.println("extracted: " + size + " " + view);
//		keyCount.dump();
		return view;
	}
	
	Schema extractSchema(RNode t) {
		Schema extra = new Schema();
		this.workingValueList.clear(0);
		assert(t != null);
		extra.extractImpl(t, this.workingValueList);
		return extra;
	}
	
	void detectedNewSchema(Schema s, int bufsiz) {
		this.flushRelation();
		this.inferedSchema = s;
		for(int i = 0; i < bufsiz; i++) {
			extractRelation(this.buffer[i]);
		}
	}

	void flushRelation() {
		if(this.inferedSchema != null) {
			System.out.println("Schema: " + this.inferedSchema);
			while(this.inferedSchema.firstData != null) {
				Object[] d = this.inferedSchema.formatEach();
				file.writeIndent(formatCSV(d));
				//System.out.println(formatCSV(d));
			}
			this.inferedSchema.count = 0;
			this.inferedSchema.lastData = null;
//			file.writeNewLine();
//			this.inferedSchema = null;
		}
	}
	
	public static String formatCSV(Object[] rel) {
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < rel.length; i++) {
			if(i > 0) {
				sb.append(",");
			}
			sb.append(formatCSVValue(rel[i]));
		}
		return sb.toString();
	}
	
	public static String formatCSVValue(Object v) {
		if(v == null) {
			return "";
		}
		if(v instanceof Integer) {
			return v.toString();
		}
		if(v instanceof Double) {
			return String.format("%f", v);
		}
		String s = v.toString();
		if(isNumber(s)) {
			return s;
		}
		return StringUtils.quoteString('"', s, '"');
	}

	public final static boolean isNumber(String v) {
		int s = 0;
		int dot = 0;
		if(v.length() > 0 && v.charAt(0) == '-') {
			s = 1;
		}
		for(int i = s; i < v.length(); i++) {
			char c = v.charAt(i);
			if(c == '.') {
				if(dot > 0) return false;
				dot++;
			}
			if(!Character.isDigit(c)) {
				return false;
			}
		}
		return true;
	}

	public void record(Recorder rec) {
		// TODO Auto-generated method stub
	}

}

class Schema {
	UMap<Integer> names;
	UList<String> nameList;
	RelationalData firstData;
	RelationalData lastData;
	int count = 0;
	Schema view = null;
	
	Schema() {
		names = new UMap<Integer>();
		nameList = new UList<String>(new String[4]);
		clearData();
	}

	void clearData() {
		firstData = null;
		lastData = null;
		count = 0;
	}
	
	int size() {
		return this.nameList.size();
	}
	
	boolean contains(String name) {
		return this.names.hasKey(name);
	}

	boolean contains(Schema r) {
		if(this.size() >= r.size()) {
			for(String n : r.nameList) {
				if(!this.contains(n)) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	
	@Override
	public String toString() {
		return this.nameList.toString();
	}
	

	double sim(Schema r) {
		int interSection = 0;
		for(String n : this.nameList) {
			if(r.contains(n)) {
				interSection += 1;
			}
		}
		return (double)interSection / (this.size() + r.size() - interSection);
	}
	
	Schema union(Schema s2) {
		int n1 = this.size(), n2 = s2.size();
		if(n1 < n2) {
			return s2.union(this); // the first one is larger
		}
		int m = n1 * n2;
		Schema u = new Schema();
		int i1=0,i2=0;
		for(int i = 0; i < m; i++) {
			/* The following is to merge two name lists in preserving
			 * partial orders ..
			 */
			if(i % n2 == 0) {
				u.add(this.nameList.ArrayValues[i1]);
				i1++;
			}
			if(i % n1 == 0) {
				u.add(s2.nameList.ArrayValues[i2]);
				i2++;
			}
		}
		System.out.println("union: " + u + "\n\t" + this + "\n\t" + s2);
		return u;
	}

	void extract(RNode t, UList<String> wlist) {
		this.extractImpl(t, wlist);
	}
	
	void extractImpl(RNode t, UList<String> wlist) {
		if(t.size() == 2) {
			if(t.get(0).size() == 0 && t.get(1).size() == 0) {
				add(t.get(0).getText(), t.get(1).getText(), wlist);
				return;
			}
		}
		if(t.size() == 0) {
			add(t.getTag().getName(), t.getText(), wlist);
		}
		for(RNode sub: t) {
			extractImpl(sub, wlist);
		}
	}
	
	private void add(String key, String value, UList<String> wlist) {
		Integer n = this.names.get(key);
		if(n != null) {
			n = n + 1;
			this.names.put(key, n);
			key = key+'#'+n;
			this.count++;
		}
		else {
			n = 1;
			this.names.put(key, n);
		}
		this.nameList.add(key);
		if(wlist != null) {
			wlist.add(value);
		}
	}

	void add(String key) {
		Integer n = this.names.get(key);
		if(n == null) {
			this.names.put(key, 1);
			this.nameList.add(key);
			this.count++;
		}
	}

	public void addRelation(long pos, double sim, Schema extracted, UList<String> workingValueList) {
		Schema view = this;
		Object[] rel = new Object[this.size()+2];
		rel[0] = pos;
		rel[1] = sim;
		int column = 2;
		for(String n : view.nameList) {
			int index = extracted.indexOf(n);
			if(index != -1) {
				rel[column] = workingValueList.ArrayValues[index];
			}
			column ++;
		}
		RelationalData d = new RelationalData(rel);
		if(firstData == null) {
			firstData = d;
			lastData = d;
		}
		else {
			lastData.next = d;
			lastData = d;
		}
		count++;
	}

	private int indexOf(String name) {
		for(int i = 0; i < this.nameList.size(); i++) {
			if(name.equals(this.nameList.ArrayValues[i])) {
				return i;
			}
		}
		return -1;
	}
	
	Object[] formatEach() {
		Object[] rel = firstData.columns;
		firstData = firstData.next;
		return rel;
	}

}

class RelationalData {
	Object[] columns;
	RelationalData next;
	RelationalData(Object[] columns) {
		this.columns = columns;
	}
}

class WordCount {
	UMap<Counter> map = new UMap<Counter>();
	int total;
	class Counter {
		int count = 0;
	}
	void count(String key) {
		Counter c = map.get(key);
		if(c == null) {
			c = new Counter();
			map.put(key, c);
		}
		c.count++;
		this.total++;
	}
	double ratio(String key) {
		Counter c = map.get(key);
		if(c == null) {
			return 0.0;
		}
		return (double)c.count / this.total;
	}
	boolean isNoise(String key) {
		if(map.size() > 0) {
			double r = ratio(key);
			return r > 2.0 / map.size();
		}
		return false;
	}
	void dump() {
		System.out.print("WordCount** ");
		for(String k: this.map.keys()) {
			System.out.print(String.format("%s[%f,%s],", k, ratio(k), isNoise(k)));
		}
		System.out.println();
	}
}

class RNode extends AbstractList<RNode> implements SourcePosition {
	RelationExtracker  tracker;
	private Source    source;
	private Tag       tag;
	private long      pos;
	private int       length;
	private Object    value  = null;
	RNode     subTree[] = null;
	private int subNodes = -1;
	
	public RNode(RelationExtracker tracker) {
		this.tracker    = tracker;
		this.tag        = Tag.tag("Text");
		this.source     = null;
		this.pos        = 0;
		this.length     = 0;
	}

	private RNode(RelationExtracker tracker, Tag tag, Source source, long pos, long epos, int size) {
		this.tracker = tracker;
		this.tag        = tag;
		this.source     = source;
		this.pos        = pos;
		this.length     = (int)(epos - pos);
		if(size > 0) {
			this.subTree = new RNode[size];
		}
	}

//	@Override
//	public SyntaxTree commit(Object value) {
//		this.value = value;
//		tracker.recieve(this);
//		return this;
//	}
//
//	@Override
//	public void abort() {
//	}

//	@Override
//	public SyntaxTree newNode(Tag tag, Source source, long spos, long epos, int size) {
//		return new RNode(this.tracker, tag == null ? this.tag : tag, source, spos, epos, size);
//	}	

	public void link(int index, RNode child) {
		this.set(index, child);
	}


	public Tag getTag() {
		return this.tag;
	}
//
//	@Override
//	public void setTag(Tag tag) {
//		this.tag = tag;
//	}
//
//	@Override
//	public void setEndingPosition(long pos) {
//		this.length = (int)(pos - this.getSourcePosition());
//	}
//
//	@Override
//	public final void expandAstToSize(int newSize) {
//		if(newSize > this.size()) {
//			this.resizeAst(newSize);
//		}
//	}
	
	public Source getSource() {
		return this.source;
	}

	public long getSourcePosition() {
		return this.pos;
	}

	@Override
	public final String formatSourceMessage(String type, String msg) {
		return this.source.formatPositionLine(type, this.getSourcePosition(), msg);
	}

	public final int subNodes() {
		if(this.subNodes == -1) {
			int c = this.size();
			for(RNode t: this) {
				c += t.subNodes();
			}
			this.subNodes = c;
		}
		return this.subNodes;
	}
	
	public int getLength() {
		return this.length;
	}
	
	public final boolean is(Tag t) {
		return this.tag == t;
	}
	
	
	
	public final boolean isEmptyToken() {
		return this.length == 0;
	}
	
	public final String getText() {
		if(this.value != null) {
			return this.value.toString();
		}
		if(this.source != null) {
			this.value = this.source.substring(this.getSourcePosition(), this.getSourcePosition() + this.getLength());
			return this.value.toString();
		}
		return "";
	}

	// subTree[]
	
	@Override
	public final int size() {
		if(this.subTree == null) {
			return 0;
		}
		return this.subTree.length;
	}

	@Override
	public final RNode get(int index) {
		return this.subTree[index];
	}

	public final RNode get(int index, RNode defaultValue) {
		if(index < this.size()) {
			return this.subTree[index];
		}
		return defaultValue;
	}

//	@Override
//	public final RNode set(int index, RNode node) {
//		RNode oldValue = null;
//		if(!(index < this.size())){
//			this.expandAstToSize(index+1);
//		}
//		oldValue = this.subTree[index];
//		this.subTree[index] = node;
//		return oldValue;
//	}

	private void resizeAst(int size) {
		if(this.subTree == null && size > 0) {
			this.subTree = new RNode[size];
		}
		else if(size == 0){
			this.subTree = null;
		}
		else if(this.subTree.length != size) {
			RNode[] newast = new RNode[size];
			if(size > this.subTree.length) {
				System.arraycopy(this.subTree, 0, newast, 0, this.subTree.length);
			}
			else {
				System.arraycopy(this.subTree, 0, newast, 0, size);
			}
			this.subTree = newast;
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		this.stringfy("", sb);
		return sb.toString();
	}

	final void stringfy(String indent, StringBuilder sb) {
		sb.append("\n");
		sb.append(indent);
		sb.append("(#");
		sb.append(this.tag.name);
		if(this.subTree == null) {
			sb.append(" ");
			StringUtils.formatQuoteString(sb, '\'', this.getText(), '\'');
			sb.append(")");
		}
		else {
			String nindent = "   " + indent;
			for(int i = 0; i < this.size(); i++) {
				if(this.subTree[i] == null) {
					sb.append("\n");
					sb.append(nindent);
					sb.append("null");
				}
				else {
					this.subTree[i].stringfy(nindent, sb);
				}
			}
			sb.append("\n");
			sb.append(indent);
			sb.append(")");
		}
	}
	
	public final String textAt(int index, String defaultValue) {
		if(index < this.size()) {
			return this.get(index).getText();
		}
		return defaultValue;
	}
	
}

