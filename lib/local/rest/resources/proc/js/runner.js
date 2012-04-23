//var vm = require('vm');

/*var localVar= 123, usingscript, evaled;
try {
    usingscript = vm.runInThisContext('localVar = ', 'myfile.vm');
} catch(e){
    console.log(e);
}
console.log('localVar: ' + localVar + ', usingscript: ' + usingscript);
evaled = eval('localVar = 1;');
console.log('localVar: ' + localVar + ', evaled: ' + evaled);*/



var net = require('net');
var fs =  require('fs');
var connections = new Object();

var handleInit = function(dataObj, strm){
        try {
            console.log('Handling init');
            var respObj = new Object();
            respObj.stat = 'ok';
            strm.write(JSON.stringify(respObj));
            connections[dataObj.sfsname] = strm;
            console.log('\tAdded ' + dataObj.sfsname);
        } catch(e){
        }
    };

var handleDataIn = function(dataObj, strm){
        var respObj = new Object();
        try {
            console.log("Handling incoming data");
            respObj.stat = 'ok';
            strm.write(JSON.stringify(respObj));
        } catch(e){
            respObj.stat = 'fail';
            respObj.error = e;
            strm.write(JSON.stringify(respObj));
        }
    };

var connectHandler = 
    function(strm){
        console.log("Connection heard");
        strm.on('data', function(data){
                    console.log("Data received");
                    try {
                        var dataObj = JSON.parse(data);
                        if(dataObj.command == 'init'){
                            handleInit(dataObj, strm);
                        } else if(dataObj.command == 'data_in'){
                            handleDataIn(dataObj, strm);
                        }
                    } catch(e){
                        var respObj = new Object();
                        respObj.stat = 'fail';
                        respObj.error = e;
                        strm.write(JSON.stringify(respObj));
                    }
                });

        strm.on('close', function(){
                    console.log("socket closed");
                    var keys = Object.keys(connections);
                    console.log(JSON.stringify(keys));
                    for(keyidx in keys){
                        console.log('checking ' + keys[keyidx]);
                        if(!connections[keys[keyidx]].readable){
                            delete connections[keys[keyidx]];
                            console.log("Connection closed, removed " + keys[keyidx]);
                        }
                    }
                }
            );
    };

var server = net.createServer(connectHandler);
server.listen(1337, 'localhost');
