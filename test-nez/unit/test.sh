TEST_DIR="."
  for test in `ls ${TEST_DIR}/pass*.in` ;do
    path=${test%\.*}
    name=${path##*/}
    echo "[${name} output]"
    java -jar ../../nez.jar parse -g ${name}.nez -i ${test}
    if [ $? -eq 0 ]; then
      echo "[${name} passed]"
    else
      echo "[${name} failed]"
      echo "<nez>\n`cat ${path}.nez`"
      echo "<input>\n`cat ${test}`"
    fi
  done
