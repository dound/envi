# this simple Makefile delegates all the work to ant

default: jar

%:
	@if ! [ "$@" = "default" ]; then ant $@; fi
