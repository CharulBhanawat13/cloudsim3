package org.cloudbus.cloudsim.mkg.fuzzylogic;

import java.util.Collections;
import java.util.List;

public class FaultDetectionFuzzyLogic {

    private final double CONTRIBUTION_VARIABLE = 0.25;

    private final double THRESHOLD_FOR_ACTION = 86;

    private final String INPUT_DATA_VARIABLE_FILE_PATH = "";

    public static void main(String[] args) {
    }

    private void detechFaultFuzzyLogic() {

        //      RuleBase fuzzyLogicRules = new RuleBase();

        DataVariables memoryDataVariable = new DataVariables();
        memoryDataVariable.setName("Memory %");
        memoryDataVariable.setContribution(0.25);
        memoryDataVariable.setInverseCalculation(true);

        DataVariables cpuLoadDataVariable = new DataVariables();
        cpuLoadDataVariable.setName("CPU Load %");
        cpuLoadDataVariable.setContribution(0.25);
        cpuLoadDataVariable.setInverseCalculation(true);

        DataVariables cpuTemperatureVariable = new DataVariables();
        cpuTemperatureVariable.setName("CPU Temperature");
        cpuTemperatureVariable.setContribution(0.25);
        cpuTemperatureVariable.setInverseCalculation(true);

        DataVariables diskSpaceAvailabilityVariable = new DataVariables();
        diskSpaceAvailabilityVariable.setName("Disk Space Available");
        diskSpaceAvailabilityVariable.setContribution(0.25);
        diskSpaceAvailabilityVariable.setInverseCalculation(true);
    }

    private List<InputVariableData> readInputVariableData() {
     /*   try {
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
        }*/
        return Collections.emptyList();
    }

    private double calculateConfidenceScore(InputVariableData inputVariableData) {
        return (100 - inputVariableData.getCpuLoad()) * CONTRIBUTION_VARIABLE
                + (100 - inputVariableData.getCpuTemperature()) * CONTRIBUTION_VARIABLE
                + (100 - inputVariableData.getDiskSpace()) * CONTRIBUTION_VARIABLE
                + inputVariableData.getMemoryPercentage() * CONTRIBUTION_VARIABLE;
    }
}

