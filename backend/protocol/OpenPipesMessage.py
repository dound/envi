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

class OPNode(Node):
    NAME_LEN = 32
    DESC_LEN = 128
    SIZE = Node.SIZE + NAME_LEN + DESC_LEN

    def __init__(self, node_type, node_id, name, desc):
        Node.__init__(self, node_type, node_id)
        self.name = str(name)
        self.desc = str(desc)

    def pack(self):
        return Node.pack(self) + struct.pack('> %us%us' % (OPNode.NAME_LEN, OPNode.DESC_LEN), self.name, self.desc)

    @staticmethod
    def unpack(buf):
        node_type = struct.unpack('> H', buf[:2])[0]
        buf = buf[2:]
        node_id = struct.unpack('> Q', buf[:8])[0]
        buf = buf[8:]
        name = struct.unpack('> %us' % OPNode.NAME_LEN, buf[:OPNode.NAME_LEN])[0][:-1]
        buf = buf[OPNode.NAME_LEN:]
        desc = struct.unpack('> %us' % OPNode.DESC_LEN, buf[:OPNode.NAME_DESC])[0][:-1]
        return OPNode(node_type, node_id, name, desc)

    def __str__(self):
        return Node.__str__(self) + ' name=%s desc=%s' % (self.name, self.desc)

class OPNodesList(OFGMessage):
    def __init__(self, nodes, xid=0):
        OFGMessage.__init__(self, xid)
        self.nodes = nodes

    def length(self):
        return OFGMessage.SIZE + len(self.nodes) * OPNode.SIZE

    def pack(self):
        return OFGMessage.pack(self) + ''.join([n.pack() for n in self.nodes])

    @staticmethod
    def unpack(body):
        xid = struct.unpack('> I', body[:4])[0]
        body = body[4:]
        num_nodes = len(body) / OPNode.SIZE
        nodes = []
        for _ in range(num_nodes):
            nodes.append(OPNode.unpack(body[OPNode.SIZE:]))
            body = body[:OPNode.SIZE]
        return OPNodesList(nodes, xid)

    def __str__(self):
        return OFGMessage.__str__(self) + ' nodes=[%s]' % ''.join([str(n) + ',' for n in self.nodes])

class OPNodesAdd(OPNodesList):
    @staticmethod
    def get_type():
        return 0xF2

    def __init__(self, nodes, xid=0):
        OPNodesList.__init__(self, nodes, xid)

    @staticmethod
    def unpack(body):
        return OPNodesList.unpack(body)

    def __str__(self):
        return 'NODES_ADD: ' + OPNodesList.__str__(self)
OFG_MESSAGES.append(OPNodesAdd)

class OPNodesDel(OPNodesList):
    @staticmethod
    def get_type():
        return 0xF3

    def __init__(self, nodes, xid=0):
        OPNodesList.__init__(self, nodes, xid)

    @staticmethod
    def unpack(body):
        return OPNodesList.unpack(body)

    def __str__(self):
        return 'NODES_DEL: ' + OPNodesList.__str__(self)
OFG_MESSAGES.append(OPNodesDel)

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

    def __init__(self, node_type, node_id, name, ports):
        Node.__init__(self, node_type, node_id)
        self.name = str(name)
        self.ports = ports

    def length(self):
        port_size = 0
        for p in self.ports:
            port_size += p.length()
        return OPModule.SIZE + 2 + port_size

    def pack(self):
        return Node.pack(self) + struct.pack('> %us H' % OPModule.NAME_LEN, self.name, len(self.ports)) + \
                ''.join([p.pack() for p in self.ports])

    @staticmethod
    def unpack(buf):
        node_type = struct.unpack('> H', buf[:2])[0]
        buf = buf[2:]
        node_id = struct.unpack('> Q', buf[:8])[0]
        buf = buf[8:]
        name = struct.unpack('> %us' % OPModule.NAME_LEN, buf[:OPModule.NAME_LEN])[0][:-1]
        buf = buf[OPModule.NAME_LEN:]
        num_ports = struct.unpack('> H', buf[:2])[0]
        buf = buf[2:]
        ports = []
        for _ in range(num_ports):
            port = OPModulePort.unpack(buf)
            ports.append(port)
            buf = buf[port.length():]
        return OPModule(node_type, node_id, name, ports)

    def __str__(self):
        return Node.__str__(self) + ' ports=[%s]' % ''.join([str(p) + ',' for p in self.ports])

