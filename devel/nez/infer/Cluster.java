package nez.infer;

import java.util.ArrayList;
import java.util.List;

public class Cluster {
	private static final double clusterTolerance = 0.01;
	private List<Token> tokenList;

	public Cluster() {
	}

	public Cluster(Token firstToken) {
		this.tokenList = new ArrayList<>();
		tokenList.add(firstToken);
	}

	public Token getToken(int index) {
		return this.tokenList.get(index);
	}

	public List<Token> getTokenList() {
		return this.tokenList;
	}

	public void addToken(Token t) {
		this.tokenList.add(t);
	}

}
