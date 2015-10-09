
for f in *k; do 
	echo $f
	java -ea -jar /usr/local/lib/nez.jar konoha $f
done
