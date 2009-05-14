"""Defines the OpenFlow GUI protocol and some associated helper functions."""

import array
import hashlib
import struct
from os import urandom

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

class AuthRequest(OFGMessage):
    @staticmethod
    def get_type():
        return 0x01

    def __init__(self, salt, xid):
        OFGMessage.__init__(self, xid)
        self.salt = salt

    def length(self):
        return OFGMessage.SIZE + len(self.salt)

    def pack(self):
        return OFGMessage.pack(self) + self.salt

    @staticmethod
    def unpack(body):
        xid = struct.unpack('> I', body[:4])[0]
        salt = body[4:]
        return AuthRequest(salt, xid)

    def __str__(self):
        return 'AUTH_REQUEST: ' + OFGMessage.__str__(self) + ' salt length=%uB' % len(self.salt)
OFG_MESSAGES.append(AuthRequest)

class AuthReply(OFGMessage):
    @staticmethod
    def get_type():
        return 0x02

    def __init__(self, username, salted_sha1_of_pw, xid=0):
        OFGMessage.__init__(self, xid)
        self.username = username
        self.ssp = salted_sha1_of_pw

    def length(self):
        return OFGMessage.SIZE + len(self.username) + len(self.ssp)

    def pack(self):
        return OFGMessage.pack(self) + struct.pack('> I', len(self.username)) + self.username + self.ssp

    @staticmethod
    def unpack(body):
        xid, username_len = struct.unpack('> 2I', body[:8])
        body = body[8:]
        username = body[:username_len]
        ssp = body[username_len:]
        return AuthReply(username, ssp, xid)

    def __str__(self):
        return 'AUTH_REPLY: ' + OFGMessage.__str__(self) + ' username=' + self.username
OFG_MESSAGES.append(AuthReply)

class AuthStatus(OFGMessage):
    @staticmethod
    def get_type():
        return 0x03

    def __init__(self, auth_ok, msg, xid=0):
        OFGMessage.__init__(self, xid)
        self.auth_ok = bool(auth_ok)
        self.msg = msg

    def length(self):
        return OFGMessage.SIZE + 1 + len(self.msg)

    def pack(self):
        return OFGMessage.pack(self) + struct.pack('> B', self.auth_ok) + self.msg

    @staticmethod
    def unpack(body):
        xid, auth_ok = struct.unpack('> IB', body[:5])
        msg = body[5:]
        return AuthStatus(auth_ok, msg, xid)

    def __str__(self):
        return 'AUTH_STATUS: ' + OFGMessage.__str__(self) + ' auth_ok=%s msg=%s' % (str(self.auth_ok), self.msg)
OFG_MESSAGES.append(AuthStatus)

class EchoRequest(OFGMessage):
    @staticmethod
    def get_type():
        return 0x08

    def __init__(self, xid=0):
        OFGMessage.__init__(self, xid)

    def __str__(self):
        return 'ECHO_REQUEST: ' + OFGMessage.__str__(self)
OFG_MESSAGES.append(EchoRequest)

class EchoReply(OFGMessage):
    @staticmethod
    def get_type():
        return 0x09

    def __init__(self, xid=0):
        OFGMessage.__init__(self, xid)

    def __str__(self):
        return 'ECHO_REPLY: ' + OFGMessage.__str__(self)
OFG_MESSAGES.append(EchoReply)

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
        fmt = 'POLL_START: ' + OFGMessage.__str__(self) + ' interval=%.1fsec msg=%s'
        return fmt % (self.interval * 10.0, str(self.lm))
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
        return 'POLL_STOP: ' + OFGMessage.__str__(self) + ' xid_to_stop_polling=%u' % self.xid_to_stop_polling
OFG_MESSAGES.append(PollStop)

class Node:
    SIZE = 10

    # default types
    TYPE_UNKNOWN = 0
    TYPE_OPENFLOW_SWITCH = 1
    TYPE_OPENFLOW_WIRELESS_ACCESS_POINT = 2
    TYPE_HOST = 100

    def __init__(self, node_type, node_id):
        self.node_type = int(node_type)
        self.id = long(node_id)

    def pack(self):
        return struct.pack('> HQ', self.node_type, self.id)

    @staticmethod
    def unpack(buf):
        t = struct.unpack('> HQ', buf[:Node.SIZE])
        return Node(t[0], t[1])

    @staticmethod
    def type_to_str(node_type):
        if node_type == Node.TYPE_OPENFLOW_SWITCH:
            return 'OFSwitch'
        elif node_type == Node.TYPE_OPENFLOW_WIRELESS_ACCESS_POINT:
            return 'AP'
        elif node_type == Node.TYPE_HOST:
            return 'Host'
        else:
            return 'unknown'

    def __str__(self):
        return '%s{%s}' % (Node.type_to_str(self.node_type), dpidstr(self.id))

