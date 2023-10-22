export JAVA_HOME=' /usr/lib/jvm/java-openjdk/'
export PATH=" /usr/lib/jvm/java-openjdk//bin:$PATH"
cd '/home/jakub/git/school/diplomka/proteins'
mkdir -p '/home/jakub/git/school/diplomka/proteins/logs'
mkdir -p '/home/jakub/git/school/diplomka/proteins/pids'
echo -n 'App started at ' >> '/home/jakub/git/school/diplomka/proteins/logs/localhost:20512.log'
date >> '/home/jakub/git/school/diplomka/proteins/logs/localhost:20512.log'
java --add-opens java.base/java.lang.invoke=ALL-UNNAMED -cp "jars:jars/json-20080701.jar:jars/mapdb-1.0.10-DAVID.jar:jars/messif-3.0.0-DEVEL.jar:jars/mindex-3.0.0-DEVEL.jar:jars/mysql-connector-java-8.0.30.jar:jars/ppp-codes-3.0.0-DEVEL.jar:jars/proteins-1.0.jar:jars/protobuf-java-3.19.4.jar:jars/SF_v3-3.0.jar:jars/simcloud-1.0.0-DEVEL.jar:jars/simcloud-mapdb-1.0.0-DEVEL.jar:jars/TmpMessif3Tests-1.0.jar:jars/trove4j-3.0.3.jar" -Xmx80g -XX:+UseG1GC -XX:TargetSurvivorRatio=1 -XX:NewSize=800m -Djgroups.tcp.address=localhost -Djgroups.tcp.port= -Djgroups.mping.mcast_port= -Djgroups.bind_addr=localhost -Djava.library.path=jars messif.appl.HttpApplication 21512 -httpThreads 50 -httpBacklog 100 20512 'manager-pppcodes.cf' rmiPort='21512' iniFile='/home/ubuntu/ProteinSearch/utils/protein_search.ini' primObjName='proteinObj' primDist='messif.distance.impl.ProteinDistanceDBImpl(/mnt/PDBe_clone_binary,0)' pivots='512' minlevel='1' capacity='0' prefix='6' indexes='4' locatorlength='0' actionsInTheEnd='statisticsDisable garbage' useStats='' visualfield='proteinObj' refinedist='messif.distance.impl.ProteinDistanceDBImpl(/mnt/PDBe_clone_binary,0.6)' serialization='algs/pppcodes.bin' mapdbfile='algs/mapdb/filestore' buildPPPcodes >> '/home/jakub/git/school/diplomka/proteins/logs/localhost:20512.log' 2>&1 &
echo $! > '/home/jakub/git/school/diplomka/proteins/pids/localhost:20512.pid'
sleep 0
