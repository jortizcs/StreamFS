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
var connections = new Object();

var connectHandler = 
    function(strm){
        strm.on('data', function(data){
                var dataObj = JSON.parse(data);
                if(dataObj.command == 'init'){
                    var respObj = new Object();
                    respObj.stat = 'ok';
                    strm.write(JSON.stringify(respObj));
                    connections[dataObj.sfsname] = strm;
                }
                });

        strm.on('close', function(){
                    var keys = Object.keys(connections);
                    for(key in keys){
                        if(!connections[key].readable){
                            delete connections[key];
                            console.log("Connection closed, removed " + key);
                        }
                    }
                }
            );
    };

var server = net.createServer(connectHandler);
server.listen(1337, 'localhost');
