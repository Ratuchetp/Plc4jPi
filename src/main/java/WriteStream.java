import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

public class WriteStream {
    private Statement statement;
    WriteStream() throws ClassNotFoundException, SQLException {
        Connection connection = null;
        Class.forName("org.apache.iotdb.jdbc.IoTDBDriver");
        //Configure connection information.Include ip address and user information.
        System.out.println("Please input the iotdb-client information:");
        Scanner sc = new Scanner(System.in);
        String[] cliInf=sc.nextLine().split(" ");

        connection = DriverManager.getConnection("jdbc:iotdb://"+cliInf[0]+"/", cliInf[1],cliInf[2]);
        statement = connection.createStatement();
    }


    public void insert(List t){
        try {
            SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
            String time=df.format(new Date());
            String sqlStence=String.format("INSERT INTO root.plc4jDemo.sensor(timestamp, sensor0) VALUES (%s,%d)",time,t.get(0));
            System.out.println(time+" "+t.get(0));
            statement.execute(sqlStence);
        } catch(SQLException e) {
            e.printStackTrace();
        }
    }
}