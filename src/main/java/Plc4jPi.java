import org.apache.commons.lang3.Range;
import org.apache.plc4x.edgent.PlcConnectionAdapter;
import org.apache.plc4x.edgent.PlcFunctions;
import org.apache.plc4x.java.api.exceptions.PlcException;
import org.apache.plc4x.java.api.messages.PlcReadRequest;
import org.apache.plc4x.java.api.messages.PlcReadResponse;
import org.apache.plc4x.java.base.util.HexUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.edgent.function.Supplier;
import org.apache.edgent.function.BiFunction;
import org.apache.edgent.providers.direct.DirectProvider;
import org.apache.edgent.topology.TStream;
import org.apache.edgent.topology.Topology;
import org.apache.edgent.topology.plumbing.PlumbingStreams;


import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class Plc4jPi {
    public static void main(String[] args) {
        System.out.println("Start...");
        Scanner sc = new Scanner(System.in);
        System.out.println("Please input the plcconnection url:");
        String plcconnectionurl = sc.nextLine();
        System.out.println("Please input the register address:");
        String address = sc.nextLine();

        switch (address.split("\\:")[0]) {
            case "readcoil":
            case "readdiscreteinputs":
                try {
                    boolReader(plcconnectionurl, address);
                } catch (SQLException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                break;

            case "readholdingregisters":
            case "readinputregisters":
                try {
                    digitalReader(plcconnectionurl, address);
                } catch (PlcException e) {
                    e.printStackTrace();
                } catch (SQLException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                break;
        }
    }


    public static void boolReader(String plcconnectionurl, String address) throws SQLException, ClassNotFoundException {//Read the contents of the Coil status  (same as the Input status )
        System.out.println("boolReader:URL=" + plcconnectionurl + "\t" + "address=" + address);
        //Create an instance of PlcConnectionAdapter with a parameter format of "mode of communication ://IP address: port number"
        PlcConnectionAdapter plcAdapter = new PlcConnectionAdapter(plcconnectionurl);
        //Create a provider
        DirectProvider dp = new DirectProvider();
        //Create a topological
        Topology top = dp.newTopology();
        //Create the Supplier instance to request the data, where the Coil status  returns a List<Boolean>
        //Set the polling register location, addressStr, to read the address and scope of the register. The format is "register : start address [offset range]"
        Supplier<List<Boolean>> plcSupplier = PlcFunctions.booleanListSupplier(plcAdapter, address);
        //A new TStream instance accepts the stream data for polling, which is set to once per 500ms.
        TStream<List<Boolean>> source = top.poll(plcSupplier, 500, TimeUnit.MILLISECONDS);
        //Write the received data to iotdb
        WriteStream writeToIotdb = new WriteStream();
        source.sink(tuple -> {
            writeToIotdb.insert(tuple);
        });
        //submit
        System.out.println("submit");
        dp.submit(top);
    }


    public static void digitalReader(String plcconnectionurl, String address) throws PlcException, SQLException, ClassNotFoundException {//read Holding registers contents (same as Input registers)
        System.out.println("digitalReader:URL=" + plcconnectionurl + " " + "address=" + address);
        //This part is similar to the operation of the bool status
        PlcConnectionAdapter plcAdapter = new PlcConnectionAdapter(plcconnectionurl);
        DirectProvider dp = new DirectProvider();
        Topology top = dp.newTopology();

        //Create a new PLC4J read request instance and set the register type, address, and scope
        PlcReadRequest readRequest = plcAdapter.readRequestBuilder()
                .addItem("test1", address)
                .build();
        //Create a new Supplier instance and request the PlcReadResponse as per the established PlcReadRequest
        Supplier<PlcReadResponse> plcSupplier = PlcFunctions.batchSupplier(plcAdapter, readRequest);
        //Set up the poll, when the returned data stream consists of PlcReadResponse.Parse each PlcReadResponse into a list of integers
        TStream<List<Integer>> source = top.poll(plcSupplier, 500, TimeUnit.MILLISECONDS)
                .map(v -> {
                    Integer[] info = v.getAllByteArrays("test1")
                            .stream()
                            .map(HexUtil::toHex)
                            .map(s -> Integer.parseInt(StringUtils.deleteWhitespace(s.substring(0, 15)), 16))
                            .toArray(Integer[]::new);
                    return Arrays.asList(info);
                });
        //Write the received data to iotdb
        WriteStream writeToIotdb = new WriteStream();
        source.sink(tuple -> {
            writeToIotdb.insert(tuple);
        });

        //submit
        System.out.println("submit");
        dp.submit(top);
    }
}


//    public static void filter() throws PlcException {//Filter test
//        int upperLimit = 50;
//        int lowLimit = 60;
//        Range<Integer> range = Range.between(lowLimit, upperLimit);
//        //Take polling Holding register as an example to establish polling
//        PlcConnectionAdapter plcAdapter = new PlcConnectionAdapter("modbus:tcp://127.0.0.1:502");
//        DirectProvider dp = new DirectProvider();
//        Topology top = dp.newTopology();
//
//        PlcReadRequest readRequest = plcAdapter.readRequestBuilder()
//                .addItem("test2", "readholdingregisters:0[3]")
//                .build();
//        Supplier<PlcReadResponse> plcSupplier = PlcFunctions.batchSupplier(plcAdapter, readRequest);
//        TStream<List<Integer>> source = top.poll(plcSupplier, 1, TimeUnit.SECONDS)
//                .map(v -> {
//                    Integer[] info = v.getAllByteArrays("test2")
//                            .stream()
//                            .map(HexUtil::toHex)
//                            .map(s -> Integer.parseInt(StringUtils.deleteWhitespace(s.substring(0, 15)), 16))
//                            .toArray(Integer[]::new);
//                    return Arrays.asList(info);
//                })
//                //Added filtering, which filters out lists in the data stream whose first register value is not between 50 and 60
//                .filter(v -> !range.contains(v.get(0)));
//        source.print();
////        TSink<List<Integer>> sink=source.sink();
//        dp.submit(top);
//    }
//
//
//    public static void parallelReading() throws PlcException {//Parallel analysis test
//        PlcConnectionAdapter plcAdapter = new PlcConnectionAdapter("modbus:tcp://127.0.0.1:502");
//        DirectProvider dp = new DirectProvider();
//        Topology top = dp.newTopology();
//
//        PlcReadRequest readRequest = plcAdapter.readRequestBuilder()
//                .addItem("test5", "readholdingregisters:0[3]")
//                .build();
//        Supplier<PlcReadResponse> plcSupplier = PlcFunctions.batchSupplier(plcAdapter, readRequest);
//        //Create a high-speed polling environment
//        TStream<PlcReadResponse> source = top.poll(plcSupplier, 100, TimeUnit.MILLISECONDS);
//
//        //Set up parallel analysis flow and process three tuples at the same time
//        TStream<List<Integer>> sourceNew = PlumbingStreams.parallelBalanced(source, 3, pipline());
//        sourceNew.sink(tuple -> System.out.println(tuple));
//        dp.submit(top);
//    }
//    //Build the analysis pipline, which writes the processing method for each stream
//    public static BiFunction<TStream<PlcReadResponse>, Integer, TStream<List<Integer>>> pipline() {
//        return (stream, channal) ->
//                stream.map(v -> {
//                    Integer[] info = v.getAllByteArrays("test5")
//                            .stream()
//                            .map(HexUtil::toHex)
//                            .map(s -> Integer.parseInt(StringUtils.deleteWhitespace(s.substring(0, 15)), 16))
//                            .toArray(Integer[]::new);
//                    return Arrays.asList(info);
//                });
//
//    }