class NodesList(OFGMessage):
    def __init__(self, nodes, xid=0):
        OFGMessage.__init__(self, xid)
        self.nodes = nodes

    def length(self):
        return OFGMessage.SIZE + len(self.nodes) * Node.SIZE

    def pack(self):
        return OFGMessage.pack(self) + ''.join([node.pack() for node in self.nodes])

    @staticmethod
    def unpack_child(clz, body):
        xid = struct.unpack('> I', body[:4])[0]
        body = body[4:]
        num_nodes = len(body) / Node.SIZE
        nodes = []
        for _ in range(num_nodes):
            nodes.append(Node.unpack(body[Node.SIZE:]))
            body = body[:Node.SIZE]
        return clz(nodes, xid)

    def __str__(self):
        return OFGMessage.__str__(self) + ' nodes=[%s]' % ''.join([str(node) + ',' for node in self.nodes])

class NodesAdd(NodesList):
    @staticmethod
    def get_type():
        return 0x11

    def __init__(self, nodes, xid=0):
        NodesList.__init__(self, nodes, xid)

    @staticmethod
    def unpack(body):
        return NodesList.unpack_child(NodesAdd, body)

    def __str__(self):
        return 'NODES_ADD: ' + NodesList.__str__(self)
OFG_MESSAGES.append(NodesAdd)

class NodesDel(NodesList):
    @staticmethod
    def get_type():
        return 0x12

    def __init__(self, dpids, xid=0):
        NodesList.__init__(self, dpids, xid)

    @staticmethod
    def unpack(body):
        return NodesList.unpack_child(NodesDel, body)

    def __str__(self):
        return 'NODES_DEL: ' + NodesList.__str__(self)
OFG_MESSAGES.append(NodesDel)

class Link:
    SIZE = 2 + (2 * (Node.SIZE + 2))

    TYPE_UNKNOWN = 0
    TYPE_WIRE = 1
    TYPE_WIRELESS = 2
    TYPE_TUNNEL = 4

    def __init__(self, link_type, src_node, src_port, dst_node, dst_port):
        self.link_type = link_type
        self.src_node = src_node
        self.src_port = src_port
        self.dst_node = dst_node
        self.dst_port = dst_port

    def pack(self):
        src = self.src_node.pack() + struct.pack('> H', self.src_port)
        dst = self.dst_node.pack() + struct.pack('> H', self.dst_port)
        return struct.pack('> H', self.link_type) + src + dst

    @staticmethod
    def unpack(buf):
        link_type = struct.unpack('> H', buf[:2])[0]
        buf = buf[2:]
        src_node = Node.unpack(buf[:Node.SIZE])
        buf = buf[Node.SIZE:]
        src_port = struct.unpack('> H', buf[:2])[0]
        buf = buf[2:]
        dst_node = Node.unpack(buf[:Node.SIZE])
        buf = buf[Node.SIZE:]
        dst_port = struct.unpack('> H', buf[:2])[0]
        return Link(link_type, src_node, src_port, dst_node, dst_port)

    @staticmethod
    def type_to_str(link_type):
        if link_type == Link.TYPE_WIRE:
            return 'wire'
        elif link_type == Link.TYPE_WIRELESS:
            return 'wireless'
        elif link_type == Link.TYPE_TUNNEL:
            return 'tunnel'
        else:
            return 'unknown'

    def __str__(self):
        return '%s:%u --(%s)-> %s:%u' % (str(self.src_node), self.src_port,
                                         Link.type_to_str(self.link_type),
                                         str(self.dst_node), self.dst_port)

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
    def unpack_child(clz, body):
        xid = struct.unpack('> I', body[:4])[0]
        body = body[4:]
        num_links = len(body) / Link.SIZE
        links = []
        for _ in range(num_links):
            links.append(Link.unpack(body[:Link.SIZE]))
            body = body[Link.SIZE:]
        return clz(links, xid)

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

    @staticmethod
    def unpack(body):
        return LinksList.unpack_child(LinksAdd, body)

    def __str__(self):
        return 'LINKS_ADD: ' + LinksList.__str__(self)
