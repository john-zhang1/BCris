/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.IMetadataValue;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.core.Context;
import org.dspace.core.service.PluginService;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Broker for ChoiceAuthority plugins, and for other information configured
 * about the choice aspect of authority control for a metadata field.
 *
 * Configuration keys, per metadata field (e.g. "dc.contributer.author")
 *
 * {@code
 *  # names the ChoiceAuthority plugin called for this field
 *  choices.plugin.<FIELD> = name-of-plugin
 *
 *  # mode of UI presentation desired in submission UI:
 *  #  "select" is dropdown menu, "lookup" is popup with selector, "suggest" is autocomplete/suggest
 *  choices.presentation.<FIELD> = "select" | "suggest"
 *
 *  # is value "closed" to the set of these choices or are non-authority values permitted?
 *  choices.closed.<FIELD> = true | false
 * }
 * @author Larry Stone
 * @see ChoiceAuthority
 */
public final class ChoiceAuthorityServiceImpl implements ChoiceAuthorityService
{
    private Logger log = Logger.getLogger(ChoiceAuthorityServiceImpl.class);

    // map of field key to authority plugin
    protected Map<String,ChoiceAuthority> controller = new HashMap<String,ChoiceAuthority>();

    // map of field key to presentation type
    protected Map<String,String> presentation = new HashMap<String,String>();

    // map of field key to closed value
    protected Map<String,Boolean> closed = new HashMap<String,Boolean>();

    // map of field key to authority plugin name
    private Map<String,String> md2authorityname = new HashMap<String,String>();
    
    @Autowired(required = true)
    protected ConfigurationService configurationService;
    @Autowired(required = true)
    protected PluginService pluginService;
    @Autowired(required = true)
    protected MetadataFieldService metadataFieldService;
    
    private final String CHOICES_PLUGIN_PREFIX = "choices.plugin.";
    private final String CHOICES_PRESENTATION_PREFIX = "choices.presentation.";
    private final String CHOICES_CLOSED_PREFIX = "choices.closed.";

    protected ChoiceAuthorityServiceImpl() {
    }

    // translate tail of configuration key (supposed to be schema.element.qual)
    // into field key
    protected String config2fkey(String field)
    {
        // field is expected to be "schema.element.qualifier"
        int dot = field.indexOf('.');
        if (dot < 0)
        {
            return null;
        }
        String schema = field.substring(0, dot);
        String element = field.substring(dot+1);
        String qualifier = null;
        dot = element.indexOf('.');
        if (dot >= 0)
        {
            qualifier = element.substring(dot+1);
            element = element.substring(0, dot);
        }
        return makeFieldKey(schema, element, qualifier);
    }

    @Override
    public Choices getMatches(String schema, String element, String qualifier,
                  String query, Collection collection, int start, int limit, String locale)
    {
        return getMatches(makeFieldKey(schema, element, qualifier), query,
                collection, start, limit, locale);
    }

    @Override
    public Choices getMatches(String fieldKey, String query, Collection collection,
            int start, int limit, String locale)
    {
        ChoiceAuthority ma = getChoiceAuthorityMap().get(fieldKey);
        if (ma == null)
        {
            throw new IllegalArgumentException(
                    "No choices plugin was configured for  field \"" + fieldKey
                            + "\".");
        }
        return ma.getMatches(fieldKey, query, collection, start, limit, locale);
    }

    @Override
    public Choices getMatches(String fieldKey, String query, Collection collection, int start, int limit, String locale, boolean externalInput) {
        ChoiceAuthority ma = getChoiceAuthorityMap().get(fieldKey);
        if (ma == null) {
            throw new IllegalArgumentException(
                    "No choices plugin was configured for  field \"" + fieldKey
                            + "\".");
        }
        if (externalInput && ma instanceof SolrAuthority) {
            ((SolrAuthority)ma).addExternalResultsInNextMatches();
        }
        return ma.getMatches(fieldKey, query, collection, start, limit, locale);
    }

    @Override
    public Choices getBestMatch(String fieldKey, String query, Collection collection,
            String locale)
    {
        ChoiceAuthority ma = getChoiceAuthorityMap().get(fieldKey);
        if (ma == null)
        {
            throw new IllegalArgumentException(
                    "No choices plugin was configured for  field \"" + fieldKey
                            + "\".");
        }
        return ma.getBestMatch(fieldKey, query, collection, locale);
    }

