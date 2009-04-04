"""Defines the OpenFlow GUI protocol and some associated helper functions."""

import array
import struct

from twisted.internet import reactor

from ltprotocol.ltprotocol import LTMessage, LTProtocol

OFG_DEFAULT_PORT = 2503

def array_to_octstr(arr):
    bstr = ''
    for byte in arr:
        if bstr != '':
            bstr += ':%02x' % (byte,)
        else:
            bstr += '%02x' %(byte,)
    return bstr

def dpidstr(ll):
    return array_to_octstr(array.array('B',struct.pack('!Q',ll))).replace('00:', '')

OFG_MESSAGES = []

class OFGMessage(LTMessage):
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
        return OFGMessage(struct.unpack('> I', body[:4])[0])

    def __str__(self):
        return 'xid=%u' % self.xid

class Disconnect(OFGMessage):
    @staticmethod
    def get_type():
        return 0x00

    def __init__(self, xid=0):
        OFGMessage.__init__(self, xid)

    def __str__(self):
        return 'DISCONNECT: ' + OFGMessage.__str__(self)
OFG_MESSAGES.append(Disconnect)

class PollStart(OFGMessage):
    @staticmethod
    def get_type():
        return 0x0E

    def __init__(self, interval_in_100ms_units, lm, xid=0):
        OFGMessage.__init__(self, xid)
        self.interval = interval_in_100ms_units
        self.lm = lm

    def length(self):
        return OFGMessage.SIZE + 2 + self.lm.length()

    def pack(self):
        return OFGMessage.pack(self) + struct.pack('> S', self.interval) + self.lm.pack()

    @staticmethod
    def unpack(body):
        xid = struct.unpack('> I', body[:4])[0]
        body = body[4:]
        interval = struct.unpack('> H', body[:2])[0]
        body = body[2:]

        _ = struct.unpack('> H', body[:2])[0]  # inner message length
        body = body[2:]
        type_val = struct.unpack('> B', body[:1])[0]
        body = body[1:]
        lm = OFG_PROTOCOL.unpack_received_msg(type_val, body)

        return PollStart(interval, lm, xid)

    def __str__(self):
        fmt = 'POLL_START: ' + OFGMessage.__str__(self) + ' interval=%s msg=%s'
        return fmt % (self.interval, str(self.lm))
OFG_MESSAGES.append(PollStart)

class PollStop(OFGMessage):
    @staticmethod
    def get_type():
        return 0x0F

    def __init__(self, xid_to_stop_polling, xid=0):
        OFGMessage.__init__(self, xid)
        self.xid_to_stop_polling = xid_to_stop_polling

    def length(self):
        return OFGMessage.SIZE + 4

    def pack(self):
        return OFGMessage.pack(self) + struct.pack('> I', self.xid_to_stop_polling)

    @staticmethod
    def unpack(body):
        xid = struct.unpack('> I', body[:4])[0]
        body = body[4:]
        xid_to_stop_polling = struct.unpack('> I', body[:4])[0]
        return PollStop(xid_to_stop_polling, xid)

    def __str__(self):
        return 'POLL_STOP: ' + OFGMessage.__str__(self) + ' xid_to_stop_polling=' + self.xid_to_stop_polling
OFG_MESSAGES.append(PollStop)

class SwitchesRequest(OFGMessage):
    @staticmethod
    def get_type():
        return 0x10

    def __init__(self, xid=0):
        OFGMessage.__init__(self, xid)

    def __str__(self):
        return 'SWITCHES_REQUEST: ' + OFGMessage.__str__(self)
OFG_MESSAGES.append(SwitchesRequest)

class SwitchesList(OFGMessage):
    def __init__(self, dpids, xid=0):
        OFGMessage.__init__(self, xid)
        self.dpids = dpids

    def length(self):
        return OFGMessage.SIZE + len(self.dpids) * 8

    def pack(self):
        return OFGMessage.pack(self) + ''.join([struct.pack('> Q', long(dpid)) for dpid in self.dpids])

    @staticmethod
    def unpack(body):
        xid = struct.unpack('> I', body[:4])[0]
        body = body[4:]
        num_dpids = len(body) / 8
        fmt = '> %uQ' % num_dpids
        dpids = [dpid for dpid in struct.unpack(fmt, body)]
        return SwitchesList(dpids, xid)

    def __str__(self):
        return OFGMessage.__str__(self) + ' dpids=[%s]' % ''.join([dpidstr(long(dpid)) + ',' for dpid in self.dpids])

class SwitchesAdd(SwitchesList):
    @staticmethod
    def get_type():
        return 0x11

    def __init__(self, dpids, xid=0):
        SwitchesList.__init__(self, dpids, xid)

    def __str__(self):
        return 'SWITCHES_ADD: ' + SwitchesList.__str__(self)
OFG_MESSAGES.append(SwitchesAdd)

class SwitchesDel(SwitchesList):
    @staticmethod
    def get_type():
        return 0x12

    def __init__(self, dpids, xid=0):
        SwitchesList.__init__(self, dpids, xid)

    def __str__(self):
        return 'SWITCHES_DEL: ' + SwitchesList.__str__(self)
