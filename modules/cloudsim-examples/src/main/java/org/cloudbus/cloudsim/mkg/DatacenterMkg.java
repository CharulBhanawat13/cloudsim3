package org.cloudbus.cloudsim.mkg;

import net.sourceforge.jFuzzyLogic.FIS;
import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;

import java.io.InputStream;
import java.util.*;

public class DatacenterMkg extends Datacenter {

    static int transmissionoverhead = 1;
    static long RFF_result = 0;
    static long BF_result = 0;
    static long FF_result = 0;
    static long pkt_size = 0;
    static long core_sw_frc = 0;
    static long agg_sw_frc = 0;
    static long edg_sw_frc = 0;
    public List<ResCloudletMkg> suspect1 = new ArrayList<>();
    public List<ResCloudletMkg> suspect2 = new ArrayList<>();
    /**
     * List of VMs to Fail
     */
    List<VmMkg> toFail = new ArrayList<>();
    /**
     * List of failed Vms as detected by the monitoring module
     */
    List<Vm> failedVMs = new ArrayList<>();
    /**
     * List failed cloudlets as detected by detection algorithm 2
     */
    List<Cloudlet> failedDetectedCloudlets = new ArrayList<>();
    /**
     * double variable to keep track of last time a monitor was called.
     * Reduces redundant calls at same time
     */
    double lastMonitorRunTime = 0.0;
    /**
     * Variable to keep track of number of times a monitor was called
     */
    int numberOfTimesMonitorWasCalled = 0;
    /**
     * Delay at which fault is to be injected
     */
    double delayOfFailure = 0.0;
    /**
     * Failure Detection Algorithm 2
     * It detects failure by keeping a track of each cloudlet's progress.
     * It checks whether a task has become stagnant or not.
     * It is called at each ConstsMkg.SCHEDULING_INTERVAL
     *
     * @return
     */
    Random random = new Random();
    List<Vm> allVms = getVmList();
    int RFFvmToScheduleOn;
    int BFvmToScheduleOn;
    int FFvmToScheduleOn;

    private boolean hostIdentifiedByFuzzyLogic = false;

    /**
     * Map of Cloudlets to their Remaining Length. Used by Detection algorithm 2
     */
    private final Map<Integer, Long> clToRemainingLength = new HashMap<>();

    public DatacenterMkg(String name,
                         DatacenterCharacteristics characteristics,
                         VmAllocationPolicy vmAllocationPolicy, List<Storage> storageList,
                         double schedulingInterval) throws Exception {
        super(name, characteristics, vmAllocationPolicy, storageList,
                schedulingInterval);
    }

    /**
     * @return last time the monitor was called
     */
    public double getLastMonitorRunTime() {
        return this.lastMonitorRunTime;
    }

    /**
     * Sets the last monitor run time
     *
     * @param t
     */
    public void setLastMonitorRunTime(double t) {
        this.lastMonitorRunTime = t;
    }

