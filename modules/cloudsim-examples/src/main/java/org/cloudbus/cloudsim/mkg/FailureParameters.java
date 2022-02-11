package org.cloudbus.cloudsim.mkg;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.CloudSim;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class FailureParameters {

    public final static int FAIL_SINGLE_VM = 1;

    public final static int FAIL_MULTI_VM = 4;

    public final static int RANDOM_DELAY = 10;

    public final static int STATIC_DELAY = 11;


    public static double FALT_INJECTION_TIME = 0.0;

    public static double FAULT_DETECTION_TIME = 0.0;

    public static int NO_OF_MONITOR_CALLS = 0;


    public static List<VmMkg> getRandomVMsToFail(List<VmMkg> vmlist, int numberOfVms) {
        List<VmMkg> toFail = new ArrayList<VmMkg>();
        Random r = new Random();
        if (numberOfVms > vmlist.size()) {
            Log.printLine(CloudSim.clock() + ":[ERROR]: Number of Vms to Fails is greater than number of Vms Created!");
            return null;
        }
        for (int i = 0; i < numberOfVms; i++) {
            toFail.add(vmlist.get(r.nextInt(vmlist.size())));
        }
        return toFail;
    }

    public double getFaultDetectionTime() {
        return FAULT_DETECTION_TIME;
    }

    public void setFaultDetectionTime(double time) {
        FAULT_DETECTION_TIME = time;
    }

    public double getFaultInjectionTime() {
        return FALT_INJECTION_TIME;
    }

    public void setFaultInjectionTime(double time) {
        FALT_INJECTION_TIME = time;
    }


}
