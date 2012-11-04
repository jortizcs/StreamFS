process.stdin.resume();

process.stdin.setEncoding('utf8');

//process.stdin.pipe(process.stdout, { end: true });
process.stdin.on('data', function(data){
        console.log("node::" + data + "\n");
        });
process.stdin.on("end", 
        function() {
            process.stdout.write("node::Goodbye\n"); 
            process.exit();
            }
        );

function dienow(){
    console.log("node::goodbye\n");
    process.exit();
}

setTimeout(dienow, 20000);
