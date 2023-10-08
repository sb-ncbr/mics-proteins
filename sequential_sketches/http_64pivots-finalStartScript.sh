export JAVA_HOME='/usr/lib/jvm/jdk-1.8'
export PATH="/usr/lib/jvm/jdk-1.8/bin:$PATH"
cd '/home/jakub/git/school/diplomka/proteins/sequential_sketches'
mkdir -p './logs'
mkdir -p './pids'
echo -n 'App started at ' >> './logs/:.log'
date >> './logs/:.log'
java --add-opens java.base/java.lang.invoke=ALL-UNNAMED -cp "" -Xmx40g -Djava.net.preferIPv4Addresses=true -Djava.library.path=/usr/local/lib messif.appl.HttpApplication 20009  'http-api.cf' iniFile='/home/ubuntu/ProteinSearch/utils/protein_search.ini' pivotsLong='true' primDist='messif.distance.impl.ProteinDistanceDBImpl(/mnt/PDBe_clone_binary,0)' refinedist='messif.distance.impl.ProteinDistanceDBImpl(/mnt/PDBe_clone_binary,0.6)' rmiPort='20003' serialization='algs/sketchesOnly_512_sk1024b.bin' csvPairsForSketches='../data/csvPivotPairs/512/1024_pairs.csv' pivotCountForSketches='489' primObjName='proteinObj' skLength='1024' pcum='0.75' pivotsLong='false' primDist='messif.distance.impl.ProteinDistanceDBImpl(/mnt/PDBe_clone_binary,0)' refinedist='messif.distance.impl.ProteinDistanceDBImpl(/mnt/PDBe_clone_binary,0.6)' rmiPort='20009' serialization='algs/sketchesOnly_64_from_512_sk194b.bin' pivotCountForSketches='61' csvPairsForSketches='../data/csvPivotPairs/512/64_512/320_pairs.csv' primObjName='proteinObj' skLength='194' startHttpApi >> './logs/:.log' 2>&1 &
echo $! > './pids/:.pid'
sleep 0
