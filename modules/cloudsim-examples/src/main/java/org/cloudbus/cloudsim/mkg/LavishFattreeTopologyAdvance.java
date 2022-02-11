package org.cloudbus.cloudsim.mkg;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.Integer.parseInt;

/**
 * Simulation with 4000 VMs and 2000 CLs. One random VM is failed before any
 * cloudlet is assigned onto VM by the broker.
 */

public class LavishFattreeTopologyAdvance {


    static List<VmMkg> toFail;
    /**
     * The cloudlet list.
     */
    private static List<Cloudlet> cloudletList;
    private static final List CoreSwitchList = new ArrayList();
    private static final List AggSwitchList = new ArrayList();
    private static final List EdgeSwitchList = new ArrayList();
    private static final List HostListids = new ArrayList();
    /**
     * The vmlist.
     */
    private static List<VmMkg> vmlist;

    // Static Mapping
    private static final boolean STATIC_MAPPPING_ONLY = true;
    private static final Map<Integer, List<Integer>> CORE_TO_AGG_LINKS_MAPPING = new HashMap<>();
    private static final Map<Integer, List<Integer>> AGG_TO_EDGE_LINKS_MAPPING = new HashMap<>();
    private static final Map<Integer, List<Integer>> EDGE_TO_HOST_MAPPING = new HashMap<>();

    private static List<VmMkg> createVM(int userId, int vms, int idShift) {
        System.out.println("\n\nStart Create VMs..........");
        //Creates a container to store VMs. This list is passed to the broker later
        LinkedList<VmMkg> list = new LinkedList<VmMkg>();

        //VM Parameters
        long size = 1000; //image size (MB)
        int ram = 1000; //vm memory (MB)
        int mips = 360;
        long bw = 900;
        int pesNumber = 1; //number of cpus
        String vmm = "Xen"; //VMM name

        //create VMs
        VmMkg[] vm = new VmMkg[vms];

        int Randomdivider1 = ThreadLocalRandom.current().nextInt(0, vms + 1);
        int Randomdivider2 = ThreadLocalRandom.current().nextInt(0, vms + 1);
        for (int i = 0; i < vms; i++) {
            if (i == 20) {
                mips = ThreadLocalRandom.current().nextInt(540, 620 + 1);
                System.out.println("\n\n\n ......mips value.........." + mips);
            } else if (i == 40) {
                mips = ThreadLocalRandom.current().nextInt(720, 860 + 1);
                System.out.println("\n\n\n ......mips value.........." + mips);
            }

            vm[i] = new VmMkg(idShift + i, userId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeSharedMkg());
            list.add(vm[i]);
        }

        return list;
    }


