# File Sharing Service

A distributed multithreaded file sharing service, using peer-to-peer architecture. The included functionality consists of:

  ~ users get to choose unique username for others to see
  
  ~ users can share any of the files on their file system, as long as its name is not the same as some other's registered file
  name
  
  ~ users can unregister previously shared files
  
  ~ users can see everything others share
  
  ~ users can download any shared file from anyone
  
  To run the server, one needs to pass a port for it as command argument.
  To run the client, one needs to pass unique username, port for its mini server, port for the peer, server's port and/or server's
  address.
