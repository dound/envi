#!/bin/sh

ssh \
-L 2501:openflow3.stanford.edu:2503 \
-L 2502:openflow4.stanford.edu:2503 \
-L 2503:openflow5.stanford.edu:2503 \
-L 2504:openflow5.stanford.edu:2504 \
-L 2505:openflow5.stanford.edu:2505 \
-L 2506:openflow5.stanford.edu:2505 \
yuba.stanford.edu
