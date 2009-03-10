"""Defines the LAVI protocol and some associated helper functions."""

import array
import struct

from twisted.internet import reactor

from ltprotocol.ltprotocol import LTMessage, LTProtocol

LAVI_DEFAULT_PORT = 2503

def array_to_octstr(arr):
    bstr = ''
    for byte in arr:
        if bstr != '':
            bstr += ':%02x' % (byte,)
        else:
            bstr += '%02x' %(byte,)
    return bstr

def dpidstr(ll):
    return array_to_octstr(array.array('B',struct.pack('!Q',ll)))

class LAVIMessage(LTMessage):
    SIZE = 4

    def __init__(self, xid=0):
        LTMessage.__init__(self)
        self.xid = xid

    def length(self):
        return self.SIZE

    def pack(self):
        return struct.pack('> I', self.xid)

    @staticmethod
    def unpack(body):
        return LAVIMessage(struct.unpack('> I', body[:4])[0])

    def __str__(self):
        return 'xid=%u' % self.xid

class Disconnect(LAVIMessage):
    @staticmethod
    def get_type():
        return 0x00

    def __init__(self, xid=0):
        LAVIMessage.__init__(self, xid)

    def __str__(self):
        return 'DISCONNECT: ' + LAVIMessage.__str__(self)

class PollStart(LAVIMessage):
    @staticmethod
    def get_type():
        return 0x0E

    def __init__(self, interval_in_100ms_units, lm, xid=0):
        LAVIMessage.__init__(self, xid)
        self.interval = interval_in_100ms_units
        self.lm = lm

    def length(self):
        return LAVIMessage.SIZE + 2 + self.lm.length()

    def pack(self):
        return LAVIMessage.pack(self) + struct.pack('> S', self.interval) + self.lm.pack()

    @staticmethod
    def unpack(body):
        xid = struct.unpack('> I', body[:4])[0]
        body = body[4:]
        interval = struct.unpack('> S', body[:2])[0]
        body = body[2:]

        _ = struct.unpack('> S', body[:2])[0]  # inner message length
        body = body[2:]
        type_val = struct.unpack('> B', body[1:])[0]
        body = body[1:]
        lm = LAVI_PROTOCOL.unpck_received_msg(type_val, body)

        return PollStart(interval, lm, xid)

    def __str__(self):
        fmt = 'POLL_START: ' + LAVIMessage.__str__(self) + ' interval=%s msg=%s'
        return fmt % (self.interval, str(self.lm))

class PollStop(LAVIMessage):
    @staticmethod
    def get_type():
        return 0x0F

    def __init__(self, xid_to_stop_polling, xid=0):
        LAVIMessage.__init__(self, xid)
        self.xid_to_stop_polling = xid_to_stop_polling

    def length(self):
        return LAVIMessage.SIZE + 4

    def pack(self):
        return LAVIMessage.pack(self) + struct.pack('> I', self.xid_to_stop_polling)

    @staticmethod
    def unpack(body):
        xid = struct.unpack('> I', body[:4])[0]
        body = body[4:]
        xid_to_stop_polling = struct.unpack('> I', body[:4])[0]
        return PollStop(xid_to_stop_polling, xid)

    def __str__(self):
        return 'POLL_STOP: ' + LAVIMessage.__str__(self) + ' xid_to_stop_polling=' + self.xid_to_stop_polling

class SwitchesRequest(LAVIMessage):
    @staticmethod
    def get_type():
        return 0x10

    def __init__(self, k):
        LAVIMessage.__init__(self, k)

    def getK(self):
        return self.xid

    def __str__(self):
        return 'SWITCHES_REQUEST: k=%u' % self.getK()