    private static List<Cloudlet> createCloudlet(int userId, int cloudlets, int idShift) {
        System.out.println("\n\nStart Create Cloudlets..........");
        // Creates a container to store Cloudlets
        LinkedList<Cloudlet> list = new LinkedList<Cloudlet>();

        //cloudlet parameters
        long length = 1000;
        long fileSize = 300;
        long outputSize = 300;
        int pesNumber = 1;
        UtilizationModel utilizationModel = new UtilizationModelFull();
        int lengthIncr = 300;
        Cloudlet[] cloudlet = new Cloudlet[cloudlets];

        int Randomdivider1 = ThreadLocalRandom.current().nextInt(0, cloudlets + 1);
        int Randomdivider2 = ThreadLocalRandom.current().nextInt(0, cloudlets + 1);

        for (int i = 0; i < cloudlets; i++) {


            if (i == Randomdivider1 || i == Randomdivider2) {
                length = ThreadLocalRandom.current().nextInt(500, 3000 + 1);
            }

            cloudlet[i] = new Cloudlet(idShift + i, length + lengthIncr, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
            // setting the owner of these Cloudlets
            cloudlet[i].setUserId(userId);
            list.add(cloudlet[i]);
            lengthIncr += 50;
        }

        return list;
    }


    ////////////////////////// STATIC METHODS ///////////////////////

    /**
     * Creates main() to run this example
     */
    public static void main(String[] args) {
        Log.printLine("Starting CloudSimExample1 Step1...");

        try {
            // First step: Initialize the CloudSim package. It should be called
            // before creating any entities.
            int num_user = 1;   // number of grid users
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false;  // mean trace events

            // Initialize the CloudSim library
            CloudSim.init(num_user, calendar, trace_flag);

            // Second step: Create Datacenters
            //Datacenters are the resource providers in CloudSim. We need at list one of them to run a CloudSim simulation
            final DatacenterMkg datacenter0 = createDatacenter("Datacenter_0");

            //Third step: Create Broker
            final DatacenterBrokerMkg broker = createBroker("Broker_0");
            int brokerId = broker.getId();

            //Fourth step: Create VMs and Cloudlets and send them to broker
            vmlist = createVM(brokerId, 4000, 0); //creating 5 vms
            cloudletList = createCloudlet(brokerId, 3000, 0); // creating 10 cloudlets
            initCSVFiles();
            broker.submitVmList(vmlist);
            broker.submitCloudletList(cloudletList);

            /** EDIT THIS to Fail a VM before simulation starts */
            datacenter0.setFailureParameters(vmlist, FailureParameters.FAIL_SINGLE_VM, FailureParameters.STATIC_DELAY, 2.0, null);

            // Fifth step: Starts the simulation
            CloudSim.startSimulation();

            // Final step: Print results when simulation is over
            //List<Cloudlet> newList = broker.getCloudletReceivedList();
            List<Cloudlet> newList1 = broker.getCloudletSubmittedList();

            CloudSim.stopSimulation();

            //printCloudletList(newList);
            printCloudletList(newList1);
            //printCloudletList(cloudletList);
            //   Thread.sleep(60000);

            ReportGenerator generator = new ReportGenerator(broker.getCloudletSubmittedList());
            generator.generateHTMLFile();
            generator.generateCloudletReport();

            //RG r = new RG(broker.getCloudletSubmittedList());
            //r.generateCloudletReport();

            //Log.printLine("\n\n\n vm fail list........ = " + toFail.get(0).getId());
            /* */
            //r.generateCloudletReport();
            //Log.printLine("\n\n\n vm fail list........ = " + toFail.get(0).getId());
            Log.printLine("\n\n\n vm transmission RFF_result........ = " + DatacenterMkg.RFF_result);
            //       Log.printLine("\n\n\n vm transmission of pakets paket_counter...... = " + SimEntity.paket_counter);
            Log.printLine("\n\n\n  total paket size which were moved from one vm to another due to failure...... = " + DatacenterMkg.pkt_size);

            Log.printLine("\n\n\n  total number of times CORE SWITCH used....... = " + DatacenterMkg.core_sw_frc);
            Log.printLine("\n\n\n total number of times AGGR SWITCH used...... = " + DatacenterMkg.agg_sw_frc);
            Log.printLine("\n\n\n  total number of times EDGE SWITCH used...... = " + DatacenterMkg.edg_sw_frc);

            Log.printLine("\n\n\n  total network used by CORE SWITCH used....... = " + DatacenterMkg.core_sw_frc * 52 * 10.0);
            int e_core = (int) (DatacenterMkg.core_sw_frc * 52 * 10.0);

            Log.printLine("\n\n\n  total network used by  AGGR SWITCH used....... = " + DatacenterMkg.agg_sw_frc * 52 * 1.0);
            int e_aggr = (int) (DatacenterMkg.agg_sw_frc * 52 * 1.0);


            Log.printLine("\n\n\n  total network used by EDGE SWITCH used....... = " + DatacenterMkg.edg_sw_frc * 52 * 10.0);
            int e_edg = (int) (DatacenterMkg.edg_sw_frc * 52 * 1.0);

            int total_network_used = e_core + e_aggr + e_edg;
            Log.printLine("\n\n\n  total network used by all switchs....... = " + total_network_used);

            Log.printLine("\n\n\n vm transmission BF_result........ = " + DatacenterMkg.BF_result);
            Log.printLine("\n\n\n vm transmission FF_result........ = " + DatacenterMkg.FF_result);

            Log.printLine("Fault Injection Time = " + FailureParameters.FALT_INJECTION_TIME);
            if (ConstsMkg.DETECTION_ALGORITHM == 1) {
                Log.printLine("Fault Detection Time(Algorithm 1) = " + FailureParameters.FAULT_DETECTION_TIME);
            } else if (ConstsMkg.DETECTION_ALGORITHM == 2) {
                Log.printLine("Fault Detection Time(Algorithm 2) = " + FailureParameters.FAULT_DETECTION_TIME);
            }
            Log.printLine("Number of Times Monitor was called = " + FailureParameters.NO_OF_MONITOR_CALLS);
            Log.printLine("CloudSimExampleAtul4 finished!");

            /* 	*/
            Log.printLine("Fault Injection Time = " + FailureParameters.FALT_INJECTION_TIME);
            if (ConstsMkg.DETECTION_ALGORITHM == 1) {
                Log.printLine("Fault Detection Time(Algorithm 1) = " + FailureParameters.FAULT_DETECTION_TIME);
            } else if (ConstsMkg.DETECTION_ALGORITHM == 2) {
                Log.printLine("Fault Detection Time(Algorithm 2) = " + FailureParameters.FAULT_DETECTION_TIME);
            }
            Log.printLine("Number of Times Monitor was called = " + FailureParameters.NO_OF_MONITOR_CALLS);
            Log.printLine("CloudSimExampleAtul4 finished!");


            //mkg code

            {

                FileWriter f = new FileWriter("C:\\Users\\gokhr\\Desktop\\mkg\\mkg.csv", true);
                f.write("");
                //	f.append("DatacenterMkg.RFF_result,DatacenterMkg.BF_result,DatacenterMkg.FF_result,FailureParameters.NO_OF_MONITOR_CALLS ,e_edg,e_aggr,e_core\n");
                //		f.append(DatacenterMkg.RFF_result + ","+DatacenterMkg.BF_result +","+DatacenterMkg.FF_result+ ","+ FailureParameters.NO_OF_MONITOR_CALLS+"," + e_edg+","+e_aggr+","+e_core +"\n");
                f.append(DatacenterMkg.RFF_result + "," + DatacenterMkg.BF_result + "," + DatacenterMkg.FF_result + "," + FailureParameters.NO_OF_MONITOR_CALLS + "," + e_edg + "," + e_aggr + "," + e_core + "," + "LAVISH" + "\n");

                f.close();
            }


        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("The simulation has been terminated due to an unexpected error");
        }
    }

    /**
     * Fail a VM before the start of simulation.
     */
    private static void failVMsBeforeSimulationStarts() {
        int numberOfVmsToFail = 1;
        if (numberOfVmsToFail > vmlist.size()) {
            Log.printLine(CloudSim.clock() + ":[ERROR]: Number of Vms to Fails is greater than number of Vms Created!");
            return;
        }
        toFail = FailureParameters.getRandomVMsToFail(vmlist, numberOfVmsToFail);
        System.out.println("\n\n\n\n..........toFail.....\n\n\n" + toFail);
        for (int i = 0; i < toFail.size(); i++) {
            VmMkg vm2 = toFail.get(i);
            System.out.println("\n\n\n\n..........vm2.....\n\n\n" + vm2);
            vm2.setVmStatus(false);
            System.out.println("\n\n\n\n..........vm2 isActive.....\n\n\n" + vm2.isActive());
        }
    }

    /**
     * Initializes and flushes the CSV files that are created for every cloudlet.
     *
     * @throws IOException
     */
    private static void initCSVFiles() throws IOException {
        for (int i = 0; i < cloudletList.size(); i++) {
            Cloudlet cloudlet = cloudletList.get(i);
            FileWriter f = new FileWriter("Cloudlet" + cloudlet.getCloudletId() + ".csv");
            f.write("");
            f.append("ClockTick,Cloudlet ID,Status,VM ID,Remaining Cloudlet Length,Cloudlet Finished So Far,EstimatedFinishTime\n");
            f.close();
        }
    }


    private static void createCoreLayerSwitch(int NUMBER) {
        System.out.print("Create Agg Layer..........");
        for (int x = 1; x <= NUMBER; x++) {
            String PREFIX = "100";
            if (x >= 10) {
                PREFIX = "10";

            }
            CoreSwitchList.add(PREFIX + x);
        }

        System.out.print("\n\n\n IDS of nodes in Core Layer.........." + CoreSwitchList);

    }

    private static void createAggLayerSwitch(int NUMBER) {
        System.out.print("Create Core Layer..........");
        for (int x = 1; x <= NUMBER; x++) {
            String PREFIX = "200";
            if (x >= 10) {
                PREFIX = "20";

            }
            AggSwitchList.add(PREFIX + x);
        }

        System.out.print("\n\n\n IDS of nodes in  Agg Layer .........." + AggSwitchList);

    }

    private static void createEdgeLayerSwitch(int NUMBER) {
        System.out.print("Create Edge Layer..........");
        for (int x = 1; x <= NUMBER; x++) {
            String PREFIX = "300";
            if (x >= 10) {
                PREFIX = "30";

            }
            EdgeSwitchList.add(PREFIX + x);
        }

        System.out.print("\n\n\n IDS of nodes in  Edge Layer .........." + EdgeSwitchList);

    }

    private static List<Host> createHost(List<Host> hostList, int NUMBER) {
        // 2. A Machine contains one or more PEs or CPUs/Cores.
        // In this example, it will have only one core.
        List<Pe> peList = new ArrayList<Pe>();

        int mips = 3720;

        // 3. Create PEs and add these into a list.
        peList.add(new Pe(0, new PeProvisionerSimple(mips))); // need to store Pe id and MIPS Rating
        int hostId;
        // 4. Create Host with its id and list of PEs and add them to the list of
        // machines
        int ram = 10000; // host memory (MB)
        long storage = 1000000; // host storage
        int bw = 10000;

        System.out.print("Create Host Layer..........");
        for (int x = 1; x <= NUMBER; x++) {
            String PREFIX = "400";
            if (x >= 10) {
                PREFIX = "40";

            }
            HostListids.add(PREFIX + x);

        }

        System.out.print("\n\n\n IDS of Host layer.........." + HostListids);


        int Randomdivider1 = ThreadLocalRandom.current().nextInt(0, NUMBER + 1);
        int Randomdivider2 = ThreadLocalRandom.current().nextInt(0, NUMBER + 1);

        for (int x = 0; x < HostListids.size(); x++) {


            if (x == Randomdivider1 || x == Randomdivider2) {
                mips = ThreadLocalRandom.current().nextInt(3725, 5320 + 1);
                System.out.println("\n\n\n ......mips value..host........" + mips);
            }


            hostList.add(
                    new Host(hostId = parseInt(HostListids.get(x).toString()), new RamProvisionerSimple(ram),
                            new BwProvisionerSimple(bw), storage, peList, new VmSchedulerTimeShared(peList))); // This
            // is
            // our
            // machine
        }

        return hostList;
    }


    private static DatacenterMkg createDatacenter(String name) {


        // Here are the steps needed to create a PowerDatacenter:
        // 1. We need to create a list to store
        //    our machine
        List<Host> hostList = new ArrayList<Host>();


        int iNUMBER = 64;
        int iCoreLayerSwitch = iNUMBER;
        int iAggLayerSwitch = iNUMBER * 2;
        int iEdgeLayerSwitch = iNUMBER * 2;
        int hostnumber = iNUMBER / 2;
        int iHost = (int) Math.pow(hostnumber, 2);


        System.out.println("\n\n iHost.........." + iHost);

        createCoreLayerSwitch(iCoreLayerSwitch);
        createAggLayerSwitch(iAggLayerSwitch);
        createEdgeLayerSwitch(iEdgeLayerSwitch);
        hostList = createHost(hostList, iHost);
        System.out.println("\n\n Got Host List.........." + hostList);


        System.out.println("\n\nStart Create Core to Agg link..........");
        int count = 0;
        int start = 0;
        for (int i = 0; i < iCoreLayerSwitch; i++) {
            if (count == 2) {
                start = start + 1;
                count = 0;
            } else {
                start = start;
            }
            for (int j = start; j < iAggLayerSwitch; j = j + 8) {
                if (STATIC_MAPPPING_ONLY) {
                    // CREATING STATIC MAPPING
                    if (!CORE_TO_AGG_LINKS_MAPPING.containsKey(parseInt(CoreSwitchList.get(i).toString()))) {
                        CORE_TO_AGG_LINKS_MAPPING.put(parseInt(CoreSwitchList.get(i).toString()), new ArrayList<>());
                    }
                    CORE_TO_AGG_LINKS_MAPPING.get(parseInt(CoreSwitchList.get(i).toString())).add(parseInt(AggSwitchList.get(j).toString()));
                } else {
                    NetworkTopology.addLink(parseInt(CoreSwitchList.get(i).toString()), parseInt(AggSwitchList.get(j).toString()), 10.00, 1);
                }
            }
            count++;
        }


        System.out.println("\n\nStart Create Agg to Edge link..........");
        count = 0;
        start = 0;
        for (int i = 0; i < iAggLayerSwitch; i++) {
            if (count == 8) {
                start = start + 8;
                count = 0;
            } else {
                start = start;
            }
            for (int j = start; j < start + 8; j++) {
                if (STATIC_MAPPPING_ONLY) {
                    // CREATING STATIC MAPPING
                    if (!AGG_TO_EDGE_LINKS_MAPPING.containsKey(parseInt(AggSwitchList.get(i).toString()))) {
                        AGG_TO_EDGE_LINKS_MAPPING.put(parseInt(AggSwitchList.get(i).toString()), new ArrayList<>());
                    }
                    AGG_TO_EDGE_LINKS_MAPPING.get(parseInt(AggSwitchList.get(i).toString())).add(parseInt(EdgeSwitchList.get(j).toString()));
                } else {
                    NetworkTopology.addLink(parseInt(AggSwitchList.get(i).toString()), parseInt(EdgeSwitchList.get(j).toString()), 10.00, 1);
                }
            }
            count++;
        }


        System.out.println("\n\n Start Create Edge to Host........");
        count = 0;
        for (int x = 0; x < iEdgeLayerSwitch; x++) {
            if (STATIC_MAPPPING_ONLY) {
                // CREATING STATIC MAPPING
                if (!EDGE_TO_HOST_MAPPING.containsKey(parseInt(EdgeSwitchList.get(x).toString()))) {
                    EDGE_TO_HOST_MAPPING.put(parseInt(EdgeSwitchList.get(x).toString()), new ArrayList<>());
                }
                EDGE_TO_HOST_MAPPING.get(parseInt(EdgeSwitchList.get(x).toString())).add(hostList.get(count).getId());
                EDGE_TO_HOST_MAPPING.get(parseInt(EdgeSwitchList.get(x).toString())).add(hostList.get(count + 1).getId());
                EDGE_TO_HOST_MAPPING.get(parseInt(EdgeSwitchList.get(x).toString())).add(hostList.get(count + 2).getId());
                EDGE_TO_HOST_MAPPING.get(parseInt(EdgeSwitchList.get(x).toString())).add(hostList.get(count + 3).getId());
                EDGE_TO_HOST_MAPPING.get(parseInt(EdgeSwitchList.get(x).toString())).add(hostList.get(count + 4).getId());
                EDGE_TO_HOST_MAPPING.get(parseInt(EdgeSwitchList.get(x).toString())).add(hostList.get(count + 5).getId());
                EDGE_TO_HOST_MAPPING.get(parseInt(EdgeSwitchList.get(x).toString())).add(hostList.get(count + 6).getId());
                EDGE_TO_HOST_MAPPING.get(parseInt(EdgeSwitchList.get(x).toString())).add(hostList.get(count + 7).getId());
            } else {
                NetworkTopology.addLink(parseInt(EdgeSwitchList.get(x).toString()), hostList.get(count).getId(), 1, 2);
                NetworkTopology.addLink(parseInt(EdgeSwitchList.get(x).toString()), hostList.get(count + 1).getId(), 1, 2);
                NetworkTopology.addLink(parseInt(EdgeSwitchList.get(x).toString()), hostList.get(count + 2).getId(), 1, 2);
                NetworkTopology.addLink(parseInt(EdgeSwitchList.get(x).toString()), hostList.get(count + 3).getId(), 1, 2);
                NetworkTopology.addLink(parseInt(EdgeSwitchList.get(x).toString()), hostList.get(count + 4).getId(), 1, 2);
                NetworkTopology.addLink(parseInt(EdgeSwitchList.get(x).toString()), hostList.get(count + 5).getId(), 1, 2);
                NetworkTopology.addLink(parseInt(EdgeSwitchList.get(x).toString()), hostList.get(count + 6).getId(), 1, 2);
                NetworkTopology.addLink(parseInt(EdgeSwitchList.get(x).toString()), hostList.get(count + 7).getId(), 1, 2);
            }

            /*
             * System.out.println("....................x value "+x+" count"+count);
             * System.out.println("....................x value "+x+" count"+(count+1));
             * System.out.println("....................x value "+x+" count"+(count+2));
             * System.out.println("....................x value "+x+" count"+(count+3));
             * System.out.println("....................x value "+x+" count"+(count+4));
             * System.out.println("....................x value "+x+" count"+(count+5));
             * System.out.println("....................x value "+x+" count"+(count+6));
             * System.out.println("....................x value "+x+" count"+(count+7));
             */
            count = count + 8;
            System.out.println("....linking hosts to edgeswitch........ " + count);
        }

        System.out.println("\n\n Done Create  Edge to Host..........");


        // 5. Create a DatacenterCharacteristics object that stores the
        //    properties of a data center: architecture, OS, list of
        //    Machines, allocation policy: time- or space-shared, time zone
        //    and its price (G$/Pe time unit).
        String arch = "x86";      // system architecture
        String os = "Linux";          // operating system
        String vmm = "Xen";
        double time_zone = 10.0;         // time zone this resource located
        double cost = 3.0;              // the cost of using processing in this resource
        double costPerMem = 0.05;        // the cost of using memory in this resource
        double costPerStorage = 0.1;    // the cost of using storage in this resource
        double costPerBw = 0.1;            // the cost of using bw in this resource
        LinkedList<Storage> storageList = new LinkedList<Storage>();    //we are not adding SAN devices by now

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                arch, os, vmm, hostList, time_zone, cost, costPerMem, costPerStorage, costPerBw);


        // 6. Finally, we need to create a PowerDatacenter object.
        DatacenterMkg datacenter = null;
        try {
            datacenter = new DatacenterMkg(name, characteristics, new VmAllocationPolicySimple(hostList), storageList, ConstsMkg.SCHEDULING_INTERVAL);
        } catch
        (Exception e) {
            e.printStackTrace();
        }

        return datacenter;
    }