OFG_MESSAGES.append(SwitchesDel)

class LinksRequest(OFGMessage):
    @staticmethod
    def get_type():
        return 0x13

    def __init__(self, src_dpid, xid=0):
        OFGMessage.__init__(self, xid)
        self.src_dpid = long(src_dpid)

    def length(self):
        return OFGMessage.SIZE + 8

    def pack(self):
        return OFGMessage.pack(self) + struct.pack('> Q', self.src_dpid)

    @staticmethod
    def unpack(body):
        xid = struct.unpack('> I', body[:4])[0]
        body = body[4:]
        src_dpid = struct.unpack('> Q', body[:8])[0]
        return LinksRequest(src_dpid, xid)

    def __str__(self):
        return 'LINKS_REQUEST: ' + OFGMessage.__str__(self) + ' src_dpid=' + dpidstr(self.src_dpid)
OFG_MESSAGES.append(LinksRequest)

class Link:
    SIZE = 20

    def __init__(self, src_dpid, src_port, dst_dpid, dst_port):
        self.src_dpid = long(src_dpid)
        self.src_port = src_port
        self.dst_dpid = long(dst_dpid)
        self.dst_port = dst_port

    def pack(self):
        return struct.pack('> QHQH', self.src_dpid, self.src_port, self.dst_dpid, self.dst_port)

    @staticmethod
    def unpack(buf):
        t = struct.unpack('> QHQH', buf[:Link.SIZE])
        return Link(t[0], t[1], t[2], t[3])

    def __str__(self):
        return '%s <--> %s' % (dpidstr(self.src_dpid), dpidstr(self.dst_dpid))

class LinksList(OFGMessage):
    def __init__(self, links, xid=0):
        OFGMessage.__init__(self, xid)
        self.links = links

    def length(self):
        return OFGMessage.SIZE + len(self.links) * Link.SIZE

    def pack(self):
        hdr = OFGMessage.pack(self)
        return hdr + ''.join([link.pack() for link in self.links])

    @staticmethod
    def unpack(body):
        xid = struct.unpack('> I', body[:4])[0]
        body = body[4:]
        num_links = len(body) / Link.SIZE
        links = []
        for _ in range(num_links):
            links.append(Link.unpack(body[:Link.SIZE]))
            body = body[Link.SIZE:]
        return LinksList(links, xid)

    def links_to_string(self):
        return '[' + ', '.join([str(l) for l in self.links]) + ']'

    def __str__(self):
        return OFGMessage.__str__(self) + ' links=%s' % str(self.links_to_string())

class LinksAdd(LinksList):
    @staticmethod
    def get_type():
        return 0x14

    def __init__(self, links, xid=0):
        LinksList.__init__(self, links, xid)

    def __str__(self):
        return 'LINKS_ADD: ' + LinksList.__str__(self)
OFG_MESSAGES.append(LinksAdd)

class LinksDel(LinksList):
    @staticmethod
    def get_type():
        return 0x15

    def __init__(self, links, xid=0):
        LinksList.__init__(self, links, xid)

    def __str__(self):
        return 'LINKS_DEL: ' + LinksList.__str__(self)
OFG_MESSAGES.append(LinksDel)

class Subscribe(OFGMessage):
    def __init__(self, subscribe, xid=0):
        OFGMessage.__init__(self, xid)
        self.subscribe = subscribe

    def length(self):
        return OFGMessage.SIZE + 1

    def pack(self):
        return OFGMessage.pack(self) + struct.pack('> B', (1 if self.subscribe else 0))

    @staticmethod
    def unpack(body):
        xid = struct.unpack('> I', body[:4])[0]
        body = body[4:]
        subscribe = struct.unpack('> B', body[:1])[0]
        return (True if subscribe==1 else False, xid)

    def __str__(self):
        what = ' ' if self.subscribe else ' un'
        return 'SUBSCRIBE: ' + OFGMessage.__str__(self) + what + 'subscribe'

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
OFG_MESSAGES.append(SwitchesSubscribe)

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
OFG_MESSAGES.append(LinksSubscribe)

OFG_PROTOCOL = LTProtocol(OFG_MESSAGES, 'H', 'B')

def create_ofg_server(port, recv_callback):
    """Starts a server which listens for OFG clients on the specified port.

    @param port  the port to listen on
    @param recv_callback  the function to call with received message content
                         (takes two arguments: transport, msg)

    @return returns the new LTTwistedServer
    """
    from ltprotocol.ltprotocol import LTTwistedServer
    server = LTTwistedServer(OFG_PROTOCOL, recv_callback)
    server.listen(port)
    return server

def run_ofg_server(port, recv_callback):
    """Creates (see create_ofg_server()) and runs a OFG server.

    @return this method does not return until the server shuts down (e.g. ctrl-c)
    """
    create_ofg_server(port, recv_callback)
    reactor.run()

def test():
    # test: simply print out all received messages
    def print_ltm(_, ltm):
        print 'recv: %s' % str(ltm)

    server = create_ofg_server(OFG_DEFAULT_PORT, print_ltm)
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
