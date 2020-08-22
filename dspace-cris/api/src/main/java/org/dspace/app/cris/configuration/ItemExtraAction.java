/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.dspace.app.cris.integration.RPAuthority;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.model.RelationPreference;
import org.dspace.content.IMetadataValue;
import org.dspace.content.Item;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.factory.ContentAuthorityServiceFactory;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.core.Context;

public class ItemExtraAction implements RelationPreferenceExtraAction
{
    private String relationName;
    
    private String authorityName;

    private List<String> metadata;
    
//    @Required
    public void setRelationName(String relationName)
    {
        this.relationName = relationName;
    }

    public void setAuthorityName(String authorityName)
    {
        this.authorityName = authorityName;
    }

    public void setMetadata(List<String> metadata)
    {
        this.metadata = metadata;
    }

    public String getRelationName()
    {
        return relationName;
    }

    public String getAuthorityName()
    {
        if(this.authorityName==null) {
            this.authorityName = RPAuthority.RP_AUTHORITY_NAME;
        }
        return authorityName;
    }

    public List<String> getMetadata()
    {
        return metadata;
    }

    @Override
    public boolean executeExtraAction(Context context, ACrisObject cris,
            UUID itemID, String previousAction, int previousPriority,
            String action, int priority)
    {
        if ((!RelationPreference.UNLINKED.equals(previousAction) && !RelationPreference.UNLINKED.equals(action)) 
                || (RelationPreference.UNLINKED.equals(previousAction) && RelationPreference.UNLINKED.equals(action)))
        {
            return false;
        }
        else if (RelationPreference.UNLINKED.equals(action))
        {
            unlink(context, cris, itemID);
            // it is not necessary to reindex the item, the index.update already
            // do
            // this trought solr consumer
            return true;
        }
        else if (RelationPreference.UNLINKED.equals(previousAction))
        {
            link(context, cris, itemID);
            // it is not necessary to reindex the item, the index.update already
            // do
            // this trought solr consumer
            return true;
        }
        throw new IllegalStateException(
                "Invalid action and/or previousAction: " + action + " / "
                        + previousAction);
    }

	protected void unlink(Context context, ACrisObject cris, UUID itemID)
    {
        try
        {
            Item item = ContentServiceFactory.getInstance().getItemService().find(context, itemID);
            String rpKey = cris.getCrisID();
            ChoiceAuthorityService cam = ContentAuthorityServiceFactory.getInstance().getChoiceAuthorityService();
            if (metadata == null)
            {
                metadata = cam.getAuthorityMetadataForAuthority(getAuthorityName());
            }
            for (String issued : metadata)
            {
                String[] metadata = issued.split("\\.");
                List<IMetadataValue> original = item.getItemService().getMetadataByMetadataString(item, issued);
                String schema = metadata[0];
                String element = metadata[1];
                String qualifier = metadata.length > 2 ? metadata[2] : null;
                item.getItemService().clearMetadata(context, item, schema, element, qualifier, Item.ANY);
                for (IMetadataValue md : original)
                {
                    if (rpKey.equals(md.getAuthority()))
                    {
                        md.setAuthority(null);
                        md.setConfidence(Choices.CF_UNSET);
						// commented the next java line as it produces a cycle
						// the RPAuthority invoke the relationPreference as the
						// default
						// authorityManagementServlet of DSpace is not yet aware
						// of the
						// relationPreferenceService... when fixed we should
						// emit a signal to the cam
                        // cam.notifyReject(item.getID(), schema, element,
                        // qualifier, rpKey);
                    }
                    item.getItemService().addMetadata(context, item, md.getSchema(), md.getElement(), md.getQualifier(),
                            md.getLanguage(), md.getValue(), md.getAuthority(), md.getConfidence());

                }
            }
            context.commit();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

	protected void link(Context context, ACrisObject cris, UUID itemID)
    {
        try
        {
            Item item = ContentServiceFactory.getInstance().getItemService().find(context, itemID);
            String rpKey = cris.getCrisID();
            // the item is not linked with the RP
            ChoiceAuthorityService cam = ContentAuthorityServiceFactory.getInstance().getChoiceAuthorityService();
            metadata = cam
                    .getAuthorityMetadataForAuthority(getAuthorityName());

            List<String> names = new ArrayList<String>();
            String fieldKey = metadata.get(0);
            String[] split = fieldKey.split("\\.");
            String fschema = split[0];
            String felement = split[1];
            String fqualifier = split.length > 2 ? split[2] : null;
            names.add(cam.getLabel(fschema, felement, fqualifier, rpKey, null));
            List<String> variants = cam.getVariants(fschema, felement, fqualifier,
                    rpKey, null);
            if (variants != null)
            {
                names.addAll(variants);
            }

            for (String issued : metadata)
            {
                String[] metadata = issued.split("\\.");
                List<IMetadataValue> original = item.getItemService().getMetadataByMetadataString(item, issued);
                String schema = metadata[0];
                String element = metadata[1];
                String qualifier = metadata.length > 2 ? metadata[2] : null;
                item.getItemService().clearMetadata(context, item, schema, element, qualifier, Item.ANY);

                for (IMetadataValue md : original)
                {
                    for (String tempName : names)
                    {
                        if (md.getAuthority() == null && md.getValue().equals(tempName))
                        {
                            md.setAuthority(rpKey);
                            md.setConfidence(Choices.CF_ACCEPTED);
                        }
                        else if (rpKey.equals(md.getAuthority()))
                        {
                            // low confidence value
                            md.setConfidence(Choices.CF_ACCEPTED);
                        }
                    }
                    item.getItemService().addMetadata(context, item, md.getSchema(), md.getElement(), md.getQualifier(),
                            md.getLanguage(), md.getValue(), md.getAuthority(), md.getConfidence());
                }
            }

			// commented the next java line as it produces a cycle
			// the RPAuthority invoke the relationPreference as the default
			// authorityManagementServlet of DSpace is not yet aware of the
			// relationPreferenceService... when fixed we should emit a signal
			// to the cam
			// cam.notifyAccept(item.getID(), schema, element,
			// qualifier, rpKey);

            context.commit();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
