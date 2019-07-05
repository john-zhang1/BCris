package org.dspace.statistics.util;

import com.google.gson.Gson;
import com.maxmind.geoip.Location;
import com.maxmind.geoip.LookupService;
import org.apache.commons.cli.*;
import org.apache.log4j.Logger;
import org.dspace.app.statistics.LogAnalyser;
import org.dspace.app.statistics.LogLine;
import org.dspace.core.Context;

import java.io.*;
import java.sql.SQLException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.text.SimpleDateFormat;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;
import java.util.logging.Level;
import org.dspace.core.ConfigurationManager;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;

public class GeoActivityExport {
    private final Logger log = Logger.getLogger(GeoActivityExport.class);

    private final Pattern ipaddrPattern = Pattern.compile("ip_addr=(\\d*\\.\\d*\\.\\d*\\.\\d*):");
    private final SimpleDateFormat dateFormatIn = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static Queue<ActionRecord> actionRecords = new LinkedList<ActionRecord>();
    public static final int MAX_EVENTS = 250;
    
    
    private static LookupService geoipLookup;
    String dbfile = ConfigurationManager.getProperty("usage-statistics", "dbfile");
    private Map<String, Map<String, String>> ipGeoList = new TreeMap<>();
    private List<GeoMapData> geos = new ArrayList<>();

    public GeoActivityExport()
    {
    }

    public int convert(String in)
    {
        int counter = 0;

        BufferedReader input;
        try {
            if (null == in || in.isEmpty() || "-".equals(in))
            {
                input = new BufferedReader(new InputStreamReader(System.in));
                in = "standard input";
            }
            else
                input = new BufferedReader(new FileReader(in));
        } catch (IOException ie) {
            log.error("File access problem", ie);
            return 0;
        }

        // Setup the regular expressions for the log file
        LogAnalyser.setRegex(in);

        synchronized (actionRecords) {
            // Open the file and read it line by line
            try {
                String line;
                LogLine lline;
                String ip = null;
                String lastIP = "";
                Long lastTimeStamp = 0L;

                while ((line = input.readLine()) != null)
                {
                    lline = LogAnalyser.getLogLine(line);
                    ActionRecord actionRecord = null;

                    if ((lline == null) || (!lline.isLevel("INFO")))
                    {
                        continue;
                    }

                    // Get the IP address of the user
                    Matcher matcher = ipaddrPattern.matcher(line);
                    if (matcher.find())
                    {
                        ip = matcher.group(1);
                        System.out.println("IP is: " + ip);

                        Date date = dateFormatIn.parse(line.substring(0, line.indexOf(',')), new ParsePosition(0));
                        long unixTime = (long)date.getTime()/1000;

                        if((lastTimeStamp == unixTime) && (lastIP == ip)) {
                            continue;
                        } else {
                            actionRecord = new ActionRecord(unixTime, ip);
                            actionRecords.add(actionRecord);
                            lastIP = ip;
                            lastTimeStamp = unixTime;                            
                        }
                    }

                    while (actionRecords.size() > MAX_EVENTS)
                    {
                        actionRecords.poll();
                    }
                }

                System.out.println(actionRecords.size());
                for (ActionRecord ac : actionRecords){
                    System.out.println("ip=" + ac.getIP() + ", time=" + Long.toString(ac.getTimeStamp()));
                }
            }
            catch (IOException e)
            {
                log.error("File access problem", e);
            }
            finally
            {
                try { input.close();  } catch (IOException e) { log.error(e.getMessage(), e); }
            }
        }
        
        List<ActionRecord> recordList= getRecords();

        try {
            getGeoList(recordList);
            String fileName = "geos.json";
            File baseDir = ensureGeosDir();
            String filePath = baseDir.getAbsolutePath() + "/" + fileName;
            writeGeoMapData(filePath);
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(GeoActivityExport.class.getName()).log(Level.SEVERE, null, ex);
        }

        return counter;
    }

    public static List<ActionRecord> getRecords()
    {
    	List<ActionRecord> list = new ArrayList<>();
    	synchronized (actionRecords) {
	    	list.addAll(actionRecords);
    	}
    	return list;
    }

    public static void main(String[] args) throws SQLException
    {
        CommandLineParser parser = new PosixParser();

        Options options = new Options();

        options.addOption("i", "in", true, "source file ('-' or omit for standard input)");

        CommandLine line;
        try
        {
            line = parser.parse(options, args);
        }
        catch (ParseException pe)
        {
            System.err.println("Error parsing command line arguments: " + pe.getMessage());
            System.exit(1);
            return;
        }

        // Create a copy of the exporter
        Context context = new Context();
        context.turnOffAuthorisationSystem();
        GeoActivityExport geoExporter = new GeoActivityExport();

        try
        {
            LogAnalyser.readConfig();
        }
        catch (IOException ioe)
        {
            System.err.println("Unable to read config file: " + LogAnalyser.getConfigFile());
            System.exit(1);
        }

        geoExporter.convert(line.getOptionValue('i'));

        context.restoreAuthSystemState();
        context.abort();
    }

