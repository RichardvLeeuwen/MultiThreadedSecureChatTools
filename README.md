# Personal multi-threaded and secure chat tools

In this personal project, I attempt to create a chat server that can handle multiple clients in a secure manner. The chat server allows client to communicate by creating and/or joining a temporary chatroom with other clients. By default, all clients are put in a large global chatroom. Each chatroom will be managed by an individual thread. Clients by default broadcast their messages to all other clients in the same chatroom, but also have the option to send private, direct messages. Clients can register accounts and login.  The endgoal is for all communication and database storage to occur in a secure manner.

Currently, on the serverside, each client has its own separate thread for receiving messages, and each client is put into a chatroom, alone or with others. Default chatroom is global. Each chatroom is its own thread that is capable of accepting input from the client threads through a concurrent queue, which can vary from exercising a command or sending a chat messages to other clients. Whenever a new user connection gets accepted, the user is passed onto a login thread, from where, upon successful login, it is sent to the global chatroom thread.

## Currently accepted commands
/users: retrieves the names of all users currently in the chatroom\
/whisper [target] [messages]: sends a personal message to target only the target is able to read. Target must still be in the same chatroom.\
/cheatechatroom [chatroomname]: creates a new chatroom that the user automatically joins. New chatrooms can only be created from the global chatroom.\
/chatrooms: retrieves the names of all chatrooms currently active