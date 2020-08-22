/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.components;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.dspace.content.Bitstream;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.statistics.ObjectCount;
import org.dspace.statistics.SolrLoggerServiceImpl;
import org.dspace.statistics.service.SolrLoggerService;
import org.springframework.beans.factory.annotation.Autowired;

public class MostViewedBitstreamManager
{

    Logger log = Logger.getLogger(MostViewedBitstreamManager.class);

    private final String TYPE = "0";

    private final String STATISTICS_TYPE = SolrLoggerServiceImpl.StatisticsType.VIEW
            .text();

    private int maxResults;

    private String time_period;

    @Autowired
    private SolrLoggerService solrLoggerService;
    
    public MostViewedBitstreamManager(int max, String period)
    {
        maxResults = max;
        time_period = period;
    }

    public MostViewedBitstreamManager()
    {
        maxResults = 10;
        time_period = "*";

    }

    public List<MostViewedItem> getMostViewed(Context context)
            throws SolrServerException, SQLException
    {

        String query = "statistics_type:" + STATISTICS_TYPE;
        String filterQuery = "type:" + TYPE;

        if (StringUtils.isNotBlank(time_period))
        {
            filterQuery += " AND time:[" + time_period + " TO *]";
        }

        ObjectCount[] oc = solrLoggerService.queryFacetField(query, filterQuery, "id",
                maxResults, false, null);

        List<MostViewedItem> viewedList = new ArrayList<MostViewedItem>();
        for (int x = 0; x < oc.length; x++)
        {
            UUID id = UUID.fromString(oc[x].getValue());
            Bitstream bitstream = ContentServiceFactory.getInstance().getBitstreamService().find(context, id);
            DSpaceObject dspaceObj = ContentServiceFactory.getInstance().getBitstreamService().getParentObject(context, bitstream);
            if (dspaceObj != null)
            {
                if (Constants.ITEM == dspaceObj.getType())
                {
                    Item item = (Item) dspaceObj;
                    if (!item.isWithdrawn())
                    {
                        MostViewedItem mvi = new MostViewedItem();
                        mvi.setBitstreamName(bitstream.getName());
                        mvi.setItem(item);
                        mvi.setVisits("" + oc[x].getCount());
                        viewedList.add(mvi);
                    }
                }
            }
            else
            {
                log.warn("A DELETED ITEM IS IN STATISTICS? bitstreamId:" + id);
            }
        }
        return viewedList;

    }

    public int getMaxResults()
    {
        return maxResults;
    }

    public void setMaxResults(int maxResults)
    {
        this.maxResults = maxResults;
    }

    public String getTime_period()
    {
        return time_period;
    }

    public void setTime_period(String time_period)
    {
        this.time_period = time_period;
    }

	public SolrLoggerService getSolrLoggerService() {
		return solrLoggerService;
	}

	public void setSolrLoggerService(SolrLoggerService solrLoggerService) {
		this.solrLoggerService = solrLoggerService;
	}

}
