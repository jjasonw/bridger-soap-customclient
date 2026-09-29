package com.lexisnexis.bridger.util;

import com.lexisnexis.bridger.generated.*;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CsvExporterTest {

    @Test
    public void exportToCSV_writesErfAdditionalInfoInFixedOrder() throws Exception {
        // Build a minimal Search request
        Search search = new Search();
        ClientContext context = new ClientContext();
        context.setClientID("CLIENT123");
        context.setUserID("USER456");
        search.setContext(context);

        // Input (not strictly needed for ERF test but keep minimal)
        SearchInput searchInput = new SearchInput();
        ArrayOfInputRecord arrayOfInputRecord = new ArrayOfInputRecord();
        InputRecord inputRecord = new InputRecord();
        InputEntity inputEntity = new InputEntity();
        InputName inName = new InputName();
        inName.setFirst("John");
        inName.setLast("Doe");
        inputEntity.setName(inName);
        inputRecord.setEntity(inputEntity);
        arrayOfInputRecord.getInputRecord().add(inputRecord);
        searchInput.setRecords(arrayOfInputRecord);
        search.setInput(searchInput);

        // Build SearchResults with one ResultRecord containing a WLMatch with ERF details
        SearchResults results = new SearchResults();
        ArrayOfResultRecord arrayOfResultRecord = new ArrayOfResultRecord();
        ResultRecord resultRecord = new ResultRecord();

        // RecordDetails (entity type + name)
        ResultRecordDetails details = new ResultRecordDetails();
        details.setEntityType(ResultEntityType.INDIVIDUAL);
        InputName resName = new InputName();
        resName.setFull("John Doe");
        details.setName(resName);
        resultRecord.setRecordDetails(details);

        // WatchlistResults / ArrayOfWLMatch / WLMatch
        WatchlistResults watchlistResults = new WatchlistResults();
        ArrayOfWLMatch arrayOfWLMatch = new ArrayOfWLMatch();
        WLMatch wlMatch = new WLMatch();
        wlMatch.setID(1L);

        // ERF details
        WLMatchErfDetails erfDetails = new WLMatchErfDetails();
        erfDetails.setGuidance("Check");
        erfDetails.setModelType(ERFModelType.NONE);
        erfDetails.setStrengthIndex(85); // integer

        // AdditionalInfo with unordered keys (to test mapping)
        ArrayOfErfAdditionalInfo arrayOfErfAdditional = new ArrayOfErfAdditionalInfo();

        ErfAdditionalInfo info1 = new ErfAdditionalInfo();
        info1.setName("NameQualityScore");
        info1.setValue("0.11");
        info1.setType(ErfAdditionalInfoType.SCORE);

        ErfAdditionalInfo info2 = new ErfAdditionalInfo();
        info2.setName("LocationScore");
        info2.setValue("0.99");
        info2.setType(ErfAdditionalInfoType.SCORE);

        ErfAdditionalInfo info3 = new ErfAdditionalInfo();
        info3.setName("DateOfBirthScore");
        info3.setValue("0.77");
        info3.setType(ErfAdditionalInfoType.SCORE);

        // Add them in non-header order
        arrayOfErfAdditional.getErfAdditionalInfo().add(info1);
        arrayOfErfAdditional.getErfAdditionalInfo().add(info2);
        arrayOfErfAdditional.getErfAdditionalInfo().add(info3);

        erfDetails.setAdditionalInfo(arrayOfErfAdditional);
        wlMatch.setErfDetails(erfDetails);
        arrayOfWLMatch.getWLMatch().add(wlMatch);

        watchlistResults.setMatches(arrayOfWLMatch);
        resultRecord.setWatchlist(watchlistResults);

        arrayOfResultRecord.getResultRecord().add(resultRecord);
        results.setRecords(arrayOfResultRecord);

        // Create temporary file and call exporter
        Path tempFile = Files.createTempFile("csvexporter-test", ".csv");
        CsvExporter.exportToCSV(tempFile, search, results);

        // Read CSV and assert ERF columns are in fixed positions
        List<String> lines = Files.readAllLines(tempFile);
        // header + one data row
        assertEquals(2, lines.size(), "Expect header + one data row");

        String data = lines.get(1);

        String[] cols = data.split(",", -1);

        // Indices based on CsvExporter.CSV_HEADER
        assertEquals("Check", cols[25], "ERF_Guidance should be present");
        assertEquals("NONE", cols[26], "ERF_ModelType should be present and use enum name");
        assertEquals("85", cols[27], "ERF_SI should be present (strength index)");
        assertEquals("0.99", cols[28], "ERF_LS (LocationScore) should be from the map");
        // Missing MiddleNameInitialScore -> empty
        assertEquals("", cols[29], "ERF_MIS should be empty when absent");
        assertEquals("0.77", cols[30], "ERF_DBS (DateOfBirthScore) should be from the map");
        // Remaining absent fields should be empty
        assertEquals("", cols[31], "ERF_BP should be empty when absent");
        assertEquals("", cols[32], "ERF_IMS should be empty when absent");
        assertEquals("", cols[33], "ERF_ARS should be empty when absent");
        assertEquals("", cols[34], "ERF_NFS should be empty when absent");
        assertEquals("0.11", cols[35], "ERF_NQS (NameQualityScore) should be from the map");

        // cleanup
        Files.deleteIfExists(tempFile);
    }
}
