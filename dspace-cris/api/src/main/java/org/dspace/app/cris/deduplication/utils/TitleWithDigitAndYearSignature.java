/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.deduplication.utils;

import java.util.Locale;

import org.apache.commons.lang.StringUtils;
import org.dspace.content.DSpaceObject;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;
import com.ibm.icu.text.Normalizer;

public class TitleWithDigitAndYearSignature extends MD5ValueSignature
{

    @Override
    protected String normalize(DSpaceObject item, String value)
    {
        if (value != null)
        {
            String temp = null;
            if (item != null)
            {
                temp = getYear(item);
            }
            String norm = Normalizer.normalize(value, Normalizer.NFD);
            CharsetDetector cd = new CharsetDetector();
            cd.setText(value.getBytes());
            CharsetMatch detect = cd.detect();
            if (detect != null && detect.getLanguage() != null)
            {
                norm = norm.replaceAll("[^\\p{L}^\\p{N}]", "").toLowerCase(
                        new Locale(detect.getLanguage()));
            }
            else
            {
                norm = norm.replaceAll("[^\\p{L}^\\p{N}]", "").toLowerCase();
            }
            if (temp != null) {
                return temp + " " + norm;
            }
            return norm;
        }
        else
        {
            return "item:" + item.getID();
        }

    }

    private String getYear(DSpaceObject item)
    {
        String year = null;
        String dcvalue = item.getMetadata("dc.date.issued");
        if (StringUtils.isNotEmpty(dcvalue))
        {
            year = StringUtils.substring(dcvalue, 0, 4);
        }
        return year;
    }

    /**
     * Use to test normalize method
     * 
     * @param args
     */
    public static void main(String[] args)
    {
        String[] testStrings = {
                "Title 1",
                "Title 2",
                "Title 3 with subtitle",
                "Evanescent wave spectroscopy of methylene blue solutions at surfaces using a continuum generated by a nonlinear fiber"};

        TitleWithDigitAndYearSignature tdss = new TitleWithDigitAndYearSignature();
        TitleSignature tss = new TitleSignature();
        for (String test : testStrings)
        {
            System.out.println(test + " -> " + tdss.normalize(null, test)
                    + " ||| " + tss.normalize(null, test));
        }
    }
}
