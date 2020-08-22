/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.generator;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.util.DateMathParser;
import org.dspace.content.IMetadataValue;
import org.dspace.content.Item;
import org.dspace.core.Context;

public class DateValueGenerator implements TemplateValueGenerator
{

    private static Logger log = Logger.getLogger(DateValueGenerator.class);

    @Override
    public List<IMetadataValue> generator(Context context, Item targetItem, Item templateItem,
            IMetadataValue IMetadataValue, String extraParams)
    {

        List<IMetadataValue> m = new ArrayList<>();
        m.add(IMetadataValue);
        String value = buildValue(extraParams);

        IMetadataValue.setValue(value);
        return m;
    }

    public static String buildValue(String extraParams)
    {
        String[] params = StringUtils.split(extraParams, "\\.");
        String operazione = "";
        String formatter = "";

        Date date = new Date();
        DateMathParser dmp = new DateMathParser();
        String value = "";
        if (params != null && params.length > 1)
        {
            operazione = params[0];
            formatter = params[1];
            try
            {
                date = dmp.parseMath(operazione);
            }
            catch (ParseException e)
            {
                log.error(e.getMessage(), e);
            }
            finally
            {
                DateFormat df = new SimpleDateFormat(formatter);
                value = df.format(date);
            }
        }
        else if (params.length == 1)
        {
            formatter = params[0];
            DateFormat df = new SimpleDateFormat(formatter);
            value = df.format(date);
        }
        else
        {
            value = date.toString();
        }
        return value;
    }

}