class OPModulesList(OFGMessage):
    def __init__(self, modules, xid=0):
        OFGMessage.__init__(self, xid)
        self.modules = modules

    def length(self):
        module_size = 0
        for m in self.modules:
            module_size += m.length()
        return OFGMessage.SIZE + module_size

    def pack(self):
        return OFGMessage.pack(self) + struct.pack('> H', len(self.modules)) + \
                ''.join([m.pack() for m in self.modules])

    @staticmethod
    def unpack(body):
        xid = struct.unpack('> I', body[:4])[0]
        body = body[4:]
        num_modules = struct.unpack('> H', body[:2])[0]
        body = body[2:]
        modules = []
        for _ in range(num_modules):
            module = OPModule.unpack(body)
            modules.append(module)
            body = body[module.length():]
        return OPModulesList(modules, xid)

    def __str__(self):
        return OFGMessage.__str__(self) + ' modules=[%s]' % ''.join([str(m) + ',' for m in self.modules])

class OPModulesAdd(OPModulesList):
    @staticmethod
    def get_type():
        return 0xF4

    def __init__(self, modules, xid=0):
        OPModulesList.__init__(self, modules, xid)

    @staticmethod
    def unpack(body):
        return OPModulesList.unpack(body)

    def __str__(self):
        return 'MODULES_ADD: ' + OPModulesList.__str__(self)
OFG_MESSAGES.append(OPModulesAdd)

class OPModulesDel(OPModulesList):
    @staticmethod
    def get_type():
        return 0xF5

    def __init__(self, dpids, xid=0):
        OPModulesList.__init__(self, dpids, xid)

    @staticmethod
    def unpack(body):
        return OPModulesList.unpack_child(body)

    def __str__(self):
        return 'MODULES_DEL: ' + OPModulesList.__str__(self)
OFG_MESSAGES.append(OPModulesDel)

class OPModulePort(object):
    NAME_LEN_MAX = 32
    DESC_LEN_MAX = 128

    def __init__(self, port_id, name, desc):
        self.id = port_id
        self.name = str(name)[0:OPModulePort.NAME_LEN_MAX]
        self.desc = str(desc)[0:OPModulePort.DESC_LEN_MAX]

    def length(self):
        return 2 + 1 + len(self.name) + 1 + 1 + len(self.desc) + 1

    def pack(self):
        name_len = len(self.name) + 1
        desc_len = len(self.desc) + 1
        return struct.pack('> H B %us B %us' % (name_len, desc_len), self.id, name_len, self.name, desc_len, self.desc)

    @staticmethod
    def unpack(buf):
        port_id = struct.unpack('> H', buf[:2])[0]
        buf = buf[2:]
        name_len = struct.unpack('> B', buf[:1])[0]
        buf = buf[1:]
        name = struct.unpack('> %us' % name_len, buf[:name_len])[0][:-1]
        buf = buf[name_len:]
        desc_len = struct.unpack('> B', buf[:1])[0]
        buf = buf[1:]
        desc = struct.unpack('> %us' % desc_len, buf[:desc_len])[0][:-1]
        return OPModulePort(port_id, name, desc)

    def __str__(self):
        fmt = "OP_MODULE_PORT: id=%d name='%s' desc='%s'"
        return fmt % (self.id, self.name, self.desc)

class OPModuleStatusRequest(OFGMessage):
    @staticmethod
    def get_type():
        return 0xF6

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
        return 0xF7

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

class OPModuleInstance(object):
    SIZE = 8 + 8

    def __init__(self, module_id, node_id):
        self.module_id = module_id
        self.node_id = node_id

    def pack(self):
        return struct.pack('> QQ', self.module_id, self.node_id)

    @staticmethod
    def unpack(body):
        module_id = struct.unpack('> Q', body[:8])[0]
        body = body[8:]
        node_id = struct.unpack('> Q', body[:8])[0]
        body = body[8:]
        return OPModuleInstance(module_id, node_id)

    def __str__(self):
        return 'module_id=%d node_id=%d' % (self.module_id, self.node_id)

class OPModuleInstancesList(OFGMessage):
    def __init__(self, instances, xid=0):
        OFGMessage.__init__(self, xid)
        self.instances = instances

    def length(self):
        return OFGMessage.SIZE + len(self.instances) * OPModuleInstance.SIZE

    def pack(self):
        return OFGMessage.pack(self) + ''.join([i.pack() for i in self.instances])

    @staticmethod
    def unpack(body):
        xid = struct.unpack('> I', body[:4])[0]
        body = body[4:]
        num_instances = len(body) / OPModuleInstance.SIZE
        instances = []
        for _ in range(num_instances):
            instances.append(OPModuleInstance.unpack(body[:OPModuleInstance.SIZE]))
            body = body[OPModuleInstance.SIZE:]
        return OPModuleInstancesList(instances, xid)

    def __str__(self):
        return OFGMessage.__str__(self) + ' instances=[%s]' % ''.join([str(i) + ',' for i in self.instances])

