var WebSocket = require('faye-websocket'),
    http      = require('http');

var server = http.createServer();
var clients = new Array();

server.on('upgrade', function(request, socket, body) {
  if (WebSocket.isWebSocket(request)) {
    var ws = new WebSocket(request, socket, body); 
    clients.push(ws);
    console.log('new connection: there are '+clients.length+' clients');
    ws.on('message', function(event) {
      console.log('message', event.data);
      for (var i = clients.length - 1; i >= 0; i--) {
        if(clients[i]!=ws){
            clients[i].send(event.data);
        }
      };
      
    });

    ws.on('close', function(event) {
      console.log('close', event.code, event.reason);
      ws = null;
    });

  }
});
console.log('server running');
server.listen(8000);
