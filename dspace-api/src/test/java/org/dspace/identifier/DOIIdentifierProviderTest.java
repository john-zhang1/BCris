/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import org.apache.log4j.Logger;
import org.dspace.AbstractUnitTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.identifier.factory.IdentifierServiceFactory;
import org.dspace.identifier.service.DOIService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.workflow.WorkflowException;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.factory.WorkflowServiceFactory;
import org.junit.*;
import static org.junit.Assert.*;
import static org.junit.Assume.*;

/**
* Tests for {@link DataCiteIdentifierProvider}.
*
* @author Mark H. Wood
* @author Pascal-Nicolas Becker
*/
public class DOIIdentifierProviderTest
        extends AbstractUnitTest
{
    /** log4j category */
    private static final Logger log = Logger.getLogger(DOIIdentifierProviderTest.class);
    
    private static final String PREFIX = "10.5072";
    private static final String NAMESPACE_SEPARATOR = "dspaceUnitTests-";

    private static ConfigurationService config = null;

    protected DOIService doiService = IdentifierServiceFactory.getInstance().getDOIService();
    protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();


    private static Community community;
    private static Collection collection;

    private static MockDOIConnector connector;
    private DOIIdentifierProvider provider;

    public DOIIdentifierProviderTest()
    {
    }

    /**
     * This method will be run before every test as per @Before. It will
     * initialize resources required for the tests.
     *
     * Other methods can be annotated with @Before here or in subclasses
     * but no execution order is guaranteed
     */
    @Before
    @Override
    public void init()
    {
        super.init();
        
        try
        {
            context.turnOffAuthorisationSystem();
            // Create an environment for our test objects to live in.
            community = communityService.create(null, context);
            communityService.setMetadata(context, community, "name", "A Test Community");
            communityService.update(context, community);
            collection = collectionService.create(context, community);
            collectionService.setMetadata(context, collection, "name", "A Test Collection");
            collectionService.update(context, collection);
            //we need to commit the changes so we don't block the table for testing
            context.restoreAuthSystemState();

            config = DSpaceServicesFactory.getInstance().getConfigurationService();
            // Configure the service under test.
            config.setProperty(DOIIdentifierProvider.CFG_PREFIX, PREFIX);
            config.setProperty(DOIIdentifierProvider.CFG_NAMESPACE_SEPARATOR, 
                NAMESPACE_SEPARATOR);
        
            connector = new MockDOIConnector();
            
            provider = DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName(DOIIdentifierProvider.class.getName(), DOIIdentifierProvider.class);
            provider.setConfigurationService(config);
            provider.setDOIConnector(connector);
        }
        catch (AuthorizeException ex)
        {
            log.error("Authorization Error in init", ex);
            fail("Authorization Error in init: " + ex.getMessage());
        }
        catch (SQLException ex)
        {
            log.error("SQL Error in init", ex);
            fail("SQL Error in init: " + ex.getMessage());
        }
        
    }
    
     /**
     * This method will be run after every test as per @After. It will
     * clean resources initialized by the @Before methods.
     *
     * Other methods can be annotated with @After here or in subclasses
     * but no execution order is guaranteed
     */
    @After
    @Override
    public void destroy()
    {
        community = null;
        collection = null;
        connector.reset();
        connector = null;
        provider = null;
        super.destroy();
    }

    /**
    * Create a fresh Item, installed in the repository.
    *
    * @throws SQLException if database error
    * @throws AuthorizeException if authorization error
    * @throws IOException if IO error
    */
    private Item newItem()
            throws SQLException, AuthorizeException, IOException, IllegalAccessException, IdentifierException, WorkflowException
    {
        context.turnOffAuthorisationSystem();

        WorkspaceItem wsItem = workspaceItemService.create(context, collection, false);

        WorkflowItem wfItem = WorkflowServiceFactory.getInstance().getWorkflowService().start(context, wsItem);

        Item item = wfItem.getItem();
        itemService.addMetadata(context, item, "dc", "contributor", "author", null, "Author, A. N.");
        itemService.addMetadata(context, item, "dc", "title", null, null, "A Test Object");
        itemService.addMetadata(context, item, "dc", "publisher", null, null, "DSpace Test Harness");

        // If DOIIdentifierProvider is configured
        // (dspace/conf/spring/api/identifier-service.xml) the new created item
        // gets automatically a DOI. We remove this DOI as it can make problems
        // with the tests.
        provider.delete(context, item);
        
        List<IMetadataValue> metadata = itemService.getMetadata(item,
                DOIIdentifierProvider.MD_SCHEMA, 
                DOIIdentifierProvider.DOI_ELEMENT,
                DOIIdentifierProvider.DOI_QUALIFIER,
                null);
        List<String> remainder = new ArrayList<String>();

        for (IMetadataValue id : metadata) {
            if (!id.getValue().startsWith(DOI.RESOLVER))
            {
                remainder.add(id.getValue());
            }
        }

        itemService.clearMetadata(context, item,
                DOIIdentifierProvider.MD_SCHEMA,
                DOIIdentifierProvider.DOI_ELEMENT,
                DOIIdentifierProvider.DOI_QUALIFIER,
                null);
        itemService.addMetadata(context, item, DOIIdentifierProvider.MD_SCHEMA,
                DOIIdentifierProvider.DOI_ELEMENT,
                DOIIdentifierProvider.DOI_QUALIFIER,
                null,
                remainder);
        
        itemService.update(context, item);
        //we need to commit the changes so we don't block the table for testing
        context.restoreAuthSystemState();

        return item;
    }
    
    public String createDOI(Item item, Integer status, boolean metadata)
            throws SQLException, IdentifierException, AuthorizeException
    {
        return this.createDOI(item, status, metadata, null);
    }
    
    /**
     * Create a DOI to an item.
     * @param item Item the DOI should be created for.
     * @param status The status of the DOI.
     * @param metadata Whether the DOI should be included in the metadata of the item.
     * @param doi The doi or null if we should generate one.
     * @return the DOI
     * @throws SQLException if database error
     */
    public String createDOI(Item item, Integer status, boolean metadata, String doi)
            throws SQLException, IdentifierException, AuthorizeException
    {
        context.turnOffAuthorisationSystem();
        // we need some random data. UUIDs would be bloated here
        Random random = new Random();
        if (null == doi)
        {
            doi = DOI.SCHEME + PREFIX + "/" + NAMESPACE_SEPARATOR 
                    + Long.toHexString(new Date().getTime()) + "-"
                    + random.nextInt(997);
        }
        
        DOI doiRow = doiService.create(context);
        doiRow.setDoi(doi.substring(DOI.SCHEME.length()));
        doiRow.setDSpaceObject(item);
        doiRow.setStatus(status);
        doiService.update(context, doiRow);
        
        if (metadata)
        {
            itemService.addMetadata(context, item, DOIIdentifierProvider.MD_SCHEMA,
                    DOIIdentifierProvider.DOI_ELEMENT,
                    DOIIdentifierProvider.DOI_QUALIFIER,
                    null,
                    doiService.DOIToExternalForm(doi));
            itemService.update(context, item);
        }
        
        //we need to commit the changes so we don't block the table for testing
        context.restoreAuthSystemState();
        return doi;
    }
    
    /**
     * Test of supports method, of class DataCiteIdentifierProvider.
     */
    @Test
    public void testSupports_Class()
    {        
        Class<? extends Identifier> identifier = DOI.class;
        assertTrue("DOI should be supported", provider.supports(identifier));
    }
    
    @Test
    public void testSupports_valid_String()
    {
        String[] validDOIs = new String[]
        {
            "10.5072/123abc-lkj/kljl",
            PREFIX + "/" + NAMESPACE_SEPARATOR + "lkjljasd1234",
            DOI.SCHEME + "10.5072/123abc-lkj/kljl",
            "http://dx.doi.org/10.5072/123abc-lkj/kljl",
            DOI.RESOLVER + "/10.5072/123abc-lkj/kljl"
        };
        
        for (String doi : validDOIs)
        {
            assertTrue("DOI should be supported", provider.supports(doi));
        }
    }
    
    @Test
    public void testDoes_not_support_invalid_String()
    {
        String[] invalidDOIs = new String[]
        {
            "11.5072/123abc-lkj/kljl",
            "http://hdl.handle.net/handle/10.5072/123abc-lkj/kljl",
            "",
            null
        };
        
        for (String notADoi : invalidDOIs)
        {
            assertFalse("Invalid DOIs shouldn't be supported",
                    provider.supports(notADoi));
        }
    }
    
    @Test
    public void testStore_DOI_as_item_metadata()
            throws SQLException, AuthorizeException, IOException, IdentifierException, IllegalAccessException, WorkflowException
    {
        Item item = newItem();
        String doi = DOI.SCHEME + PREFIX + "/" + NAMESPACE_SEPARATOR 
                + Long.toHexString(new Date().getTime());
        context.turnOffAuthorisationSystem();
        provider.saveDOIToObject(context, item, doi);
        context.restoreAuthSystemState();
        
        List<IMetadataValue> metadata = itemService.getMetadata(item, DOIIdentifierProvider.MD_SCHEMA,
                DOIIdentifierProvider.DOI_ELEMENT,
                DOIIdentifierProvider.DOI_QUALIFIER,
                null);
        boolean result = false;
        for (IMetadataValue id : metadata)
        {
            if (id.getValue().equals(doiService.DOIToExternalForm(doi)))
            {
                result = true;
            }
        }
        assertTrue("Cannot store DOI as item metadata value.", result);
    }
    
    @Test
    public void testGet_DOI_out_of_item_metadata()
            throws SQLException, AuthorizeException, IOException, IdentifierException, IllegalAccessException, WorkflowException
    {
        Item item = newItem();
        String doi = DOI.SCHEME + PREFIX + "/" + NAMESPACE_SEPARATOR 
                + Long.toHexString(new Date().getTime());

        context.turnOffAuthorisationSystem();
        itemService.addMetadata(context, item, DOIIdentifierProvider.MD_SCHEMA,
                DOIIdentifierProvider.DOI_ELEMENT,
                DOIIdentifierProvider.DOI_QUALIFIER,
                null,
                doiService.DOIToExternalForm(doi));
        itemService.update(context, item);
        context.restoreAuthSystemState();

        assertTrue("Failed to recognize DOI in item metadata.", 
                doi.equals(provider.getDOIOutOfObject(item)));
    }

    @Test
    public void testRemove_DOI_from_item_metadata()
            throws SQLException, AuthorizeException, IOException, IdentifierException, WorkflowException, IllegalAccessException
    {
        Item item = newItem();
        String doi = DOI.SCHEME + PREFIX + "/" + NAMESPACE_SEPARATOR 
                + Long.toHexString(new Date().getTime());

        context.turnOffAuthorisationSystem();
        itemService.addMetadata(context, item, DOIIdentifierProvider.MD_SCHEMA,
                DOIIdentifierProvider.DOI_ELEMENT,
                DOIIdentifierProvider.DOI_QUALIFIER,
                null,
                doiService.DOIToExternalForm(doi));
        itemService.update(context, item);
        
        provider.removeDOIFromObject(context, item, doi);
        context.restoreAuthSystemState();
        
        List<IMetadataValue> metadata = itemService.getMetadata(item, DOIIdentifierProvider.MD_SCHEMA,
                DOIIdentifierProvider.DOI_ELEMENT,
                DOIIdentifierProvider.DOI_QUALIFIER,
                null);
        boolean foundDOI = false;
        for (IMetadataValue id : metadata)
        {
            if (id.getValue().equals(doiService.DOIToExternalForm(doi)))
            {
                foundDOI = true;
            }
        }
        assertFalse("Cannot remove DOI from item metadata.", foundDOI);
    }
    
    @Test
    public void testGet_DOI_by_DSpaceObject()
            throws SQLException, AuthorizeException, IOException,
            IllegalArgumentException, IdentifierException, WorkflowException, IllegalAccessException
    {
        Item item = newItem();
        String doi = this.createDOI(item, DOIIdentifierProvider.IS_REGISTERED, false);
        
        String retrievedDOI = provider.getDOIByObject(context, item);
        
        assertNotNull("Failed to load DOI by DSpaceObject.", retrievedDOI);
        assertTrue("Loaded wrong DOI by DSpaceObject.", doi.equals(retrievedDOI));
    }
    
    @Test
    public void testGet_DOI_lookup()
            throws SQLException, AuthorizeException, IOException,
            IllegalArgumentException, IdentifierException, WorkflowException, IllegalAccessException
    {
        Item item = newItem();
        String doi = this.createDOI(item, DOIIdentifierProvider.IS_REGISTERED, false);
        
        String retrievedDOI = provider.lookup(context, (DSpaceObject) item);
        
        assertNotNull("Failed to loookup doi.", retrievedDOI);
        assertTrue("Loaded wrong DOI on lookup.", doi.equals(retrievedDOI));
    }
    
    @Test
    public void testGet_DSpaceObject_by_DOI()
            throws SQLException, AuthorizeException, IOException,
            IllegalArgumentException, IdentifierException, WorkflowException, IllegalAccessException
    {
        Item item = newItem();
        String doi = this.createDOI(item, DOIIdentifierProvider.IS_REGISTERED, false);
        
        DSpaceObject dso = provider.getObjectByDOI(context, doi);
        
        assertNotNull("Failed to load DSpaceObject by DOI.", dso);
        if (item.getType() != dso.getType() || item.getID() != dso.getID())
        {
            fail("Object loaded by DOI was another object then expected!");
        }
    }

    @Test
    public void testResolve_DOI()
            throws SQLException, AuthorizeException, IOException,
            IllegalArgumentException, IdentifierException, WorkflowException, IllegalAccessException
    {
        Item item = newItem();
        String doi = this.createDOI(item, DOIIdentifierProvider.IS_REGISTERED, false);
        
        DSpaceObject dso = provider.resolve(context, doi);
        
        assertNotNull("Failed to resolve DOI.", dso);
        if (item.getType() != dso.getType() || item.getID() != dso.getID())
        {
            fail("Object return by DOI lookup was another object then expected!");
        }
    }

    /*
     * The following test seems a bit silly, but it was helpful to debug some
     * problems while deleting DOIs.
     */
    @Test
    public void testRemove_two_DOIs_from_item_metadata()
            throws SQLException, AuthorizeException, IOException, IdentifierException, WorkflowException, IllegalAccessException
    {
        // add two DOIs.
        Item item = newItem();
        String doi1 = this.createDOI(item, DOIIdentifierProvider.IS_REGISTERED, true);
        String doi2 = this.createDOI(item, DOIIdentifierProvider.IS_REGISTERED, true);
        
        // remove one of it
        context.turnOffAuthorisationSystem();
        provider.removeDOIFromObject(context, item, doi1);
        context.restoreAuthSystemState();

        // assure that the right one was removed
        List<IMetadataValue> metadata = itemService.getMetadata(item, DOIIdentifierProvider.MD_SCHEMA,
                DOIIdentifierProvider.DOI_ELEMENT,
                DOIIdentifierProvider.DOI_QUALIFIER,
                null);
        boolean foundDOI1 = false;
        boolean foundDOI2 = false;
        for (IMetadataValue id : metadata)
        {
            if (id.getValue().equals(doiService.DOIToExternalForm(doi1)))
            {
                foundDOI1 = true;
            }
            if (id.getValue().equals(doiService.DOIToExternalForm(doi2)))
            {
                foundDOI2 = true;
            }

        }
        assertFalse("Cannot remove DOI from item metadata.", foundDOI1);
        assertTrue("Removed wrong DOI from item metadata.", foundDOI2);
        
        // remove the otherone as well.
        context.turnOffAuthorisationSystem();
        provider.removeDOIFromObject(context, item, doi2);
        context.restoreAuthSystemState();
        
        // check it
        metadata = itemService.getMetadata(item, DOIIdentifierProvider.MD_SCHEMA,
                DOIIdentifierProvider.DOI_ELEMENT,
                DOIIdentifierProvider.DOI_QUALIFIER,
                null);
        foundDOI1 = false;
        foundDOI2 = false;
        for (IMetadataValue id : metadata)
        {
            if (id.getValue().equals(doiService.DOIToExternalForm(doi1)))
            {
                foundDOI1 = true;
            }
            if (id.getValue().equals(doiService.DOIToExternalForm(doi2)))
            {
                foundDOI2 = true;
            }

        }
        assertFalse("Cannot remove DOI from item metadata.", foundDOI1);
        assertFalse("Cannot remove DOI from item metadata.", foundDOI2);
    }
    
    @Test
    public void testMintDOI() throws SQLException, AuthorizeException, IOException, IllegalAccessException, IdentifierException, WorkflowException
    {
        Item item = newItem();
        String doi = null;
        try
        {
            // get a DOI:
            doi = provider.mint(context, item);
        }
        catch(IdentifierException e)
        {
            e.printStackTrace();
            fail("Got an IdentifierException: " + e.getMessage());
        }
        
        assertNotNull("Minted DOI is null!", doi);
        assertFalse("Minted DOI is empty!", doi.isEmpty());
        
        try
        {
            doiService.formatIdentifier(doi);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            fail("Minted an unrecognizable DOI: " + e.getMessage());
        }
    }
    
    @Test
    public void testMint_returns_existing_DOI()
            throws SQLException, AuthorizeException, IOException, IdentifierException, WorkflowException, IllegalAccessException
    {
        Item item = newItem();
        String doi = this.createDOI(item, null, true);
        
        String retrievedDOI = provider.mint(context, item);

        assertNotNull("Minted DOI is null?!", retrievedDOI);
        assertEquals("Mint did not returned an existing DOI!", doi, retrievedDOI);
    }

    @Test
    public void testReserve_DOI()
            throws SQLException, SQLException, AuthorizeException, IOException,
            IdentifierException, WorkflowException, IllegalAccessException
    {
        Item item = newItem();
        String doi = this.createDOI(item, null, true);
        
        provider.reserve(context, item, doi);
        
        DOI doiRow = doiService.findByDoi(context, doi.substring(DOI.SCHEME.length()));
        assumeNotNull(doiRow);
        
        assertTrue("Reservation of DOI did not set the corret DOI status.",
                DOIIdentifierProvider.TO_BE_RESERVED.equals(doiRow.getStatus()));
    }
    
    @Test
    public void testRegister_unreserved_DOI()
            throws SQLException, SQLException, AuthorizeException, IOException,
            IdentifierException, WorkflowException, IllegalAccessException
    {
        Item item = newItem();
        String doi = this.createDOI(item, null, true);
        
        provider.register(context, item, doi);
        
        DOI doiRow = doiService.findByDoi(context, doi.substring(DOI.SCHEME.length()));
        assumeNotNull(doiRow);
        
        assertTrue("Registration of DOI did not set the corret DOI status.",
                DOIIdentifierProvider.TO_BE_REGISTERED.equals(doiRow.getStatus()));
    }

    @Test
    public void testRegister_reserved_DOI()
            throws SQLException, SQLException, AuthorizeException, IOException,
            IdentifierException, WorkflowException, IllegalAccessException
    {
        Item item = newItem();
        String doi = this.createDOI(item, DOIIdentifierProvider.IS_RESERVED, true);
        
        provider.register(context, item, doi);
        
        DOI doiRow = doiService.findByDoi(context, doi.substring(DOI.SCHEME.length()));
        assumeNotNull(doiRow);
        
        assertTrue("Registration of DOI did not set the corret DOI status.",
                DOIIdentifierProvider.TO_BE_REGISTERED.equals(doiRow.getStatus()));
    }
    
    @Test
    public void testCreate_and_Register_DOI()
            throws SQLException, SQLException, AuthorizeException, IOException,
            IdentifierException, WorkflowException, IllegalAccessException
    {
        Item item = newItem();
        
        String doi = provider.register(context, item);
        
        // we want the created DOI to be returned in the following format:
        // doi:10.<prefix>/<suffix>.
        String formated_doi = doiService.formatIdentifier(doi);
        assertTrue("DOI was not in the expected format!", doi.equals(formated_doi));
        
        DOI doiRow = doiService.findByDoi(context, doi.substring(DOI.SCHEME.length()));
        assertNotNull("Created DOI was not stored in database.", doiRow);
        
        assertTrue("Registration of DOI did not set the corret DOI status.",
                DOIIdentifierProvider.TO_BE_REGISTERED.equals(doiRow.getStatus()));
    }
    
    @Test
    public void testDelete_specified_DOI()
            throws SQLException, AuthorizeException, IOException, IdentifierException, WorkflowException, IllegalAccessException
    {
        Item item = newItem();
        String doi1 = this.createDOI(item, DOIIdentifierProvider.IS_REGISTERED, true);
        String doi2 = this.createDOI(item, DOIIdentifierProvider.IS_REGISTERED, true);
        
        // remove one of it
        context.turnOffAuthorisationSystem();
        provider.delete(context, item, doi1);
        context.restoreAuthSystemState();

        // assure that the right one was removed
        List<IMetadataValue> metadata = itemService.getMetadata(item, DOIIdentifierProvider.MD_SCHEMA,
                DOIIdentifierProvider.DOI_ELEMENT,
                DOIIdentifierProvider.DOI_QUALIFIER,
                null);
        boolean foundDOI1 = false;
        boolean foundDOI2 = false;
        for (IMetadataValue id : metadata)
        {
            if (id.getValue().equals(doiService.DOIToExternalForm(doi1)))
            {
                foundDOI1 = true;
            }
            if (id.getValue().equals(doiService.DOIToExternalForm(doi2)))
            {
                foundDOI2 = true;
            }
        }
        assertFalse("Cannot remove DOI from item metadata.", foundDOI1);
        assertTrue("Removed wrong DOI from item metadata.", foundDOI2);
        
        DOI doiRow1 = doiService.findByDoi(context, doi1.substring(DOI.SCHEME.length()));
        assumeNotNull(doiRow1);
        assertTrue("Status of deleted DOI was not set correctly.",
                DOIIdentifierProvider.TO_BE_DELETED.equals(doiRow1.getStatus()));

        DOI doiRow2 = doiService.findByDoi(context, doi2.substring(DOI.SCHEME.length()));
        assumeNotNull(doiRow2);
        assertTrue("While deleting a DOI the status of another changed.",
                DOIIdentifierProvider.IS_REGISTERED.equals(doiRow2.getStatus()));
    }
    
    @Test
    public void testDelete_all_DOIs()
            throws SQLException, AuthorizeException, IOException, IdentifierException, IllegalAccessException, WorkflowException {
        Item item = newItem();
        String doi1 = this.createDOI(item, DOIIdentifierProvider.IS_REGISTERED, true);
        String doi2 = this.createDOI(item, DOIIdentifierProvider.IS_REGISTERED, true);
        
        // remove one of it
        context.turnOffAuthorisationSystem();
        provider.delete(context, item);
        context.restoreAuthSystemState();

        // assure that the right one was removed
        List<IMetadataValue> metadata = itemService.getMetadata(item, DOIIdentifierProvider.MD_SCHEMA,
                DOIIdentifierProvider.DOI_ELEMENT,
                DOIIdentifierProvider.DOI_QUALIFIER,
                null);
        boolean foundDOI1 = false;
        boolean foundDOI2 = false;
        for (IMetadataValue id : metadata)
        {
            if (id.getValue().equals(doiService.DOIToExternalForm(doi1)))
            {
                foundDOI1 = true;
            }
            if (id.getValue().equals(doiService.DOIToExternalForm(doi2)))
            {
                foundDOI2 = true;
            }
        }
        assertFalse("Cannot remove DOI from item metadata.", foundDOI1);
        assertFalse("Did not removed all DOIs from item metadata.", foundDOI2);
        
        DOI doiRow1 = doiService.findByDoi(context, doi1.substring(DOI.SCHEME.length()));
        assumeNotNull(doiRow1);
        assertTrue("Status of deleted DOI was not set correctly.",
                DOIIdentifierProvider.TO_BE_DELETED.equals(doiRow1.getStatus()));

        DOI doiRow2 = doiService.findByDoi(context, doi1.substring(DOI.SCHEME.length()));
        assumeNotNull(doiRow2);
        assertTrue("Did not set the status of all deleted DOIs as expected.",
                DOIIdentifierProvider.TO_BE_DELETED.equals(doiRow2.getStatus()));
    }

    
    // test the following methods using the MockDOIConnector.
    // updateMetadataOnline
    // registerOnline
    // reserveOnline
    
}