class OPModuleInstancesAdd(OPModuleInstancesList):
    @staticmethod
    def get_type():
        return 0xF8

    def __init__(self, instances, xid=0):
        OPModuleInstancesList.__init__(self, instances, xid)

    @staticmethod
    def unpack(body):
        return OPModuleInstancesList.unpack(body)

    def __str__(self):
        return 'MODULE_INSTANCES_ADD: ' + OPModuleInstancesList.__str__(self)
OFG_MESSAGES.append(OPModuleInstancesAdd)

class OPModuleInstancesDel(OPModuleInstancesList):
    @staticmethod
    def get_type():
        return 0xF9

    def __init__(self, instances, xid=0):
        OPModuleInstancesList.__init__(self, instances, xid)

    @staticmethod
    def unpack(body):
        return OPModuleInstancesList.unpack(body)

    def __str__(self):
        return 'MODULE_INSTANCES_DEL: ' + OPModuleInstancesList.__str__(self)
OFG_MESSAGES.append(OPModuleInstancesDel)

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
            OPModule(Node.TYPE_MODULE_HW, 1, "MAC Lookup", []),
            OPModule(Node.TYPE_MODULE_HW, 2, "TTL Decrement", []),
            OPModule(Node.TYPE_MODULE_HW, 3, "TTL Decrement (FAULTY)", []),
            OPModule(Node.TYPE_MODULE_HW, 4, "Route Lookup", []),
            OPModule(Node.TYPE_MODULE_HW, 5, "Checksum Update", []),
            OPModule(Node.TYPE_MODULE_HW, 6, "TTL / Checksum Validate", []),
            OPModule(Node.TYPE_MODULE_SW, 100, "TTL / Checksum Validate", []),
            OPModule(Node.TYPE_MODULE_SW, 101, "Compar-ison Module", []),
            ]
        server.send_msg_to_client(conn, OPModulesAdd(modules))

        nodes = [
            OPNode(Node.TYPE_IN,       111, "Input", "Input"),
            OPNode(Node.TYPE_OUT,      999, "Output", "Output"),
            OPNode(Node.TYPE_NETFPGA, 1000, "NetFPGA", "NetFPGA"),
            OPNode(Node.TYPE_NETFPGA, 1001, "NetFPGA", "NetFPGA"),
            OPNode(Node.TYPE_NETFPGA, 1002, "NetFPGA", "NetFPGA"),
            OPNode(Node.TYPE_NETFPGA, 1003, "NetFPGA", "NetFPGA"),
            OPNode(Node.TYPE_NETFPGA, 1004, "NetFPGA", "NetFPGA"),
            OPNode(Node.TYPE_NETFPGA, 1005, "NetFPGA", "NetFPGA"),
            OPNode(Node.TYPE_NETFPGA, 1006, "NetFPGA", "NetFPGA"),
            OPNode(Node.TYPE_PC,      2000, "pc1", "Core 2 Duo with 1G RAM"),
            OPNode(Node.TYPE_PC,      2001, "pc2", "Core 2 Duo with 2G RAM"),
            OPNode(Node.TYPE_PC,      2002, "pc3", "Centrino with 1G RAM"),
            OPNode(Node.TYPE_PC,      2003, "pc4", "Centrino with 1G RAM"),
            ]
        server.send_msg_to_client(conn, OPNodesAdd(nodes))

        nodes = [
            OPNode(Node.TYPE_NETFPGA, 1006, "NetFPGA", "NetFPGA"),
            OPNode(Node.TYPE_PC,      2003, "pc2", "Core 2 Duo with 2G RAM"),
            ]
        server.send_msg_to_client(conn, OPNodesDel(nodes))

        server.send_msg_to_client(conn, OPTestInfo("hello world", "happy world"))

        # tell the gui the route lookup module on netfpga 1000 works
        n = Node(Node.TYPE_NETFPGA, 1000)
        m = Node(Node.TYPE_MODULE_HW, 4)
        server.send_msg_to_client(conn, OPModuleStatusReply(n, m, "it works!"))

    server.new_conn_callback = new_conn_callback
    reactor.run()

if __name__ == "__main__":
    test()
