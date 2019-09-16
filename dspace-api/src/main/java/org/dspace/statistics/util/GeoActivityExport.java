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
import java.text.DateFormat;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.text.SimpleDateFormat;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Calendar;
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
    private static ConfigurationService config = new DSpace().getConfigurationService();
    public static final int MAX_EVENTS = 250;
    
    
    private static LookupService geoipLookup;
    String dbfile = ConfigurationManager.getProperty("usage-statistics", "dbfile");
    private Map<String, Map<String, String>> ipGeoList = new TreeMap<>();
    private List<GeoMapData> geos = new ArrayList<>();

    public GeoActivityExport()
    {
    }

    public void convert(String in) throws FileNotFoundException
    {
        BufferedReader input = new BufferedReader(new FileReader(in));
        LogAnalyser.setRegex(in);

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

    private void writeGeoData() throws FileNotFoundException {
        List<String> files =  getFileNames();

        for(String name : files) {
            try {
                convert(name);
            } catch(Exception e) {
                System.err.println("File " + name + " encountered error.");
            }
        }

        try {
            List<ActionRecord> records = getRecords();
            getGeoList(records);
            String fileName = "geos.json";
            File baseDir = ensureGeosDir();
            String filePath = baseDir.getAbsolutePath() + "/" + fileName;
            writeGeoMapData(filePath);
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(GeoActivityExport.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public List<ActionRecord> getRecords()
    {
    	List<ActionRecord> list = new ArrayList<>();
    	synchronized (actionRecords) {
	    	list.addAll(actionRecords);
    	}
    	return list;
    }

    public static void main(String[] args) throws SQLException, FileNotFoundException
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

        geoExporter.writeGeoData();

        context.restoreAuthSystemState();
        context.abort();
    }

    public void getGeoList(List<ActionRecord> records) throws IOException {
        float longitude = 0f;
        float latitude = 0f;
        String city = null;
        String countryCode = null;
        String countryName = null;

        // Get unique IP addresses
        Map<String, String> ipAddresses = new TreeMap<>();
        long currentTime = System.currentTimeMillis();
        for(ActionRecord record : records){
            String key = record.getIP();

            if ((ipAddresses.get(key) == null) && isWithinOneDay(currentTime, record.getTimeStamp()))
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
                city = location.city;
                items = addItems("city", city, items);
                countryCode = location.countryCode;
                items = addItems("countryCode", countryCode, items);
                countryName = location.countryName;
                items = addItems("countryName", countryName, items);
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
            String city = ipGeoList.get(key).get("city");
            String countryCode = ipGeoList.get(key).get("countryCode");
            String countryName = ipGeoList.get(key).get("countryName");
            GeoMapData geoMapData = new GeoMapData(lat, lng, time, ip, city, countryCode, countryName);
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

    private Date yesterday() {
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        return cal.getTime();
    }
    private String getDateString(Date date) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        return dateFormat.format(date);
    }

    private Date today() {
	final Calendar cal = Calendar.getInstance();
        return cal.getTime();
    }

    // Get two days' file names (today and yesterday)
    private List<String> getFileNames() {
        List<String> names = new ArrayList<>();
        String yesterday = getDateString(yesterday());
        String today = getDateString(today());
        String dir = config.getProperty("log.dir");
        String baseFile = "dspace.log";
        String yesterdayFile = dir + File.separator + baseFile + "." + yesterday;
        String todayFile = dir + File.separator + baseFile + "." + today;
        names.add(yesterdayFile);
        names.add(todayFile);
        return names;
    }

    protected File ensureGeosDir()
    {
        String dir = config.getProperty("geo.json.dir");
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

    private boolean isWithinOneDay(long currentTime, long timeStamp) {
        long time = timeStamp*1000;
        return (currentTime - time) < Long.valueOf(24 * 60 * 60 * 1000);
    }

    public class GeoMapData {
        private String latitude;
        private String longitude;
        private String time;
        private String ip;
        private String city;
        private String countryCode;
        private String countryName;

        public GeoMapData(String latitude, String longitude, String time, String ip, String city, String countryCode, String countryName) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.time = time;
            this.ip = ip;
            this.city = city;
            this.countryCode = countryCode;
            this.countryName = countryName;
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

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getCountryName() {
            return countryName;
        }

        public void setCountryName(String countryName) {
            this.countryName = countryName;
        }

    }


    public class ActionRecord {

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
