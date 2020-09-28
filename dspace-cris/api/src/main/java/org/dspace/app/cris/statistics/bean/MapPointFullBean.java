/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.statistics.bean;

public class MapPointFullBean implements java.io.Serializable
{
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private String latitude;

    private String longitude;

    private String city;

    private String countryCode;

    public MapPointFullBean(String latitude, String longitude, String city, String countryCode)
    {
        super();
        this.latitude = latitude;
        this.longitude = longitude;
        this.city = city;
        this.countryCode = countryCode;
    }

    public String getLatitude()
    {
        return latitude;
    }

    public void setLatitude(String latitude)
    {
        this.latitude = latitude;
    }

    public String getLongitude()
    {
        return longitude;
    }

    public void setLongitude(String longitude)
    {
        this.longitude = longitude;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

}
