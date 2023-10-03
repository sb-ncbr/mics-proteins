# sync jar to sequential scan stage
rsync -av /home/jakub/git/school/diplomka/proteins/out/artifacts/proteins_jar/proteins.jar protein-jo:/home/ubuntu/protein-search/08_rebuildSeqScanAlgs/proteins-1.0-jar-with-dependencies.jar

# sync rest of jars
# todo this can be accomplished by baking in the jars here
rsync -av jars/ protein-jo:/home/ubuntu/protein-search/05_PPP-codes/jars/

# sync protein jar to pppcodes
# this has to be done after so the jar is overwritten
rsync -av /home/jakub/git/school/diplomka/proteins/out/artifacts/proteins_jar/proteins.jar protein-jo:/home/ubuntu/protein-search/05_PPP-codes/jars/proteins-1.0.jar

# sync startup script
rsync -av pppcodes.sh protein-jo:/home/ubuntu/protein-search/05_PPP-codes/

# sync configuration files
rsync -av pppcodes.cf protein-jo:/home/ubuntu/protein-search/05_PPP-codes/
rsync -av manager-pppcodes.cf protein-jo:/home/ubuntu/protein-search/05_PPP-codes/

# sync ini file
# todo one source of truth
rsync -av protein_search.ini protein-jo:/home/ubuntu/protein-search/05_PPP-codes/