var JASON = require('JASON');           //interprets json and functions
var Sandbox = require('sandbox');       //runs code safely
var http = require('http');             //to post the data to the correct target
//var monogo = require('mongo');          //for recording performance statistics

//held globally for event-based communication with master
var config_cb = null;       //configuration callback function
var msg_cb=null;            //message callback function

//state variable used to run the procedure in this worker
var subid = null;
var procedure = null;
var winsize = 10;
var period = -1;

//where to send the procedure result
var host = null;
var port = null;
var path = null;

//the global object accesible by the procedure
var state = new Object();

var jobref = null;

function returnProcCheckRes(m_callback, res){
    var respObj=new Object();
    try {
        if(res.stat != 'ok'){
            //console.log("worker: testproc check NOT OK::" + JSON.stringify(res));
            console.log("worker.returnProcCheckRes.respObj=" + JASON.stringify(respObj));
            m_callback(res.error, res);
            return;
        }
        respObj.stat = 'ok';
        console.log("worker.returnProcCheckRes.respObj=" + JASON.stringify(respObj));
        m_callback(new Object(), respObj);
    } catch(e){
        console.log("worker:returnProcCheckRes::error:" + JASON.stringify(e));
        respObj.stat = 'fail';
        respObj.error = e;
        m_callback(e, respObj);
    }
}

process.on('config',
        function(config, callback){
            var respObj = new Object();
            procedure = JASON.parse(config.procedure);
            subid = config.subid;
            host = config.host;
            port = config.port;
            path = config.path;
            if(typeof config.winsize != 'undefined' && config.winsize>0)
                winsize = config.winsize;
            if(typeof config.period != 'undefined')
                period = config.period;
            callback();
        });

process.on('message', 
        function(message, callback){
            var respObj = new Object();
            try {
                if(message.type =='testprocedure'){
                    console.log("worker::testprocedure called");
                    testproc(procedure, callback);
                }
                else if(message.type=='kill'){
                    clearInterval(jobref);
                    respObj.stat = 'ok';
                    respObj.subid = subid;
                    callback(null, respObj);
                    return;
                } 
                else if(message.type == 'start' && period>0){
                    jobref = setInterval(getBuffer, period);
                    respObj.stat = 'ok';
                    respObj.subid = subid;
                    callback(new Object(), respObj);
                }
                else if(message.type=='buffer_resp' && typeof message.buffer != 'undefined'){
                    task(message.buffer);
                }
                else if(message.type=='process' && typeof message.buffer != 'undefined'){
                    if(buffer.length>=winsize)
                        task(message.buffer);
                    respObj.stat = 'ok';
                    respObj.subid=subid;
                    callback(null, respObj);
                }
                else {
                    respObj.stat='fail';
                    respObj.subid = subid;
                    var error = "unknown request";
                    callback(error, respObj);
                }
            } catch(e){
                respObj.stat = 'fail';
                respObj.subid = subid;
                callback(e, respObj);
            }
        });

var retries = 0;     //maintains the number of retries to sfs target server
var post=function (m_host, m_port, m_path, m_data){
    var loc = "http://" + m_host + ":" + m_port + m_path;
    console.log("POSTING " + JSON.stringify(m_data) + " --> " + loc);
    try {
        var options = {
            host:m_host,
            port:m_port,
            path:m_path,
            method:'POST',
            header:{
                'Content-Type':'application/json',
                'Content-Length':m_data.length
            }
        };
        var post_req = http.request(options, 
            function(res){
                res.on('data', function(chunk){
                    try {
                        var response = JSON.parse(chunk);
                        if(response.status != "success"){
                            console.error("couldn't post to " + m_host + ":" + m_port + m_path );
                        } else {
                            console.log("Posted successfully to " + m_host + ":" + m_port + m_path ); 
                        }
                    } catch(e){
                        console.error(e);
                    }
                });
            });
        post_req.on('error', function(error){
                try {
                    console.log("error occurred");
                    console.error(error);
                    retries+= 1;        
                    console.log("\tRetries_cnt="  + retries);
                    if(retries == 3){
                        var suicide = new Object();
                        suicuide.type = 'kill';
                        suicide.subid = subid;
                        process.send(suicide);
                    }
                } catch(e){
                    console.error(e);
                }
            });
        post_req.on('response', function(res){
                if(res.statusCode >= 200 && res.statusCode <=210){
                    retries[name][subid]=0;
                    console.log("POST successful");
                }
            });
        post_req.write(JSON.stringify(m_data));
        post_req.end();
    } catch (e2) {
        console.error(e2);
    }
}

