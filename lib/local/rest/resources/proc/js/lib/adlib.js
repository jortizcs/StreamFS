var state = {};
var jStat= require('jStat').jStat;

var adlib = function(){
    this.isanamolous=function(ts, value, path, ptile){
        var output = new Object();
        if(typeof state.mean == 'undefined'){
            state.mean =0;
            state.stddev=0;
            state.min = 0;
            state.max=9007199254740992;
            state.ptcnt = 0;
            state.sumsqr = 0;
            state.variance = 0;
        }

        //imprint this process with the first signal heard
        if(typeof state.sig == 'undefined' && typeof path != 'undefined'){
            state.sig = path;
            console.log("Imprinted=" + state.sig);
        }

        if(path == state.sig){
            state.ptcnt +=1;
            var oldMean = state.mean;
            state.mean =((oldMean * (state.ptcnt-1))+value)/state.ptcnt;
            state.sumsqr += (value - oldMean)*(state.mean);
            state.variance = state.sumsqr/(state.ptcnt-1);
            state.stddev = Math.sqrt(state.variance);
            //console.log(JSON.stringify(state));
        }

        var upper =0;
        var lower =0;
        if(state.mean>0 && state.ptcnt>10){
            var distribution = jStat.normal(state.mean, state.stddev);
            var upper = distribution.inv(ptile);
            var lower = distribution.inv(1-ptile);
            console.log("upper=" + upper + "; lower=" + lower);
            if(value>=upper || value<=lower){
                if(typeof output.buf == 'undefined')
                    output.buf = [];
                var alarmval = new Object();
                alarmval.path = path;
                alarmval.value = value;
                alarmval.ts = ts;
                output.buf.push(alarmval);
            }
        } else if(state.ptcnt<=10){
            output.status='pending';
            output.numpts = state.ptcnt;
        }

        if(typeof output.buf !='undefined'){
            output.alarm=true;
            output.upper_t = upper;
            output.lower_t = lower;
        }else {
            output.alarm=false;
        }

        console.log(JSON.stringify(output) + "\n\n");
        return output;    

    }

};

module.exports = adlib;
