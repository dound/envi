#!/bin/sh

#servers.add(new Triple("openflow8.stanford.edu",5432, "Slice: Plug-n-Serve"));
#servers.add(new Triple("openflow5.stanford.edu",2503,"Physical Network"));
#servers.add(new Triple("openflow5.stanford.edu",2507, "Slice: OpenPipes"));
#servers.add(new Triple("openflow3.stanford.edu",2503, "Slice: OpenRoads"));
#servers.add(new Triple("openflow5.stanford.edu",2506,"All Slices + Production"));
#servers.add(new Triple("openflow6.stanford.edu",2503, "Slice: Aggregation"));


ssh \
-L 2501:openflow5.stanford.edu:31340 \
-L 2502:openflow5.stanford.edu:31341 \
-L 2503:openflow5.stanford.edu:31342 \
-L 2504:openflow5.stanford.edu:31343 \
yuba.stanford.edu 'while `true` ; do echo I am the gec8 lavi tunnel `date` ; sleep 5 ; done '
#-L 2507:openflow6.stanford.edu:2503 \
#-L 2504:openflow4.stanford.edu:2503 \
