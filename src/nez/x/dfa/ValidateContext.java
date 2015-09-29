package nez.x.dfa;

public class ValidateContext {
	private StringBuilder vcontext;

	public ValidateContext() {
		this.vcontext = new StringBuilder();
	}

	public ValidateContext(StringBuilder vcontext) {
		this.vcontext = vcontext;
	}

	public ValidateContext(String vcontext) {
		this.vcontext = new StringBuilder(vcontext);
	}

	public StringBuilder getVcontext() {
		return this.vcontext;
	}

	public void setVcontext() {
		this.vcontext = vcontext;
	}

	public char charAt(int pos) {
		return vcontext.charAt(pos);
	}

	public int length() {
		return vcontext.length();
	}

	public void append(char ch) {
		vcontext.append(ch);
	}

	public void append(ValidateContext vc) {
		vcontext.append(vc.getVcontext());
	}

	public void append(String str) {
		vcontext.append(str);
	}

	@Override
	public String toString() {
		return this.vcontext.toString();
	}

	public String substring(int left, int right) {
		return this.vcontext.substring(left, right);
	}

	static public ValidateContext replaceAllNonTerminal(ValidateContext context, String before, String after) {
		ValidateContext newContext = new ValidateContext();
		int before_len = before.length();
		for (int i = 0; i < context.length(); i++) {
			char c = context.charAt(i);
			if (c == before.charAt(0) && i + before_len - 1 < context.length()) {

				String part = context.substring(i, i + before_len);

				if (before.equals(part)) {
					if (i - 1 >= 0 && Character.isDigit(context.charAt(i - 1))) {
						newContext.append(c);
						continue;
					}
					if (i + before_len < context.length() && Character.isDigit(context.charAt(i + before_len))) {
						newContext.append(c);
						continue;
					}
					newContext.append(after);
					i += (before_len - 1);
				} else {
					newContext.append(c);
				}
			} else {
				newContext.append(c);
			}
		}
		// System.out.println("after " + newContext);
		return newContext;
	}

	public void removeRedundantWords() {
		boolean update = true;
		while (update) {
			update = false;
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < vcontext.length(); i++) {
				char c = vcontext.charAt(i);
				int len = sb.length();
				if (len == 0) {
					sb.append(c);
					continue;
				}
				if (c == 'a') {
					if (sb.charAt(len - 1) != 'a') {
						sb.append(c);
					}
				} else if (c == '/') {
					if (len - 2 >= 0 && sb.charAt(len - 2) == '/' && sb.charAt(len - 1) == 'a') {

					} else {
						sb.append(c);
					}
				} else {
					sb.append(c);
				}
			}
			if (!vcontext.toString().equals(sb.toString())) {
				vcontext = sb;
				update = true;
			}
		}
	}

	public boolean eval(int nonTerminalID) {
		for (int i = 0; i < vcontext.length(); i++) {
			if (Character.isDigit(vcontext.charAt(i))) {
				int ID = 0;
				int L = i;
				while (i < vcontext.length() && vcontext.charAt(i) != '$') {
					ID *= 10;
					ID += (vcontext.charAt(i++) - '0');
				}
				int R = i;
				if (ID == nonTerminalID) {
					boolean hasLeft = hasLeftChar(L - 1);
					boolean hasRight = hasRightChar(R + 1);

					if (!hasLeft && !hasRight) {
						System.out.println("INVALID : A <- A");
						System.out.println(nonTerminalID + "-th : " + vcontext);
						return false;
					} else if (hasLeft && !hasRight) {
						// VALID
					} else if (!hasLeft && hasRight) {
						System.out.println("INVALID : A <- Aa");
						System.out.println(nonTerminalID + "-th : " + vcontext);
						return false;
					} else if (hasLeft && hasRight) {
						System.out.println("FOUND : A <- aAa");
						System.out.println(nonTerminalID + "-th : " + vcontext);
						return false;
					}
				}
			}
		}
		return true;
	}

	public boolean hasLeftChar(int pos) {
		return hasChar(pos, -1);
	}

	public boolean hasRightChar(int pos) {
		return hasChar(pos, +1);
	}

	private boolean hasChar(int start_pos, int dir) {
		int depth = 0;
		for (int i = start_pos; 0 <= i && i < vcontext.length(); i += dir) {
			char c = vcontext.charAt(i);
			if (depth == 0) {
				if (dir == -1 && c == '{') {
					return false;
				}
				if (dir == +1 && c == '}') {
					return false;
				}
				if (c == '/') {
					return false;
				}
			}
			if (c == '}') {
				++depth;
			}
			if (c == '{') {
				--depth;
			}
			if (depth == 0 && c == 'a') {
				return true;
			}
		}
		return false;
	}

}