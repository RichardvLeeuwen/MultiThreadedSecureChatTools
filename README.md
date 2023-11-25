## Personal multi-threaded and secure chat tools

In this personal project, I attempt to create a chat server that can handle multiple clients in a secure manner. The chat server allows client to communicate by creating and/or joining a temporary chatroom with other clients. By default, all clients are put in a large global chatroom. Each chatroom will be managed by an individual thread. Clients by default broadcast their messages to all other clients in the same chatroom, but also have the option to send private, direct messages. Clients can register accounts and login.  The endgoal is for all communication and database storage to occur in a secure manner.

