package org.cloudbus.cloudsim.mkg;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Consts;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.ResCloudlet;
import org.cloudbus.cloudsim.core.CloudSim;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CloudletSchedulerTimeSharedMkg extends CloudletSchedulerMkg {

    /**
     * The current cp us.
     */
    protected int currentCPUs;
    /**
     * The cloudlet exec list.
     */
    private List<? extends ResCloudletMkg> cloudletExecList;
    /**
     * The cloudlet paused list.
     */
    private List<? extends ResCloudletMkg> cloudletPausedList;
    /**
     * The cloudlet finished list.
     */
    private List<? extends ResCloudletMkg> cloudletFinishedList;
    /**
     * The failed cloudlet list.
     */
    private final List<? extends ResCloudletMkg> cloudletFailedList;

    /**
     * Creates a new CloudletSchedulerTimeSharedAtul object. This method must be invoked before starting
     * the actual simulation.
     *
     * @pre $none
     * @post $none
     */
    public CloudletSchedulerTimeSharedMkg() {
        super();
        cloudletExecList = new ArrayList<ResCloudletMkg>();
        cloudletPausedList = new ArrayList<ResCloudletMkg>();
        cloudletFinishedList = new ArrayList<ResCloudletMkg>();
        cloudletFailedList = new ArrayList<ResCloudletMkg>();
        currentCPUs = 0;
    }

    /**
     * Updates the processing of cloudlets running under management of this scheduler.
     *
     * @param currentTime current simulation time
     * @param mipsShare   array with MIPS share of each processor available to the scheduler
     * @return time predicted completion time of the earliest finishing cloudlet, or 0 if there is
     * no next events
     * @pre currentTime >= 0
     * @post $none
     */
    @Override
    public double updateVmProcessing(double currentTime, List<Double> mipsShare) {
        setCurrentMipsShare(mipsShare);
        double timeSpam = currentTime - getPreviousTime();

        for (ResCloudletMkg rcl : getCloudletExecList()) {
            rcl.updateCloudletFinishedSoFar((long) (getCapacity(mipsShare) * timeSpam * rcl.getNumberOfPes() * Consts.MILLION));
            //Log.printLine(CloudSim.clock()+"#"+getPreviousTime()+":Cloudlet:"+rcl.getCloudletId()+":Remaining Length="+ rcl.getRemainingCloudletLength() +"Status="+
            //rcl.getCloudletStatus()+"EstimatedFinishTime=" + rcl.getEstimatedFinishTime() );
            try {
                writeToCSV(rcl.getCloudletId(), rcl);
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }


        }
        for (ResCloudletMkg rcl : getCloudletFailedList()) {
            try {
                writeToCSV(rcl.getCloudletId(), rcl);
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }


        }
        if (getCloudletExecList().size() == 0) {
            setPreviousTime(currentTime);
            return 0.0;
        }


        // check finished cloudlets
        double nextEvent = Double.MAX_VALUE;
        List<ResCloudletMkg> toRemove = new ArrayList<>();
        for (ResCloudletMkg rcl : getCloudletExecList()) {
            long remainingLength = rcl.getRemainingCloudletLength();
            if (remainingLength == 0) {// finished: remove from the list
                toRemove.add(rcl);
                cloudletFinish(rcl);
            }
        }
        getCloudletExecList().removeAll(toRemove);

        //ATUL
        //Check Failed Cloudlets
        checkFailedVms();

        // estimate finish time of cloudlets
        for (ResCloudletMkg rcl : getCloudletExecList()) {
            double estimatedFinishTime = currentTime
                    + (rcl.getRemainingCloudletLength() / (getCapacity(mipsShare) * rcl.getNumberOfPes()));
            if (estimatedFinishTime - currentTime < CloudSim.getMinTimeBetweenEvents()) {
                estimatedFinishTime = currentTime + CloudSim.getMinTimeBetweenEvents();
            }

            if (estimatedFinishTime < nextEvent) {
                nextEvent = estimatedFinishTime;
            }
        }

        setPreviousTime(currentTime);
        return nextEvent;
    }

    /**
     * Writes Cloudlet Statistics to CSV file for a particular cloudlet
     *
     * @param cloudletId
     * @param rcl
     */
    private void writeToCSV(int cloudletId, ResCloudletMkg rcl) throws IOException {
        double previousTime = getPreviousTime();
        double currentTime = CloudSim.clock();
        if (currentTime > previousTime) {
            //[New Entry]Write to CSV File
            String FILE_NAME = "Cloudlet" + cloudletId + ".csv";
            FileWriter fw = new FileWriter(FILE_NAME, true);
            fw.append(currentTime + "," +
                    rcl.getCloudletId() + "," +
                    rcl.getCloudletStatus() + "," +
                    rcl.getCloudlet().getVmId() + "," +
                    rcl.getRemainingCloudletLength() + "," +
                    rcl.getCloudletFinishedSoFar() + "," +
                    rcl.getEstimatedFinishTime() + "\n");
            fw.close();
        }
        //[Failed Rows]
        //Final Rows from main

    }

    /**
     * Gets the capacity.
     *
     * @param mipsShare the mips share
     * @return the capacity
     */
    protected double getCapacity(List<Double> mipsShare) {
        double capacity = 0.0;
        int cpus = 0;
        for (Double mips : mipsShare) {
            capacity += mips;
            if (mips > 0.0) {
                cpus++;
            }
        }
        currentCPUs = cpus;

        int pesInUse = 0;
        for (ResCloudletMkg rcl : getCloudletExecList()) {
            pesInUse += rcl.getNumberOfPes();
        }

        if (pesInUse > currentCPUs) {
            capacity /= pesInUse;
        } else {
            capacity /= currentCPUs;
        }
        return capacity;
    }

    /**
     * Cancels execution of a cloudlet.
     *
     * @param cloudletId ID of the cloudlet being cancealed
     * @return the canceled cloudlet, $null if not found
     * @pre $none
     * @post $none
     */
    @Override
    public Cloudlet cloudletCancel(int cloudletId) {
        boolean found = false;
        int position = 0;
        // First, looks in the finished queue
        found = false;
        for (ResCloudletMkg rcl : getCloudletFinishedList()) {
            if (rcl.getCloudletId() == cloudletId) {
                found = true;
                break;
            }
            position++;
        }

        if (found) {
            return getCloudletFinishedList().remove(position).getCloudlet();
        }

        // Then searches in the exec list
        position = 0;
        for (ResCloudletMkg rcl : getCloudletExecList()) {
            if (rcl.getCloudletId() == cloudletId) {
                found = true;
                break;
            }
            position++;
        }

        if (found) {
            ResCloudletMkg rcl = getCloudletExecList().remove(position);
            if (rcl.getRemainingCloudletLength() == 0) {
                cloudletFinish(rcl);
            } else {
                rcl.setCloudletStatus(Cloudlet.CANCELED);
            }
            return rcl.getCloudlet();
        }

        // Now, looks in the paused queue
        found = false;
        position = 0;
        for (ResCloudletMkg rcl : getCloudletPausedList()) {
            if (rcl.getCloudletId() == cloudletId) {
                found = true;
                rcl.setCloudletStatus(Cloudlet.CANCELED);
                break;
            }
            position++;
        }

        if (found) {
            return getCloudletPausedList().remove(position).getCloudlet();
        }

        //ATUL
        // Now, looks in the failed list
        found = false;
        position = 0;
        for (ResCloudletMkg rcl : getCloudletFailedList()) {
            if (rcl.getCloudletId() == cloudletId) {
                found = true;
                rcl.setCloudletStatus(Cloudlet.CANCELED);
                break;
            }
            position++;
        }
        if (found) {
            return getCloudletFailedList().remove(position).getCloudlet();
        }
        return null;
    }

    //ATUL

    /**
     * Fails the execution of a cloudlet.
     *
     * @param cloudletId ID of the cloudlet being failed
     * @return the canceled failed, $null if not found
     * @pre $none
     * @post $none
     */
    @Override
    public Cloudlet cloudletFail(int cloudletId) {
        boolean found = false;
        int position = 0;
        Log.printLine(CloudSim.clock() + "@@@@@@@@@@@@Cloudlet Fail Init");
        // First, looks in the finished queue
        Log.printLine("@@@@@@@@@@@@Searching in Finished List");
        found = false;
        for (ResCloudletMkg rcl : getCloudletFinishedList()) {
            if (rcl.getCloudletId() == cloudletId) {
                found = true;
                break;
            }
            position++;
        }

        if (found) {
            Log.printLine("@@@@@@@@@@@@Cloudlet-" + cloudletId + " Found in Finished List!");
            return getCloudletFinishedList().remove(position).getCloudlet();
        }

        // Then searches in the exec list
        Log.printLine("@@@@@@@@@@@@Searching in Exec List");
        position = 0;
        for (ResCloudletMkg rcl : getCloudletExecList()) {
            if (rcl.getCloudletId() == cloudletId) {
                found = true;
                break;
            }
            position++;
        }

        if (found) {
            Log.printLine("@@@@@@@@@@@@Cloudlet-" + cloudletId + " Found in Exec List!");
            ResCloudletMkg rcl = getCloudletExecList().remove(position);
            if (rcl.getRemainingCloudletLength() == 0) {
                Log.printLine("@@@@@@@@@@@@Remaining Cloudlet Length =0;");
                cloudletFinish(rcl);
            } else {
                Log.printLine("@@@@@@@@@@@@FAILED");
                rcl.setCloudletStatus(Cloudlet.FAILED);
            }
            return rcl.getCloudlet();
        }

        // Now, looks in the paused queue
        Log.printLine("@@@@@@@@@@@@Searching in Paused List");
        found = false;
        position = 0;
        for (ResCloudletMkg rcl : getCloudletPausedList()) {
            if (rcl.getCloudletId() == cloudletId) {
                found = true;
                rcl.setCloudletStatus(Cloudlet.FAILED);
                break;
            }
            position++;
        }

        if (found) {
            Log.printLine("@@@@@@@@@@@@Cloudlet-" + cloudletId + " Found in Paused List!");
            return getCloudletPausedList().remove(position).getCloudlet();
        }
        Log.printLine("@@@@@@@@@@@@Return Null");
        return null;
    }

    /**
     * Pauses execution of a cloudlet.
     *
     * @param cloudletId ID of the cloudlet being paused
     * @return $true if cloudlet paused, $false otherwise
     * @pre $none
     * @post $none
     */
    @Override
    public boolean cloudletPause(int cloudletId) {
        boolean found = false;
        int position = 0;

        for (ResCloudletMkg rcl : getCloudletExecList()) {
            if (rcl.getCloudletId() == cloudletId) {
                found = true;
                break;
            }
            position++;
        }

        if (found) {
            // remove cloudlet from the exec list and put it in the paused list
            ResCloudletMkg rcl = getCloudletExecList().remove(position);
            if (rcl.getRemainingCloudletLength() == 0) {
                cloudletFinish(rcl);
            } else {
                rcl.setCloudletStatus(Cloudlet.PAUSED);
                getCloudletPausedList().add(rcl);
            }
            return true;
        }
        return false;
    }

    /**
     * Processes a finished cloudlet.
     *
     * @param rcl finished cloudlet
     * @pre rgl != $null
     * @post $none
     */
    @Override
    public void cloudletFinish(ResCloudletMkg rcl) {
        rcl.setCloudletStatus(Cloudlet.SUCCESS);
        rcl.finalizeCloudlet();
        getCloudletFinishedList().add(rcl);
    }

    /**
     * Resumes execution of a paused cloudlet.
     *
     * @param cloudletId ID of the cloudlet being resumed
     * @return expected finish time of the cloudlet, 0.0 if queued
     * @pre $none
     * @post $none
     */
    @Override
    public double cloudletResume(int cloudletId) {
        boolean found = false;
        int position = 0;

        // look for the cloudlet in the paused list
        for (ResCloudletMkg rcl : getCloudletPausedList()) {
            if (rcl.getCloudletId() == cloudletId) {
                found = true;
                break;
            }
            position++;
        }

        if (found) {
            ResCloudletMkg rgl = getCloudletPausedList().remove(position);
            //ATUL
            VmMkg vm = getVm();
            if (!vm.isActive()) {
                Log.printLine("#######RESUMEVM Failed!: ");
                rgl.setCloudletStatus(Cloudlet.FAILED);
                cloudletFail(rgl.getCloudletId());
                return 0.0;
            }

            rgl.setCloudletStatus(Cloudlet.INEXEC);
            getCloudletExecList().add(rgl);

            // calculate the expected time for cloudlet completion
            // first: how many PEs do we have?

            double remainingLength = rgl.getRemainingCloudletLength();
            double estimatedFinishTime = CloudSim.clock()
                    + (remainingLength / (getCapacity(getCurrentMipsShare()) * rgl.getNumberOfPes()));

            return estimatedFinishTime;
        }

        return 0.0;
    }

    /**
     * Receives an cloudlet to be executed in the VM managed by this scheduler.
     *
     * @param cloudlet         the submited cloudlet
     * @param fileTransferTime time required to move the required files from the SAN to the VM
     * @return expected finish time of this cloudlet
     * @pre gl != null
     * @post $none
     */
    @Override
    public double cloudletSubmit(Cloudlet cloudlet, double fileTransferTime) {
        //Comments
        ResCloudletMkg rcl = new ResCloudletMkg(cloudlet);

        //ATUL
        rcl.setEstimatedFinishTime(cloudlet.getCloudletLength() / getCapacity(getCurrentMipsShare()));
        VmMkg vm = getVm();

        //Log.printLine(CloudSim.clock()+"*************************VM ID to which cloudlet was submitted : " + cloudlet.getVmId());
        //Log.printLine("*************************VM ID to which cloudlet was submitted(from Vm obj) : " + vm.getId());
        //Log.printLine("*************************Vm Status : " + vm.isActive());
        //Log.printLine("*************************Status While Introducting : " + rcl.getCloudletStatus());

        rcl.setCloudletStatus(Cloudlet.INEXEC);
        for (int i = 0; i < cloudlet.getNumberOfPes(); i++) {
            rcl.setMachineAndPeId(0, i);
        }

        getCloudletExecList().add(rcl);

        // use the current capacity to estimate the extra amount of
        // time to file transferring. It must be added to the cloudlet length
        double extraSize = getCapacity(getCurrentMipsShare()) * fileTransferTime;
        long length = (long) (cloudlet.getCloudletLength() + extraSize);
        cloudlet.setCloudletLength(length);
        if (!vm.isActive()) {
            Log.printLine(CloudSim.clock() + "#######VM Failed!: " + cloudlet.getVmId() + ",cloudlet:" + cloudlet.getCloudletId());
            rcl.setCloudletStatus(Cloudlet.FAILED);
            cloudletFail(cloudlet.getCloudletId());
            getCloudletFailedList().add(rcl);
            return 0.0;
            //No need to remove from Exec list since it has not been added yet
        }

        return cloudlet.getCloudletLength() / getCapacity(getCurrentMipsShare());
    }

    /*
     * (non-Javadoc)
     * @see cloudsim.CloudletScheduler#cloudletSubmit(cloudsim.Cloudlet)
     */
    @Override
    public double cloudletSubmit(Cloudlet cloudlet) {
        return cloudletSubmit(cloudlet, 0.0);
    }

    /**
     * Gets the status of a cloudlet.
     *
     * @param cloudletId ID of the cloudlet
     * @return status of the cloudlet, -1 if cloudlet not found
     * @pre $none
     * @post $none
     */
    @Override
    public int getCloudletStatus(int cloudletId) {
        for (ResCloudletMkg rcl : getCloudletExecList()) {
            if (rcl.getCloudletId() == cloudletId) {
                return rcl.getCloudletStatus();
            }
        }
        for (ResCloudletMkg rcl : getCloudletPausedList()) {
            if (rcl.getCloudletId() == cloudletId) {
                return rcl.getCloudletStatus();
            }
        }
        return -1;
    }

    /**
     * Get utilization created by all cloudlets.
     *
     * @param time the time
     * @return total utilization
     */
    @Override
    public double getTotalUtilizationOfCpu(double time) {
        double totalUtilization = 0;
        for (ResCloudletMkg gl : getCloudletExecList()) {
            totalUtilization += gl.getCloudlet().getUtilizationOfCpu(time);
        }
        return totalUtilization;
    }

    /**
     * Informs about completion of some cloudlet in the VM managed by this scheduler.
     *
     * @return $true if there is at least one finished cloudlet; $false otherwise
     * @pre $none
     * @post $none
     */
    @Override
    public boolean isFinishedCloudlets() {
        return getCloudletFinishedList().size() > 0;
    }

    //ATUL

    /**
     * Informs about completion of some cloudlet in the VM managed by this scheduler.
     *
     * @return $true if there is at least one finished cloudlet; $false otherwise
     * @pre $none
     * @post $none
     */
    @Override
    public boolean isFailedCloudlets() {
        return getCloudletFailedList().size() > 0;
    }

    /**
     * Returns the next cloudlet in the finished list, $null if this list is empty.
     *
     * @return a finished cloudlet
     * @pre $none
     * @post $none
     */
    @Override
    public Cloudlet getNextFinishedCloudlet() {
        if (getCloudletFinishedList().size() > 0) {
            return getCloudletFinishedList().remove(0).getCloudlet();
        }
        return null;
    }

    /**
     * Returns the number of cloudlets runnning in the virtual machine.
     *
     * @return number of cloudlets runnning
     * @pre $none
     * @post $none
     */
    @Override
    public int runningCloudlets() {
        return getCloudletExecList().size();
    }

    /**
     * Returns one cloudlet to migrate to another vm.
     *
     * @return one running cloudlet
     * @pre $none
     * @post $none
     */
    @Override
    public Cloudlet migrateCloudlet() {
        ResCloudletMkg rgl = getCloudletExecList().remove(0);
        rgl.finalizeCloudlet();
        return rgl.getCloudlet();
    }

    //ATUL

    /**
     * Gets the cloudlet failed list.
     *
     * @param <T> the generic type
     * @return the cloudlet failed list
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T extends ResCloudletMkg> List<T> getCloudletFailedList() {
        return (List<T>) cloudletFailedList;
    }

    /**
     * Gets the cloudlet exec list.
     *
     * @param <T> the generic type
     * @return the cloudlet exec list
     */
    @SuppressWarnings("unchecked")
    public <T extends ResCloudletMkg> List<T> getCloudletExecList() {
        return (List<T>) cloudletExecList;
    }

    /**
     * Sets the cloudlet exec list.
     *
     * @param <T>              the generic type
     * @param cloudletExecList the new cloudlet exec list
     */
    protected <T extends ResCloudletMkg> void setCloudletExecList(List<T> cloudletExecList) {
        this.cloudletExecList = cloudletExecList;
    }

    /**
     * Gets the cloudlet paused list.
     *
     * @param <T> the generic type
     * @return the cloudlet paused list
     */
    @SuppressWarnings("unchecked")
    protected <T extends ResCloudletMkg> List<T> getCloudletPausedList() {
        return (List<T>) cloudletPausedList;
    }

    /**
     * Sets the cloudlet paused list.
     *
     * @param <T>                the generic type
     * @param cloudletPausedList the new cloudlet paused list
     */
    protected <T extends ResCloudletMkg> void setCloudletPausedList(List<T> cloudletPausedList) {
        this.cloudletPausedList = cloudletPausedList;
    }

    /**
     * Gets the cloudlet finished list.
     *
     * @param <T> the generic type
     * @return the cloudlet finished list
     */
    @SuppressWarnings("unchecked")
    protected <T extends ResCloudletMkg> List<T> getCloudletFinishedList() {
        return (List<T>) cloudletFinishedList;
    }

    /**
     * Sets the cloudlet finished list.
     *
     * @param <T>                  the generic type
     * @param cloudletFinishedList the new cloudlet finished list
     */
    protected <T extends ResCloudletMkg> void setCloudletFinishedList(List<T> cloudletFinishedList) {
        this.cloudletFinishedList = cloudletFinishedList;
    }

    /*
     * (non-Javadoc)
     * @see cloudsim.CloudletScheduler#getCurrentRequestedMips()
     */
    @Override
    public List<Double> getCurrentRequestedMips() {
        List<Double> mipsShare = new ArrayList<Double>();
        return mipsShare;
    }

    /*
     * (non-Javadoc)
     * @see cloudsim.CloudletScheduler#getTotalCurrentAvailableMipsForCloudlet(cloudsim.ResCloudlet,
     * java.util.List)
     */
    @Override
    public double getTotalCurrentAvailableMipsForCloudlet(ResCloudletMkg rcl, List<Double> mipsShare) {
        return getCapacity(getCurrentMipsShare());
    }

    /*
     * (non-Javadoc)
     * @see cloudsim.CloudletScheduler#getTotalCurrentAllocatedMipsForCloudlet(cloudsim.ResCloudlet,
     * double)
     */
    @Override
    public double getTotalCurrentAllocatedMipsForCloudlet(ResCloudletMkg rcl, double time) {
        return 0.0;
    }

    /*
     * (non-Javadoc)
     * @see cloudsim.CloudletScheduler#getTotalCurrentRequestedMipsForCloudlet(cloudsim.ResCloudlet,
     * double)
     */
    @Override
    public double getTotalCurrentRequestedMipsForCloudlet(ResCloudletMkg rcl, double time) {
        // TODO Auto-generated method stub
        return 0.0;
    }

    @Override
    public double getCurrentRequestedUtilizationOfRam() {
        double ram = 0;
        for (ResCloudletMkg cloudlet : cloudletExecList) {
            ram += cloudlet.getCloudlet().getUtilizationOfRam(CloudSim.clock());
        }
        return ram;
    }

    @Override
    public double getCurrentRequestedUtilizationOfBw() {
        double bw = 0;
        for (ResCloudletMkg cloudlet : cloudletExecList) {
            bw += cloudlet.getCloudlet().getUtilizationOfBw(CloudSim.clock());
        }
        return bw;
    }

    //ATUL

    /**
     * Checks if any VM has failed or not during processing
     * of cloudlets. In case a Vm has failed, the cloudlets submitted on
     * are failed and dropped.
     */
    public void checkFailedVms() {
        List<ResCloudletMkg> failed = new ArrayList<ResCloudletMkg>();
        for (ResCloudletMkg rcl : getCloudletExecList()) {
            int VmId = rcl.getCloudlet().getVmId();
            VmMkg vm = getVm();
            if (!vm.isActive()) {
                Log.printLine(CloudSim.clock() + ":VM ID #" + VmId + "has Failed! For cloudlet-" + rcl.getCloudletId());
                rcl.finalizeCloudlet();
                rcl.setCloudletStatus(Cloudlet.FAILED);
                failed.add(rcl);
                //cloudletCancel(rcl.getCloudletId());
            }
        }
        getCloudletExecList().removeAll(failed);
        getCloudletFailedList().addAll(failed);
    }

    @Override
    public void cloudletFinish(ResCloudlet rcl) {
        // TODO Auto-generated method stub

    }

    @Override
    public double getTotalCurrentAvailableMipsForCloudlet(ResCloudlet rcl,
                                                          List<Double> mipsShare) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public double getTotalCurrentRequestedMipsForCloudlet(ResCloudlet rcl,
                                                          double time) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public double getTotalCurrentAllocatedMipsForCloudlet(ResCloudlet rcl,
                                                          double time) {
        // TODO Auto-generated method stub
        return 0;
    }
}
