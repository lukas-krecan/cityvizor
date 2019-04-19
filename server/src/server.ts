console.log("Starting CityVizor Server");
console.log("Node version: " + process.version);

const http = require('http');
const express = require('express');

const config = require("../config");

/* SET UP ROUTING */
const app = express();

/* CORS FOR DEVELOPMENT */
if(config.cors.enabled){
  const cors = require("cors");
  app.use(cors(config.cors));  
  console.log("[SERVER] CORS enabled");
}

// polyfill before express allows for async middleware
require('express-async-errors');

if (config.server.compression) {
	const compression = require('compression');
	app.use(compression());
}

// parse body
const bodyParser = require("body-parser");
app.use(bodyParser.json({})); // support json encoded bodies
app.use(bodyParser.urlencoded({
	extended: true,
	limit: "10000kb"
})); // support urlencoded bodies

/* FILE STORAGE */
require("./file-storage");

/* DATABASE */
require("./db");

/* AUTHENTICATION */
const jwt = require('express-jwt');
app.use(jwt(config.jwt));

/* ACCESS CONTROL */
const acl = require("express-dynacl");
const aclOptions = {
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
};
acl.config(aclOptions);

/* ROUTING */
app.use(require("./routers"));

// error handling
app.use(require("./middleware/error-handler"));


/* SET UP SERVER */
const host = config.server.host || "127.0.0.1";
const port = config.server.port || 80;

http.createServer(app).listen(port, host, () => {
	console.log('[SERVER] Listening on ' + host + ':' + port + '!');
});