    /**
     * Processes events or services that are available for this PowerDatacenter.
     *
     * @param ev a Sim_event object
     * @pre ev != null
     * @post $none
     */
    @Override
    public void processEvent(SimEvent ev) {
        int srcId = -1;

        switch (ev.getTag()) {
            // Resource characteristics inquiry
            case CloudSimTags.RESOURCE_CHARACTERISTICS:
                srcId = (Integer) ev.getData();
                sendNow(srcId, ev.getTag(), getCharacteristics());
                break;

            // Resource dynamic info inquiry
            case CloudSimTags.RESOURCE_DYNAMICS:
                srcId = (Integer) ev.getData();
                sendNow(srcId, ev.getTag(), 0);
                break;

            case CloudSimTags.RESOURCE_NUM_PE:
                srcId = (Integer) ev.getData();
                int numPE = getCharacteristics().getNumberOfPes();
                sendNow(srcId, ev.getTag(), numPE);
                break;

            case CloudSimTags.RESOURCE_NUM_FREE_PE:
                srcId = (Integer) ev.getData();
                int freePesNumber = getCharacteristics().getNumberOfFreePes();
                sendNow(srcId, ev.getTag(), freePesNumber);
                break;

            // New Cloudlet arrives
            case CloudSimTags.CLOUDLET_SUBMIT:
                processCloudletSubmit(ev, false);
                break;

            // New Cloudlet arrives, but the sender asks for an ack
            case CloudSimTags.CLOUDLET_SUBMIT_ACK:
                processCloudletSubmit(ev, true);
                break;

            // Cancels a previously submitted Cloudlet
            case CloudSimTags.CLOUDLET_CANCEL:
                processCloudlet(ev, CloudSimTags.CLOUDLET_CANCEL);
                break;

            // Pauses a previously submitted Cloudlet
            case CloudSimTags.CLOUDLET_PAUSE:
                processCloudlet(ev, CloudSimTags.CLOUDLET_PAUSE);
                break;

            // Pauses a previously submitted Cloudlet, but the sender
            // asks for an acknowledgement
            case CloudSimTags.CLOUDLET_PAUSE_ACK:
                processCloudlet(ev, CloudSimTags.CLOUDLET_PAUSE_ACK);
                break;

            // Resumes a previously submitted Cloudlet
            case CloudSimTags.CLOUDLET_RESUME:
                processCloudlet(ev, CloudSimTags.CLOUDLET_RESUME);
                break;

            // Resumes a previously submitted Cloudlet, but the sender
            // asks for an acknowledgement
            case CloudSimTags.CLOUDLET_RESUME_ACK:
                processCloudlet(ev, CloudSimTags.CLOUDLET_RESUME_ACK);
                break;

            // Moves a previously submitted Cloudlet to a different resource
            case CloudSimTags.CLOUDLET_MOVE:
                processCloudletMove((int[]) ev.getData(), CloudSimTags.CLOUDLET_MOVE);
                break;

            // Moves a previously submitted Cloudlet to a different resource
            case CloudSimTags.CLOUDLET_MOVE_ACK:
                processCloudletMove((int[]) ev.getData(), CloudSimTags.CLOUDLET_MOVE_ACK);
                break;

            // Checks the status of a Cloudlet
            case CloudSimTags.CLOUDLET_STATUS:
                processCloudletStatus(ev);
                break;

            // Ping packet
            case CloudSimTags.INFOPKT_SUBMIT:
                processPingRequest(ev);
                break;

            case CloudSimTags.VM_CREATE:
                processVmCreate(ev, false);
                break;

            case CloudSimTags.VM_CREATE_ACK:
                processVmCreate(ev, true);
                break;

            case CloudSimTags.VM_DESTROY:
                processVmDestroy(ev, false);
                break;

            case CloudSimTags.VM_DESTROY_ACK:
                processVmDestroy(ev, true);
                break;

            case CloudSimTags.VM_MIGRATE:
                //processVmMigrate(ev, false);
                break;

            case CloudSimTags.VM_MIGRATE_ACK:
                //processVmMigrate(ev, true);
                break;

            case CloudSimTags.VM_DATA_ADD:
                processDataAdd(ev, false);
                break;

            case CloudSimTags.VM_DATA_ADD_ACK:
                processDataAdd(ev, true);
                break;

            case CloudSimTags.VM_DATA_DEL:
                processDataDelete(ev, false);
                break;

            case CloudSimTags.VM_DATA_DEL_ACK:
                processDataDelete(ev, true);
                break;

            case CloudSimTags.VM_DATACENTER_EVENT:
                updateCloudletProcessing();
                checkCloudletCompletion();
                break;
            case CloudSimTagsMkg.VM_FAILING_EVENT:
                VmMkg vm = (VmMkg) ev.getData();
                vm.setVmStatus(false);
                //updateCloudletProcessing();
                //checkCloudletCompletion();
                break;
            case CloudSimTagsMkg.VM_MONITORING_EVENT:
                monitorCloudlets2();
                break;
            case CloudSimTagsMkg.MODIFY_HOST_CONFIGURATION:
                updateHostParameters(ev);
                break;
            // other unknown tags are processed by this method
            default:
                processOtherEvent(ev);
                break;
        }
    }

    @SuppressWarnings("unchecked")
    private void updateHostParameters(SimEvent ev) {
        Map<String, Object> dataMap = (Map<String, Object>) ev.getData();
        Host host = (Host) dataMap.get("host");
        VmMkg vm = (VmMkg) dataMap.get("vm");
        HostParameter newHostParameters = (HostParameter) dataMap.get("newHostParameters");

        host.getRamProvisioner().allocateRamForVm(vm, newHostParameters.getOccupiedRam());
        host.getBwProvisioner().allocateBwForVm(vm, newHostParameters.getOccupiedBw());
    }

