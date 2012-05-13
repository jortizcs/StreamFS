var vm = require('vm');
var net = require('net');
var fs =  require('fs');
var http = require('http');

var connections = new Object();
var retries = new Object();

//indexed by the streamfs instance name, which give you another object
//of registered subscription ids which gives you a reference to an object
//which holds the script that should be run, the execution parameters,
//and the current state of the job
var job_state = new Object();

var post=function (m_host, m_port, m_path, m_data, name, subid){
    if(typeof retries[name] == 'undefined'){
        retries[name] = new Object();
    }
    if(typeof retries[name][subid] == 'undefined'){
        retries[name][subid] = 0;
    }
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
                    retries[name][subid] += 1;        
          
                    console.log("\tRetries_cnt="  + retries[name][subid]);
                    if(retries[name][subid] == 3){ 
                        job_state[name][subid].kill = true; 
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

var task = function(name, subid){
    try {
        console.log("task called::[name=" + name + ",subid=" + subid + "]");
        if(typeof job_state[name] != 'undefined' && typeof job_state[name][subid] != 'undefined'){
            if(job_state[name][subid].kill == true){
                clearInterval(job_state[name][subid].jobid);
                delete job_state[name][subid];
                delete retries[name][subid];
                console.log("Deleted [name=" + name + ",subid=" + subid + "]");
                return;
            }
            var loc = "http://" + job_state[name][subid].host + ":" + job_state[name][subid].port + job_state[name][subid].path;
            var retbuf = job_state[name][subid].procedure(job_state[name][subid].params.buffer, job_state[name][subid].params.internal);
            job_state.last_exec_time = new Date();
            console.log("POSTING " + JSON.stringify(retbuf) + " --> " + loc);
            if(typeof retbuf == 'object' && JSON.stringify(retbuf) != '{}' &&
                JSON.stringify(retbuf) != '[]'){
                post(job_state[name][subid].host,
                        job_state[name][subid].port, 
                        job_state[name][subid].path, 
                        retbuf,
                        name,
                        subid);
            } else {
                console.log("\tDid not POST " + JSON.stringify(retbuf) + "; object failed format pre-conditions");
            }
        }
    } catch(e){
        console.error(e);
        console.log("Error occurred while executing task");
    }
};

var handleDataIn = function(dataObj, strm){
        var respObj = new Object();
        try {
            console.log("Handling incoming data");
           
            var name = dataObj.name;
            var subid = dataObj.subid; 
            if(typeof dataObj.name == 'undefined' || typeof dataObj.subid == 'undefined'){
                respObj.stat = 'fail';
                respObj.msg = 'name and/or subid not included in request';
                strm.write(JSON.stringify(respObj) + '\n');
                console.log(JSON.stringify(respObj) + '\n');
            } 
            if(typeof job_state[name] == 'undefined' || typeof job_state[name][subid] == 'undefined'){
                respObj.stat = 'fail';
                respObj.msg = 'name and/or subid not included in request';
                strm.write(JSON.stringify(respObj) + '\n');
                console.log(JSON.stringify(respObj) + '\n');
                
            }
            //add it to the buffer and if the buffer is full, send it to get processed!a
            var entry = job_state[name][subid];
            var thisBuf = job_state[name][subid].params.buffer;
            thisBuf.push(dataObj.data);
            if(thisBuf.length >= entry.params.winsize){
                task(name, subid);
                delete job_state[name][subid].params.buffer;
                job_state[name][subid].params.buffer = new Array();
            }
            respObj.stat = 'ok';
            respObj.debug = new Object();
            respObj.debug.buflength = thisBuf.length;
            respObj.debug.winsize = entry.params.winsize;
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
        if(typeof dataObj.name == 'undefined'){
            respObj.stat = 'fail';
            respObj.errorCode = 1;
            strm.write(JSON.stringify(respObj) + '\n');
            console.log(JSON.stringify(respObj) + '\n');
            return;
        }

        if(typeof job_state[dataObj.name] == 'undefined'){
            job_state[dataObj.name] = new Object();
        }
        job_state[dataObj.name][dataObj.subid] = new Object();
        var jobinfo = job_state[dataObj.name][dataObj.subid];
        jobinfo.host = dataObj.dest.host;
        jobinfo.port = dataObj.dest.port;
        jobinfo.path = dataObj.dest.path;

        jobinfo.procedure = dataObj.proc_config.script.func;
        //test procedure
        try {
            console.log("dataObj=" + JSON.stringify(dataObj));
            var testbuf = new Array();
            var teststate = new Object();
            jobinfo.procedure(testbuf, teststate);
        } catch(procerror){
            respObj.stat = 'fail';
            respObj.error = "Invalid procedure; Tried to run procedure and test failed: " + procerror;
            strm.write(JSON.stringify(respObj));
            console.error("procerror:" + procerror + ", procedure:" + JSON.stringify(jobinfo));
            delete job_state[dataObj.name][dataObj.subid];
            return; 
        }

        jobinfo.params = new Object();
        jobinfo.params.winsize = dataObj.proc_config.script.winsize;
        jobinfo.params.buffer = new Array();
        jobinfo.params.internal = new Object();

        jobinfo.state = new Object();
        jobinfo.state.winsize = 0;
        jobinfo.last_exec_time = -1;

        try {
            if(typeof dataObj.proc_config.script.timeout != 'undefined'){
                jobinfo.params.period = dataObj.proc_config.script.timeout;
                var job=setInterval(task, jobinfo.params.period, dataObj.name, dataObj.subid);
                jobinfo.jobid = job;
                console.log("job scheduled");
            }
        } catch(e3){
            console.log("errorCode:3");
            console.error(e);
        }

        respObj.stat = 'ok';
        respObj.installed = dataObj.subid;
        strm.write(JSON.stringify(respObj) + '\n');
        console.log(JSON.stringify(respObj) + '\n');
        console.log(JSON.stringify(job_state) + '\n');
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
                        typeof job_state[dataObj.name] != 'undefined' &&
                        typeof job_state[dataObj.name][dataObj.subid] != 'undefined'){
                        respObj.stat = 'ok';
                        respObj.name = dataObj.name;
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
                    if(typeof dataObj.name != 'undefined' && 
                        typeof dataObj.subid != 'undefined' && typeof job_state[dataObj.name] != 'undefined' &&
                        typeof job_state[dataObj.name][dataObj.subid] != 'undefined')
                    {
                        job_state[dataObj.name][dataObj.subid].kill=true;
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

/*var dataObj = {
    "command":"procinstall",
    "name":"myprocedure",
    "subid":"abcde-12345",
    "post_loc":"http://sfsserver.com/location/to/post/data?query=true&pubid=abcde-12345",
    "script":{
        "winsize":1,
        "timeout":2000,
        "func":function(buffer, internal){
            var ret = new Object();
            ret.msg = "hello world";
            ret.length = buffer.length;
            ret.internal = internal;
            console.log(JSON.stringify(ret));
            return ret;
        }
    }
};
handleProcInstall(dataObj, null);

var updateBuffer = function (name,subid){
    try {
        if(typeof job_state[name] == 'undefined'){
            return;
        }
        var entry = new Object();
        entry.time = new Date();
        job_state[name][subid].params.buffer.push(entry);
    } catch(e){
        //console.error(e);
    }
};
var loadbuf = setInterval(updateBuffer, 1000, dataObj.name, dataObj.subid);

var killIt=function(name, subid){
    if(typeof job_state[name] != 'undefined' && typeof job_state[name][subid] != 'undefined'){
        job_state[name][subid].kill=true;
    }

    clearTimeout(loadbuf);
}
setTimeout(killIt, 10000, dataObj.name, dataObj.subid); */
