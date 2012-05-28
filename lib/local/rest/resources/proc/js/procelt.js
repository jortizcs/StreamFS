var net = require('net');
var JASON = require('JASON');
var backgrounder = require('backgrounder');

var connections = new Object();

//indexed by the streamfs subid, which refers to the associated worker object 
var job_state = new Object();

var handleInit = function(dataObj, strm){
        var respObj = new Object();
        try {
            console.log('Handling init');
            respObj.stat = 'ok';
            connections[dataObj.name] = strm;
            console.log('\tAdded ' + dataObj.name);
            strm.write(JSON.stringify(respObj) + '\n');
        } catch(e){
            try {
                respObj.stat = 'fail';
                respObj.error = e;
                strm.write(JSON.stringify(respObj) + '\n');
                console.log(JSON.stringify(respObj) + '\n');
                strm.close();
            } catch(e2){
                console.log(e2);
            }
        }
    };

var handleDataIn = function(dataObj, strm){
        var respObj = new Object();
        try {
            console.log("Handling incoming data");
           
            var subid = dataObj.subid; 
            if(typeof dataObj.subid == 'undefined'){
                respObj.stat = 'fail';
                respObj.msg = 'subid not included in request';
                strm.write(JSON.stringify(respObj) + '\n');
                console.log(JSON.stringify(respObj) + '\n');
            } 
            //add it to the buffer and if the buffer is full, send it to get processed!
            var entry = job_state[subid];
            var thisBuf = job_state[subid].buffer;
            thisBuf.push(dataObj.data);
            job_state[subid].worker.send({"type":"process", "buffer":thisBuf},
                    function(error, message){
                        console.log(message);
                    });
            respObj.stat = 'ok';
            /*respObj.debug = new Object();
            respObj.debug.buflength = thisBuf.length;
            respObj.debug.winsize = entry.params.winsize;*/
            strm.write(JSON.stringify(respObj) + '\n');
            console.log(JSON.stringify(respObj) + '\n');
        } catch(e){
            respObj.stat = 'fail';
            respObj.error = e;
            strm.write(JSON.stringify(respObj) + '\n');
            console.log(e);
            console.log(JSON.stringify(respObj) + '\n');
        }
    };

var handleProcInstall = function(dataObj, strm){
    console.log("handleProcInstall called");
    var respObj = new Object();
    try {
        if(typeof dataObj.subid == 'undefined'){
            respObj.stat = 'fail';
            respObj.errorCode = 1;
            strm.write(JSON.stringify(respObj) + '\n');
            console.log(JSON.stringify(respObj) + '\n');
            return;
        }

        if(typeof job_state[dataObj.subid] == 'undefined'){
            job_state[dataObj.subid] = new Object();
        }
        var jobinfo = job_state[dataObj.subid];
        jobinfo.buffer = new Array();

        var configWorker = new Object();
        configWorker.subid = dataObj.subid;
        configWorker.host = dataObj.dest.host;
        configWorker.port = dataObj.dest.port;
        configWorker.path = dataObj.dest.path;
        configWorker.procedure = JASON.stringify(dataObj.proc_config.script.func);
        console.log("master: dataObj=" + JASON.parse(JASON.stringify(configWorker.procedure)));
        configWorker.winsize = 10;
        if(dataObj.proc_config.script.winsize != 'undefined')
            configWorker.winsize = dataObj.proc_config.script.winsize;
        if(typeof dataObj.proc_config.script.timeout != 'undefined')
            configWorker.period = dataObj.proc_config.script.timeout;
            
        //configWorker.strm = strm;
        //start the worker thread and pass it the procedure to run
        //console.log(JASON.stringify(configWorker));
        var worker = backgrounder.spawn(__dirname + "/worker.js", configWorker, 
                function(worker){
                    worker.send({"type":"testprocedure"}, 
                        function(error, message){
                            console.log("master::" + arguments.length);
                            if(typeof error != 'undefined' && error != null &&
                                JASON.stringify(error) != '{}'){
                                respObj.stat = 'fail';
                                respObj.errorCode = 3;
                                respObj.error = error;
                                respObj.message= message;
                                if(typeof message != 'undefined')
                                    respObj.message = message;
                                strm.write(JASON.stringify(respObj) + '\n');
                                console.log(JASON.stringify(respObj) + '\n');
                                worker.terminate();
                            } else {
                                respObj.stat = 'ok';
                                respObj.installed = dataObj.subid;
                                if(typeof configWorker.period != 'undefined'){
                                    worker.send({"type":"start"}, 
                                        function(error, message){
                                            console.log("master:[error " + JASON.stringify(error) + 
                                                ", msg=" + JASON.stringify(message) );
                                        });
                                    job_state[dataObj.subid].worker = worker;
                                    job_state[dataObj.subid].buffer = new Array();

                                    worker.on('message', function(message){
                                            if(message.type=='get_buffer'){
                                                worker.send(
                                                    {"type":"buffer_resp", 
                                                    "buffer":job_state[message.subid].buffer});

                                            } else if(message.type=='clear_buffer'){
                                                job_state[message.subid].buffer = new Array();
                                            }
                                    });
                                }
                                strm.write(JASON.stringify(respObj) + '\n');
                                console.log(JASON.stringify(respObj) + '\n');
                            }
                        });
        });
    } catch(e){
        respObj.stat = 'fail';
        respObj.errorCode = 2;
        try{
            strm.write(JSON.stringify(respObj) + '\n');
            console.log(JSON.stringify(respObj) + '\n');
            console.log(e);
        } catch(e2){
            console.log(e);
            console.log(e2);
        }
    }
};

