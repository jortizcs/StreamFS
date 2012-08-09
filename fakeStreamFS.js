var http = require('http');

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

    console.log(req.headers);

    req.on('data', function(chunk) {
        console.log("Receive_Event::" + chunk);
    });

    req.on('end', function() {
        console.log('on end');
        console.log("Bytes received: " + req.socket.bytesRead);
        if(req.method=='POST'){
            handlePost(req,res);
        } else if(req.method == 'GET'){
            res.writeHead(200, {'Content-Type': 'text/plain', 'connection':'close'});
            res.write(JSON.stringify({'Now':new Date().getTime()}));
            res.end();
        }
        else{
            res.writeHead(200, {'Content-Type': 'text/plain'});
            res.end();
        }
    });
});
server.listen(8081, "energylens.sfsdev.is4server.com");
console.log('Server running at http://energylens.sfsdev.is4server.com:8081/');