    @Override
    public String getLabel(IMetadataValue metadataValue, String locale)
    {
        return getLabel(metadataValue.getMetadataField().toString(), metadataValue.getAuthority(), locale);
    }

    @Override
    public String getLabel(String fieldKey, String authKey, String locale)
    {
        ChoiceAuthority ma = getChoiceAuthorityMap().get(fieldKey);
        if (ma == null)
        {
            throw new IllegalArgumentException("No choices plugin was configured for  field \"" + fieldKey + "\".");
        }
        return ma.getLabel(fieldKey, authKey, locale);
    }

    @Override
    public boolean isChoicesConfigured(String fieldKey)
    {
        return getChoiceAuthorityMap().containsKey(fieldKey);
    }

    @Override
    public String getPresentation(String fieldKey)
    {
        return getPresentationMap().get(fieldKey);
    }

    @Override
    public boolean isClosed(String fieldKey)
    {
        return getClosedMap().containsKey(fieldKey) && getClosedMap().get(fieldKey);
    }

    @Override
    public List<String> getVariants(IMetadataValue metadataValue)
    {
        ChoiceAuthority ma = getChoiceAuthorityMap().get(metadataValue.getMetadataField().toString());
        if (ma instanceof AuthorityVariantsSupport)
        {
            AuthorityVariantsSupport avs = (AuthorityVariantsSupport) ma;
            return avs.getVariants(metadataValue.getAuthority(), metadataValue.getLanguage());
        }
        return null;
    }
    
    /**
     * Wrapper to call plugin's getVariants().
     */
    public List<String> getVariants(String schema, String element,
            String qualifier, String authorityKey, String language)
    {
        ChoiceAuthority ma = controller.get(makeFieldKey(schema, element,
                qualifier));
        if (ma instanceof AuthorityVariantsSupport)
        {
            AuthorityVariantsSupport avs = (AuthorityVariantsSupport) ma;
            return avs.getVariants(authorityKey, language);
        }
        return null;
    }
    
    
    public String makeFieldKey(String schema, String element, String qualifier)
    {
    	return MetadataField.formKey(schema, element, qualifier);
    }

    /**
     * Construct a single key from the "dot" notation e.g. "dc.rights"
     */
    public static String makeFieldKey(String dotty)
    {
        return dotty.replace(".", "_");
    }
    
    /**
     * Return map of key to ChoiceAuthority plugin
     * @return
     */
    private Map<String,ChoiceAuthority> getChoiceAuthorityMap()
    {
        // If empty, load from configuration
        if(controller.isEmpty())
        {
            Context context = null;
            try 
            {
                context = new Context();
                String defaultKey =  makeFieldKey(MetadataSchema.DC_SCHEMA, "authority", "default");
                String defaultfKey = configurationService.getProperty(CHOICES_PLUGIN_PREFIX+MetadataSchema.DC_SCHEMA+".authority.default");
                ChoiceAuthority maDefault = (ChoiceAuthority) pluginService
                        .getNamedPlugin(ChoiceAuthority.class,
                        		configurationService.getProperty(defaultKey));
                List<MetadataField> tmp = metadataFieldService.findFieldsByElementNameUnqualified(context, MetadataSchema.DC_SCHEMA, "authority");
                for(MetadataField mf : tmp) {
                    String tmpKey = makeFieldKey(MetadataSchema.DC_SCHEMA, mf.getElement(), mf.getQualifier());
                    String tmpfKey = configurationService.getProperty(CHOICES_PLUGIN_PREFIX+MetadataSchema.DC_SCHEMA+"."+mf.getElement()+"."+mf.getQualifier());
                    ChoiceAuthority ma = (ChoiceAuthority) pluginService
                            .getNamedPlugin(ChoiceAuthority.class,
                            		configurationService.getProperty(tmpKey));
                    if (ma == null)
                    {
                        ma = maDefault;
                        tmpfKey = defaultfKey;
                    }

                    md2authorityname.put(tmpKey,
                    		configurationService.getProperty(tmpfKey));
                    controller.put(tmpKey, ma);
                }
            }
            catch (Exception e) {
                log.error(e.getMessage(), e);
            }
            
            // Get all configuration keys starting with a given prefix
            List<String> propKeys = configurationService.getPropertyKeys(CHOICES_PLUGIN_PREFIX);
            Iterator<String> keyIterator = propKeys.iterator();
            while(keyIterator.hasNext())
            {
                String key = keyIterator.next();
                String fkey = config2fkey(key.substring(CHOICES_PLUGIN_PREFIX.length()));
                if (fkey == null)
                {
                    log.warn("Skipping invalid ChoiceAuthority configuration property: "+key+": does not have schema.element.qualifier");
                    continue;
                }

                // XXX FIXME maybe add sanity check, call
                // MetadataField.findByElement to make sure it's a real field.
                ChoiceAuthority ma = (ChoiceAuthority)
                    pluginService.getNamedPlugin(ChoiceAuthority.class, configurationService.getProperty(key));
                if (ma == null)
                {
                    log.warn("Skipping invalid configuration for "+key+" because named plugin not found: "+configurationService.getProperty(key));
                    continue;
                }
                controller.put(fkey, ma);

                md2authorityname.put(fkey,
                        configurationService.getProperty(key));
                log.debug("Choice Control: For field="+fkey+", Plugin="+ma);
            }
        }

        return controller;
    }

