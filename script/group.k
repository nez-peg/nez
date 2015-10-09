
function main() {
	println("#(1): %d" , (1));
	println("#(1+2): %d" , (1+2));
	println("#(1+2*3): %d" , (1+2*3));
	println("#((1+2)*3): %d" , ((1+2)*3));
	println("#(1+(2)+3): %d" , (1+(2)+3));
	assert((1) == 1);
	assert((1+2) == 3);
	assert((1+2*3) == 7);
	assert(((1+2)*3) == 9);
	assert((1+(2)+3) == 6);
}

main()

	
