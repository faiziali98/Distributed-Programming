import Pyro4
import sys
import hashlib 
from threading import Event, Thread

@Pyro4.expose
class MessagePassing(object):
    def __init__(self, is_first):
        self.is_first = is_first
        self.nPeers = 0
        self.succ = {'h':0}
        self.c_node = {}

        self.c_node['i'] = self.ipaddr = "127.0.0.1" 
        self.c_node['p'] = self.port = input("What is your port? ")
        self.hashval = hashlib.sha256((self.ipaddr+self.port).encode()).hexdigest()
        self.suchandle = None 
        
        self.notes = {}
        self.c_node['h'] = self.hashval = int(self.hashval, 16)

        print(self.hashval)
        
    def startConn(self, msg):
        to_ret = None 
        print(msg)
        if msg[0] == "connReq":
            if self.nPeers == 1:
                to_ret = (self.c_node, "succ", self.nPeers)
                self.updateSucc(msg[1])
            elif self.hashval > msg[1]['h']:
                to_ret = (self.c_node, "succ", self.nPeers)
            elif self.hashval > self.succ['h']:
                to_ret = (self.succ, "succ", self.nPeers)
                self.updateSucc(msg[1])
            else:
                print("Client Forwarded")
                to_ret = (self.succ, "contactthis")

        # print(self.succ)
        return(to_ret)
    
    def updateSucc(self, succs):
        self.succ = succs
        self.suchandle = Pyro4.core.Proxy('PYRO:Server@' + succs['i'] + ":" + succs['p'])

    def post_msg(self, msg):
        if msg[0] == "peeradded":
            if self.hashval != msg[-2]['h']:
                self.nPeers = msg[1]
                if self.succ['h'] == msg[-1]['h']:
                    self.updateSucc(msg[-2])
                else:
                    self.suchandle.post_msg(msg)
        print (msg)
        print(self.succ)

    def ServerSide(self):
        Pyro4.Daemon.serveSimple({
            self: 'Server',
        }, host="127.0.0.1", port=int(self.port), ns=False, verbose=False)

    def insertNote(self, h, s, b):
        if h in self.notes.keys():
            self.notes[h] = self.notes[h] + b
        else:
            self.notes[h] = s + " " + b

    def remoteInsertNote(self, hval, sub, body):
        if self.hashval > hval:
            self.insertNote(hval, sub, body)
        else:
            self.succ.remoteInsertNote(hval, sub, body)

    def ClientSide(self):
        if not self.is_first:
            ipAdr = "127.0.0.1"   
            port = input("Whom to connect to? ").strip()
            
            while True:
                try:
                    remote_server = Pyro4.core.Proxy('PYRO:Server@' + ipAdr + ":" + port)
                    n_info = remote_server.startConn(["connReq", self.c_node])
                except Exception:
                    print("".join(Pyro4.util.getPyroTraceback()))

                if n_info[1] == "succ":
                    self.updateSucc(n_info[0])
                    self.nPeers = n_info[2] + 1
                    break
                elif n_info[1] == "contactthis":
                    prev = remote_server
                    ipAdr = n_info[0]['i']
                    port = n_info[0]['p']

            self.suchandle.post_msg(["peeradded", self.nPeers, self.c_node, self.succ])
        else:
            self.nPeers = 1
        
        print(self.succ)

        # while True:
        #     print ("\n1. Getting a note\n2.Posting a note\n3.Departing the system\n")
        #     opt = int(input("What would you like to do? "))

        #     if opt == 2:
        #         sub = input("Enter Subject: ")
        #         body = "Hey All!!!!"
        #         hval = int(hashlib.sha256((sub).encode()).hexdigest(), 16)
        #         self.remoteInsertNote(hval, sub, body)

    def startpeer(self):
        Thread(target=self.ServerSide).start()
        print("Server Started")
        self.ClientSide()

if __name__ == "__main__":
    if (sys.argv[1] == "first"):
        program = MessagePassing(True)
    else:
        program = MessagePassing(False)

    program.startpeer()
    