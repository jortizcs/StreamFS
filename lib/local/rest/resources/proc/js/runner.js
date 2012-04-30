var vm = require('vm');

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

//indexed by the streamfs instance name, which give you another object
//of registered subscription ids which gives you a reference to an object
//which holds the script that should be run, the execution parameters,
//and the current state of the job
var job_state = new Object();

var handleInit = function(dataObj, strm){
        var respObj = new Object();
        try {
            console.log('Handling init');
            respObj.stat = 'ok';
            connections[dataObj.sfsname] = strm;
            console.log('\tAdded ' + dataObj.sfsname);
            strm.write(JSON.stringify(respObj) + '\n');
        } catch(e){
            try {
                respObj.stat = 'fail';
                respObj.error = e;
                strm.write(JSON.stringify(respObj) + '\n');
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
            respObj.stat = 'ok';
            strm.write(JSON.stringify(respObj) + '\n');
        } catch(e){
            respObj.stat = 'fail';
            respObj.error = e;
            strm.write(JSON.stringify(respObj) + '\n');
            strm.blah;
        }
    };

var task = function(name, subid){
    if(typeof job_state[name] != 'undefined' && typeof job_state[name][subid] != 'undefined'){
        loc = job_state[name][subid].post_loc;
        var retbuf = job_state[name][subid].procedure(job_state[name][subid].buffer, job_state[name][subid].internal);
        job_state.last_exec_time = new Date();
        console.log("POSTING " + JSON.stringify(retbuf) + " --> " + loc);
    }
};

var handleProcInstall = function(dataObj, strm){
    var respObj = new Object();
    try {
        if(typeof dataObj.name == 'undefined'){
            respObj.stat = 'fail';
            respObj.errorCode = 1;
            //strm.write(JSON.stringify(respObj) + '\n');
            console.log(JSON.stringify(respObj) + '\n');
            return;
        }

        if(typeof job_state[dataObj.name] == 'undefined'){
            job_state[dataObj.name] = new Object();
        }
        job_state[dataObj.name][dataObj.subid] = new Object();
        var jobinfo = job_state[dataObj.name][dataObj.subid];
        jobinfo.post_loc = dataObj.post_loc;
        jobinfo.procedure = dataObj.script.func;

        jobinfo.params = new Object();
        jobinfo.params.winsize = dataObj.script.winsize;
        jobinfo.params.buffer = new Array();
        jobinfo.params.internal = new Object();

        jobinfo.state = new Object();
        jobinfo.state.winsize = 0;
        jobinfo.last_exec_time = -1;

        if(typeof dataObj.script.timeout != 'undefined'){
            jobinfo.params.period = dataObj.script.timeout;
            var job=setInterval(jobinfo.procedure, jobinfo.params.period, jobinfo.params.buffer, jobinfo.params.internal);
            jobinfo.jobid = job;
            console.log("job scheduled");
        }

        respObj.stat = 'ok';
        respObj.installed = dataObj.subid;
        //strm.write(JSON.stringify(respObj) + '\n');
        console.log(JSON.stringify(respObj) + '\n');
    } catch(e){
        respObj.stat = 'fail';
        respObj.errorCode = 2;
        try{
            //strm.write(Json.stringify(respobj) + '\n');
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
            console.log("Data received");
            try {
                var dataObj = JSON.parse(data);
                if(dataObj.command == 'data_in'){
                    handleDataIn(dataObj, strm);
                } else if(dataObj.command == 'init'){
                    handleInit(dataObj, strm);
                } else if(dataobj.command == 'procinstall'){
                    handleProcInstall(dataObj, strm);
                } else {
                    var resp = new Object();
                    resp.status = 'success';
                    strm.write(JSON.stringify(resp) + '\n');
                    console.log(JSON.stringify(JSON.parse(data)));
                }
            } catch(e){
                var respObj = new Object();
                respObj.stat = 'fail';
                respObj.error = e;
                strm.write(JSON.stringify(respObj) + '\n');
                console.log(e);
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

//var server = net.createServer(connectHandler);
//server.listen(1337, 'localhost');

/*var state = new Object();
state.internal = new Object();
state.procedure = function(buffer, internal){
    console.log(buffer.a);
    internal.msg = a;
};
var a = "hello world";
var b = new Object();
b.a = a;
state.procedure(b, state.internal);
console.log(JSON.stringify(state.internal));*/

var dataObj = {
    "command":"procinstall",
    "name":"myprocedure",
    "subid":"abcde=12345",
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
    if(typeof job_state[name] == 'undefined'){
        return;
    }
    var entry = new Object();
    entry.time = new Date();
    job_state[name][subid].params.buffer.push(entry);
};
setInterval(updateBuffer, 1000, dataObj.name, dataObj.subid);

var killIt=function(name, subid){
    if(typeof job_state[name] != 'undefined'){
        clearInterval(job_state[name][subid].jobid);
        delete job_state[name];
    }
    console.log("Deleted " + subid);
}
setInterval(killIt, 10000, dataObj.name, dataObj.subid); 