    //We strongly encourage users to develop their own broker policies, to submit vms and cloudlets according
    //to the specific rules of the simulated scenario
    private static DatacenterBrokerMkg createBroker(String name) {

        DatacenterBrokerMkg broker = null;
        try {
            broker = new DatacenterBrokerMkg(name);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return broker;
    }

    /**
     * Prints the Cloudlet objects
     *
     * @param list list of Cloudlets
     */
    private static void printCloudletList(List<Cloudlet> list) {
        int size = list.size();
        Cloudlet cloudlet;

        String indent = "    ";
        Log.printLine();
        Log.printLine("========== OUTPUT ==========");
        Log.printLine("Cloudlet ID" + indent + "STATUS" + indent +
                "Data center ID" + indent + "VM ID" + indent + indent + "Time" + indent + "Start Time" + indent + "Finish Time");

        DecimalFormat dft = new DecimalFormat("###.##");
        for (int i = 0; i < size; i++) {
            cloudlet = list.get(i);
            Log.print(indent + cloudlet.getCloudletId() + indent + indent);

            if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
                Log.print("SUCCESS");
                //Log.print(cloudlet.getCloudletStatus()+""+indent);

                Log.printLine(indent + indent + cloudlet.getResourceId() + indent + indent + indent + indent + cloudlet.getVmId() +
                        indent + indent + indent + dft.format(cloudlet.getActualCPUTime()) +
                        indent + indent + dft.format(cloudlet.getExecStartTime()) + indent + indent + indent + dft.format(cloudlet.getFinishTime()));
            } else if (cloudlet.getCloudletStatus() == Cloudlet.FAILED) {
                Log.print("FAILED ");

                Log.printLine(indent + indent + cloudlet.getResourceId() + indent + indent + indent + indent + cloudlet.getVmId() +
                        indent + indent + indent + dft.format(cloudlet.getActualCPUTime()) +
                        indent + indent + dft.format(cloudlet.getExecStartTime()) + indent + indent + indent + dft.format(cloudlet.getFinishTime()));
            }
        }

    }
}
