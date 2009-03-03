# Makefile for the OpenFlow GUI backend

# base folder into which openflow and nox should be installed
INSTALL_DIR = $(HOME)

# OF and NOX installation paths
OF_DIR = $(INSTALL_DIR)/openflow
NOX_DIR = $(INSTALL_DIR)/noxcore
COMPON_DIR = $(NOX_DIR)/src/nox/netapps/of_gui_backend

.PHONY: get-openflow update-openflow get-nox update-nox update install

# shorthand to update to the latest NOX and OpenFlow
update: update-nox

# gets the latest OpenFlow distribution
get-openflow:
	@if [ ! -d $(OF_DIR) ]; then \
	    git clone yuba.stanford.edu:/usr/local/git/openflow $(OF_DIR); \
	fi

# updates to  the latest OpenFlow distribution
update-openflow: get-openflow
	@cd $(OF_DIR) && git pull --rebase

# gets the latest NOX distribution
get-nox:
	@if [ ! -d $(NOX_DIR) ]; then \
	    git clone git://noxrepo.org/noxcore $(NOX_DIR) && cd $(NOX_DIR) && ./boot.sh && ./configure --with-python=yes && make; \
	fi

# updates to the latest NOX distribution
update-nox: get-nox update-openflow
	cd $(NOX_DIR) && git pull --rebase && make

# installs the latest code for the component into the local nox installation
install:
	@if [ ! -d $(NOX_DIR) ]; then \
	    make update-nox; \
	fi
	rm -rf $(COMPON_DIR)
	mkdir $(COMPON_DIR)
	cp *.py ../protocol/* $(COMPON_DIR)/
