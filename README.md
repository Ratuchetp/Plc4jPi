# Plc4jPi
Using the plc4j integrated edgent interface to read PLC register information through modbus tcp/ip protocol on Raspberry Pi,then write the data to iotdb.

Preparations:
1.Iotdb server is running;
2.PLC ip address;
3.Iotdb ip address, port and user imformation;
4.Set ip address of Pi and PLC in the same network segment.
  (eth0)
5.Set ip address of Pi and iotdb in the same network segment.
  (wlan0)
  PS:You can get your ip information by inputing
	ifconfig
     in terminal.

Use:
1.Open terminal under this folder, input
	sh start.sh
2.Input PLC connection url,the fromat is 
	{prorocol}://{PLC ip}:{port}
  For example:
	modbus:tcp://192.168.3.250:502
3.Input modbus registers address,the format is :
	{registers}:{start address}[{offset}]
  For example:
        readholdingregisters:0[1]
  There are four registers supported.You can input:
  readcoil/readdiscreteinputs to read bool registers;
  readholdingregisters/readinputregisters to read digital registers.
4.Input iotdb-client login information,the format is:
  	{iotdb ip address}:{port} username password
  For example:
        192.168.130.177:6667 root root

You will see the data flow printed when successful.
Attention
1.Only support modbus:tcp protocol.
2.The data will write to root.plc4jDemo.sensor.sensor0 in iotdb default.
	
	
