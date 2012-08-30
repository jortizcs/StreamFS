var http = require('http');
var chunktotal = "";
function handlePost(req, res){
        req.on('data', function(chunk) {
                                console.log("got some data here");
                                                  //console.log("Receive_Event::" + chunk.toString());
                                                                   });
            res.writeHead(200, {'Content-Type': 'text/json'});
                res.end("{\"status\":\"success\"}");
}

var server= http.createServer(function(req,res){
    req.setEncoding('utf8');

    //console.log(req.headers);

    req.on('data', function(chunk) {
        //console.log("Receive_Event::" + chunk);
        chunktotal+=chunk;
    });

    req.on('end', function() {
        //console.log('on end');
        //console.log("Bytes received: " + req.socket.bytesRead);
        console.log('data=' + chunktotal);
        chunktotal="";
        if(req.method=='POST'){
            handlePost(req,res);
        } else{
            res.writeHead(200, {'Content-Type': 'text/plain'});
            res.end();
        }
    });
});
server.listen(1338, "energylens.sfsprod.is4server.com");
console.log('Server running at http://energylens.sfsprod.is4server.com:1338/');
