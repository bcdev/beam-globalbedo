package org.esa.beam.globalbedo.auxdata.AVHRR;

import org.esa.beam.util.io.CsvReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Class providing the list of dates with corrupt AVHRR BRF input data used in QA4ECV
 *
 * @author olafd
 */
public class AvhrrBrfBlacklist {

    private static AvhrrBrfBlacklist instance = null;
    private static final char[] SEPARATOR = new char[]{','};

    private List<String> brfBadDates;

    public AvhrrBrfBlacklist() {
        loadAuxData();
    }

    public static AvhrrBrfBlacklist getInstance() {
        if(instance == null) {
            instance = new AvhrrBrfBlacklist();
        }
        return instance;
    }

    public List<String> getBrfBadDatesList() {
        return brfBadDates;
    }

    public int getBrfBadDatesNumber() {
        return brfBadDates.size();
    }

    public String getBrfBadDate(int index) {
        return brfBadDates.get(index);
    }

    private void loadAuxData() {
        InputStream inputStream = AvhrrBrfBlacklist.class.getResourceAsStream("avhrr_brf_blacklist.txt");
        InputStreamReader streamReader = new InputStreamReader(inputStream);
        CsvReader csvReader = new CsvReader(streamReader, SEPARATOR);
        List<String[]> records;
        try {
            records = csvReader.readStringRecords();
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not load auxdata", e);
        }
        brfBadDates = new ArrayList<>(records.size());
        for (String[] record : records) {
            String name = record[0].trim();
            brfBadDates.add(name);
        }
    }

}
