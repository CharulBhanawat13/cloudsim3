package org.cloudbus.cloudsim.mkg;

import java.util.ArrayList;
import java.util.HashMap;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class HostFluctuatingParameters {

    // This class is not meant to be instantiated
    private HostFluctuatingParameters() {
        throw new RuntimeException("Don't instantiate this class");
    }

    public static final Map<Double, HostParameter> PARAMETER_MAP = new HashMap<>();
    public static final double actionTimeParameter = ThreadLocalRandom.current().nextDouble(5,15);

    /*
        int mips = 3720;
        int ram = 2048; // host memory (MB)
        long storage = 1000000; // host storage
        int bw = 10000;
    * */
    static {
        initialize();
        System.out.println("Parameter map: " + PARAMETER_MAP);
        /*PARAMETER_MAP.put(1.52, new HostParameter(380 / 2, 587 / 2, 1500 / 2, 400 / 2));
        PARAMETER_MAP.put(1.67, new HostParameter(500 / 2, 1234 / 2, 2000 / 2, 1000 / 2));
        PARAMETER_MAP.put(1.74, new HostParameter(800 / 2, 1298 / 2, 3000 / 2, 1500 / 2));
        PARAMETER_MAP.put(1.79, new HostParameter(1000 / 2, 1545 / 2, 10000 / 2, 1800 / 2));
        PARAMETER_MAP.put(1.81, new HostParameter(1800 / 2, 1762 / 2, 15000 / 2, 2500 / 2));
        PARAMETER_MAP.put(1.88, new HostParameter(2000 / 2, 1873 / 2, 20000 / 2, 3500 / 2));
        PARAMETER_MAP.put(1.92, new HostParameter(3000 / 2, 1984 / 2, 979990 / 2, 8500 / 2));
        PARAMETER_MAP.put(ThreadLocalRandom.current().nextDouble(1.93, 2.01), new HostParameter(3700 / 2, 2046 / 2, 999990 / 2, 9900 / 2));*/
    }

    private static void initialize() {

        int varyingCount = 7;

        List<Double> time = getPartitions(1, actionTimeParameter - 0.3, varyingCount);
        List<Double> mips = getPartitions(190, 1500, varyingCount);
        List<Double> rams = getPartitions(293.5, 992, varyingCount);
        List<Double> storages = getPartitions(750, 489995, varyingCount);
        List<Double> bandwidths = getPartitions(200, 4250, varyingCount);

        for (int i = 0; i < varyingCount; i++) {
            PARAMETER_MAP.put(
                    time.get(i),
                    new HostParameter(
                            mips.get(i).intValue(),
                            rams.get(i).intValue(),
                            storages.get(i).longValue(),
                            bandwidths.get(i).longValue()
                    )
            );
        }

        PARAMETER_MAP.put(
                actionTimeParameter - randomize(0.2, 0.2, 100),
                new HostParameter(
                        randomize(1850, 100, 10).intValue(),
                        randomize(1023, 100, 10).intValue(),
                        randomize(499995, 100, 10).longValue(),
                        randomize(4950, 100, 10).longValue()
                )
        );

    }

    private static List<Double> getPartitions(double start, double end, int numberOfPartitions) {
        double widthOfEachPartition = (end-start) / (numberOfPartitions + 1);
        List<Double> result = new ArrayList<>();
        for (int i=0;i<numberOfPartitions;i++) {
            double value = start + widthOfEachPartition * (i + 1);
            result.add(randomize(value, widthOfEachPartition, 20));
        }
        return result;
    }


    private static Double randomize(double x, double width, double percent) {
        double value = width * percent / 100.0;
        double changer = ThreadLocalRandom.current().nextDouble(-value, value);
        return x + changer;
    }


}
