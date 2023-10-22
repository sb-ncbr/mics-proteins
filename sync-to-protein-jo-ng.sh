#!/bin/bash

HOST=protein-jo
#REMOTE_FOLDER=/home/ubuntu/protein-search
REMOTE_FOLDER=/home/ubuntu/protein_search_ng

# sync rest of jars
# todo this can be accomplished by baking in the jars here
rsync --mkpath -av jars/ "$HOST":$REMOTE_FOLDER/messiff_stage/jars/

# sync protein jar to pppcodes
# this has to be done after all jats so the jar is overwritten to the newest version
rsync --mkpath -av /home/jakub/git/school/diplomka/proteins/out/artifacts/proteins_jar/proteins.jar "$HOST":$REMOTE_FOLDER/messiff_stage/jars/proteins-1.0.jar

# sync startup script
rsync --mkpath -av pppcodes.sh "$HOST":$REMOTE_FOLDER/messiff_stage/pppcodes/

# sync configuration files
rsync --mkpath -av pppcodes.cf "$HOST":$REMOTE_FOLDER/messiff_stage/pppcodes/
rsync --mkpath -av manager-pppcodes.cf "$HOST":$REMOTE_FOLDER/messiff_stage/pppcodes/

# sync ini file
# todo one source of truth
rsync --mkpath -av protein_search.ini "$HOST":$REMOTE_FOLDER/messiff_stage/pppcodes/

### Sequential scan stage

# rebuild runner
rsync --mkpath -av sequential_sketches/rebuildPPPCodes.sh      "$HOST":$REMOTE_FOLDER/messiff_stage/seqscan/

# http dependencies
# todo together with new destination

# http runners
rsync --mkpath -av sequential_sketches/http-api.cf             "$HOST":$REMOTE_FOLDER/messiff_stage/seqscan/
rsync --mkpath -av sequential_sketches/http_64pivots.defaults  "$HOST":$REMOTE_FOLDER/messiff_stage/seqscan/
rsync --mkpath -av sequential_sketches/http_64pivots.sh        "$HOST":$REMOTE_FOLDER/messiff_stage/seqscan/
rsync --mkpath -av sequential_sketches/http_512pivots.defaults "$HOST":$REMOTE_FOLDER/messiff_stage/seqscan/
rsync --mkpath -av sequential_sketches/http_512pivots.sh       "$HOST":$REMOTE_FOLDER/messiff_stage/seqscan/