var vm = require('vm');
var net = require('net');
var fs =  require('fs');
var http = require('http');

var connections = new Object();

//indexed by the streamfs instance name, which give you another object
//of registered subscription ids which gives you a reference to an object
//which holds the script that should be run, the execution parameters,
//and the current state of the job
var job_state = new Object();

var post=function (m_host, m_port, m_path, m_data){
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
    post_req.write(JSON.stringify(m_data));
    post_req.end();
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
    console.log("task called::[name=" + name + ",subid=" + subid + "]");
    if(typeof job_state[name] != 'undefined' && typeof job_state[name][subid] != 'undefined'){
        if(job_state[name][subid].kill == true){
            clearInterval(job_state[name][subid].jobid);
            delete job_state[name][subid];
            console.log("Deleted [name=" + name + ",subid=" + subid + "]");
            return;
        }
        var loc = "http://" + job_state[name][subid].host + ":" + job_state[name][subid].port + job_state[name][subid].path;
        var retbuf = job_state[name][subid].procedure(job_state[name][subid].params.buffer, job_state[name][subid].params.internal);
        job_state.last_exec_time = new Date();
        console.log("POSTING " + JSON.stringify(retbuf) + " --> " + loc);
        post(job_state[name][subid].host,
                job_state[name][subid].port, 
                job_state[name][subid].path, 
                retbuf);
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
            } 
            if(typeof job_info[name] == 'undefined' || typeof job_info[name][subid] == 'undefined'){
                respObj.stat = 'fail';
                respObj.msg = 'name and/or subid not included in request';
                strm.write(JSON.stringify(respObj) + '\n');
                
            }
            //add it to the buffer and if the buffer is full, send it to get processed!a
            var entry = job_info[name][subid];
            var thisBuf = job_info[name][subid].params.buffer;
            thisBuf.push(dataObj.data);
            if(thisBuf.length >= entry.winsize){
                task(name, subid);
            }
            respObj.stat = 'ok';
            strm.write(JSON.stringify(respObj) + '\n');
        } catch(e){
            respObj.stat = 'fail';
            respObj.error = e;
            strm.write(JSON.stringify(respObj) + '\n');
        }
    };

var handleProcInstall = function(dataObj, strm){
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

        jobinfo.procedure = dataObj.script.func;
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
        jobinfo.params.winsize = dataObj.script.winsize;
        jobinfo.params.buffer = new Array();
        jobinfo.params.internal = new Object();

        jobinfo.state = new Object();
        jobinfo.state.winsize = 0;
        jobinfo.last_exec_time = -1;

        if(typeof dataObj.script.timeout != 'undefined'){
            jobinfo.params.period = dataObj.script.timeout;
            var job=setInterval(task, jobinfo.params.period, dataObj.name, dataObj.subid);
            jobinfo.jobid = job;
            console.log("job scheduled");
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
            console.log("Data received: " + data + ", \ttype=" + typeof data + "\n");
            /*if(typeof data != 'string'){
                var dataStr = JSON.stringify(data);
                console.log("OK::" + dataStr);
            }*/
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
                } else if(dataObj.command == 'kill'){
                    if(typeof dataObj.name != 'undefined' && 
                        typeof dataObj.subid != 'undefined'){
                        job_state[name][subid].kill=true;
                        respObj.stat = 'ok';
                    } else {
                        respObj.stat = 'fail';    
                        strm.write(JSON.stringify(respObj) + '\n');
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