class SwitchesList(LAVIMessage):
    def __init__(self, dpids, xid=0):
        LAVIMessage.__init__(self, xid)
        self.dpids = dpids

    def length(self):
        return LAVIMessage.SIZE + self.dpids * 8

    def pack(self):
        return LAVIMessage.pack(self) + ''.join([struct.pack('> Q', dpid) for dpid in self.dpids])

    @staticmethod
    def unpack(body):
        xid = struct.unpack('> I', body[:4])[0]
        body = body[4:]
        num_dpids = len(body) / 8
        fmt = '> %uQ' % num_dpids
        dpids = [dpid for dpid in struct.unpack(fmt, body)]
        return SwitchesList(dpids, xid)

    def __str__(self):
        return LAVIMessage.__str__(self) + ' dpids=[%s]' % ''.join([dpidstr(dpid) + ',' for dpid in self.dpids])

class SwitchesAdd(SwitchesList):
    @staticmethod
    def get_type():
        return 0x11

    def __init__(self, dpids, xid=0):
        SwitchesList.__init__(self, dpids, xid)

    def __str__(self):
        return 'SWITCHES_ADD: ' + SwitchesList.__str__(self)

class SwitchesDel(SwitchesList):
    @staticmethod
    def get_type():
        return 0x12

    def __init__(self, dpids, xid=0):
        SwitchesList.__init__(self, dpids, xid)

    def __str__(self):
        return 'SWITCHES_DEL: ' + SwitchesList.__str__(self)

class LinksRequest(LAVIMessage):
    @staticmethod
    def get_type():
        return 0x13

    def __init__(self, src_dpid, xid=0):
        LAVIMessage.__init__(self, xid)
        self.src_dpid = src_dpid

    def length(self):
        return LAVIMessage.SIZE + 8

    def pack(self):
        return LAVIMessage.pack(self) + struct.pack('> Q', self.src_dpid)

    @staticmethod
    def unpack(body):
        xid = struct.unpack('> I', body[:4])[0]
        body = body[4:]
        src_dpid = struct.unpack('> Q', body[:8])[0]
        return LinksRequest(src_dpid, xid)

    def __str__(self):
        return 'LINKS_REQUEST: ' + LAVIMessage.__str__(self) + ' src_dpid=' + dpidstr(self.src_dpid)

class Link:
    SIZE = 20

    def __init__(self, src_dpid, src_port, dst_dpid, dst_port):
        self.src_dpid = src_dpid
        self.src_port = src_port
        self.dst_dpid = dst_dpid
        self.dst_port = dst_port

    def pack(self):
        return struct.pack('> SQS', self.src_port, self.dst_dpid, self.dst_port)

    @staticmethod
    def unpack(src_dpid, buf):
        t = struct.unpack('> SQS', buf[:12])
        return Link(src_dpid, t[1], t[2], t[3])

class LinksList(LAVIMessage):
    def __init__(self, links, xid=0):
        LAVIMessage.__init__(self, xid)
        self.links = links

    def length(self):
        return LAVIMessage.SIZE + 8 + self.links * 12

    def pack(self):
        src_dpid = 0
        if len(self.links) > 0:
            src_dpid = self.links[0].src_dpid
            for dpid in self.links:
                if src_dpid != dpid:
                    raise AssertionError('not all dpids match in LinksList.links: ' + str(self.links))
        hdr = LAVIMessage.pack(self) + struct.pack('> Q', src_dpid)
        return hdr + ''.join([link.pack() for link in self.links])

    @staticmethod
    def unpack(body):
        xid = struct.unpack('> I', body[:4])[0]
        body = body[4:]
        src_dpid = struct.unpack('> Q', body[:8])[0]
        body = body[8:]
        num_links = len(body) / 12
        links = []
        for _ in range(num_links):
            links.append(Link.unpack(src_dpid, body[:12]))
            body = body[12:]
        return LinksList(links, xid)

    def __str__(self):
        return LAVIMessage.__str__(self) + ' links=%s' % str(self.links)

class LinksAdd(LinksList):
    @staticmethod
    def get_type():
        return 0x14

    def __init__(self, links, xid=0):
        LinksList.__init__(self, links, xid)

    def __str__(self):
        return 'LINKS_ADD: ' + LinksList.__str__(self)

