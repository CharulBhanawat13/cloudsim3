package org.cloudbus.cloudsim.mkg;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.Integer.parseInt;

public class FaultDetectionFuzzyLogic {

    private final double CONTRIBUTION_VARIABLE = 0.25;

    private final double THRESHOLD_FOR_ACTION = 86;

    private final String INPUT_DATA_VARIABLE_FILE_PATH = "";

    private void detechFaultFuzzyLogic() {
    }

    private List<InputVariableData> readInputVariableData() {
        try {
            System.out.println("Reading Input CSV Variable Data csv file" + INPUT_DATA_VARIABLE_FILE_PATH);
            File initialFile = new File(INPUT_DATA_VARIABLE_FILE_PATH);
            InputStream targetStream = new FileInputStream(initialFile);

            CSVReader csvReader = new CSVReaderBuilder(new InputStreamReader(targetStream))
                    .build();
            List<String[]> dataSourceProductIdList = csvReader.readAll();

            return dataSourceProductIdList.stream()
                    .map(entry -> new InputVariableData(parseInt(entry[0]), parseInt(entry[1]), parseInt(entry[2]), parseInt(entry[3])))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.out.println("Exception reading Input Variable Data" + e);
        }
        return Collections.emptyList();
    }

    private double calculateConfidenceScore(InputVariableData inputVariableData) {
        return (100 - inputVariableData.getCpuLoad()) * CONTRIBUTION_VARIABLE
                + (100 - inputVariableData.getCpuTemperature()) * CONTRIBUTION_VARIABLE
                + (100 - inputVariableData.getDiskSpace()) * CONTRIBUTION_VARIABLE
                + inputVariableData.getMemoryPercentage() * CONTRIBUTION_VARIABLE;
    }
}

