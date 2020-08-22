/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.IMetadataValue;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;

/**
 * Implements virtual field processing for split pagenumber range information.
 * 
 * @author bollini
 */
public class VirtualFieldPageNumber implements VirtualFieldDisseminator, VirtualFieldIngester {
	public String[] getMetadata(Item item, Map<String, String> fieldCache, String fieldName) {
		// Check to see if the virtual field is already in the cache
		// - processing is quite intensive, so we generate all the values on
		// first request
		if (fieldCache.containsKey(fieldName))
			return new String[] { fieldCache.get(fieldName) };

		String[] virtualFieldName = fieldName.split("\\.");

		String qualifier = virtualFieldName[2];
		String separator = " - ";
		if (qualifier.equals("bibtex")) {
			separator = "--";
		}

		// Get metadata 
		String firstpage = (StringUtils.isNotEmpty(
				ConfigurationManager.getProperty("crosswalk.virtualfieldpage.firstpage.metadata"))) ?
				ConfigurationManager.getProperty("crosswalk.virtualfieldpage.firstpage.metadata") :
				"dc.relation.firstpage";
		String lastpage = (StringUtils.isNotEmpty(
				ConfigurationManager.getProperty("crosswalk.virtualfieldpage.lastpage.metadata"))) ?
				ConfigurationManager.getProperty("crosswalk.virtualfieldpage.lastpage.metadata") :
				"dc.relation.lastpage";		

		// Get pages from the item
		List<IMetadataValue> dcvs = item.getMetadataValueInDCFormat(firstpage);
		List<IMetadataValue> dcvs2 = item.getMetadataValueInDCFormat(lastpage);

		if ((dcvs != null && dcvs.size() > 0) && (dcvs2 != null && dcvs2.size() > 0)) {
			String value = dcvs.get(0).getValue() + separator + dcvs2.get(0).getValue();
			fieldCache.put(fieldName, value);
			return new String[] { value };
		}

		return null;
	}

	public boolean addMetadata(Item item, Map<String, String> fieldCache, String fieldName, String value) {
		// NOOP - we won't add any metadata yet, we'll pick it up when we
		// finalise the item
		return true;
	}

	public boolean finalizeItem(Item item, Map<String, String> fieldCache) {
		return false;
	}
}
