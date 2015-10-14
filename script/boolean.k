
function TestTrue(b: boolean) {
	println("#b=true,b: %s", b);
	assert(b);
}

function TestFalse(b: boolean) {
	println("#b=false,b: %s", b);
	assert(!b);
}

function main() {
	println("#true: %s", true);
	println("#false: %s", false);	
	TestTrue(true);
	TestFalse(false);
}

main();

