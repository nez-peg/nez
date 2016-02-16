package nez.dfa;

import java.util.TreeMap;

//Reference : http://www-erato.ist.hokudai.ac.jp/html/php/seminar5_docs/minato_alg2010-5.pdf
//変数の展開は論理変数のIDを昇順に行う
//!,&,| のみ対応 ( 論理記号の追加は容易だがAFAにおいてこれ以上必要ない )
public class BDD {
	public static int MAX_TABLE_SIZE = 10000000;
	private static int topOfNodeTable;
	public static BDDNode[] nodeTable = null; // 0番地は0定数節点,1番地は1定数節点
	public static TreeMap<BDDNode, Integer> nodeTableCache;
	public static TreeMap<BinaryOperatorMemoState, Integer> binaryOperatorCache;

	public int addressOfNodeTable; // BooleanExpression の nodeTable 内のアドレス

	public BDD() {
		if (nodeTable == null) {
			nodeTable = new BDDNode[MAX_TABLE_SIZE];
			nodeTable[0] = new BDDNode(-1, -1, -1);
			nodeTable[1] = new BDDNode(-1, -1, -1);
			topOfNodeTable = 2;
			nodeTableCache = new TreeMap<BDDNode, Integer>();
			binaryOperatorCache = new TreeMap<BinaryOperatorMemoState, Integer>();
		}
	}

	public BDD(BooleanExpression be) {
		this();
		addressOfNodeTable = build(be);
	}

	public int build(BooleanExpression be) {
		return be.traverse();
	}

	public static int getNode(BDDNode bn) {
		if (bn.zeroID == bn.oneID) {
			return bn.zeroID;
		}

		if (nodeTableCache.containsKey(bn.deepCopy())) {
			return nodeTableCache.get(bn.deepCopy());
		}
		if (topOfNodeTable >= MAX_TABLE_SIZE) {
			System.out.println("FATAL ERROR : NODE TABLE => OVERFLOW : size : " + topOfNodeTable + " capacity : " + MAX_TABLE_SIZE);
		}

		nodeTable[topOfNodeTable] = bn.deepCopy();
		nodeTableCache.put(bn.deepCopy(), topOfNodeTable);
		return topOfNodeTable++;
	}

	public static boolean isConst(int address) {
		return address == 0 || address == 1;
	}

	public static int apply(char op, int F, int G) {
		// 1. F,Gのいずれかが定数のとき、およびF=Gのとき
		if (isConst(F) || isConst(G) || F == G) {
			if (F == G) {
				return F;
			}
			if (op == '|') {
				if (isConst(F) && isConst(G))
					return F | G;
				if (isConst(F)) {
					int tmp = F;
					F = G;
					G = tmp;
				}
				if (G == 0) {
					return F;
				} else {
					return 1;
				}
			} else if (op == '&') {
				if (isConst(F) && isConst(G))
					return F & G;
				if (isConst(F)) {
					int tmp = F;
					F = G;
					G = tmp;
				}
				if (G == 0) {
					return 0;
				} else {
					return F;
				}
			} else {
				System.out.println("INVALID OPERATOR : WHAT IS " + op + "?");
			}
		}

		BinaryOperatorMemoState boms = new BinaryOperatorMemoState(op, F, G);
		if (binaryOperatorCache.containsKey(boms)) {
			return binaryOperatorCache.get(boms);
		}

		// 2.両者の最上位変数F.vとG.vが同じとき
		BDDNode bnF = nodeTable[F].deepCopy(); // deepCopyでないと手順４を実行した場合にnodeTableの値が書きかわるので注意
		BDDNode bnG = nodeTable[G].deepCopy();
		if (bnF.variableID == bnG.variableID) {
			int H0 = apply(op, bnF.zeroID, bnG.zeroID);
			int H1 = apply(op, bnF.oneID, bnG.oneID);
			if (H0 == H1) {
				binaryOperatorCache.put(boms, H0);
				return H0;
			}
			int address = getNode(new BDDNode(bnF.variableID, H0, H1));
			binaryOperatorCache.put(boms, address);
			return address;
		}
		// 変数の番号が大きいほど下位であることに注意
		// 4.F.vがG.vよりも下位のとき
		// FとGを入れ替えて、3.と同様に処理
		if (bnF.variableID > bnG.variableID) {
			int tmp = F;
			F = G;
			G = tmp;
			bnF = nodeTable[F].deepCopy();
			bnG = nodeTable[G].deepCopy();
		}

		// 3.F.vがG.vよりも上位のとき
		int H0 = apply(op, bnF.zeroID, G);
		int H1 = apply(op, bnF.oneID, G);
		if (H0 == H1) {
			binaryOperatorCache.put(boms, H0);
			return H0;
		}
		int address = getNode(new BDDNode(bnF.variableID, H0, H1));
		binaryOperatorCache.put(boms, address);
		return address;
	}

	public static void printNodeTable() {
		System.out.println("table size = " + topOfNodeTable);
		for (int i = 0; i < topOfNodeTable; i++) {
			BDDNode bn = nodeTable[i];
			System.out.println(i + "-th : " + bn);
		}
	}

	public boolean equals(BDD bdd) {
		// System.out.println("bdd1 = " + this.addressOfNodeTable);
		// System.out.println("bdd2 = " + bdd.addressOfNodeTable);
		return this.addressOfNodeTable == bdd.addressOfNodeTable;
	}

	public static int getNodeTableSize() {
		return topOfNodeTable;
	}

}