    /**
     * Processes a Cloudlet based on the event type.
     * Changes: sends VM to the Cloudlet Scheduler
     *
     * @param ev   a Sim_event object
     * @param type event type
     * @pre ev != null
     * @pre type > 0
     * @post $none
     */
    protected void processCloudlet(SimEvent ev, int type) {
        int cloudletId = 0;
        int userId = 0;
        int vmId = 0;

        try { // if the sender using cloudletXXX() methods
            int[] data = (int[]) ev.getData();
            cloudletId = data[0];
            userId = data[1];
            vmId = data[2];
        }

        // if the sender using normal send() methods
        catch (ClassCastException c) {
            try {
                Cloudlet cl = (Cloudlet) ev.getData();
                cloudletId = cl.getCloudletId();
                userId = cl.getUserId();
                vmId = cl.getVmId();
            } catch (Exception e) {
                Log.printLine(super.getName() + ": Error in processing Cloudlet");
                Log.printLine(e.getMessage());
                return;
            }
        } catch (Exception e) {
            Log.printLine(super.getName() + ": Error in processing a Cloudlet.");
            Log.printLine(e.getMessage());
            return;
        }

        //ATUL
        Cloudlet cl = (Cloudlet) ev.getData();
        int uId = cl.getUserId();
        int vId = cl.getVmId();
        Host host = getVmAllocationPolicy().getHost(vId, uId);
        VmMkg vm = (VmMkg) host.getVm(vId, uId);
        CloudletSchedulerMkg scheduler = (CloudletSchedulerMkg) vm.getCloudletScheduler();
        scheduler.setVm(vm);


        // begins executing ....
        switch (type) {
            case CloudSimTags.CLOUDLET_CANCEL:
                processCloudletCancel(cloudletId, userId, vmId);
                break;

            case CloudSimTags.CLOUDLET_PAUSE:
                processCloudletPause(cloudletId, userId, vmId, false);
                break;

            case CloudSimTags.CLOUDLET_PAUSE_ACK:
                processCloudletPause(cloudletId, userId, vmId, true);
                break;

            case CloudSimTags.CLOUDLET_RESUME:
                processCloudletResume(cloudletId, userId, vmId, false);
                break;

            case CloudSimTags.CLOUDLET_RESUME_ACK:
                processCloudletResume(cloudletId, userId, vmId, true);
                break;
            default:
                break;
        }

    }

    /**
     * Processes a Cloudlet submission.
     *
     * @param ev  a SimEvent object
     * @param ack an acknowledgement
     * @pre ev != null
     * @post $none
     */
    protected void processCloudletSubmit(SimEvent ev, boolean ack) {
        updateCloudletProcessing();

        try {
            // gets the Cloudlet object
            Cloudlet cl = (Cloudlet) ev.getData();

            // checks whether this Cloudlet has finished or not
            if (cl.isFinished()) {
                String name = CloudSim.getEntityName(cl.getUserId());
                Log.printLine(getName() + ": Warning - Cloudlet #" + cl.getCloudletId() + " owned by " + name
                        + " is already completed/finished.");
                Log.printLine("Therefore, it is not being executed again");
                Log.printLine();

                // NOTE: If a Cloudlet has finished, then it won't be processed.
                // So, if ack is required, this method sends back a result.
                // If ack is not required, this method don't send back a result.
                // Hence, this might cause CloudSim to be hanged since waiting
                // for this Cloudlet back.
                if (ack) {
                    int[] data = new int[3];
                    data[0] = getId();
                    data[1] = cl.getCloudletId();
                    data[2] = CloudSimTags.FALSE;

                    // unique tag = operation tag
                    int tag = CloudSimTags.CLOUDLET_SUBMIT_ACK;
                    sendNow(cl.getUserId(), tag, data);
                }

                sendNow(cl.getUserId(), CloudSimTags.CLOUDLET_RETURN, cl);

                return;
            }

            // process this Cloudlet to this CloudResource
            cl.setResourceParameter(getId(), getCharacteristics().getCostPerSecond(), getCharacteristics()
                    .getCostPerBw());

            int userId = cl.getUserId();
            int vmId = cl.getVmId();

            // time to transfer the files
            double fileTransferTime = predictFileTransferTime(cl.getRequiredFiles());

            Host host = getVmAllocationPolicy().getHost(vmId, userId);
            VmMkg vm = (VmMkg) host.getVm(vmId, userId);
            CloudletSchedulerMkg scheduler = (CloudletSchedulerMkg) vm.getCloudletScheduler();
            //ATUL
            scheduler.setVm(vm);
            vm.getSubmittedCloudletList().add(cl);
            double estimatedFinishTime = scheduler.cloudletSubmit(cl, fileTransferTime);

            // if this cloudlet is in the exec queue
            if (estimatedFinishTime > 0.0 && !Double.isInfinite(estimatedFinishTime)) {
                estimatedFinishTime += fileTransferTime;
                send(getId(), estimatedFinishTime, CloudSimTags.VM_DATACENTER_EVENT);
            }

            if (ack) {
                int[] data = new int[3];
                data[0] = getId();
                data[1] = cl.getCloudletId();
                data[2] = CloudSimTags.TRUE;

                // unique tag = operation tag
                int tag = CloudSimTags.CLOUDLET_SUBMIT_ACK;
                sendNow(cl.getUserId(), tag, data);
            }
        } catch (ClassCastException c) {
            Log.printLine(getName() + ".processCloudletSubmit(): " + "ClassCastException error.");
            c.printStackTrace();
        } catch (Exception e) {
            Log.printLine(getName() + ".processCloudletSubmit(): " + "Exception error.");
            e.printStackTrace();
        }

        checkCloudletCompletion();
    }

    private void callSend(VmMkg vm, double changeTime) {
        Map<String, Object> data = new HashMap<>();
        data.put("host", vm.getHost());
        data.put("vm", vm);
        data.put("newHostParameters", HostFluctuatingParameters.PARAMETER_MAP.get(changeTime));
        send(
                getId(),
                changeTime, // time at which the configuration will change
                CloudSimTagsMkg.MODIFY_HOST_CONFIGURATION,
                data
        );
    }

