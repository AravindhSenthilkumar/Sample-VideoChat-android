var WebSocket = require('faye-websocket'),
    ws        = new WebSocket.Client('ws://192.168.0.116:8000');

ws.on('open', function(event) {
  console.log('open');
  	setTimeout((function() {
  		ws.send('Hello, world! i am '+ws.id);
	}), 2000);
});

ws.on('message', function(event) {
  	console.log('message', event.data);
  	setTimeout((function() {
  		ws.send('Hello, world! i am '+ws.id);
	}), 2000);
});

ws.on('close', function(event) {
  console.log('close', event.code, event.reason);
  ws = null;
});