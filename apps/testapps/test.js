{
	buffer_size:100,
	s:function(x) {
		var buffer = new Array();
		var sum=1; 
		var obj=new Object(); 
		for(i=1; i<this.buffer_size; i++){
			sum += i;
		} 
		obj.reply=sum; 
		obj.hello="world";
		
		dat1 = new Object();
		dat2 = new Object();
		buffer[0] = dat1;
		buffer[1] = dat2;
		obj.buffer = buffer;
		
		return obj;
	}
}