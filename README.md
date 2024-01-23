# Personal multi-threaded and secure chat tools

In this personal project, I attempt to create a chat server that can handle multiple clients in a secure manner. The chat server allows client to communicate by creating and/or joining a temporary chatroom with other clients. There is a single server thread that accepts new incoming connections. Once accepted, the socket associated with the client is passed in to the login thread. Once logged in, all clients are put, by default, in a large global chatroom. This global chatroom manages all other chatrooms. Each chatroom is managed by an individual thread that processes all incoming messages and commands. Further, each chatroom is responsible for their own Client threads that are used to receive messages and commands from individual clients. Clients by default broadcast their messages to all other clients in the same chatroom, but also have the option to send private, direct messages. 

The endgoal is for all communication to occur in a secure manner. For this purpose, I use TLS sockets to verify the identity of the server and to encrypt the connection between server and client. For development purposes, I used the Java keytool to create my own self-signed certificates and keystore.

## Currently accepted commands
/users: retrieves the names of all users currently in the chatroom\
/whisper [target] [message]: sends a personal message to target only the target is able to read. Target must be in the same chatroom.\
/create [chatroomname]: creates a new chatroom that the user automatically joins. New chatrooms can only be created from the global chatroom.\
/join [chatroomname]: join an existing chatroom. Joining chatrooms can only be created from the global chatroom.\
/chatrooms: retrieves the names of all chatrooms currently active, only works in global\
/leave: leaves the chatroom and returns to the global chatroom.\
/exit: exists the client application.\

## Current Bugs and ToDos
- Check for and fix concurrency issues (especially after latest changes with how client threads get passed around)\
- Fix shutting down sockets issues\
- Write unit tests\
- Implement SQL database to store user accounts, chatmessages and other logging needs\