    public void getGeoList(List<ActionRecord> records) throws IOException {
        float longitude = 0f;
        float latitude = 0f;

        // Get unique IP addresses
        Map<String, String> ipAddresses = new TreeMap<>();
        for(ActionRecord record : records){
            String key = record.getIP();
            if (ipAddresses.get(key) == null)
            {
                ipAddresses.put(key, Long.toString(record.getTimeStamp()));
            }
        }
        
        if(geoipLookup == null) {
            geoipLookup = new LookupService(dbfile, LookupService.GEOIP_STANDARD);
        }
        
        for (String key : ipAddresses.keySet()){
            String value = ipAddresses.get(key);

            Map<String, String> items = new TreeMap<>();
            items = addItems("time", value, items);
            items = addItems("ip", key, items);

            Location location;
            try {
                location = geoipLookup.getLocation(key);
                latitude = location.latitude;
                items = addItems("latitude", Float.toString(latitude), items);
                longitude = location.longitude;
                items = addItems("longitude", Float.toString(longitude), items);
                addMap(key, items);
            } catch(Exception e) {

            }                
        }
    }

    private Map<String, String> add(String key, String value)
    {
        Map<String, String> items = new TreeMap<>();
        if (items.get(key) == null)
        {
            items.put(key, value);
        }
        return items;
    }

    private Map<String, String> addItems(String key, String value, Map<String, String> items)
    {
        if (items.get(key) == null)
        {
            items.put(key, value);
        }
        return items;
    }

    private void addMap(String key, Map<String, String> value)
    {
        if (ipGeoList.get(key) == null)
        {
            ipGeoList.put(key, value);
        }
    }


    public void getGeoMapData() {
        for(String key : ipGeoList.keySet()) {
            String lat = ipGeoList.get(key).get("latitude");
            String lng = ipGeoList.get(key).get("longitude");
            String time = ipGeoList.get(key).get("time");
            String ip = ipGeoList.get(key).get("ip");
            GeoMapData geoMapData = new GeoMapData(lat, lng, time, ip);
            addGeoMapData(geoMapData);
        }
    }

    public void addGeoMapData(GeoMapData geoMapData) {
        geos.add(geoMapData);
    }

    public String toJson() {
        getGeoMapData();
        Gson gson = new Gson();
        return gson.toJson(geos);
    }


    protected File ensureGeosDir()
    {
        ConfigurationService config = new DSpace().getConfigurationService();
        String dir = config.getProperty("geo.ip.dir");
        File baseDir = new File(dir);
        if (!baseDir.exists() && !baseDir.mkdirs())
        {
            throw new IllegalStateException("Unable to create directories");
        }

        return baseDir;
    }

    public void writeGeoMapData(String outFile) {
        try 
        {
            BufferedWriter out = new BufferedWriter(new FileWriter(outFile));
            out.write(toJson());
            out.flush();
            out.close();
        } 
        catch (IOException e) 
        {
            System.out.println("Unable to write to output file " + outFile);
            System.exit(0);
        }

    }

    public static class GeoMapData {
        private String latitude;
        private String longitude;
        private String time;
        private String ip;

        public GeoMapData(String latitude, String longitude, String time, String ip) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.time = time;
            this.ip = ip;
        }

        public String getLatitude() {
            return latitude;
        }

        public void setLatitude(String latitude) {
            this.latitude = latitude;
        }

        public String getLongitude() {
            return longitude;
        }

        public void setLongitude(String longitude) {
            this.longitude = longitude;
        }

        public String getTime() {
            return time;
        }

        public void setTime(String time) {
            this.time = time;
        }

        public String getIP() {
            return ip;
        }

        public void setIP(String ip) {
            this.ip = ip;
        }

    }


    public static class ActionRecord {

        private long timeStamp;
        private String ip;

        public ActionRecord(long timeStamp, String ip) {
            this.timeStamp = timeStamp;
            this.ip = ip;
        }

        public long getTimeStamp() {
            return timeStamp;
        }

        public void setTimeStamp(long timeStamp) {
            this.timeStamp = timeStamp;
        }

        public String getIP() {
            return ip;
        }

        public void setIP(String ip) {
            this.ip = ip;
        }

    }
}
