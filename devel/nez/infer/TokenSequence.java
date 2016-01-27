package nez.infer;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class TokenSequence {
	protected Map<String, Token> tokenMap;
	private int tokenCount = 0;
	private int maxTokenCount = 0;

	public TokenSequence() {
		this.tokenMap = new HashMap<String, Token>();
	}

	public Map<String, Token> getTokenMap() {
		return tokenMap;
	}

	public int getMaxTokenSize() {
		return this.maxTokenCount;
	}

	// public final void transaction(String label) {
	// if (!tokenMap.containsKey(label)) {
	// Token token = new Token(label, this.totalNumOfChunks);
	// token.getHistogram().update(tokenCount++);
	// tokenMap.put(label, token);
	// } else {
	// tokenMap.get(label).getHistogram().update(tokenCount++);
	// }
	// }

	public final void transaction(String label, Token token) {
		if (!tokenMap.containsKey(label)) {
			token.getHistogram().update(tokenCount++);
			tokenMap.put(label, token);
		} else {
			tokenMap.get(label).getHistogram().update(tokenCount++);
		}
	}

	public final Token[] getTokenList() {
		Token[] tokenList = new Token[this.tokenMap.size()];
		int index = 0;
		for (Entry<String, Token> entry : this.tokenMap.entrySet()) {
			tokenList[index++] = entry.getValue();
		}
		normalizeAllHistograms(tokenList);
		return tokenList;
	}

	public final void commitAllHistograms() {
		for (Entry<String, Token> token : this.tokenMap.entrySet()) {
			token.getValue().getHistogram().commit();
		}
		this.maxTokenCount = this.maxTokenCount < tokenCount ? tokenCount : maxTokenCount;
		tokenCount = 0;
	}

	private final void normalizeAllHistograms(Token[] tokenList) {
		for (Token token : tokenList) {
			token.getHistogram().normalize();
		}
	}

}
