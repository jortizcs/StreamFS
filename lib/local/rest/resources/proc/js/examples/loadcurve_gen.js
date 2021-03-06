var http = require('http');
var JASON = require('JASON');
var chunktotal = "";
var buffer = new Array();
var state = new Object();

function procedure(){
    var outObj = new Object();
    var timestamps = new Object();
    outObj.msg = 'processed';
    if(typeof state.slope == 'undefined'){
        state.slope = function(p1, p2){
            if(typeof p1 != 'undefined' && typeof p2 != 'undefined' &&
                typeof p1.value != 'undefined' && typeof p1.ts != 'undefined' &&
                typeof p2.value != 'undefined' && typeof p2.ts != 'undefined'){
                if(p1.ts == p2.ts)
                    return 'inf';
                return (p2.value-p1.value)/(p2.ts-p1.ts);
            }
            return 'error:undefined data point parameter';
        };
        state.intercept = function(slope,p1){
            if(typeof p1 != 'undefined' &&
                typeof p1.value != 'undefined' && typeof p1.ts != 'undefined'){
                return p1.value - (slope*p1.ts);
            }
            return 'error:undefined data point parameter';
        };
    }
    if(typeof state.multibuf == 'undefined'){
        state.multibuf = new Object();
    }
    outObj.inputs = new Array();
    var noted = new Object();
    for(i=0; i<buffer.length; i++){
        var streamid = buffer[i].pubid;
        var ts = buffer[i].ts;
        if(typeof state.multibuf[streamid] == 'undefined'){
            state.multibuf[streamid] = new Array();
        }
        state.multibuf[streamid].push({'ts':buffer[i].ts, 'value':buffer[i].value, 'path':buffer[i].is4_uri});

        if(typeof noted[buffer[i].is4_uri] == 'undefined'){
            noted[buffer[i].is4_uri]=true;
            outObj.inputs.push(buffer[i].is4_uri);
        }
        timestamps[ts] = true;
    }
    var streamids = Object.keys(state.multibuf);
    var tss = Object.keys(timestamps);
    tss = tss.sort();
    //console.log(tss);

    var ts_per_stream = new Object();
    if(streamids.length>=2){
        //check that you have at least 2 data points per stream
        for(j=0; j<streamids.length; j++){
            var this_streamid = streamids[j];
            var dpts = state.multibuf[this_streamid];
            if(dpts.length<2){
                outObj.stat = 'pending';
                //console.log('pending');
                return outObj;
            } else {
                //console.log(this_streamid + "=" + JSON.stringify(dpts));
                //put only the timestamps of the data stream into the ts_per_stream array
                //so that we can quickly determine what's there and what has to be interpolated
                for(dpidx = 0; dpidx<dpts.length; dpidx ++){
                    if(typeof ts_per_stream[this_streamid] == 'undefined'){
                        ts_per_stream[this_streamid] = new Object();
                    }
                    var thists = dpts[dpidx].ts;
                    ts_per_stream[this_streamid][thists]=true;
                }
            }
        }
        //console.log(JSON.stringify(ts_per_stream));

        var cleaned = new Object();
        //generate a point for each timestamp on the tss list
        for(j=0; j<streamids.length; j++){
            var this_streamid = streamids[j];
            var dpts = state.multibuf[this_streamid];
            cleaned[this_streamid]=new Array();
            for(tss_idx = 0; tss_idx < tss.length; tss_idx++){
                var timestamp = tss[tss_idx];
                if(typeof ts_per_stream[this_streamid][timestamp] == 'undefined'){
                    var p1 = dpts[0];
                    var p2 = dpts[dpts.length-1];
                    var slope = state.slope(p1,p2);
                    if(slope != 'inf' || slope.indexOf('error:')<0){
                        var intercept = state.intercept(slope,p1);
                        //console.log("(" + this_streamid + ", " + timestamp + ") has slope=" + slope + ", y-intercept=" + intercept);
                        var newdpt = new Object();
                        newdpt.ts = timestamp;
                        newdpt.value = (slope*timestamp)+intercept;
                        cleaned[this_streamid].push(newdpt);
                    } else {
                        console.log(slope);
                    }
                } else {
                    for(idx = 0; idx<dpts.length; idx++){
                        if(dpts[idx].ts==timestamp){
                            cleaned[this_streamid].push(dpts[idx]);
                            break;
                        }
                    }
                }
            }
        }
        //console.log('\n\n' + JSON.stringify(cleaned) + '\n');
       
        var loadcurve = new Array(); 
        //add them together
        var cleaned_keys = Object.keys(cleaned);
        var pts_per_key = cleaned[cleaned_keys[0]].length;
        for(ts_idx=0; ts_idx<tss.length; ts_idx++){
            var sum = 0;
            for(idx=0; idx<cleaned_keys.length; idx++){
                var thissubid = cleaned_keys[idx];
                sum += cleaned[thissubid][ts_idx].value;
            }
            loadcurve.push({'ts':cleaned[thissubid][ts_idx].ts,
                            'value':sum});
        }
        console.log('\n\n' + JSON.stringify(loadcurve) + '\n');
        outObj.loadcurve = loadcurve;
    } else {
        outObj.stat = 'pending';
    }
    buffer = new Array();
    return outObj;
}

function handlePost(req, res, data){
    buffer.push(data);
    res.writeHead(200, {'Content-Type': 'text/json'});
    res.end("{\"status\":\"success\"}");
}

var server= http.createServer(function(req,res){
    req.setEncoding('utf8');

    //console.log(req.headers);

    req.on('data', function(chunk) {
        chunktotal+=chunk;
    });

    req.on('end', function() {
        console.log('data=' + chunktotal);
        try {
            var data = JSON.parse(chunktotal);
            chunktotal="";
            if(req.method=='POST'){
                handlePost(req,res,data);
            } else{
                res.writeHead(200, {'Content-Type': 'text/plain'});
                res.end();
            }
        } catch(e){
            console.log(e);
        }
    });
});
server.listen(1339, "energylens.sfsprod.is4server.com");
console.log('Server running at http://energylens.sfsprod.is4server.com:1339/');

setInterval(procedure, 60000);
