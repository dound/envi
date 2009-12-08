#!/bin/sh

#servers.add(new Triple("openflow8.stanford.edu",5432, "Slice: Plug-n-Serve"));
#servers.add(new Triple("openflow5.stanford.edu",2503,"Physical Network"));
#servers.add(new Triple("openflow5.stanford.edu",2507, "Slice: OpenPipes"));
#servers.add(new Triple("openflow3.stanford.edu",2503, "Slice: OpenRoads"));
#servers.add(new Triple("openflow5.stanford.edu",2506,"All Slices + Production"));
#servers.add(new Triple("openflow6.stanford.edu",2503, "Slice: Aggregation"));


ssh \
-L 2501:openflow8.stanford.edu:2503 \
-L 2502:openflow5.stanford.edu:2503 \
-L 2503:openflow5.stanford.edu:2507 \
-L 2504:openflow3.stanford.edu:2503 \
-L 2505:openflow5.stanford.edu:2506 \
-L 2506:openflow6.stanford.edu:2503 \
yuba.stanford.edu 'while `true` ; do echo I am the flowvisor tunnel `date` ; sleep 20 ; done '
#-L 2507:openflow6.stanford.edu:2503 \
#-L 2504:openflow4.stanford.edu:2503 \