function isFunction(functionToCheck) {
    var getType = {};
    return functionToCheck && getType.toString.call(functionToCheck) == '[object Function]';
}

function getBuffer(){
    console.log("getBuffer called");
    process.send({'type':'get_buffer', 'subid':subid});
}

function testproc(f, m_callback){
    var respObj = new Object();
    if(!isFunction(f)){
        respObj.stat = 'fail';
        respObj.error = "Invalid procedure; Tried to run procedure; not a function";
        console.log("worker.testproc.respObj=" + JASON.stringify(respObj));
        returnProcCheckRes(m_callback, respObj);
    } else {
        try {
            var codetxt = "var buffer = new Array();";
            codetxt += "var state = new Object();";
            codetxt += "var procedure = " + JASON.stringify(f) + ";";
            codetxt += "procedure(buffer, state);";
            var sandbox = new Sandbox();
            sandbox.run(codetxt, function(output){
                        if(output.result.indexOf("Error")!=-1){
                            respObj.stat = 'fail';
                            respObj.error = "Invalid procedure; Tried to run " + 
                                            "procedure and test failed: " + output.result;
                        } else
                            respObj.stat = 'ok';

                        console.log("worker.testproc.respObj=" + JASON.stringify(respObj));
                        returnProcCheckRes(m_callback, respObj);
                    });
        } catch(procerror){
            respObj.stat = 'fail';
            respObj.error = "Invalid procedure; Tried to run procedure " + 
                "and test failed: " + procerror;
            console.error("procerror:" + procerror + ", procedure:");
            console.log("worker.testproc.respObj=" + JASON.stringify(respObj));
            returnProcCheckRes(m_callback, respObj);
        }
    }
}

function task(buffer){
    try {
        console.log("task called::[subid=" + subid + "]");
        var codetxt = "var buffer = " + JSON.stringify(buffer) + ";\n";
        process.send({"type":"clear_buffer", "subid":subid});
        codetxt += "var state = " + JASON.stringify(state) + ";\n";
        codetxt += "var procedure = " + JASON.stringify(procedure) + ";\n";
        codetxt += "procedure(buffer, state);\n";
        //console.log("worker:codetxt=" + codetxt);
        var sandbox = new Sandbox();
        sandbox.run(codetxt, 
            function(output){
                var retbuf=null;
                var resultStr = JASON.stringify(output.result);
                console.log("worker:output=" + JASON.stringify(output));
                if(resultStr.indexOf("Error") == -1){
                    retbuf = JASON.parse(output.result);
                    if(typeof retbuf == 'object' && JSON.stringify(retbuf) != '{}' &&
                        JSON.stringify(retbuf) != '[]'){
                        var loc = "http://" + host + ":" + port + path;
                        console.log("1 POSTING " + JSON.stringify(retbuf) + " --> " + loc);
                        //post(host, port, path, retbuf);
                        return;
                    }
                    var loc = "http://" + host + ":" + port + path;
                    console.log("2 POSTING " + JSON.stringify(retbuf) + " --> " + loc);
                } else
                    console.log("\tDid not POST " + JSON.stringify(retbuf) + "; object failed format pre-conditions");
       });
    } catch(e){
        console.error(e);
        console.log("Error occurred while executing task");
    }
};