var connectHandler = 
    function(strm){
        console.log("Connection heard");
        strm.on('data', function(data){
            var respObj = new Object();
            console.log("Data received: " + data + ", \ttype=" + typeof data + "\n");
            try {
                var dataObj = data;
                console.log("Parsing data");
                if(Buffer.isBuffer(data)){
                    eval('dataObj=' + data.toString());
                } else {
                    dataObj = JSON.parse(data);
                }
                console.log("Done");
                if(dataObj.command == 'data'){
                    handleDataIn(dataObj, strm);
                } else if(dataObj.command == 'init'){
                    handleInit(dataObj, strm);
                } else if(dataObj.command == 'procinstall'){
                    handleProcInstall(dataObj, strm);
                } else if(dataObj.command == 'job_status'){
                    console.log(JSON.stringify(dataObj));
                    if(typeof dataObj.name != 'undefined' && 
                        typeof dataObj.subid != 'undefined' && 
                        typeof job_state[dataObj.subid] != 'undefined'){
                        respObj.stat = 'ok';
                        respObj.subid = dataObj.subid;
                        respObj.job_status = 'running';
                        strm.write(JSON.stringify(respObj) + '\n');
                        console.log(JSON.stringify(respObj));
                    } else {
                        respObj.stat = 'fail';
                        respObj.code = 1;
                        respObj.msg = 'Job does not exist or missing name/subid in request';
                        strm.write(JSON.stringify(respObj) + '\n');
                        console.log(JSON.stringify(respObj));
                    }
                } else if(dataObj.command == 'kill'){
                    if(typeof dataObj.subid != 'undefined' &&
                        typeof job_state[dataObj.subid] != 'undefined')
                    {
                        job_state[dataObj.subid].kill=true;
                        respObj.stat = 'ok';
                        console.log("response::" + JSON.stringify(respObj));
                        strm.write(JSON.stringify(respObj) + '\n');
                    } else {
                        respObj.stat = 'fail';    
                        strm.write(JSON.stringify(respObj) + '\n');
                        console.log("response:: " + JSON.stringify(respObj));
                        console.log(JSON.stringify(job_state,4)+"\n\n\n\n");
                        console.log(JSON.stringify(dataObj,4));
                    }
                }else {
                    var resp = new Object();
                    resp.status = 'success';
                    strm.write(JSON.stringify(resp) + '\n');
                    console.log(JSON.stringify(JSON.parse(data)));
                }
            } catch(e){
                var respObj = new Object();
                respObj.stat = 'fail';
                respObj.error = e;
                respObj.msg = "I don't know what's wrong";
                strm.write(JSON.stringify(respObj) + '\n');
                console.log(JSON.stringify(respObj) + '\n');
                console.log(e);
                //console.log(typeof e);
                console.log(dataObj);
                console.log(Buffer.isBuffer(dataObj));
                console.log(dataObj.toString());
                var obj = dataObj.toString();
                evaled = eval('obj=' +obj);
                console.log(obj.command);
                console.log(typeof obj.script.script.func);
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



/*var requestObj = new Object();
requestObj.subid = 12345;

requestObj.dest = new Object();
requestObj.dest.host = "sfshost";
requestObj.dest.port = 8080;
requestObj.dest.path = "/path";

requestObj.proc_config = new Object();
requestObj.proc_config.script = new Object();
requestObj.proc_config.script.func = 
   function(buffer, state){
       var ret = new Object();
       ret.msg = "hello world";
       return ret;
   };
requestObj.proc_config.script.timeout=10000;
requestObj.proc_config.script.winsize=10;

handleProcInstall(requestObj, process.stdout);*/
