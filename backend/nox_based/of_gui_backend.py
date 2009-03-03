# OF_GUI_Backend - a backend for OF GUI
# David Underhill

from nox.lib.core import Component
from nox.lib.packet.packet_utils import longlong_to_octstr
from time import time

class OF_GUI_Backend(Component):
    def __init__(self, ctxt):
        Component.__init__(self, ctxt)
        self.switches = {}              # maps DPIDs to switch description (or None if not present)
        self.numSwitches = 0            # current number of switches in the network

    def getInterface(self):
        return str(OF_GUI_Backend)

    def install(self):
        # have nox tell us which switches are in our network
        self.register_for_datapath_join( lambda dpid, stats : self.datapath_join_callback(dpid,stats))
        self.register_for_datapath_leave(lambda dpid :        self.datapath_leave_callback(dpid))

        # tell nox we want to handle desc stats replies
        self.register_for_desc_stats_in( lambda dpid, desc :  self.desc_stats_in_handler(dpid,desc))

    # handle the addition of a switch to the network
    def datapath_join_callback(self, dpid, stats):
        print '##SWITCH_JOIN %s' % longlong_to_octstr(dpid)[6:]
        self.ctxt.send_desc_stats_request(dpid)  # figure out info about this switch

    # handle the removal of a switch from the network
    def datapath_leave_callback(self, dpid):
        print '##SWITCH_LEAVE %s' % longlong_to_octstr(dpid)[6:]
        if self.switches.has_key(dpid):
            del self.switches[dpid]
            self.setNumSwitches -= 1

    # handle switch description reply
    def desc_stats_in_handler(self, dpid, desc):
        print '##SWITCH_DESC %s %s' % (longlong_to_octstr(dpid)[6:], desc_to_str(desc))
        self.switches[dpid] = desc
        self.numSwitches += 1

def desc_to_str(desc):
    return '%s %s %s %s' % (desc['hw_desc'].replace(' ', '_'),
                            desc['sw_desc'].replace(' ', '_'),
                            desc['serial_num'].replace(' ', '_'),
                            desc['mfr_desc'].replace(' ', '_'))

def getFactory():
    class Factory:
        def instance(self, ctxt):
            return OF_GUI_Backend(ctxt)
    return Factory()