    /**
     * Updates processing of each cloudlet running in this PowerDatacenter. It is necessary because
     * Hosts and VirtualMachines are simple objects, not entities. So, they don't receive events and
     * updating cloudlets inside them must be called from the outside.
     *
     * @pre $none
     * @post $none
     */
    protected void updateCloudletProcessing() {
        // if some time passed since last processing
        // R: for term is to allow loop at simulation start. Otherwise, one initial
        // simulation step is skipped and schedulers are not properly initialized
        if (CloudSim.clock() < 1.0 && CloudSim.clock() > getLastProcessTime()) {
            // Fails Vms as specified in the Failure Parameters
            toFail.stream()
                    .filter(VmMkg::isActive)
                    .forEach(vm -> send(getId(), delayOfFailure, CloudSimTagsMkg.VM_FAILING_EVENT, vm));


            for (VmMkg vmToFail : toFail) {
                if (vmToFail.isActive()) {
                    for (double changeTime : HostFluctuatingParameters.PARAMETER_MAP.keySet()) {
                        for (Vm cvm : vmToFail.getHost().getVmList()) {
                            callSend((VmMkg) cvm, changeTime);
                        }
                        send(
                                getId(),
                                changeTime + FuzzyLogicConstants.MONITORING_DELAY_AFTER_FLUCTUATE,
                                CloudSimTagsMkg.VM_MONITORING_EVENT,
                                null
                        );
                    }
                }
            }

        }
        if (CloudSim.clock() < 0.111 || CloudSim.clock() > getLastProcessTime() + CloudSim.getMinTimeBetweenEvents()) {
            List<? extends Host> hostList = getVmAllocationPolicy().getHostList();
            double smallerTime = Double.MAX_VALUE;
            // for each host...
            for (Host host : hostList) {
                // inform VMs to update processing
                double time = host.updateVmsProcessing(CloudSim.clock());

                // what time do we expect that the next cloudlet will finish?
                if (time < smallerTime) {
                    smallerTime = time;
                }
            }
            // guarantees a minimal interval before scheduling the event
            if (smallerTime < CloudSim.clock() + CloudSim.getMinTimeBetweenEvents() + 0.01) {
                smallerTime = CloudSim.clock() + CloudSim.getMinTimeBetweenEvents() + 0.01;
            }
            if (smallerTime != Double.MAX_VALUE) {
                //schedule(getId(), (smallerTime - CloudSim.clock()), CloudSimTags.VM_DATACENTER_EVENT);

                // schedules an event to the next time
                send(getId(), getSchedulingInterval(), CloudSimTags.VM_DATACENTER_EVENT);

                //If the monitor module is ON, call the monitor
                if (ConstsMkg.CMM_ON) {
                    if (ConstsMkg.DETECTION_ALGORITHM == 1) {
                        //Detection Algo 1: Monitor invoked at all MONITORING INTERVALS
                        send(getId(), ConstsMkg.MONITORING_INTERVAL, CloudSimTagsMkg.VM_MONITORING_EVENT);
                    } else if (ConstsMkg.DETECTION_ALGORITHM == 2) {
                        // Detection Algo 1: Monitor invoked at all SCHEDULING INTERVALS
                        send(getId(), getSchedulingInterval(), CloudSimTagsMkg.VM_MONITORING_EVENT);
                    } else if (ConstsMkg.DETECTION_ALGORITHM == 3) {
                        // Detection Algo 3: Fuzzy Logic
                        send(getId(), ConstsMkg.MONITORING_INTERVAL, CloudSimTagsMkg.VM_MONITORING_EVENT);
                    }

                }

            }

            setLastProcessTime(CloudSim.clock());
        }

    }

    private int RFF() {
        int RFFloadOnVm = (int) Double.MAX_VALUE;
        RFFvmToScheduleOn = -1;
        for (int k = 0; k < allVms.size(); k++) {
            int Randomdnum = random.nextInt(allVms.size());
            VmMkg RFFv1 = (VmMkg) allVms.get(Randomdnum);
            if (RFFv1.isActive()) {//Log.printLine("[JHA]:VM="+v1.getId() + " is Active");
                int loadOnThisVm = RFFv1.getSubmittedCloudletList().size();

                //Log.printLine("[JHA]:VM="+v1.getId() + " has Load = " + loadOnThisVm);

                if (loadOnThisVm < RFFloadOnVm) {
                    //Log.printLine("[AKJ]:YUPPIE");
                    RFFloadOnVm = loadOnThisVm;
                    RFFvmToScheduleOn = RFFv1.getId();
                    break;
                }
            }
        }
        return RFFvmToScheduleOn;
    }