OFG_MESSAGES.append(LinksAdd)

class LinksDel(LinksList):
    @staticmethod
    def get_type():
        return 0x15

    def __init__(self, links, xid=0):
        LinksList.__init__(self, links, xid)

    @staticmethod
    def unpack(body):
        return LinksList.unpack_child(LinksDel, body)

    def __str__(self):
        return 'LINKS_DEL: ' + LinksList.__str__(self)
OFG_MESSAGES.append(LinksDel)

class FlowHop:
    SIZE = 2 + Node.SIZE + 2

    def __init__(self, inport, node, outport):
        self.inport = int(inport)
        self.node = node
        self.outport = int(outport)

    def pack(self):
        return struct.pack('> H', self.inport) + self.node.pack() + struct.pack('> H', self.outport)

    @staticmethod
    def unpack(buf):
        inport = struct.unpack('> H', buf[:2])[0]
        buf = buf[2:]
        node = Node.unpack(buf[:Node.SIZE])
        buf = buf[Node.SIZE:]
        outport = struct.unpack('> H', buf[:2])[0]
        return FlowHop(inport, node, outport)

    def __str__(self):
        return '%s:%u:%u' % (str(self.node), self.inport, self.outport)

class Flow:
    TYPE_UNKNOWN = 0

    def __init__(self, flow_type, flow_id, src_node, src_port, dst_node, dst_port, path):
        self.flow_type = int(flow_type)
        self.flow_id = int(flow_id)
        self.src_node = src_node
        self.src_port = int(src_port)
        self.dst_node = dst_node
        self.dst_port = int(dst_port)
        self.path = path

    def pack(self):
        src = self.src_node.pack() + struct.pack('> H', self.src_port)
        dst = self.dst_node.pack() + struct.pack('> H', self.dst_port)
        header = struct.pack('> H I', self.flow_type, self.flow_id) + src + dst + struct.pack('> H', len(self.path))
        body = ''.join(hop.pack() for hop in self.path)
        return header + body

    @staticmethod
    def unpack(buf):
        flow_type, flow_id = struct.unpack('> H I', buf[:6])
        buf = buf[6:]
        src_node = Node.unpack(buf[:Node.SIZE])
        buf = buf[Node.SIZE:]
        src_port = struct.unpack('> H', buf[:2])[0]
        buf = buf[2:]
        dst_node = Node.unpack(buf[:Node.SIZE])
        dst_port = struct.unpack('> H', buf[:2])[0]
        buf = buf[2:]
        num_hops = struct.unpack('> H', buf[:2])[0]
        buf = buf[2:]

        path = []
        for _ in range(num_hops):
            path.append(FlowHop.unpack(buf[:FlowHop.SIZE]))
            buf = buf[FlowHop.SIZE:]

        return Flow(flow_type, flow_id, src_node, src_port, dst_node, dst_port, path)

    def length(self):
        return 8 + 2*(2+Node.SIZE) + FlowHop.SIZE * len(self.path)

    @staticmethod
    def type_to_str(flow_type):
        return 'unknown'

    def __str__(self):
        return 'Flow:%s:%u:src=%s:%u{%s}dst=%s:%u' % (Flow.type_to_str(self.flow_type), self.flow_id,
                                                      str(self.src_node), self.src_port,
                                                      ','.join(str(hop) for hop in self.path),
                                                      str(self.dst_node), self.dst_port)

class FlowsList(OFGMessage):
    def __init__(self, flows, xid=0):
        OFGMessage.__init__(self, xid)
        self.flows = flows

    def length(self):
        return OFGMessage.SIZE + 4 + sum(flow.length() for flow in self.flows)

    def pack(self):
        hdr = OFGMessage.pack(self) + struct.pack('> I', len(self.flows))
        return hdr + ''.join([flow.pack() for flow in self.flows])

    @staticmethod
    def unpack_child(clz, body):
        xid = struct.unpack('> I', body[:4])[0]
        body = body[4:]
        num_flows = struct.unpack('> I', body)[0]
        body = body[4:]
        flows = []
        for _ in range(num_flows):
            f = Flow.unpack(body)
            flows.append(f)
            body = body[f.length():]
        return clz(flows, xid)

    def flows_to_string(self):
        return '[' + ', '.join([str(f) for f in self.flows]) + ']'

    def __str__(self):
        return OFGMessage.__str__(self) + ' flows=%s' % str(self.flows_to_string())

