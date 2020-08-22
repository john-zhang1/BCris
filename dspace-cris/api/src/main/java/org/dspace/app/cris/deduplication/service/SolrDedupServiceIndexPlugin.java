/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.deduplication.service;

import java.util.UUID;

import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;

public interface SolrDedupServiceIndexPlugin
{
    public void additionalIndex(Context context, UUID dsoFirst, UUID dsoSecond, Integer type, SolrInputDocument document);
}