    private int BF() {


        for (Vm allVm : allVms) {
            VmMkg BFv1 = (VmMkg) allVm;
            if (BFv1.isActive()) {//Log.printLine("[JHA]:VM="+v1.getId() + " is Active");
                int loadOnThisVm = BFv1.getSubmittedCloudletList().size();

                //Log.printLine("[JHA]:VM="+v1.getId() + " has Load = " + loadOnThisVm);

                if (BFv1.getMips() > 720.00) {
                    System.out.println("\n\n\n\n\n\n............" + BFv1.getMips());

                    BFvmToScheduleOn = BFv1.getId();
                    break;
                }
            }
        }
        System.out.println("\n\n\n\n\n\n...vmToScheduleOn bf........." + BFvmToScheduleOn);

        return BFvmToScheduleOn;

    }

    private int FF() {
        int FFloadOnVm = (int) Double.MAX_VALUE;
        FFvmToScheduleOn = -1;
        for (Vm allVm : allVms) {
            VmMkg FFv1 = (VmMkg) allVm;
            if (FFv1.isActive()) {
                //Log.printLine("[JHA]:VM="+v1.getId() + " is Active");
                int loadOnThisVm = FFv1.getSubmittedCloudletList().size();

                //Log.printLine("[JHA]:VM="+v1.getId() + " has Load = " + loadOnThisVm);

                if (loadOnThisVm < FFloadOnVm) {
                    //Log.printLine("[AKJ]:YUPPIE");
                    FFvmToScheduleOn = FFv1.getId();
                    break;
                }

            }
        }
        System.out.println("\n\n\n\n\n\n...vmToScheduleOn ff........." + FFvmToScheduleOn);
        return FFvmToScheduleOn;
    }


    private void monitorCloudlets2() {
        if (CloudSim.clock() > lastMonitorRunTime) {
            numberOfTimesMonitorWasCalled++;
            Log.printLine(CloudSim.clock() + ": [CMM2] Cloudlet Monitoring Module:");
            if (ConstsMkg.DETECTION_ALGORITHM == 1 || ConstsMkg.DETECTION_ALGORITHM == 2) {
                findSuspectsByAlgo1And2();
            } else if (ConstsMkg.DETECTION_ALGORITHM == 3 && !hostIdentifiedByFuzzyLogic) {
                findSuspectsByFuzzyLogic();
            }

        }
    }

    private void findSuspectsByFuzzyLogic() {
        List<? extends Host> hostList = getVmAllocationPolicy().getHostList();
       // System.out.println("host to fail: " + toFail.get(0).getHost().getId());
        for (Host host : hostList) {

            int totalMips = host.getTotalMips();
            int totalRam = host.getRam();
            long totalStorage = host.getStorage();
            long totalBw = host.getBw();

            int occupiedMips = host.getTotalMips() / host.getNumberOfPes();
            int occupiedRam = host.getRamProvisioner().getUsedRam();
            long occupiedStorage = host.getStorage();
            long occupiedBw = host.getBwProvisioner().getUsedBw();

            double percentOccupiedMips = occupiedMips / (double) totalMips * 100;
            double percentOccupiedRam = occupiedRam / (double) totalRam * 100;
            double percentOccupiedStorage = occupiedStorage / (double) totalStorage * 100;
            double percentOccupiedBw = occupiedBw / (double) totalBw * 100;

            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("fcl/fuzzytip.fcl");
            FIS fis = FIS.load(inputStream, true); // Load from 'FCL' file
            fis.setVariable("cpu", percentOccupiedMips); // Set inputs
            fis.setVariable("memory", percentOccupiedRam);
            fis.setVariable("disk", percentOccupiedStorage);
            fis.setVariable("network", percentOccupiedBw);
            fis.evaluate(); // Evaluate

            double risk = fis.getVariable("risk").getValue();
            System.out.println(
                    "hostId = " + host.getId() +
                            " occupied-ram-% = " + percentOccupiedRam +
                            " occupied-bw-% = " + percentOccupiedBw +
                            " occupied-storage-% = " + percentOccupiedStorage +
                            " occupied-mips-% = " + percentOccupiedMips +
                            "  RISK = " + risk
            );
            if (!hostIdentifiedByFuzzyLogic && risk > FuzzyLogicConstants.RISK_THRESHOLD_FOR_FAULT_DETECTION) {
                for (Vm vmMkg : host.getVmList()) {
                    List<ResCloudletMkg> failedCloudlets = ((CloudletSchedulerMkg) vmMkg.getCloudletScheduler()).getCloudletExecList();
                    for (ResCloudletMkg res : failedCloudlets) {
                        failedDetectedCloudlets.add(res.getCloudlet());
                    }
                }
                for (VmMkg vmMkg : toFail) {
                    hostIdentifiedByFuzzyLogic = true;
                    processDetectedFault(vmMkg);
                }
            }
        }
        FailureParameters.NO_OF_MONITOR_CALLS = numberOfTimesMonitorWasCalled;
    }

