// written by kimio kuramitsu

boolean TestFunc0() {
	int i = 0;
	int c = 0;
	for(i = 0; i < 10; i = i + 1) {
		if(i > 5) {
			break;
		}
		c = c + 1;
	}
	assert(c == 6);
	return true;
}

boolean TestFunc1() {
	int i = 0;
	int c = 0;
	for(i = 0; i < 10; i = i + 1) {
		if(i % 2 == 0) {
			continue;
		}
		c = c + 1;
	}
	assert(c == 5);
	return true;
}

boolean TestFunc2() {
	int i = 0;
	for(i = 0; false; i = i + 1) {
	}
	assert(i == 0);
	return true;
}

boolean TestFunc3() {
	int i = 0;
	int c = 0;
	for(i = 0; i < 10; i = i + 1) {	
		if(i > 5) break;
		c = c + 1;
	}
	assert(c == 6);
	return true;
}

boolean TestFunc4() {
	int i = 0;
	int c = 0;
	for(i = 0; i < 10; i = i + 1) {
		if(i % 2 == 0) continue;
		c = c + 1;
	}
	assert(c == 5);
	return true;
}

boolean TestFunc5() {
	int i = 0;
	int c = 0;
	for(i = 0; i < 10; i = i + 1) {
		int j = 0;
		for(j = 0; j < 10; j = j + 1) {
			c = c + 1;
		}
	}
	assert(c == 100);
	return true;
}

boolean TestFunc6() {
	int i = 0;
	for(i = 0; i < 10; i = i + 1);
	assert(i == 10);
	return true;
}

boolean TestFunc7() {
	int c = 0;
	for(int i = 0; i < 10; i = i + 1) {
		if(i > 5) {
			break;
		}
		c = c + 1;
	}
	assert(c == 6);
	return true;
}

boolean TestFunc8() {
	int c = 0;
	int i = 0;
	for(i = 1; i < 10; i = i + 1) {
		if(i > 5) {
			break;
		}
		c = c + 1;
	}
	assert(c == 6);
	return true;
}



assert(TestFunc0());
assert(TestFunc1());
assert(TestFunc2());
assert(TestFunc3());
assert(TestFunc4());
assert(TestFunc5());
assert(TestFunc6());
assert(TestFunc7());
assert(TestFunc8());

