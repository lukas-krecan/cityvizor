console.log("Starting CityVizor Server");
console.log("Node version: " + process.version);

var http = require('http');
var https = require('https');
var fs = require('fs');
var express = require('express');

var config = require("./config/config.js");

/* SET UP ROUTING */
var app = express();
console.log("Express running in " + app.get('env') + " environment");

// set cors so that we can access from localhost
var cors = require("cors");
app.use(cors());

// polyfill before express allows for async middleware
require('express-async-errors');

if (config.server.compression) {
	var compression = require('compression');
	app.use(compression());
}

// parse body
var bodyParser = require("body-parser");
app.use(bodyParser.json({})); // support json encoded bodies
app.use(bodyParser.urlencoded({
	extended: true,
	limit: "10000kb"
})); // support urlencoded bodies

var mongoose = require('mongoose');
mongoose.plugin(require('mongoose-write-stream'));
mongoose.plugin(require('mongoose-paginate'));
mongoose.Promise = global.Promise;

mongoose.connect(config.database.uri, { useNewUrlParser: true })
	.then(() => console.log("Connected to database " + config.database.db))
	.catch(err => {
		console.error("Error when connectiong to DB " + config.database.db + ": " + err.message); // if not connected the app will not throw any errors when accessing DB models, better to fail hard and fix
    
    console.log("Will retry connection in 10s...");
    setTimeout(() => {
      mongoose.connect(config.database.uri, { useNewUrlParser: true })
        .then(() => console.log("Connected to database " + config.database.db))
	      .catch(err => console.error("Error when reconnectiong to DB " + config.database.db + ": " + err.message));
    }, 10000);
    
	});




// configure express-jwt
var jwt = require('express-jwt');
app.use(jwt(config.jwt));

// configure DynACL
var acl = require("express-dynacl");
var aclOptions = {
	roles: {
		"guest": require("./acl/guest"),
		"user": require("./acl/user"),
		"profile-manager": require("./acl/profile-manager"),
		"profile-admin": require("./acl/profile-admin"),
		"admin": require("./acl/admin")
	},
	defaultRoles: ["guest"],
	userRoles: ["user"],
	logConsole: true
}
acl.config(aclOptions);

/* Mongo Express Database viewer */
if (config.mongoExpress.enable) {
	var mongo_express = require('mongo-express/lib/middleware');
	var mongo_express_config = require('./config/mongo-express-config.js');
	app.use('/db', mongo_express(mongo_express_config))
	console.log("Mongo Express accessible at /db");
}

/* SET UP ROUTES */
app.use("/api", require("./routers/api"));

app.use("/api/search",require("./routers/search"));

app.use("/exports/v1", require("./routers/exports-v1"));

// error handling
app.use(require("./middleware/error-handler"));


/* SET UP SERVER */
let host = config.server.host || "127.0.0.1";
let port = config.server.port || 80;

if (config.ssl.enable) {

	// start https server
	https.createServer(config.ssl, app).listen(443, host, function() {
		console.log('CityVizor Server listening on ' + host + ':443!')
	});

	let redirectPort = config.ssl.redirectPort || port || 80;

	// Redirect to https
	if (config.ssl.redirect && redirectPort) {
		http.createServer(function(req, res) {
			res.writeHead(301, {
				"Location": "https://" + req.headers.host + req.url
			});
			res.end();
		}).listen(redirectPort, host, function() {
			console.log('CityVizor Server redirecting from ' + host + ':' + port + ' to ' + host + ':443!')
		});
	}

} else {

	http.createServer(app).listen(port, host, function() {
		console.log('CityVizor Server listening on ' + host + ':' + port + '!');
	});

}
