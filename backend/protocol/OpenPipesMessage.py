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
