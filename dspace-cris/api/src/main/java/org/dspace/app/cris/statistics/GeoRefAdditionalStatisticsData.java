/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.statistics;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.browse.BrowsableDSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.statistics.SolrLoggerServiceImpl;
import org.dspace.statistics.StatisticsMetadataGenerator;
import org.dspace.statistics.util.LocationUtils;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;


public class GeoRefAdditionalStatisticsData implements
        StatisticsMetadataGenerator
{
	private DatabaseReader locationService;

    private static Logger log = Logger
            .getLogger(GeoRefAdditionalStatisticsData.class);

    @Override
    public void addMetadata(SolrInputDocument doc1, HttpServletRequest request,
    		BrowsableDSpaceObject dspaceObject)
    {
        String ip = (String) doc1.getFieldValue("ip");
        if (ip == null)
            return;
        // Save the location information if valid, save the event without
        // location information if not valid
        if (ConfigurationManager.getBooleanProperty(
        		SolrLoggerServiceImpl.CFG_USAGE_MODULE, "randomize-localhost", false)
                && ip.equals("127.0.0.1"))
        {
            ip = "";
            for (int j = 0; j < 4; j++)
            {
                ip += getRandomNumberInRange(0, 254);
                if (j != 3)
                    ip += ".";
            }
            String dns = (String) doc1.getFieldValue("dns");
            if (StringUtils.isEmpty(dns))
            {
                doc1.addField("dns",
                        new String[] { "cilea.it", "enel.com", "hub.hku.hk",
                                "mit.edu" }[(int) getRandomNumberInRange(0, 3)]);
            }
        }

        String dnsValue = (String) doc1.getFieldValue("dns");
        if (StringUtils.isNotBlank(dnsValue )){
        	int firstIndexof =  dnsValue.indexOf(".");
        	if(firstIndexof != -1){
        		doc1.addField("domaindns", dnsValue.substring(firstIndexof));
        	}
        	
        	if( dnsValue != doc1.getFieldValue("ip")){
        		int lastIndexOf = dnsValue.lastIndexOf(".");
        		if (lastIndexOf != -1)
        		{
        			doc1.addField("topdomain", dnsValue.substring(lastIndexOf));
        		}
       		}
        }

        try {
            InetAddress ipAddress = InetAddress.getByName(ip);
            DatabaseReader locationService = getLocationService();
            if (locationService != null)
            {
                CityResponse location = locationService.city(ipAddress);
                String countryCode = location.getCountry().getIsoCode();
                double latitude = location.getLocation().getLatitude();
                double longitude = location.getLocation().getLongitude();
                if (!("--".equals(countryCode) && latitude == -180
                        && longitude == -180))
                {

                    doc1.addField("countryCode", countryCode);
                    doc1.addField("city", location.getCity().getName());
                    doc1.addField("latitude", latitude);
                    doc1.addField("longitude", longitude);
                    doc1.addField("location", latitude + "," + longitude);
                    if (countryCode != null)
                    {
                        try
                        {
                            doc1.addField("continent", LocationUtils
                                    .getContinentCode(countryCode));
                        }
                        catch (Exception e)
                        {
                            System.out.println("COUNTRY ERROR: " + countryCode);
                        }
                    }
                }
            }
        } catch (IOException | GeoIp2Exception e) {
            log.warn("Unable to get location of request: " + e.getMessage());
        }
    
    }

    private static long getRandomNumberInRange(long min, long max)
    {
        return min + (long) (Math.random() * ((max - min) + 1));
    }

    public DatabaseReader getLocationService()
    {

        if (locationService == null)
        {
            
            DatabaseReader service = null;
            // Get the db file for the location
            String dbPath = ConfigurationManager.getProperty(SolrLoggerServiceImpl.CFG_USAGE_MODULE, "dbfile");
            if (dbPath != null) {
                try {
                    File dbFile = new File(dbPath);
                    service = new DatabaseReader.Builder(dbFile).build();
                } catch (FileNotFoundException fe) {
                    log.error(
                        "The GeoLite Database file is missing (" + dbPath + ")! Solr Statistics has been generated without location " +
                            "based reports! GeoLite databases are now managed outside of DSpace. " +
                            "Please see the DSpace installation instructions for more information.",
                        fe);
                } catch (IOException e) {
                    log.error(
                        "Unable to load GeoLite Database file (" + dbPath + ")! You may need to reinstall it. " +
                                "GeoLite databases are now managed outside of DSpace. " +
                                "Please see the DSpace installation instructions for more information.",
                        e);
                }
            }
            else
            {
                log.error("The required 'dbfile' configuration is missing in usage-statistics.cfg! " +
                        "GeoLite databases are now managed outside of DSpace. " +
                        "Please see the DSpace installation instructions for more information.");
            }
            locationService = service;
        }
        return locationService;
    }
}
