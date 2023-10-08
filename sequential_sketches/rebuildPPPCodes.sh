#!/bin/bash

java --add-opens java.base/java.lang.invoke=ALL-UNNAMED -jar proteins-1.0-jar-with-dependencies.jar \
  algs/sketchesOnly_64_from_512_sk194b.bin \
  algs/sketchesOnly_512_sk1024b.bin \
  /home/ubuntu/protein-search/ProteinSearch/utils/protein_search.ini # todo undo hardcoding
