"""Defines the OpenFlow GUI-based OpenPipes protocol."""

import struct

from twisted.internet import reactor

from OFGMessage import OFG_DEFAULT_PORT, OFGMessage, OFG_MESSAGES, Node, create_ofg_server
from ltprotocol.ltprotocol import LTProtocol

OP_MESSAGES = []

class OPMoveModule(OFGMessage):
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
    SIZE = Node.SIZE + OPModule.NAME_LEN

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
        return 0x11

    def __init__(self, modules, xid=0):
        OPModulesList.__init__(self, modules, xid)

    def __str__(self):
        return 'MODULES_ADD: ' + OPModulesList.__str__(self)
OFG_MESSAGES.append(OPModulesAdd)

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
    def print_ltm(_, ltm):
        if ltm is not None:
            print 'recv: %s' % str(ltm)

    server = create_ofg_server(OFG_DEFAULT_PORT, print_ltm)
    reactor.run()

if __name__ == "__main__":
    test()
