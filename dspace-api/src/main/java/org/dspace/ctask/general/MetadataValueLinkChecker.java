/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.ctask.general;

import java.util.ArrayList;
import java.util.List;

import org.dspace.content.IMetadataValue;
import org.dspace.content.Item;

/**
 * A link checker that builds upon the BasicLinkChecker to check URLs that
 * appear in all metadata fields where the field starts with http:// or https://
 *
 * Of course this assumes that there is no extra metadata following the URL.
 *
 * @author Stuart Lewis
 */
public class MetadataValueLinkChecker extends BasicLinkChecker {

    @Override
    protected List<String> getURLs(Item item)
    {
        // Get all metadata elements that start with http:// or https://
        List<IMetadataValue> urls = itemService.getMetadata(item, Item.ANY, Item.ANY, Item.ANY, Item.ANY);
        ArrayList<String> theURLs = new ArrayList<String>();
        for (IMetadataValue url : urls)
        {
            if ((url.getValue().startsWith("http://")) || (url.getValue().startsWith("https://")))
            {
                theURLs.add(url.getValue());
            }
        }
        return theURLs;
    }
}
