
for f in script/*.k; do 
	java -ea -jar /usr/local/lib/konoha.jar $f
	echo "TESTED $f $?"
done
