var express = require('express');	
var mongoose = require("mongoose");

var router = express.Router({mergeParams: true});

var acl = require("express-dynacl");

var etlFilter = require("../middleware/etl-filter");

var Payment = require("../models/expenditures").Payment;

router.get("/", etlFilter({visible:true}), acl("profile-payments", "list"), (req,res,next) => {

	var query = {
		profile: req.params.profile,
		etl: {$in: req.etls}
	};

	if(req.query.dateFrom || req.query.dateTo){
		query.date = {};
		if(req.query.dateFrom) query.date.$gte = new Date(req.query.dateFrom);
		if(req.query.dateTo) query.date.$lt = new Date(req.query.dateTo);
	}

	var options = {};
	options.page = req.query.page || 1;
	if(req.query.sort) options.sort = req.query.sort;
	options.limit = req.query.limit ? Math.min(100,Number(req.query.limit)) : 20;

	Payment.paginate(query, options)
		.then(payments => res.json(payments ? payments : []))
		.catch(err => next(err));
});


router.get("/months", etlFilter({visible:true}), acl("profile-payments", "list"), (req,res,next) => {

	let aggregation = [
		{
			$match: { profile: mongoose.Types.ObjectId(req.params.profile), etl: {$in: req.etls}, date: {$ne: null, $exists: true} }
		},
		{
			$project: {year: { $year: "$date" }, month: { $month: "$date" }}
		},
		{ "$group": {
			"_id": null, 
			"months": { "$addToSet": { "year": "$year", "month": "$month" }}
		}}
	];

	Payment.aggregate(aggregation)
		.then(result => {
			if(!result[0]) return res.json([]);
			res.json(result[0].months);
		})
		.catch(err => next(err));
	
});

router.get("/:year/csv", etlFilter({visible:true}), acl("profile-payments", "list"), (req,res,next) => {
	
	Payment.find({profile: req.params.profile, year: req.params.year, etl: req.etls}).populate("event","_id srcId").lean()
		.then(payments => {
		
			res.statusCode = 200;
			res.setHeader("Content-disposition", "attachment; filename=" + req.params.profile + "-" + req.params.year + ".payments.csv");
  		res.setHeader('Content-Type', 'text/csv');
			
			var header = ['profile','year','event','eventSrcId','type','item','paragraph','date','amount','counterpartyId','counterpartyName','description'];
			
			// UTF BOM for MS EXCEL
			res.write("\ufeff");
		
			// write header
			res.write(header.map(field => '"' + field + '"').join(";") + '\r\n');
			
			// write data
			payments.forEach(payment => {
				
				payment.date = payment.date && payment.date.toISOString ? payment.date.toISOString() : null;
				if(payment.event){
					payment.eventSrcId = payment.event.srcId;
					payment.event = payment.event._id;
				}
				else{
					payment.eventSrcId = null;
					payment.event = null;
				}
				
				res.write(makeCSVLine(header.map(field => payment[field])));
			});
		
			res.end();
		})
		.catch(err => res.send(err.message));
	
});

function makeCSVLine(array){
	
	// clean and format values
	array = array.map(value => makeCSVItem(value));
	
	return array.join(";") + "\r\n";
}

function makeCSVItem(value){
	
	// number, replace , to . and no quotes
	if(typeof(value) === 'number' || (typeof(value) === 'string' && value.match(/^\d+([\.,]\d+)?$/))){
		 value = value + "";
		 return value.replace(",",".");
	}
	
	// boolean, replace to binary 0/1
	if(typeof(value) === "boolean") return value ? 1 : 0;
		
	// empty values
	if(Number.isNaN(value) || value === null) return "";
	
	// string, escape quotes and encapsulate in quotes
	value = value + "";
	return "\"" + value.replace("\"","\"\"") + "\"";
	
}

module.exports = router;