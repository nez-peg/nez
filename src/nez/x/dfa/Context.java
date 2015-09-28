package nez.x.dfa;

import nez.io.StringContext;

public class Context {
	public StringContext sc;
	private int top;

	public Context(String context) {
		sc = new StringContext(context);
		top = 0;
	}

	public Context() {

	}

	public void setTop(int top) {
		this.top = top;
	}

	public boolean isEmpty() {
		return top == sc.length();
	}

	public char getChar() {
		return (char) sc.byteAt(top);
	}

	public int getTop() {
		return this.top;
	}

	public Context getContext() {
		Context context = new Context();
		context.sc = this.sc;
		context.setTop(this.top);
		return context;
	}

	void incTop() {
		if (top < sc.length()) {
			++top;
		}
	}
}
