var http=require('http');
var exec = require('child_process').exec;
var spawn = require('child_process').spawn;

var svrRequest = "";
var requestObj = new Object();
var next_resp = null;

var host = process.env.HOST;
var port = parseInt(process.env.PORT);

function handler(request, response){
    request.setEncoding('utf8');
    
    request.on('data', 
            function(chunk){
                try {
                    if(request.method == 'POST' || request.method == 'PUT'){
                        svrRequest +=chunk;
                    }
                } catch(e){
                    console.log("1");
                    console.log(e);
                }
            });

    request.on('end',
            function(){
                try {
                    requestObj = JSON.parse(svrRequest);
                    svrRequest = "";
                    if(requestObj.type == "start"){
                        next_resp = response;
                        startup();
                    } else {
                        var resp = new Object();
                        resp.stat = 'fail';
                        resp.msg = 'unknown command';
                        response.writeHead(200, 
                            {"Content-Length":JSON.stringify(resp).length,
                            "Content-Type":"application/json"});
                        response.write(JSON.stringify(resp));
                        response.end();
                    }
                } catch(e){
                    console.log("2");
                    console.log(e);
                    var resp = new Object();
                    resp.stat = 'fail';
                    resp.msg = JSON.stringify(e);
                    response.writeHead(200, 
                        {"Content-Length":JSON.stringify(resp).length,
                        "Content-Type":"application.json"});
                    response.write(JSON.stringify(resp));
                    response.end();
                }

            });
    
}

function startup(){
    console.log(typeof port);
    console.log("starting process element::host=" + host + ", port=" + port);
    var child = spawn("node", ['procelt.js', host, port]);

    child.on('end', function(){
            console.log(child.pid + " died");
        });
    
    child.stdout.setEncoding('utf8');
    child.stdout.on('data', function(data){
            if(data.indexOf(port.toString())>-1){
                var resp = new Object();
                resp.stat='success';
                resp.host= host;
                resp.port = port;
                next_resp.writeHead(200, 
                    {"Content-Length":JSON.stringify(resp).length,
                    "Content-Type":"application/json"});
                next_resp.write(JSON.stringify(resp));
                next_resp.end();
            }
            //console.log("data=" + data +" and port=" + port);
        });
    
    child.stderr.setEncoding('utf8');
    child.stderr.on('data', function(data){
            if(data.indexOf("EADDRINUSE")>-1){
                port +=1;
                startup();
            }
        });
}

var server = http.createServer(handler);
server.listen(8000);

//console.log(process.env.HOST);
//console.log(process.env.PORT);