class LinksDel(LinksList):
    @staticmethod
    def get_type():
        return 0x15

    def __init__(self, links, xid=0):
        LinksList.__init__(self, links, xid)

    def __str__(self):
        return 'LINKS_DEL: ' + LinksList.__str__(self)

class Subscribe(LAVIMessage):
    def __init__(self, subscribe, xid=0):
        LAVIMessage.__init__(self, xid)
        self.subscribe = subscribe

    def length(self):
        return LAVIMessage.SIZE + 1

    def pack(self):
        return LAVIMessage.pack(self) + struct.pack('> B', (1 if self.subscribe else 0))

    @staticmethod
    def unpack(body):
        xid = struct.unpack('> I', body[:4])[0]
        body = body[4:]
        subscribe = struct.unpack('> B', body[:1])[0]
        return (True if subscribe==1 else False, xid)

    def __str__(self):
        what = ' ' if self.subscribe else ' un'
        return 'SUBSCRIBE: ' + LAVIMessage.__str__(self) + what + 'subscribe'

class SwitchesSubscribe(Subscribe):
    @staticmethod
    def get_type():
        return 0x16

    def __init__(self, xid, subscribe):
        Subscribe.__init__(self, xid, subscribe)

    @staticmethod
    def unpack(body):
        t = Subscribe.unpack(body)
        return SwitchesSubscribe(t[0], t[1])

    def __str__(self):
        return 'SWITCHES_' + Subscribe.__str__(self)

class LinksSubscribe(Subscribe):
    @staticmethod
    def get_type():
        return 0x17

    def __init__(self, subscribe, xid=0):
        Subscribe.__init__(self, subscribe, xid)

    @staticmethod
    def unpack(body):
        t = Subscribe.unpack(body)
        return LinksSubscribe(t[0], t[1])

    def __str__(self):
        return 'LINKS_' + Subscribe.__str__(self)

class ETTrafficMatrix(LAVIMessage):
    @staticmethod
    def get_type():
        return 0xF0

    def __init__(self, k, demand, edge, agg, plen, xid=0):
        LAVIMessage.__init__(self, xid)
        self.k      = int(k)
        self.demand = int(demand)
        self.edge   = int(edge)
        self.agg    = int(agg)
        self.plen   = int(plen)

    def length(self):
        return LAVIMessage.SIZE + 20

    def pack(self):
        return LAVIMessage.pack(self) + struct.pack('> 5I', self.k, self.demand, self.edge, self.agg, self.plen)

    @staticmethod
    def unpack(body):
        xid = struct.unpack('> I', body[:4])[0]
        body = body[4:]
        t = struct.unpack('> 5I', body[:20])
        return ETTrafficMatrix(t[0], t[1], t[2], t[3], t[4], xid)

    def __str__(self):
        fmt = 'ET_TRAFFIC_MATRIX: ' + LAVIMessage.__str__(self) + " k=%u demand=%u edge=%u agg=%u plen=%u"
        return fmt % (self.k, self.demand, self.edge, self.agg, self.plen)

class ETLinkUtil(Link):
    SIZE = Link.SIZE + 4

    def __init__(self, src_dpid, src_port, dst_dpid, dst_port, util):
        Link.__init__(self, src_dpid, src_port, dst_dpid, dst_port)
        self.util = float(util)

    def pack(self):
        return struct.pack('> QSQSf', self.src_dpid, self.src_port, self.dst_dpid, self.dst_port, self.util)

    @staticmethod
    def unpack(buf):
        t = struct.unpack('> QSQS', buf[:20])
        return Link(t[0], t[1], t[2], t[3])

