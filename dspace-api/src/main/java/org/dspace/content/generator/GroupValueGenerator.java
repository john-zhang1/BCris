/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.generator;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.IMetadataValue;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;

public class GroupValueGenerator implements TemplateValueGenerator
{

    private static Logger log = Logger.getLogger(GroupValueGenerator.class);

    @Override
    public List<IMetadataValue> generator(Context context, Item targetItem, Item templateItem,
            IMetadataValue IMetadataValue, String extraParams)
    {
        String[] params = StringUtils.split(extraParams, "\\.");
        String prefix = params[0];
        String suffix = "";
        if (params.length > 1)
        {
            suffix = params[1];
        }
        String value = prefix;
        try
        {
            if (StringUtils.startsWith(prefix, "community"))
            {
                String metadata = prefix.substring("community[".length(),
                        prefix.length() - 1);

                Collection collection = (Collection)templateItem.getItemService().getParentObject(context, templateItem);
                value = ((Community)collection.getDSpaceObjectService().getParentObject(context, collection)).getMetadata(metadata);

            }
            else if (StringUtils.startsWith(prefix, "collection"))
            {
                String metadata = prefix.substring("collection[".length(),
                        prefix.length() - 1);
                Collection collection = (Collection)templateItem.getItemService().getParentObject(context, templateItem);
                value = collection.getMetadata(metadata);
            }
            else if (StringUtils.startsWith(prefix, "item"))
            {
                value = targetItem.getMetadata(prefix.replace("_", "."));
            }
        }
        catch (SQLException e)
        {
            log.error(e.getMessage());
        }

        if (StringUtils.isNotBlank(suffix))
        {
            value = value + "-" + suffix;
        }

        List<IMetadataValue> m = new ArrayList<>();
        m.add(IMetadataValue);
        Group group = null;
        try
        {
            group = EPersonServiceFactory.getInstance().getGroupService().findByName(context, value);
        }
        catch (SQLException e)
        {
            log.error(e.getMessage());
        }
        String result = "";
        if(group!=null) {
            result = "" + group.getID();
        }        
        IMetadataValue.setValue(result);
        return m;
    }
}
