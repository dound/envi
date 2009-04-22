"""Defines the OpenFlow GUI-based OpenPipes protocol."""

import struct

from twisted.internet import reactor

from OFGMessage import OFG_DEFAULT_PORT, OFG_MESSAGES
from OFGMessage import OFGMessage, LinksAdd, LinksDel, Node, NodesAdd
from ltprotocol.ltprotocol import LTProtocol

OP_MESSAGES = []

class OPMoveModule(OFGMessage):
    # used by MoveModule to represent when a module is added (from_node is NONE) or
    # removed (to_node is NONE)
    NODE_NONE = Node(Node.TYPE_UNKNOWN, 0x00000000FFFFFFFFL)

    @staticmethod
    def get_type():
        return 0xF0

    def __init__(self, module, from_node, to_node, xid=0):
        OFGMessage.__init__(self, xid)
        self.module = module
        self.from_node = from_node
        self.to_node = to_node

    def length(self):
        return OFGMessage.SIZE + 3 * Node.SIZE

    def pack(self):
        hdr = OFGMessage.pack(self)
        body = self.module.pack() + self.from_node.pack() + self.to_node.pack()
        return hdr + body

    @staticmethod
    def unpack(body):
        xid = struct.unpack('> I', body[:4])[0]
        body = body[4:]
        module = Node.unpack(body[:Node.SIZE])
        body = body[Node.SIZE:]
        from_node = Node.unpack(body[:Node.SIZE])
        body = body[Node.SIZE:]
        to_node = Node.unpack(body[:Node.SIZE])
        return OPMoveModule(module, from_node, to_node, xid)

    def __str__(self):
        noneID = OPMoveModule.NODE_NONE.id
        noneT = OPMoveModule.NODE_NONE.node_type
        if self.from_node.id==noneID and self.from_node.node_type==noneT:
            fmt = 'OP_MOVE_MODULE: ' + OFGMessage.__str__(self) + " add %s to %s"
            return fmt % (self.module, self.to_node)
        elif self.to_node.id == noneID and self.to_node.node_type == noneT:
            fmt = 'OP_MOVE_MODULE: ' + OFGMessage.__str__(self) + " remove %s from %s"
            return fmt % (self.module, self.from_node)
        else:
            fmt = 'OP_MOVE_MODULE: ' + OFGMessage.__str__(self) + " move %s from %s to %s"
            return fmt % (self.module, self.from_node, self.to_node)

OP_MESSAGES.append(OPMoveModule)

class OPTestInfo(OFGMessage):
    @staticmethod
    def get_type():
        return 0xF1

    def __init__(self, test_input, test_output, xid=0):
        OFGMessage.__init__(self, xid)
        self.input = str(test_input)
        self.output = str(test_output)

    def length(self):
        return OFGMessage.SIZE + len(self.input) + 1 + len(self.output) + 1

    def pack(self):
        hdr = OFGMessage.pack(self)
        body = struct.pack('> %us %us' % (len(self.input)+1, len(self.output)+1),
                           self.input, self.output)
        return hdr + body

    @staticmethod
    def unpack(body):
        raise Exception('OPTestInfo.unpack() not implemented (one-way message)')

    def __str__(self):
        fmt = 'OP_TEST_INFO: ' + OFGMessage.__str__(self) + " %s ==> %s"
        return fmt % (self.input, self.output)
OP_MESSAGES.append(OPTestInfo)

class OPModule(Node):
    NAME_LEN = 32
    SIZE = Node.SIZE + 32

    @staticmethod
    def extractModuleID(nid):
        """Extracts the portion of the ID which correspond to module ID"""
        return int(nid & 0x00000000FFFFFFFFL)

    @staticmethod
    def extractCopyID(nid):
        """Extracts the portion of the ID which correspond to module copy ID"""
        return int(nid >> 32L)

    @staticmethod
    def createNodeID(mid, cid):
        """create the node ID from its constituent parts"""
        if (0xFFFFFFFF00000000L & mid) != 0:
            raise Exception("Error: upper 4 bytes of module IDs should be 0 for original modules!  Got: %0X" % mid)

        return (cid << 32L) | mid

    def __init__(self, node_type, node_id, name):
        Node.__init__(self, node_type, node_id)
        self.name = str(name)

    def pack(self):
        return Node.pack(self) + struct.pack('> %us' % OPModule.NAME_LEN, self.name)

    @staticmethod
    def unpack(buf):
        node_type = struct.unpack('> H', buf[:2])[0]
        buf = buf[2:]
        node_id = struct.unpack('> Q', buf[:8])[0]
        buf = buf[8:]
        name = struct.unpack('> %us' % OPModule.NAME_LEN, buf[:OPModule.NAME_LEN])[0]
        return OPModule(node_type, node_id, name)

class OPModulesList(OFGMessage):
    def __init__(self, modules, xid=0):
        OFGMessage.__init__(self, xid)
        self.modules = modules

    def length(self):
        return OFGMessage.SIZE + len(self.modules) * OPModule.SIZE

    def pack(self):
        return OFGMessage.pack(self) + ''.join([m.pack() for m in self.modules])

    @staticmethod
    def unpack(body):
        xid = struct.unpack('> I', body[:4])[0]
        body = body[4:]
        num_modules = len(body) / OPModule.SIZE
        modules = []
        for _ in range(num_modules):
            modules.append(OPModule.unpack(body[OPModule.SIZE:]))
            body = body[:OPModule.SIZE]
        return OPModulesList(modules, xid)

    def __str__(self):
        return OFGMessage.__str__(self) + ' modules=[%s]' % ''.join([str(m) + ',' for m in self.modules])