class FlowsAdd(FlowsList):
    @staticmethod
    def get_type():
        return 0x17

    def __init__(self, flows, xid=0):
        FlowsList.__init__(self, flows, xid)

    @staticmethod
    def unpack(body):
        return FlowsList.unpack_child(FlowsAdd, body)

    def __str__(self):
        return 'FLOWS_ADD: ' + FlowsList.__str__(self)
OFG_MESSAGES.append(FlowsAdd)

class FlowsDel(FlowsList):
    @staticmethod
    def get_type():
        return 0x18

    def __init__(self, flows, xid=0):
        FlowsList.__init__(self, flows, xid)

    @staticmethod
    def unpack(body):
        return FlowsList.unpack_child(FlowsDel, body)

    def __str__(self):
        return 'FLOWS_DEL: ' + FlowsList.__str__(self)
OFG_MESSAGES.append(FlowsDel)

class Request(OFGMessage):
    TYPE_UNKNOWN = 0
    TYPE_ONETIME = 1
    TYPE_SUBSCRIBE = 2
    TYPE_UNSUBSCRIBE = 3

    def __init__(self, request_type, otype, xid=0):
        OFGMessage.__init__(self, xid)
        self.request_type = request_type
        self.type = otype

    def length(self):
        return OFGMessage.SIZE + 3

    def pack(self):
        return OFGMessage.pack(self) + struct.pack('> 2H', self.request_type, self.type)

    @staticmethod
    def unpack_child(clz, body):
        xid = struct.unpack('> I', body[:4])[0]
        body = body[4:]
        t = struct.unpack('> BH', body)
        return clz(t[0], t[1], xid)

    @staticmethod
    def type_to_str(request_type):
        if request_type == Request.TYPE_ONETIME:
            return 'ONETIME'
        elif request_type == Request.TYPE_SUBSCRIBE:
            return 'SUBSCRIBE'
        elif request_type == Request.TYPE_UNSUBSCRIBE:
            return 'UNSUBSCRIBE'
        else:
            return 'unknown'

    def otype_to_str(self, otype):
        return str(otype)

    def __str__(self):
        rstr = Request.type_to_str(self.request_type)
        ostr = self.otype_to_str(self.type)
        return OFGMessage.__str__(self) + ' %s %s' % (rstr, ostr)

class NodesRequest(Request):
    @staticmethod
    def get_type():
        return 0x10

    def __init__(self, request_type, node_type, xid=0):
        Request.__init__(self, request_type, node_type, xid)

    @staticmethod
    def unpack(body):
        return Request.unpack_child(NodesRequest, body)

    def otype_to_str(self, otype):
        return Node.type_to_str(otype)

    def __str__(self):
        return 'REQUEST for Nodes: ' + Request.__str__(self)
OFG_MESSAGES.append(NodesRequest)

class LinksRequest(Request):
    @staticmethod
    def get_type():
        return 0x13

    def __init__(self, request_type, link_type, src_node, xid=0):
        Request.__init__(self, request_type, link_type, xid)
        self.src_node = src_node

    def pack(self):
        return Request.pack(self) + self.src_node.pack()

    @staticmethod
    def unpack(body):
        xid = struct.unpack('> I', body[:4])[0]
        body = body[4:]
        t = struct.unpack('> BH', body[:3])
        body = body[3:]
        src_node = Node.unpack(body)
        return LinksRequest(t[0], t[1], src_node, xid)

    def otype_to_str(self, otype):
        return Link.type_to_str(otype)

    def __str__(self):
        return 'REQUEST for Links: ' + Request.__str__(self)
OFG_MESSAGES.append(LinksRequest)

