#########################################################
# A DHT created on pyro4
#  
# How this implementation is different from the real DHT
# 1. Real DHT has both pred and succ
# 2. Real DHT, new node is detected by frequently asking succ for its pred
# 3. First new node update finger table and then it asks to 
# those entries to update their finger table.
# 
######################################################### 
# By: Faizan Safdar Ali
# For: Distributed Computing assignment
#########################################################


import Pyro4
import sys
import hashlib 
from socket import *
from threading import Event, Thread
import operator
from Pyro4 import errors, socketutil, util, constants, message, futures

@Pyro4.expose
class MessagePassing(object):
    ''' Class which will be binded for RMI'''
    # Initialization
    def __init__(self, is_first):
        # Initialize variables
        #
        # See if this is the only node
        self.is_first = is_first
        # Number of peers in the DHT
        self.nPeers = 0
        # Successors information
        self.succ = {'h':0}
        # Self informtation
        self.c_node = {}
        # Predecesor
        self.minval = 0

        # Setting the IP of self
        self.c_node['i'] = self.ipaddr = "127.0.0.1" 
        # Set self port
        self.c_node['p'] = self.port = input("What is your port? ")
        # Find current hash value
        self.hashval = hashlib.sha256((self.ipaddr+self.port).encode()).hexdigest()
        # Successor handle
        self.suchandle = None

        # Finger Tables initialization
        self.fingerTable = {}
        # Notes initialization
        self.notes = {}
        self.c_node['h'] = self.hashval = int(self.hashval, 16)
        
        print(self.hashval)
        self.resetFt()
        
    def resetFt(self):
        '''Set the initial values of the finger table'''

        for x in range(0, 257):
            in_key = ((2**x) + self.hashval)%(2**257)
            self.fingerTable[in_key] = self.c_node

    def startConn(self, msg):
        '''Start the connection request'''

        to_ret = None 
        if msg[0] == "connReq":
            # If only one peer, then this one is the successor and 
            # pred of new node
            if self.nPeers == 1:
                to_ret = (self.c_node, "succ", self.nPeers)
                self.updateSucc(msg[1])
            # If this is the node then keep it here
            elif self.hashval >= msg[1]['h'] and msg[1]['h'] > self.minval:
                to_ret = (self.c_node, "succ", self.nPeers)
            # Else if it is the last node, than next one will be the 
            # hosting server
            elif self.hashval > self.succ['h'] and msg[1]['h'] > self.minval:
                to_ret = (self.succ, "succ", self.nPeers)
                self.updateSucc(msg[1])
            # Forward using the fingettable
            else:
                print("Client Forwarded")
                to_ret = self.findToSend(msg[1]['h']).startConn(msg)

        return(to_ret)
    
    def updateSucc(self, succs):
        ''' Update the successor and connect to its RMI'''

        self.succ = succs
        # Proxy is the function in the daemon which helps the connection
        self.suchandle = Pyro4.core.Proxy('PYRO:Server@' + succs['i'] + ":" + succs['p'])

    def post_msg(self, msg):
        ''' This is the function to do random message passing'''

        # If peer is adding then this condition
        if msg[0] == "peeradded":
            self.nPeers = msg[1]
            self.minval = msg[-1]
            if self.hashval != msg[-3]['h']:
                if self.succ['h'] == msg[-2]['h']:
                    self.updateSucc(msg[-3])
                if self.hashval > self.succ['h']:
                    msg[-1] = 0
                else:
                    msg[-1] = self.hashval
                self.suchandle.post_msg(msg)
        # If client is leaving
        elif msg[0] == "Leaving":
            self.nPeers -= 1
            if self.nPeers != 1:
                if msg[-2]['h'] < self.hashval:
                    self.minval = msg[-1]
                self.notes.update(msg[1])
                self.suchandle.post_msg(["ClientLeft", msg[-2], self.c_node])
            else:
                self.resetFt()

        # If client is left, the succ will send the client left
        # to otehr people
        elif msg[0] == "ClientLeft":
            self.nPeers -= 1
            if self.succ['h'] != msg[1]['h']:
                self.suchandle.post_msg(msg)
            else:
                self.updateSucc(msg[-1])
                self.updateFingerTable()
                self.suchandle.post_msg(["updateFT", self.hashval])
        
        # If finger table is to be upadated, it will receive that
        # message
        elif msg[0] == "updateFT":
            if msg[1] != self.hashval:
                self.updateFingerTable()
                if self.nPeers > 2: 
                    self.suchandle.post_msg(msg)

        return True

    def findToSend(self, hval):
        # Find the server to send to using the finget table
        ft = sorted(self.fingerTable.items(), key=operator.itemgetter(0))
        maxk = None
        for f in ft:
            # See the condition 
            if f[0] > hval:
                if maxk is None:
                    maxk = f[0]
                break
            maxk = f[0]
        # Return the item from the fingettable
        item = self.fingerTable[maxk]
        # Connect to it and then return the handle
        return Pyro4.core.Proxy('PYRO:Server@' + item['i'] + ":" + item['p'])


    def LookUp(self, hval, sender):
        # Loop up is done using this
        #
        # If current is not the initiator
        if self.hashval != sender:
            # If this the right one
            if self.hashval >= hval and hval > self.minval:
                return self.c_node
            else:
                # if this is the last
                if self.hashval > self.succ['h'] and hval > self.minval:
                    return self.succ
                else:
                    return self.suchandle.LookUp(hval, sender)
        else:
            # Else return this is the node
            return self.c_node

    def insertNote(self, h, s, b, fw):
        # insert the note itself
        if h in self.notes.keys():
            self.notes[h] = self.notes[h] + b
        else:
            self.notes[h] = s + " " + b
        print(fw)
        print(self.notes[h])
        
    def remoteInsertNote(self, hval, sub, body, fw):
        # Insert the note to the remote peer
        print(self.c_node)
        if self.hashval >= hval and hval > self.minval:
            self.insertNote(hval, sub, body, fw)
        else:
            if self.hashval > self.succ['h'] and hval > self.minval:
                self.insertNote(hval, sub, body, fw)
            else:
                self.findToSend(hval).remoteInsertNote(hval, sub, body, fw)
        return True
        
    def retreiveNote(self, h, fw):
        # Get the note from self notes
        if h in self.notes.keys():
            print(fw)
            return self.notes[h]

    def remoteRetreiveNote(self, sub, fw = {'h':0}):
        # Other wise get from the remote peer
        hval = int(hashlib.sha256((sub).encode()).hexdigest(), 16)
        print(self.c_node)

        # use the look up to find the right peer
        if self.hashval >= hval and hval > self.minval:
            return self.retreiveNote(hval, fw)
        else:
            if self.hashval > self.succ['h'] and hval > self.minval:
                return self.retreiveNote(hval, fw)
            else:
                return self.suchandle.remoteRetreiveNote(sub, fw)

    def updateFingerTable(self):
        # updates the finget table
        ft = self.fingerTable

        for k in ft.keys():
            # Look up for each key in the finget table and then
            # update it with the right peer
            # Pyro does not allow loops
            cond = self.nPeers == 2 or (self.hashval > self.succ['h'] and k > self.minval)
            if cond:
                ft[k] = self.succ
            else:
                ft[k] = self.suchandle.LookUp(k, self.hashval)

        self.fingerTable = ft
        print(sorted(ft.items(), key=operator.itemgetter(0)), "\n\n")

    def ClientSide(self):
        # Client side of the peer
        if not self.is_first:
            ipAdr = "127.0.0.1"   
            # Ask the port of the already attached peer
            port = input("Whom to connect to? ").strip()

            # Connect to the peer
            try:
                remote_server = Pyro4.core.Proxy('PYRO:Server@' + ipAdr + ":" + port)
                n_info = remote_server.startConn(["connReq", self.c_node])
            except Exception:
                print("".join(Pyro4.util.getPyroTraceback()))

            # Keep connecting until get the right successor
            if n_info[1] == "succ":
                self.updateSucc(n_info[0])
                self.nPeers = n_info[2] + 1
                    
            # Update that peer is added to every other peer
            self.suchandle.post_msg(["peeradded", self.nPeers, self.c_node, self.succ, self.hashval])
            # Update the current finger table
            self.updateFingerTable()
            # Then ask others to update their finget table
            self.suchandle.post_msg(["updateFT", self.hashval])
        else:
            self.nPeers = 1
        

        # Show the menue to the users
        while True:
            print ("\n1. Getting a note\n2. Posting a note\n3. Departing the system\n")
            # ask for the option
            opt = int(input("What would you like to do? "))

            # If 2, add the note
            if opt == 2:
                sub = input("Enter Subject: ")
                body = "Hey All!!!!"
                hval = int(hashlib.sha256((sub).encode()).hexdigest(), 16)
                self.remoteInsertNote(hval, sub, body, self.c_node)
            # if 1, get the note
            elif opt == 1:
                sub = input("Enter Subject: ")
                note = self.remoteRetreiveNote(sub, self.c_node)
                if note is not None:
                    print(note)
                else:
                    print("Note not found!!")
            # if 3, depart the system
            elif opt == 3:
                if self.suchandle is not None and self.nPeers != 1:
                    self.suchandle.post_msg(["Leaving", self.notes, self.c_node, self.minval])
                break



    def ServerSide(self, d):
        # Start eh simple server, d is the daemon used to
        # shut it down later
        Pyro4.Daemon.serveSimple({
            self: 'Server',
        }, daemon = d, ns=False, verbose=False)

    def startpeer(self):
        # Start the server daemon using Ip and pOrt
        d = Pyro4.Daemon("127.0.0.1", int(self.port))
        # Run in the different thread
        Thread(target=self.ServerSide, args=(d,)).start()
        print("Server Started")
        # Start the client side
        self.ClientSide()
        # Shutdown daemon if peer wants to leave
        d.shutdown()

if __name__ == "__main__":
    # if first then run different code
    if (sys.argv[1] == "first"):
        program = MessagePassing(True)
    else:
        program = MessagePassing(False)
    # Start the peer client and server code
    program.startpeer()
    