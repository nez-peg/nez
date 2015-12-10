package nez.ast;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nez.parser.ParserStrategy;
import nez.util.StringUtils;

public class LiteralConstructor extends TreeVisitor implements Transducer {

	protected ParserStrategy strategy;

	public LiteralConstructor() {
		this(null);
	}

	public LiteralConstructor(ParserStrategy strategy) {
		this.strategy = ParserStrategy.nullCheck(strategy);
	}

	@Override
	public Object newInstance(Tree<?> node) {
		return visit("new", node);
	}

	@Override
	protected Object visitUndefinedNode(Tree<?> node) {
		if (node.size() == 0) {
			return newText(node);
		}
		if (node.isAllLabeled()) {
			return newMap(node);
		}
		return newList(node);
	}

	public Boolean newBoolean(Tree<?> node) {
		try {
			if (node.toText().equals("true")) {
				return true;
			}
			return false;
		} catch (LiteralFormatException e) {
			throw e;
		} catch (RuntimeException e) {
			return report("unknown boolean literal: " + node.toText(), node, false);
		}
	}

	public Integer newInteger(Tree<?> node) {
		try {
			return Integer.parseInt(node.toText());
		} catch (LiteralFormatException e) {
			throw e;
		} catch (RuntimeException e) {
			return report(e, node, 0);
		}
	}

	public Long newLong(Tree<?> node) {
		try {
			return Long.parseLong(node.toText());
		} catch (LiteralFormatException e) {
			throw e;
		} catch (RuntimeException e) {
			return report(e, node, 0L);
		}
	}

	public BigInteger newBigInteger(Tree<?> node) {
		try {
			return new BigInteger(node.toText());
		} catch (LiteralFormatException e) {
			throw e;
		} catch (RuntimeException e) {
			return report(e, node, BigInteger.ZERO);
		}
	}

	public Float newFloat(Tree<?> node) {
		try {
			return Float.parseFloat(node.toText());
		} catch (LiteralFormatException e) {
			throw e;
		} catch (RuntimeException e) {
			return report(e, node, 0.0f);
		}
	}

	public Double newDouble(Tree<?> node) {
		try {
			return Double.parseDouble(node.toText());
		} catch (LiteralFormatException e) {
			throw e;
		} catch (RuntimeException e) {
			return report(e, node, 0.0);
		}
	}

	public String newString(Tree<?> node) {
		try {
			return StringUtils.unquoteString(node.toText());
		} catch (LiteralFormatException e) {
			throw e;
		} catch (RuntimeException e) {
			return report(e, node, "");
		}
	}

	public String newText(Tree<?> node) {
		return node.toText();
	}

	protected List<Object> newList(int n) {
		return new ArrayList<Object>(n);
	}

	public List<Object> newList(Tree<?> node) {
		try {
			List<Object> l = newList(node.size());
			for (Tree<?> sub : node) {
				l.add(newInstance(sub));
			}
			return l;
		} catch (LiteralFormatException e) {
			throw e;
		} catch (RuntimeException e) {
			return report(e, node, newList(0));
		}
	}

	protected Map<String, Object> newMap() {
		return new HashMap<String, Object>();
	}

	public Map<String, Object> newMap(Tree<?> node) {
		Map<String, Object> m = newMap();
		for (int i = 0; i < node.size(); i++) {
			Symbol label = node.getLabel(i);
			if (label != null) {
				m.put(label.getSymbol(), newInstance(node.get(i)));
			}
		}
		return m;
	}

	// alias

	public Integer newInt(Tree<?> node) {
		return newInteger(node);
	}

	// report

	private <T> T report(RuntimeException e, Tree<?> node, T initValue) {
		if (strategy == null) {
			throw new LiteralFormatException(node.formatSourceMessage("error", e.getMessage()));
		}
		strategy.reportError(node, e.getMessage());
		return initValue;
	}

	private <T> T report(String msg, Tree<?> node, T initValue) {
		if (strategy == null) {
			throw new LiteralFormatException(node.formatSourceMessage("error", msg));
		}
		strategy.reportError(node, msg);
		return initValue;
	}

}

class LiteralFormatException extends RuntimeException {

	public LiteralFormatException(String msg) {
		super(msg);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 9141813132121528343L;

}