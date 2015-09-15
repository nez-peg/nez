
java -jar nez.jar compile -p unit_test/resources/issue0046.nez 

if [ -f issue0046.moz ]; then
	rm issue0046.moz
else
	exit 1
fi

