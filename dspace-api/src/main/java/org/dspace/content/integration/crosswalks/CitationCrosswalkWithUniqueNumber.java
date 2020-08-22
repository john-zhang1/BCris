/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.browse.BrowsableDSpaceObject;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.core.Context;

/**
 * 
 * Disseminate citations crosswalk, wrap ReferCrosswalk to manage custom
 * operation after default dissemination. TODO ingest?
 * 
 * @author pascarelli
 *
 */
public class CitationCrosswalkWithUniqueNumber extends ReferCrosswalk implements IDisseminateUniqueNumber {

	@Override
	public void disseminate(Context context, BrowsableDSpaceObject dso, OutputStream out)
			throws CrosswalkException, IOException, SQLException, AuthorizeException {

		disseminate(context, dso, out, null);

	}

	@Override
	public void disseminate(Context context, BrowsableDSpaceObject dso, OutputStream out, Integer index)
            throws CrosswalkException, IOException, SQLException,
            AuthorizeException
    {
 
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        super.disseminate(context, dso, stream);
        
        String s = ""; 
        if(index!=null) {	
        	s = "("+index+")";
        }
        s += stream.toString().replaceAll("\\r\\n", "");
        s += " \r\n";
        
        out.write(s.getBytes());
    }
	
	@Override
	public boolean assignUniqueNumber() {
		return true;
	}
}
