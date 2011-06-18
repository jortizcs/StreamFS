{
	winsize:2,
	select:function(buffer){
		var newdat = new Object();
		var tBufVal = buffer[0];
		newdat.timestamp = tBufVal.properties.head.timestamp;
		newdat.reading = tBufVal.properties.head.Reading;
		return newdat;
	},
	agg:function(buffer){
		var newdata = new Object();
		var sum = 0;
		for(v=0; v<buffer.length; v++)
			sum = sum + buffer[v].properties.head.Reading;
		newdata.timestamp = buffer[buffer.length-1].properties.head.timestamp;
		newdata.sum = sum;
		newdata.avg = sum/buffer.length;
		return newdata;
	},
	val1:10,
	alarm:function(buffer){
		var data = buffer[0];
		var alarmObj = new Object();
		if(data.reading>val1){
			alarmObj.alarm_Set=true;
		}else{
			alarmObj.alarm_Set=false;
		}
		return alarmObj;
	}
}