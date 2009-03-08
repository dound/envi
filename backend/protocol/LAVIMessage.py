from ltprotocol.ltprotocol import LTMessage, LTProtocol
import struct

class LAVIMessage(LTMessage):
    SIZE = 4

    def __init__(self, xid):
        LTMessage.__init__(self, xid)
        self.xid = xid

    def length(self):
        return self.SIZE

    def pack(self):
        return struct.pack("> I", self.xid)

    @staticmethod
    def unpack(body):
        return LAVIMessage(struct.unpack("> I", body)[0])

    def __str__(self):
        return "xid=%u" % str(self.xid)

class Disconnect(LAVIMessage):
    @staticmethod
    def get_type():
        return 0x00

    def __init__(self, xid):
        LAVIMessage.__init__(xid)

    def __str__(self):
        return 'DISCONNECT: ' + LAVIMessage.__str__(self)

class SwitchesRequest(LAVIMessage):
    @staticmethod
    def get_type():
        return 0x10

    def __init__(self, xid):
        LAVIMessage.__init__(xid)

    def __str__(self):
        return 'SWITCHES_REQUEST: ' + LAVIMessage.__str__(self)

class SwitchesList(LAVIMessage):
    def __init__(self, xid, dpids):
        LAVIMessage.__init__(self, xid)
        self.dpids = dpids

    def length(self):
        return LAVIMessage.SIZE + self.dpids * 8

    def pack(self):
        return LAVIMessage.pack(self) + ''.join([struct.pack("> Q", dpid) for dpid in self.dpids])

    @staticmethod
    def unpack(body):
        xid = struct.unpack("> I", body)[0]
        body = body[4:]
        num_dpids = len(body) / 8
        fmt = '> %uQ' % num_dpids
        dpids = [dpid for dpid in struct.unpack(fmt, body)]
        return SwitchesList(xid, dpids)

    def __str__(self):
        return LAVIMessage.__str__(self) + " dpids=" % str(self.dpids)

class SwitchesAdd(SwitchesList):
    @staticmethod
    def get_type():
        return 0x11

    def __init__(self, xid, dpids):
        SwitchesList.__init__(self, xid, dpids)

    def __str__(self):
        return 'SWITCHES_ADD: ' + SwitchesList.__str__(self)

class SwitchesDel(SwitchesList):
    @staticmethod
    def get_type():
        return 0x12

    def __init__(self, xid, dpids):
        SwitchesList.__init__(self, xid, dpids)

    def __str__(self):
        return 'SWITCHES_DEL: ' + SwitchesList.__str__(self)

class LinksRequest(LAVIMessage):
    @staticmethod
    def get_type():
        return 0x13

    def __init__(self, xid, src_dpid):
        LAVIMessage.__init__(xid)
        self.src_dpid = src_dpid

    def length(self):
        return LAVIMessage.SIZE + 8

    def pack(self):
        return LAVIMessage.pack(self) + struct.pack('> Q', self.src_dpid)

    @staticmethod
    def unpack(body):
        xid = struct.unpack('> I', body)[0]
        body = body[4:]
        src_dpid = struct.unpack('> Q', body)[0]
        return LinksRequest(xid, src_dpid)

    def __str__(self):
        return 'LINKS_REQUEST: ' + LAVIMessage.__str__(self) + " src_dpid=" + self.src_dpid

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
        t = struct.unpack('> SQS', buf)
        return Link(src_dpid, t[1], t[2], t[3])

class LinksList(LAVIMessage):
    def __init__(self, xid, links):
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
                    raise AssertionError("not all dpids match in LinksList.links: " + str(self.links))
        hdr = LAVIMessage.pack(self) + struct.pack('> Q', src_dpid)
        return hdr + ''.join([link.pack() for link in self.links])

    @staticmethod
    def unpack(body):
        xid = struct.unpack('> I', body)[0]
        body = body[4:]
        src_dpid = struct.unpack('> Q', body)[0]
        body = body[8:]
        num_links = len(body) / 12
        links = []
        for _ in range(num_links):
            links.append(Link.unpack(src_dpid, body))
            body = body[12:]
        return LinksList(xid, links)

    def __str__(self):
        return LAVIMessage.__str__(self) + " links=" % str(self.links)

class LinksAdd(LinksList):
    @staticmethod
    def get_type():
        return 0x14

    def __init__(self, xid, links):
        LinksList.__init__(self, xid, links)

    def __str__(self):
        return 'LINKS_ADD: ' + LinksList.__str__(self)

class LinksDel(LinksList):
    @staticmethod
    def get_type():
        return 0x15

    def __init__(self, xid, links):
        LinksList.__init__(self, xid, links)

    def __str__(self):
        return 'LINKS_DEL: ' + LinksList.__str__(self)

class Subscribe(LAVIMessage):
    @staticmethod
    def get_type():
        return 0x16

    def __init__(self, xid, subscribe):
        LAVIMessage.__init__(xid)
        self.subscribe = subscribe

    def length(self):
        return LAVIMessage.SIZE + 1

    def pack(self):
        return LAVIMessage.pack(self) + struct.pack('> ?', self.subscribe)

    @staticmethod
    def unpack(body):
        xid = struct.unpack('> I', body)[0]
        body = body[4:]
        subscribe = struct.unpack('> ?', body)[0]
        return SwitchesSubscribe(xid, subscribe)

    def __str__(self):
        what = '' if self.subscribe else 'un'
        return 'SUBSCRIBE: ' + LAVIMessage.__str__(self) + what + 'subscribe'

class SwitchesSubscribe(Subscribe):
    @staticmethod
    def get_type():
        return 0x17

    def __init__(self, xid, subscribe):
        Subscribe.__init__(xid, subscribe)

    def __str__(self):
        return 'SWITCHES_' + Subscribe.__str__(self)

class LinksSubscribe(Subscribe):
    @staticmethod
    def get_type():
        return 0x17

    def __init__(self, xid, subscribe):
        Subscribe.__init__(xid, subscribe)

    def __str__(self):
        return 'LINKS_' + Subscribe.__str__(self)

LAVI_PROTOCOL = LTProtocol([Disconnect,
                            SwitchesRequest, SwitchesAdd, SwitchesDel,
                            LinksRequest, LinksAdd, LinksDel,
                            SwitchesSubscribe, LinksSubscribe],
                           'S', 'B')