class FlowsRequest(Request):
    @staticmethod
    def get_type():
        return 0x16

    def __init__(self, request_type, flow_type, xid=0):
        Request.__init__(self, request_type, flow_type, xid)

    @staticmethod
    def unpack(body):
        return Request.unpack_child(FlowsRequest, body)

    def otype_to_str(self, otype):
        return Flow.type_to_str(otype)

    def __str__(self):
        return 'REQUEST for Flows: ' + Request.__str__(self)
OFG_MESSAGES.append(FlowsRequest)

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

def sha1(s):
    """Return the SHA1 digest of the string s"""
    d = hashlib.sha1()
    d.update(s)
    return d.digest()

class _Test():
    """A simple test server for the OFG protocol"""
    def __init__(self, num_nodes, test_auth):
        self.num_nodes = num_nodes
        self.test_auth = test_auth
        self.server = None

        # create some simple data structures for basic authentication support
        self.salt_id_on = 1
        self.salt_db = {}
        self.user_db = {}

    def add_user(self, username, pw):
        """Adds a user to the database"""
        self.user_db[username] = sha1(pw)

    # test: simply print out all received messages
    def print_ltm(self, _, ltm):
        if ltm is not None:
            print 'recv: %s' % str(ltm)
            if ltm.get_type() == NodesRequest.get_type():
                nodes = [Node(Node.TYPE_OPENFLOW_SWITCH, i+1) for i in range(self.num_nodes)]
                links = [Link(i % 2 + 1, nodes[i], 0, nodes[i+1], 1) for i in range(self.num_nodes-1)]

                # add a second path from 2 to 3 via two additional nodes
                if self.num_nodes >= 3:
                    nodes.append(Node(Node.TYPE_OPENFLOW_SWITCH, 10000))
                    nodes.append(Node(Node.TYPE_OPENFLOW_SWITCH, 10001))
                    n = self.num_nodes
                    self.server.send(NodesAdd(nodes))
                    links.append(Link(0, nodes[1], 2, nodes[n], 3))  # 2 to 10000
                    links.append(Link(0, nodes[n], 2, nodes[n+1], 3))  # 10000 to 10001
                    links.append(Link(0, nodes[n+1], 2, nodes[2], 3))  # 10001 to 3

                self.server.send(LinksAdd(links))
                hops = [FlowHop(0, nodes[i+1], 1) for i in range(2)]
                flow_type = 3
                flow_id = 44
                f = Flow(flow_type, flow_id, nodes[0], 0, nodes[3], 1, hops)
                flows = [f]

                # add another flow which simulates bicast of the original flow
                if self.num_nodes >= 3:
                    hops = [FlowHop(0, nodes[1], 2),
                            FlowHop(3, nodes[n], 2),
                            FlowHop(3, nodes[n+1], 2),
                            FlowHop(3, nodes[2], 1)]
                    f = Flow(flow_type, flow_id, nodes[0], 0, nodes[3], 1, hops)
                    flows.append(f)

                self.server.send(FlowsAdd(flows))
            elif ltm.get_type() == AuthReply.get_type():
                # get the salt associated with this transaction
                if not self.salt_db.has_key(ltm.xid):
                    print 'unknown xid in auth reply: %u' % ltm.xid
                    return
                salt = self.salt_db[ltm.xid]

                # check the username's validity
                if not self.user_db.has_key(ltm.username):
                    self.server.send(AuthStatus(False, 'Unknown username', ltm.xid))
                    return

                # check the password
                sha1pw = self.user_db[ltm.username]
                shouldbe = sha1(sha1pw + salt)
                if shouldbe != ltm.ssp:
                    self.server.send(AuthStatus(False, 'Invalid password', ltm.xid))
                else:
                    self.server.send(AuthStatus(True, 'login as %s successful' % ltm.username, ltm.xid))

    # when the gui connects, ask it to authenticate
    def new_conn_callback(self, conn):
        if self.test_auth:
            ar = AuthRequest(urandom(20), self.salt_id_on)
            self.salt_db[self.salt_id_on] = ar.salt
            self.salt_id_on += 1
            self.server.send_msg_to_client(conn, ar)

def test():
    t = _Test(num_nodes=6, test_auth=False)
    t.add_user('dgu', 'envi')
    server = create_ofg_server(OFG_DEFAULT_PORT, lambda a,b : t.print_ltm(a,b))
    server.new_conn_callback = lambda a : t.new_conn_callback(a)
    t.server = server
    reactor.run()

if __name__ == "__main__":
    test()