class OPModulesAdd(OPModulesList):
    @staticmethod
    def get_type():
        return 0xF2

    def __init__(self, modules, xid=0):
        OPModulesList.__init__(self, modules, xid)

    def __str__(self):
        return 'MODULES_ADD: ' + OPModulesList.__str__(self)
OFG_MESSAGES.append(OPModulesAdd)

class OPModuleStatusRequest(OFGMessage):
    @staticmethod
    def get_type():
        return 0xF3

    def __init__(self, node, module, xid=0):
        OFGMessage.__init__(self, xid)
        self.node = node
        self.module = module

    def length(self):
        return OFGMessage.SIZE + 2 * Node.SIZE

    def pack(self):
        hdr = OFGMessage.pack(self)
        body = self.node.pack() + self.module.pack()
        return hdr + body

    @staticmethod
    def unpack(body):
        xid = struct.unpack('> I', body[:4])[0]
        body = body[4:]
        node = Node.unpack(body[:Node.SIZE])
        body = body[Node.SIZE:]
        module = Node.unpack(body[:Node.SIZE])
        return OPModuleStatusRequest(node, module, xid)

    def __str__(self):
        fmt = 'OP_MODULE_STATUS_REQUEST: ' + OFGMessage.__str__(self) + " request status for module %s on %s"
        return fmt % (self.module, self.node)
OP_MESSAGES.append(OPModuleStatusRequest)

class OPModuleStatusReply(OFGMessage):
    @staticmethod
    def get_type():
        return 0xF4

    def __init__(self, node, module, status, xid=0):
        OFGMessage.__init__(self, xid)
        self.node = node
        self.module = module
        self.status = str(status)

    def length(self):
        return OFGMessage.SIZE + 2 * Node.SIZE + len(self.status) + 1

    def pack(self):
        hdr = OFGMessage.pack(self)
        body = self.node.pack() + self.module.pack()
        body += struct.pack('> %us' % (len(self.status)+1), self.status)
        return hdr + body

    @staticmethod
    def unpack(body):
        raise Exception('OPModuleStatusReply.unpack() not implemented (one-way message)')

    def __str__(self):
        fmt = 'OP_MODULE_STATUS_REPLY: ' + OFGMessage.__str__(self) + " status for module %s on %s: %s"
        return fmt % (self.module, self.node, self.status)
OP_MESSAGES.append(OPModuleStatusReply)

OP_PROTOCOL = LTProtocol(OFG_MESSAGES + OP_MESSAGES, 'H', 'B')

def run_op_server(port, recv_callback):
    """Starts a server which listens for Open Pipes clients on the specified port.

    @param port  the port to listen on
    @param recv_callback  the function to call with received message content
                         (takes two arguments: transport, msg)

    @return returns the new LTTwistedServer
    """
    from ltprotocol.ltprotocol import LTTwistedServer
    server = LTTwistedServer(OP_PROTOCOL, recv_callback)
    server.listen(port)
    reactor.run()

def test():
    # simply print out all received messages
    def print_ltm(xport, ltm):
        if ltm is not None:
            print 'recv: %s' % str(ltm)
            t = ltm.get_type()
            if t==LinksAdd.get_type() or t==LinksDel.get_type():
                # got request to add/del a link: tell the GUI we've done so
                xport.write(OP_PROTOCOL.pack_with_header(ltm))

    from ltprotocol.ltprotocol import LTTwistedServer
    server = LTTwistedServer(OP_PROTOCOL, print_ltm)
    server.listen(OFG_DEFAULT_PORT)

    # when the gui connects, tell it about the modules and nodes
    def new_conn_callback(conn):
        modules = [
            OPModule(Node.TYPE_MODULE_HW, 1, "MAC Lookup"),
            OPModule(Node.TYPE_MODULE_HW, 2, "TTL Decrement"),
            OPModule(Node.TYPE_MODULE_HW, 3, "TTL Decrement (FAULTY)"),
            OPModule(Node.TYPE_MODULE_HW, 4, "Route Lookup"),
            OPModule(Node.TYPE_MODULE_HW, 5, "Checksum Update"),
            OPModule(Node.TYPE_MODULE_HW, 6, "TTL / Checksum Validate"),
            OPModule(Node.TYPE_MODULE_SW, 100, "TTL / Checksum Validate"),
            OPModule(Node.TYPE_MODULE_SW, 101, "Compar-ison Module"),
            ]
        server.send_msg_to_client(conn, OPModulesAdd(modules))

        nodes = [
            Node(Node.TYPE_IN,       111),
            Node(Node.TYPE_OUT,      999),
            Node(Node.TYPE_NETFPGA, 1000),
            Node(Node.TYPE_NETFPGA, 1001),
            Node(Node.TYPE_NETFPGA, 1002),
            Node(Node.TYPE_NETFPGA, 1003),
            Node(Node.TYPE_NETFPGA, 1004),
            Node(Node.TYPE_NETFPGA, 1005),
            Node(Node.TYPE_LAPTOP,  2000),
            Node(Node.TYPE_LAPTOP,  2001),
            Node(Node.TYPE_LAPTOP,  2002),
            ]
        server.send_msg_to_client(conn, NodesAdd(nodes))

        server.send_msg_to_client(conn, OPTestInfo("hello world", "happy world"))

        # tell the gui the route lookup module on netfpga 1000 works
        n = Node(Node.TYPE_NETFPGA, 1000)
        m = Node(Node.TYPE_MODULE_HW, 4)
        server.send_msg_to_client(conn, OPModuleStatusReply(n, m, "it works!"))

    server.new_conn_callback = new_conn_callback
    reactor.run()

if __name__ == "__main__":
    test()
