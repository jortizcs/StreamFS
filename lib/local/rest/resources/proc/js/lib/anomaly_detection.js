var http = require('http');
var JASON = require('JASON');
//var jStat= require('jStat').jStat;
var chunktotal = "";
var buffer = new Array();
var state = new Object();

var adlib = require('./adlib.js');

function procedure(){
    for(var i=0; i<buffer.length; i++){
        var a  = new adlib();
        var ret = a.isanamolous(buffer[i].ts, buffer[i].value, buffer[i].path, 0.95);
        console.log("ret=" + JSON.stringify(ret));
    }
}

function procedure2(){
    var output = new Object();
    if(typeof state.mean == 'undefined'){
        state.mean =0;
        state.stddev=0;
        state.min = 0;
        state.max=9007199254740992;
        state.ptcnt = 0;
        state.sumsqr = 0;
        state.variance = 0;
    }

    for(var i=0; i<buffer.length; i++){

        //imprint this process with the first signal heard
        if(typeof state.sig == 'undefined' && typeof buffer[i].is4_uri != 'undefined'){
            state.sig = buffer[i].is4_uri;
            console.log("Imprinted=" + state.sig);
        }

        if(buffer[i].is4_uri == state.sig){
            state.ptcnt +=1;
            var oldMean = state.mean;
            state.mean =((oldMean * (state.ptcnt-1))+buffer[i].value)/state.ptcnt;
            state.sumsqr += (buffer[i].value - oldMean)*(state.mean);
            state.variance = state.sumsqr/(state.ptcnt-1);
            state.stddev = Math.sqrt(state.variance);
            //console.log(JSON.stringify(state));
        }
    }

    var upper =0;
    var lower =0;
    if(state.mean>0 && state.ptcnt>10){
        var distribution = jStat.normal(state.mean, state.stddev);
        var upper = distribution.inv(0.95);
        var lower = distribution.inv(0.05);
        console.log("upper=" + upper + "; lower=" + lower);
        for(var i=0; i<buffer.length; i++){
            if(buffer[i].value>=upper || buffer[i].value<=lower){
                if(typeof output.buf == 'undefined')
                    output.buf = [];
                var alarmval = new Object();
                alarmval.path = buffer[i].is4_uri;
                alarmval.value = buffer[i].value;
                alarmval.ts = buffer[i].ts;
                output.buf.push(alarmval);
            }
        }
    } else if(state.ptcnt<=10){
        output.status='pending';
        output.numpts = state.ptcnt;
    }

    if(typeof output.buf !='undefined'){
        output.alarm=true;
        output.upper_t = upper;
        output.lower_t = lower;
    }else {
        output.alarm=false;
    }

    console.log(JSON.stringify(output) + "\n\n");
    buffer = new Array();
    return output;
}

function handlePost(req, res, data){
    buffer.push(data);
    res.writeHead(200, {'Content-Type': 'text/json'});
    res.end("{\"status\":\"success\"}");
}

var server= http.createServer(function(req,res){
    req.setEncoding('utf8');

    //console.log(req.headers);

    req.on('data', function(chunk) {
        chunktotal+=chunk;
    });

    req.on('end', function() {
        console.log('data=' + chunktotal);
        try {
            var data = JSON.parse(chunktotal);
            chunktotal="";
            if(req.method=='POST'){
                handlePost(req,res,data);
            } else{
                res.writeHead(200, {'Content-Type': 'text/plain'});
                res.end();
            }
        } catch(e){
            console.log(e);
        }
    });
});
var host = "localhost"; //"energylends.sfsprod.is4server.com";
var port = 1339;
server.listen(port, host);
console.log('Server running at http://' + host + ":" + port);

setInterval(procedure, 60000);

