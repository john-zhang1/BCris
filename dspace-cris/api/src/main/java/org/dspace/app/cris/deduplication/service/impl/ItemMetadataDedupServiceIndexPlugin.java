/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.deduplication.service.impl;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.app.cris.deduplication.service.SolrDedupServiceIndexPlugin;
import org.dspace.content.IMetadataValue;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.Constants;
import org.dspace.core.Context;

public class ItemMetadataDedupServiceIndexPlugin
        implements SolrDedupServiceIndexPlugin
{

    private static final Logger log = Logger
            .getLogger(ItemMetadataDedupServiceIndexPlugin.class);

    private List<String> metadata;

    private String field;

    @Override
    public void additionalIndex(Context context, UUID firstId, UUID secondId, Integer type, SolrInputDocument document)
    {

        if (type == Constants.ITEM)
        {
            internal(context, firstId, document);
            if(firstId!=secondId) {
                internal(context, secondId, document);
            }
        }

    }

    private void internal(Context context, UUID itemId,
            SolrInputDocument document)
    {
        try
        {

            Item item = ContentServiceFactory.getInstance().getItemService().find(context, itemId);

            for (String meta : metadata)
            {
                for (IMetadataValue mm : ContentServiceFactory.getInstance().getItemService().getMetadataByMetadataString(item, meta))
                {
                    if (StringUtils.isNotEmpty(field))
                    {
                        document.addField(field, mm.getValue());
                    }
                    else
                    {
                        document.addField(mm.getMetadataField().toString('.') + "_s", mm.getValue());
                    }
                }
            }
        }
        catch (SQLException e)
        {
            log.error(e.getMessage(), e);
        }
    }

    public List<String> getMetadata()
    {
        return metadata;
    }

    public void setMetadata(List<String> metadata)
    {
        this.metadata = metadata;
    }

    public String getField()
    {
        return field;
    }

    public void setField(String field)
    {
        this.field = field;
    }

}
