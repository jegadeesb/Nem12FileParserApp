package main.java.com.redenergy.parser.service.impl;

import main.java.com.redenergy.parser.service.SimpleNem12Parser;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.logging.Logger;

import main.java.com.redenergy.parser.model.*;

public class SimpleNem12ParserImpl implements SimpleNem12Parser {

    private final static Logger logger = Logger.getLogger(SimpleNem12ParserImpl.class.getName());

    private static final String TYPE_100 = "100";
    private static final String TYPE_200 = "200";
    private static final String TYPE_300 = "300";
    private static final String TYPE_900 = "900";

    /*
     * This method is used to parse NEM12 file
     * @Param - Accepts File as an input for parsing
     * @return - returns a Collection<MeterRead> of type MeterReads
     * */
    @Override
    public Collection<MeterRead> parseSimpleNem12(File csvFile) {
        List<MeterRead> readingList = new ArrayList<>();
        Path filePath = Paths.get(String.valueOf(csvFile));
        try {
            if (Files.exists(filePath)) {
                List<String> csvFileList = Files.readAllLines(filePath);
                if (!csvFileList.isEmpty()) {
                    fileParserController(csvFileList, readingList);
                }
            } else {
                throw new Exception("Input File does not exist in the path");
            }
        } catch (Exception e) {
            logger.severe(e.getMessage());
        }
        return readingList;
    }

    /*
     * This method acts as a controller which loops through each of the rows in the CSV file
     * @Param - Accepts a List<String> which contains the reading records
     * @Param - Accepts an empty List<MeterRead> to store objects of type MeterRead
     * @return - returns a List<MeterRead> of type MeterRead
     * */
    private List<MeterRead> fileParserController(List<String> csvFileList, List<MeterRead> readingList) throws Exception {
        meterReadingValidator(csvFileList);
        csvFileList.forEach(row -> fileParser(row, readingList));
        return readingList;
    }

    /*
     * This method is used to split the row based on the COMMA_DELIMITER and stores the readings to an object based on the type
     * @Param - Accepts individual row of type String as an input
     * @Param - Accepts an empty List<MeterRead> to store objects of type MeterRead
     * @return - none
     * */
    private void fileParser(String row, List<MeterRead> readingList) {
        final String COMMA_DELIMITER = ",";
        String[] csvRowSplitArr = row.split(COMMA_DELIMITER);
        String recIdentifier = csvRowSplitArr[0];

        if (recIdentifier.equals(TYPE_200)) {
            meterReadParser(csvRowSplitArr, readingList);
        } else if (recIdentifier.equals(TYPE_300)) {
            meterVolumeParser(csvRowSplitArr, readingList);
        }
    }

    /*
     * This method used to store the Meter Reading with TYPE_200 to the MeterRead object
     * @Param - Accepts a split Array of type String based on the TYPE_200
     * @Param - Accepts a List of type MeterRead to add the Meter reading
     * Condition - Validate and add only if the reading value length is equal to 10 chars and EnergyUnit is KWH.
     * @exception - throws an IllegalArgumentException to indicate a validation failure
     * */
    private void meterReadParser(String[] readArr, List<MeterRead> readingList) {
        if (fieldValidator(readArr[1], s1 -> s1.length() == 10) && fieldValidator(readArr[2], s2 -> s2.equals(EnergyUnit.KWH.name()))) {
            readingList.add(new MeterRead(readArr[1], EnergyUnit.valueOf(readArr[2])));
        } else
            throw new IllegalArgumentException("Validation Failure: NMI length is not equal to 10 characters / EnergyUnit not matching KWH");
    }

    /*
     * This method used to store the Meter volume Reading with TYPE_300 to the MeterVolume object
     * @Param - Accepts a split Array of type String based on the TYPE_300
     * @Param - Accepts a List of type MeterRead to add or append the MeterVolume reading
     * Condition - Validate and add only if the reading value is either A or E.
     * @exception - throws an IllegalArgumentException
     * */
    private void meterVolumeParser(String[] volumeArr, List<MeterRead> readingList) {
        if (fieldValidator(volumeArr[3], s1 -> s1.equals(Quality.A.name()) || fieldValidator(volumeArr[3], s2 -> s2.equals(Quality.E.name())))) {
            MeterVolume meterVolumeReading = new MeterVolume(new BigDecimal(volumeArr[2]), Quality.valueOf(volumeArr[3]));
            MeterRead meterRead = readingList.get(readingList.size() - 1);
            meterRead.appendVolume(stringToLocalDateConverter(volumeArr[1]), meterVolumeReading);
        } else
            throw new IllegalArgumentException("Validation failure : Meter Read Quality does not represent either Active(A) or Estimate(E)");
    }

    /*
     * This method used to convert the String to a Local date
     * @Param - Accepts a String which contains the date (20161113)
     * Condition - checks if the input dateStr is not empty.
     * @exception - throws an Exception to indicate the parse failure
     * @return - LocalDate
     * */
    private LocalDate stringToLocalDateConverter(String dateStr) {
        try {
            if (!dateStr.isEmpty()) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
                return LocalDate.parse(dateStr, formatter);
            }
        } catch (Exception e) {
            logger.severe("Date Parser failure: Input date cannot be parsed");
        }
        return null;
    }

    /*
     * Generic fieldValidator which accepts a String and a Predicate<String>
     * @Param - Accepts a String for validation
     * @Param - Accepts a Predicate to test the passed behavior
     * @return - returns a TRUE or FALSE based
     * */
    private boolean fieldValidator(String s, Predicate<String> predicate) {
        return predicate.test(s);
    }


    /*
     * This method used to validate the records to confirm the beginRec is of TYPE_100 and endRec is of TYPE_900
     * @Param - Accepts a List<String> which contains the meter Reading records
     * Condition - Checks if the beginRec is present and of TYPE_100 and endRec is present and of TYPE_900
     * @exception - throws an Exception if any of the above condition check fails
     * */
    private void meterReadingValidator(List<String> records) throws Exception {
        Optional<String> beginRec = records.stream().findFirst();
        if (beginRec.isPresent() && !beginRec.get().startsWith(TYPE_100)) {
            throw new Exception("Validation failure: Expected 100 as the beginning record of meter reading");
        }
        Optional<String> endRec = records.stream().skip(records.size() - 1).findFirst();
        if (endRec.isPresent() && !endRec.get().startsWith(TYPE_900)) {
            throw new Exception("Validation failure: Expected 900 as the end record of meter reading");
        }
    }
}