    /**
     * Return map of key to presentation
     * @return
     */
    private Map<String,String> getPresentationMap()
    {
        // If empty, load from configuration
        if(presentation.isEmpty())
        {
            // Get all configuration keys starting with a given prefix
            List<String> propKeys = configurationService.getPropertyKeys(CHOICES_PRESENTATION_PREFIX);
            Iterator<String> keyIterator = propKeys.iterator();
            while(keyIterator.hasNext())
            {
                String key = keyIterator.next();

                String fkey = config2fkey(key.substring(CHOICES_PRESENTATION_PREFIX.length()));
                if (fkey == null)
                {
                    log.warn("Skipping invalid ChoiceAuthority configuration property: "+key+": does not have schema.element.qualifier");
                    continue;
                }
                presentation.put(fkey, configurationService.getProperty(key));
            }
        }

        return presentation;
    }

    /**
     * Return map of key to closed setting
     * @return
     */
    private Map<String,Boolean> getClosedMap()
    {
        // If empty, load from configuration
        if(closed.isEmpty())
        {
            // Get all configuration keys starting with a given prefix
            List<String> propKeys = configurationService.getPropertyKeys(CHOICES_CLOSED_PREFIX);
            Iterator<String> keyIterator = propKeys.iterator();
            while(keyIterator.hasNext())
            {
                String key = keyIterator.next();

                String fkey = config2fkey(key.substring(CHOICES_CLOSED_PREFIX.length()));
                if (fkey == null)
                {
                    log.warn("Skipping invalid ChoiceAuthority configuration property: "+key+": does not have schema.element.qualifier");
                    continue;
                }
                closed.put(fkey, configurationService.getBooleanProperty(key));
            }
        }

        return closed;
    }


    /**
     * @deprecated
     * 
     * Wrapper that calls reject method of the plugin corresponding
     * 
     */
    public void notifyReject(int itemID, String schema, String element,
            String qualifier, String authorityKey)
    {
        String makeFieldKey = makeFieldKey(schema, element,
                qualifier);
        ChoiceAuthority ma = getChoiceAuthorityMap().get(makeFieldKey);
        if(ma == null) {
            reloadCache(makeFieldKey);
        }
        if (ma instanceof NotificableAuthority)
        {
            NotificableAuthority avs = (NotificableAuthority) ma;
            avs.reject(itemID, authorityKey);
        }
    }

    /**
     * @deprecated
     * 
     * Wrapper that calls accept potential method of the plugin corresponding
     * 
     */
    public void notifyAccept(int itemID, String schema, String element,
			String qualifier, String authorityKey, int confidence)
    {
        String makeFieldKey = makeFieldKey(schema, element,
                qualifier);
        ChoiceAuthority ma = getChoiceAuthorityMap().get(makeFieldKey);
        if(ma == null) {
            reloadCache(makeFieldKey);
        }
        if (ma instanceof NotificableAuthority)
        {
            NotificableAuthority avs = (NotificableAuthority) ma;
			avs.accept(itemID, authorityKey, confidence);
        }
    }
        
    /**
     * @deprecated
     * 
     * Wrapper that calls reject method of the plugin corresponding
     * 
     */
    public void notifyReject(int[] itemIDs, String schema, String element,
            String qualifier, String authorityKey)
    {
        String makeFieldKey = makeFieldKey(schema, element,
                qualifier);
        ChoiceAuthority ma = getChoiceAuthorityMap().get(makeFieldKey);
        if(ma == null) {
            reloadCache(makeFieldKey);
        }
        if (ma instanceof NotificableAuthority)
        {
            NotificableAuthority avs = (NotificableAuthority) ma;
            avs.reject(itemIDs, authorityKey);
        }
    }