    private void findSuspectsByAlgo1And2() {

        List<? extends Host> hostList = getVmAllocationPolicy().getHostList();
        /* If all Cloudlets on that particular VM has failed. Conclude that VM has failed. */
        for (Host host : hostList) {
            for (Vm vm : host.getVmList()) {
                if (!failedVMs.contains(vm)) {
                    if (((CloudletSchedulerMkg) vm.getCloudletScheduler()).isFailedCloudlets()) {
                        //List of failed ResCloudlets
                        List<ResCloudletMkg> failedCloudlets = ((CloudletSchedulerMkg) vm.getCloudletScheduler()).getCloudletFailedList();

                        for (ResCloudletMkg failedCloudlet : failedCloudlets) {
                            if (failedCloudlet.getRemainingCloudletLength() < clToRemainingLength.get(failedCloudlet.getCloudletId())) {
                                //OKAY. Update Length
                                Log.printLine(CloudSim.clock() + ":Cloudlet ID=" + failedCloudlet.getCloudletId() + " Length is changing. Previous=" + clToRemainingLength.get(failedCloudlet.getCloudletId())
                                        + " New=" + failedCloudlet.getRemainingCloudletLength());
                                clToRemainingLength.remove(failedCloudlet.getCloudletId());
                                clToRemainingLength.put(failedCloudlet.getCloudletId(), failedCloudlet.getRemainingCloudletLength());

                                //Remove from both suspect lists
                                //Log.printLine(CloudSim.clock()+"Removing Cloudlet ID#"+rcl.getCloudletId() + " from suspect lists");
                                suspect1.remove(failedCloudlet);
                                suspect2.remove(failedCloudlet);
                            } else {
                                //NOT OKAY. Add to suspect list accordingly
                                if (suspect1.contains(failedCloudlet)) {
                                    //Already in Suspect 1. Add to suspect 2
                                    Log.printLine(CloudSim.clock() + ":Adding Cloudlet ID #" + failedCloudlet.getCloudletId() + " to Suspect 2 List");
                                    suspect2.add(failedCloudlet);
                                } else {
                                    //Not in suspect 1. Add to suspect 1
                                    Log.printLine(CloudSim.clock() + ":Adding Cloudlet ID #" + failedCloudlet.getCloudletId() + " to Suspect 1 List");
                                    suspect1.add(failedCloudlet);
                                }
                            }
                        }
                        /* If all CLoudlets on that particular VM has failed. Conclude that VM has failed. */
                        for (ResCloudletMkg a : suspect2) {
                            if (!failedDetectedCloudlets.contains(a)) {
                                failedDetectedCloudlets.add(a.getCloudlet());
                                Log.printLine(CloudSim.clock() + ":[CMM2]Cloudlet ID = " + a.getCloudletId() + " on VM ID " + a.getCloudlet().getVmId());
                                //Checking if all cloudlets on that particular VM has failed or not.
                                boolean allFailed = true;
                                Log.printLine("List of cloudlets scheduled on VM ID#" + a.getCloudlet().getVmId() + ":");
                                VmMkg v = (VmMkg) getVmAllocationPolicy().getHost(a.getCloudlet().getVmId(), a.getUserId()).getVm(a.getCloudlet().getVmId(), a.getUserId());
                                for (int k = 0; k < v.getSubmittedCloudletList().size(); k++) {
                                    Cloudlet c = v.getSubmittedCloudletList().get(k);
                                    Log.printLine("[CMM2]CLOUDLET ID#" + c.getCloudletId() + " VMID#" + c.getVmId());
                                    if (failedDetectedCloudlets.contains(c)) {
                                        Log.printLine("[CMM2] The Cloudlet ID#" + c.getCloudletId() + " is in the failed list");
                                    } else {
                                        Log.printLine("[CMM2] The Cloudlet ID#" + c.getCloudletId() + " is NOT in the failed list");
                                        allFailed = false;
                                    }

                                }
                                /* If all Cloudlets on that particular VM has failed. Conclude that VM has failed. */
                                if (allFailed) {
                                    processDetectedFault(v);
                                }
                            }
                        }


                    } else {
                        //No CL in failed list. Updating cloudlet length from Exec List
                        List<ResCloudletMkg> eCl = ((CloudletSchedulerMkg) vm.getCloudletScheduler()).getCloudletExecList();
                        for (ResCloudletMkg r : eCl) {
                            clToRemainingLength.remove(r.getCloudletId());
                            clToRemainingLength.put(r.getCloudletId(), r.getRemainingCloudletLength());
                        }
                        Log.printLine(CloudSim.clock() + ":[CMM]:No failures Recorded on VM ID#" + vm.getId());
                    }
                }
            }
        }
        Log.printLine(CloudSim.clock() + ":" + getLastProcessTime());
        setLastMonitorRunTime(CloudSim.clock());
        FailureParameters.NO_OF_MONITOR_CALLS = numberOfTimesMonitorWasCalled;
    }