class ETLinkUtils(LAVIMessage):
    @staticmethod
    def get_type():
        return 0xF1

    def __init__(self, utils, xid=0):
        LAVIMessage.__init__(self, xid)
        self.utils = utils

    def length(self):
        return LAVIMessage.SIZE + self.utils * ETLinkUtil.SIZE

    def pack(self):
        hdr = LAVIMessage.pack(self)
        return hdr + ''.join([util.pack() for util in self.utils])

    @staticmethod
    def unpack(body):
        xid = struct.unpack('> I', body[:4])[0]
        body = body[4:]
        num_utils = len(body) / ETLinkUtil.SIZE
        utils = []
        for _ in range(num_utils):
            utils.append(Link.unpack(body[:ETLinkUtil.SIZE]))
            body = body[ETLinkUtil.SIZE:]
        return ETLinkUtils(utils, xid)

    def __str__(self):
        return 'ET_LINK_UTILS: ' + LAVIMessage.__str__(self) + ' utils=%s' % str(self.utils)

class ETPowerUsage(LAVIMessage):
    @staticmethod
    def get_type():
        return 0xF2

    def __init__(self, watts_current, watts_traditional, watts_max, xid=0):
        LAVIMessage.__init__(self, xid)
        self.watts_current = int(watts_current)
        self.watts_traditional = int(watts_traditional)
        self.watts_max = int(watts_max)

    def length(self):
        return LAVIMessage.SIZE + 12

    def pack(self):
        body = struct.pack('> 3I', self.watts_current, self.watts_traditional, self.watts_max)
        return LAVIMessage.pack(self) + body

    @staticmethod
    def unpack(body):
        xid = struct.unpack('> I', body[:4])[0]
        body = body[4:]
        watts_current = struct.unpack('> I', body[:4])[0]
        body = body[4:]
        watts_traditional = struct.unpack('> I', body[:4])[0]
        body = body[4:]
        watts_max = struct.unpack('> I', body[:4])[0]
        return ETPowerUsage(watts_current, watts_traditional, watts_max, xid)

    def __str__(self):
        fmt = 'ET_POWER_USAGE: ' + LAVIMessage.__str__(self) + " cur=%u trad=%u max=%u"
        return fmt % (self.watts_current, self.watts_traditional, self.watts_max)

class ETSwitchesOff(SwitchesList):
    @staticmethod
    def get_type():
        return 0xF3

    def __init__(self, dpids, xid=0):
        SwitchesList.__init__(self, dpids, xid)

    def __str__(self):
        return 'SWITCHES_OFF: ' + SwitchesList.__str__(self)

LAVI_PROTOCOL = LTProtocol([Disconnect,
                            PollStart, PollStop,
                            SwitchesRequest, SwitchesAdd, SwitchesDel,
                            LinksRequest, LinksAdd, LinksDel,
                            SwitchesSubscribe, LinksSubscribe,
                            ETTrafficMatrix, ETLinkUtils, ETPowerUsage, ETSwitchesOff],
                           'H', 'B')

def create_lavi_server(port, recv_callback):
    """Starts a server which listens for LAVI clients on the specified port.

    @param port  the port to listen on
    @param recv_callback  the function to call with received message content
                         (takes two arguments: transport, msg)

    @return returns the new LTTwistedServer
    """
    from ltprotocol.ltprotocol import LTTwistedServer
    server = LTTwistedServer(LAVI_PROTOCOL, recv_callback)
    server.listen(port)
    return server

def run_lavi_server(port, recv_callback):
    """Creates (see create_lavi_server()) and runs a LAVI server.

    @return this method does not return until the server shuts down (e.g. ctrl-c)
    """
    create_lavi_server(port, recv_callback)
    reactor.run()

def test():
    # test: simply print out all received messages
    def print_ltm(_, ltm):
        print 'recv: %s' % str(ltm)

    server = create_lavi_server(LAVI_DEFAULT_PORT, print_ltm)
    def callback():
        if len(server.connections) > 0:
            print 'sending ...'
            server.send(SwitchesAdd([v+1 for v in range(99)]))
        else:
            reactor.callLater(1, callback)
    reactor.callLater(1, callback)
    reactor.run()

if __name__ == "__main__":
    test()
