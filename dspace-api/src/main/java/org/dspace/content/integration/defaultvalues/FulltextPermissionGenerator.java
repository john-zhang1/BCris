/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.defaultvalues;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.Group;

public class FulltextPermissionGenerator implements EnhancedValuesGenerator
{
	/** log4j logger */
    private static Logger log = Logger.getLogger(FulltextPermissionGenerator.class);
    
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
    
    @Override
    public DefaultValuesBean generateValues(Item item, String schema,
            String element, String qualifier, String value)
    {
        Context context = null;
        DefaultValuesBean result = new DefaultValuesBean();
        String values = "none";
        try
        {
            context = new Context();
            result.setLanguage("en");
            result.setMetadataSchema(schema);
            result.setMetadataElement(element);
            result.setMetadataQualifier(qualifier);
            List<Bundle> bnds = new ArrayList<Bundle>();
            try
            {
            	List<Bundle> obnds = item.getItemService().getBundles(item, Constants.DEFAULT_BUNDLE_NAME);
                List<Bundle> dbnds = item.getItemService().getBundles(item, "DISPLAY");
                bnds.addAll(dbnds);
                bnds.addAll(obnds);
            }
            catch (SQLException e)
            {
                throw new RuntimeException(e.getMessage(), e);
            }

            values = buildPermission(context, values, bnds);
        }
        catch (Exception ex)
        {
        	log.error(ex.getMessage(), ex);
        }
        result.setValues(values);
        return result;
    }

    private String buildPermission(Context context, String values,
    		List<Bundle> bnds) throws SQLException
    {
        Date now = new Date();
        
        int count = 0;
        int openAccess = 0;
        int restricted = 0;
        int groupRestricted = 0;
        int withEmbargo = 0;
        int withRestrictedAndEmbargo = 0;
        
        Date firstDateExpirationForEmbargo = null;
        Date lastDateExpirationForEmbargo = null;
        
        external: for (Bundle bnd : bnds)
        {
            internal: for (Bitstream b : bnd.getBitstreams())
            {
                count++;
                List<ResourcePolicy> rps = AuthorizeServiceFactory.getInstance().getAuthorizeService()
                        .getPoliciesActionFilter(context, b, Constants.READ);
                boolean bRestricted = true;
                if (rps != null)
                {
                    for (ResourcePolicy rp : rps)
                    {
                        if (Group.ANONYMOUS.equals(rp.getGroup().getName()))
                        {
                            if (rp.isDateValid())
                            {
                                openAccess++;
                                bRestricted = false;
                                break internal;
                            }
                            else if (rp.getEndDate() == null
                                    || rp.getEndDate().after(now))
                            {
                                withEmbargo++;
                                if (firstDateExpirationForEmbargo == null
                                        || firstDateExpirationForEmbargo
                                                .after(rp.getStartDate()))
                                {
                                    firstDateExpirationForEmbargo = rp.getStartDate();
                                }
                                if (lastDateExpirationForEmbargo == null || lastDateExpirationForEmbargo
                                        .before(rp.getStartDate()))
                                {
                                    lastDateExpirationForEmbargo = rp.getStartDate();
                                }
                                
                                //no embargo but this is an expired policy
                                bRestricted = false;
                                break internal;
                            }
                        }
                        else if (bRestricted && !Group.ADMIN.equals(rp.getGroup().getName()))
                        {
                            if (rp.isDateValid())
                            {
                                groupRestricted++;
                                bRestricted = false;
                                break internal;
                            }
                            else if (rp.getEndDate() == null
                                    || rp.getEndDate().after(now))
                            { 
                                withRestrictedAndEmbargo++;
                                if (firstDateExpirationForEmbargo == null
                                        || firstDateExpirationForEmbargo
                                                .after(rp.getStartDate()))
                                {
                                    firstDateExpirationForEmbargo = rp.getStartDate();
                                }
                                if (lastDateExpirationForEmbargo == null || lastDateExpirationForEmbargo
                                        .before(rp.getStartDate()))
                                {
                                    lastDateExpirationForEmbargo = rp.getStartDate();
                                }
                                
                                //no embargo but this is an expired policy
                                bRestricted = false;
                                break internal;
                            }
                        }
                    }
                }
                if (bRestricted)
                {
                    restricted++;
                }
            }
        }
        
        //if there are fulltext build the values
        if (count > 0) {
            if (openAccess > 0) {
                if (restricted > 0 || withEmbargo > 0 || withRestrictedAndEmbargo > 0 || groupRestricted > 0) {
                    // some bitstream are reserved
                    values = "mixedopen";
                } else {
                    // open access
                    values = "open";
                }
            } else if (withEmbargo > 0 && groupRestricted == 0 && withRestrictedAndEmbargo == 0 && restricted == 0) {
                // all embargoed
                values = "embargo_" + sdf.format(firstDateExpirationForEmbargo)
                        + (!firstDateExpirationForEmbargo.equals(lastDateExpirationForEmbargo) ? "_" + sdf.format(lastDateExpirationForEmbargo) : "");
            } else if (groupRestricted > 0 && withEmbargo == 0 && withRestrictedAndEmbargo == 0 && restricted == 0) {
                // all restricted
                values = "restricted";
            } else if (withRestrictedAndEmbargo > 0 && groupRestricted == 0 && withEmbargo == 0 && restricted == 0) {
                // embargoed group
                values  = "embargo_restricted_" + sdf.format(firstDateExpirationForEmbargo)
                        + (!firstDateExpirationForEmbargo.equals(lastDateExpirationForEmbargo) ? "_" + sdf.format(lastDateExpirationForEmbargo) : "");
            } else if (restricted > 0 && groupRestricted == 0 && withEmbargo == 0 && withRestrictedAndEmbargo == 0) {
                // all reserved
                values = "reserved";
            } else {
                // no openaccess but mixed situation
                values = "mixedrestricted";
            }
        }
        return values;
    }

}