    private void processDetectedFault(VmMkg failedVm) {

        Log.printLine(
                "--------------------\nFAULT DETECTED\n--------------------\n" +
                        CloudSim.clock() + ":VM ID #" + failedVm.getId() + "has failed."
        );
        failedVMs.add(failedVm);
        FailureParameters.FAULT_DETECTION_TIME = CloudSim.clock();

        Scanner scan = new Scanner(System.in);
        System.out.println("Press 1: RFF ");
        System.out.println("Press 2: BF ");
        System.out.println("Press 3: FF ");
        int number = scan.nextInt();
        /* CL Migration Code Starts*/
        // TODO: Lavish - check why this list is empty
        for (Cloudlet cx : failedDetectedCloudlets) {
            //TODO Decide which VM to schedule Cloudlet on
            int vmToScheduleOn = -1;
            switch (number) {
                case 1:
                    BF();
                    FF();
                    vmToScheduleOn = RFF();
                    break;

                case 2:
                    RFF();
                    FF();
                    vmToScheduleOn = BF();
                    break;

                case 3:
                    RFF();
                    BF();
                    vmToScheduleOn = FF();
                    break;


            }
            int[] array = new int[5];
            array[0] = cx.getCloudletId();
            array[1] = cx.getUserId();
            array[2] = cx.getVmId();
            array[3] = vmToScheduleOn;  //vmDestId=4
            array[4] = getId();
            if (ConstsMkg.CL_MIGRATION_ON) {

                VmMkg old_vm = (VmMkg) allVms.get(array[2]);
                VmMkg RFF_new_vm = (VmMkg) allVms.get(RFFvmToScheduleOn);
                VmMkg BF_new_vm = (VmMkg) allVms.get(BFvmToScheduleOn);
                VmMkg FF_new_vm = (VmMkg) allVms.get(FFvmToScheduleOn);

                RFF_result = RFF_result + transmissionoverhead * (old_vm.getBw() + RFF_new_vm.getBw());
                BF_result = BF_result + transmissionoverhead * (old_vm.getBw() + BF_new_vm.getBw());
                FF_result = FF_result + transmissionoverhead * (old_vm.getBw() + FF_new_vm.getBw());

                sendNow(getId(), CloudSimTags.CLOUDLET_MOVE, array);
                System.out.println("\n\n\n\n\n\n\n\n..........transmissionoverhead RFF_result......." + RFF_result);
                System.out.println("\n\n\n\n\n\n\n\n..........transmissionoverhead BF_result......." + BF_result);
                System.out.println("\n\n\n\n\n\n\n\n..........transmissionoverhead FF_result......." + FF_result);
                transmissionoverhead = transmissionoverhead + 1;
            }

            /* CL Migration Code Ends */
        }
    }


    /**
     * Process the event for an User/Broker who wants to move a Cloudlet.
     *
     * @pre receivedData != null
     * @pre type > 0
     * @post $none
     */

    public int get_pod_number(Vm vm) {
        int pod_number = 0;
        int hostid = vm.getHost().getId();
        hostid = hostid - 4000;

        System.out.println("\n\n\n\n....." + hostid);

        if (hostid > 4) {
            if ((hostid % 2) == 0) {
                if ((hostid % 4) == 0) {
                    pod_number = hostid / 4;
                } else {
                    pod_number = (hostid / 4) + 1;
                }
            } else {
                pod_number = (hostid / 4) + 1;
            }
        } else {
            pod_number = 1;
        }


        return pod_number;
    }


    public int get_subpod_number(Vm vm) {
        int subpod_number;
        int number_of_hosts_in_one_subpod = ConstsMkg.number_of_host_in_one_subpod;
        int hostid = vm.getHost().getId();
        hostid = hostid - 4000;

        System.out.println("\n\n\n\n....." + hostid);


        if ((hostid % 2) == 0) {
            subpod_number = hostid / number_of_hosts_in_one_subpod;
        } else {
            subpod_number = (hostid / number_of_hosts_in_one_subpod) + 1;
        }


        return subpod_number;
    }


