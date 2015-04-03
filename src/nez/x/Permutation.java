package nez.x;

public class Permutation {
	private int number, list_size;
	private int[] perm, target;
	private boolean[] flag;
	private int[][] perm_list;
	private int perm_list_index;


	public Permutation(int[] target) {
		this.target = target;
		this.number = target.length;
		this.list_size = this.fact(this.number);
		this.perm = new int[this.number];
		this.flag = new boolean[this.number + 1];
		this.perm_list = new int[this.list_size][this.number];
		this.perm_list_index = 0;
		this.createPermutation(0, this.target);
	}

	public int[][] getPermList() {
		return this.perm_list;
	}

	private int fact(int n) {
		return n == 0 ? 1 : n * fact(n - 1);
	}
	
	private void printPerm(){
		for (int[] x : perm_list) {
			for (int i : x) {
				System.out.print(i + " ");
			}
			System.out.println();
		}
	}

	public void createPermutation(int n, int[] target) {
		if (n == this.number) {
			for (int i = 0; i < n; i++) {
				perm_list[perm_list_index][i] = perm[i];
			}
			perm_list_index++;
		} else {
			for (int i = 0; i < perm.length; i++) {
				if (flag[i])
					continue;
				perm[n] = target[i];
				flag[i] = true;
				createPermutation(n + 1, target);
				flag[i] = false;
			}
		}
	}

	public static void main(String[] args) {
		int[] target = {2, 4, 6, 8};
		Permutation permutation = new Permutation(target);
		permutation.printPerm();
	}
}