    /**
     * Wrapper that calls reject method of the plugin corresponding
     * 
     */
    public void notifyReject(UUID itemID, String schema, String element,
            String qualifier, String authorityKey)
    {
        String makeFieldKey = makeFieldKey(schema, element,
                qualifier);
        ChoiceAuthority ma = getChoiceAuthorityMap().get(makeFieldKey);
        if(ma == null) {
            reloadCache(makeFieldKey);
        }
        if (ma instanceof NotificableAuthority)
        {
            NotificableAuthority avs = (NotificableAuthority) ma;
            avs.reject(itemID, authorityKey);
        }
    }

    /**
     * Wrapper that calls accept potential method of the plugin corresponding
     * 
     */
    public void notifyAccept(UUID itemID, String schema, String element,
			String qualifier, String authorityKey, int confidence)
    {
        String makeFieldKey = makeFieldKey(schema, element,
                qualifier);
        ChoiceAuthority ma = getChoiceAuthorityMap().get(makeFieldKey);
        if(ma == null) {
            reloadCache(makeFieldKey);
        }
        if (ma instanceof NotificableAuthority)
        {
            NotificableAuthority avs = (NotificableAuthority) ma;
			avs.accept(itemID, authorityKey, confidence);
        }
    }
        
    /**
     * Wrapper that calls reject method of the plugin corresponding
     * 
     */
    public void notifyReject(UUID[] itemIDs, String schema, String element,
            String qualifier, String authorityKey)
    {
        String makeFieldKey = makeFieldKey(schema, element,
                qualifier);
        ChoiceAuthority ma = getChoiceAuthorityMap().get(makeFieldKey);
        if(ma == null) {
            reloadCache(makeFieldKey);
        }
        if (ma instanceof NotificableAuthority)
        {
            NotificableAuthority avs = (NotificableAuthority) ma;
            avs.reject(itemIDs, authorityKey);
        }
    }
    
    public Object getDetailsInfo(String field, String key, String locale)
    {
        ChoiceAuthority ma = getChoiceAuthorityMap().get(field);
        if(ma == null) {
            reloadCache(field);
        }
        if (ma instanceof ChoiceAuthorityDetails)
        {
        	ChoiceAuthorityDetails avs = (ChoiceAuthorityDetails) ma;
            return avs.getDetailsInfo(field, key, locale);
        }
        return null;
    }
    
    
    public Set<String> getAuthorities()
    {
    	getChoiceAuthorityMap();
        Set<String> set = new HashSet<String>();
        for (String alias : md2authorityname.values())
        {
            set.add(alias);
        }
        return set;
    }
    
    public List<String> getAuthorityMetadataForAuthority(String authorityName)
    {
    	getChoiceAuthorityMap();
        List<String> result = new LinkedList<String>();
        for (String md : md2authorityname.keySet())
        {
            if (md2authorityname.get(md).equalsIgnoreCase(authorityName))
            {
                result.add(md.replaceAll("_", "\\."));
            }
        }
        return result;
    }
    
    private ChoiceAuthority reloadCache(String fieldKey)
    {
        ChoiceAuthority ma = null;
        if(fieldKey.contains("_authority_")) {
            reloadCache();
            ma = getChoiceAuthorityMap().get(fieldKey);
        }
        return ma;
    }
    
    public void reloadCache() {
        controller.clear();        
    }

    public String getAuthorityName(String fieldKey) {
        return md2authorityname.get(fieldKey);
    }
    
    public ChoiceAuthority getChoiceAuthority(String metadata) {
        String fieldKey = makeFieldKey(metadata);
        return getChoose(fieldKey);
    }

    public ChoiceAuthority getChoiceAuthority(String schema, String element,
            String qualifier) {
        String fieldKey = makeFieldKey(schema, element, qualifier);
        return getChoose(fieldKey);
    }
    
    private ChoiceAuthority getChoose(String fieldKey) {
        ChoiceAuthority ma = getChoiceAuthorityMap().get(fieldKey);
        if (ma == null)
        {
            ma = reloadCache(fieldKey);
            if(ma == null) {
                throw new IllegalArgumentException(
                        "No choices plugin was configured for  field \""
                                + fieldKey + "\".");
            }
        }
        return ma;
    }

	@Override
	public String getLabel(String schema, String element, String qualifier, String authKey, String locale) {
		return getLabel(makeFieldKey(schema, element, qualifier), authKey, locale);
	}

}