    protected void processCloudletMove(int[] receivedData, int type) {
        updateCloudletProcessing();

        pkt_size = pkt_size + (32 + (4 * receivedData.length));


        int[] array = receivedData;
        int cloudletId = array[0];
        int userId = array[1];
        int vmId = array[2];
        int vmDestId = array[3];
        int destId = array[4];


        VmMkg src_vm = (VmMkg) getVmAllocationPolicy().getHost(vmId, userId).getVm(vmId, userId);
        VmMkg dst_vm = (VmMkg) getVmAllocationPolicy().getHost(vmDestId, userId).getVm(vmDestId, userId);

        int pod_src = get_pod_number(src_vm);
        int pod_dst = get_pod_number(dst_vm);
        if (dst_vm.isActive()) {
            if (pod_src != pod_dst) {
                core_sw_frc++;
                agg_sw_frc += 2;
                edg_sw_frc += 2;

            } else {

                int subpod_src = get_subpod_number(src_vm);
                int subpod_dst = get_subpod_number(dst_vm);

                System.out.println("\n\n\n subpod_src...." + subpod_src);
                System.out.println("\n\n\n subpod_src...." + subpod_src);

                if (subpod_src != subpod_dst) {
                    agg_sw_frc++;
                    edg_sw_frc += 2;

                } else {
                    edg_sw_frc++;
                }

            }
        }


        // get the cloudlet
        Cloudlet cl = getVmAllocationPolicy().getHost(vmId, userId).getVm(vmId, userId).getCloudletScheduler().cloudletCancel(cloudletId);

        boolean failed = false;
        if (cl == null) {// cloudlet doesn't exist
            failed = true;
        } else {
            // has the cloudlet already finished?
            if (cl.getCloudletStatus() == Cloudlet.SUCCESS) {    // if yes, send it back to user
                int[] data = new int[3];
                data[0] = getId();
                data[1] = cloudletId;
                data[2] = 0;
                sendNow(cl.getUserId(), CloudSimTags.CLOUDLET_SUBMIT_ACK, data);
                sendNow(cl.getUserId(), CloudSimTags.CLOUDLET_RETURN, cl);
            }

            // prepare cloudlet for migration
            cl.setVmId(vmDestId);

            // the cloudlet will migrate from one vm to another does the destination VM exist?
            if (destId == getId()) {
                Vm vm = getVmAllocationPolicy().getHost(vmDestId, userId).getVm(vmDestId, userId);
                if (vm == null) {
                    failed = true;
                } else {
                    // time to transfer the files
                    double fileTransferTime = predictFileTransferTime(cl.getRequiredFiles());
                    vm.getCloudletScheduler().cloudletSubmit(cl, fileTransferTime);
                }

            } else {
                // the cloudlet will migrate from one resource to another
                int tag = ((type == CloudSimTags.CLOUDLET_MOVE_ACK) ? CloudSimTags.CLOUDLET_SUBMIT_ACK
                        : CloudSimTags.CLOUDLET_SUBMIT);
                sendNow(destId, tag, cl);
            }
        }

        if (type == CloudSimTags.CLOUDLET_MOVE_ACK) {    // send ACK if requested
            int[] data = new int[3];
            data[0] = getId();
            data[1] = cloudletId;
            if (failed) {
                data[2] = 0;
            } else {
                data[2] = 1;
            }
            sendNow(cl.getUserId(), CloudSimTags.CLOUDLET_SUBMIT_ACK, data);
        }
    }

    /**
     * Sets the failure parameters for faults to be injected in the simulation
     *
     * @param vmlist:        List of VMs created by the broker
     * @param numberOfVms:   number of VMs to fail
     * @param delayType:     Time of failure- static or random
     * @param timeOfFailure: The actual time of failure
     * @param toFailList:    List of VMs to fail
     */
    public void setFailureParameters(
            List<VmMkg> vmlist,
            int numberOfVms,
            int delayType,
            double timeOfFailure,
            List<VmMkg> toFailList
    ) {
        switch (numberOfVms) {
            case FailureParameters.FAIL_SINGLE_VM:
                /*
                 * Fail any single VM
                 */
                VmMkg vmToFail;
                /* If user has not specified which VM to Fail */
                if (toFailList == null) {
                    //Randomly select a VM to Fail
                    Random rand = new Random();
                    vmToFail = vmlist.get(rand.nextInt(vmlist.size()));
                }
                /* If user has specified which VM to Fail */
                else {
                    //choose first Vm from toFailList
                    vmToFail = toFailList.get(0);
                }

                toFail.add(vmToFail);
                break;
            case FailureParameters.FAIL_MULTI_VM:
                /*
                 * Fail multiple VMs
                 */
                /* Vm List to Fail is not specified */
                if (toFailList == null) {
                    //Randomly select number of VMs to Fail
                    Random num = new Random();
                    int size = num.nextInt(vmlist.size());
                    for (int i = 0; i < size; i++) {
                        //Randomly select a VM to Fail
                        Random failVm = new Random();
                        VmMkg vmFail = vmlist.get(failVm.nextInt(vmlist.size()));
                        toFail.add(vmFail);
                    }
                }
                /* Vm List to Fail is specified */
                else {
                    //If Number of Vms to Fail is greater than Vm list Size
                    if (toFailList.size() > vmlist.size()) {
                        Log.printLine(CloudSim.clock() + ":[ERROR_FAILING_VM]:Number of VMs to fail is greater than Number of VMs created");
                    }
                    toFail.addAll(toFailList);
                }
                break;
        }

        switch (delayType) {
            case FailureParameters.STATIC_DELAY:
                /*
                 * Time of Failure specified by the user
                 */
                delayOfFailure = timeOfFailure;
                break;

            case FailureParameters.RANDOM_DELAY:
                /*
                 * Time of Failure to be kept random
                 */
                Random r = new Random();
                delayOfFailure = r.nextInt(300);
                break;
        }

    }


}
