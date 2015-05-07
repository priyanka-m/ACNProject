MAINFILEDIR=src/
n=0
while [$n -le 3]
do
java -classpath $MAINFILEDIR Node $n &
n=$((n+1))
done
