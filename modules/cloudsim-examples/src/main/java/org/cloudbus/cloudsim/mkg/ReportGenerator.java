package org.cloudbus.cloudsim.mkg;

import org.cloudbus.cloudsim.Cloudlet;

import java.io.*;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.List;


public class ReportGenerator {

    /**
     * List of cloudlet submitted to broker for execution
     */
    public List<Cloudlet> cloudletList;

    public ReportGenerator(List<Cloudlet> cloudletList) {
        this.cloudletList = cloudletList;
    }

    /**
     * Make changes in index.html file of the Report to generate
     * report of the simulation
     *
     * @throws IOException
     */
    public void generateHTMLFile() throws IOException {
        //Read index.html file into a buffer
//		BufferedReader file = new BufferedReader(new FileReader("Report/index.html"));
        BufferedReader file = new BufferedReader(new InputStreamReader(
                getClass().getClassLoader().getResourceAsStream("Report/index.html")
        ));
        String line;
        String input = "";
        while ((line = file.readLine()) != null) input += line + '\n';
        file.close();

        //String for cloudlet list in the dropdown
        StringBuilder clListSB = new StringBuilder();
        //String for div elements of the graph of each cloudlet
        StringBuilder clGraphListCB = new StringBuilder();
        //String for the html table code of the cloudlet logs
        StringBuilder clLogsSB = new StringBuilder();
        //String for the html table code of the simulation overview table
        StringBuilder simOverviewSB = new StringBuilder();

        //Appending table html code headers for the simulation overview table
        simOverviewSB.append("<div class=\"table-responsive\"><table class=\"table table-striped\"><thead>");
        simOverviewSB.append("<tr><th>Cloudlet ID</th><th>Status</th><th>Datacenter ID</th><th>VM ID</th><th>Time</th><th>Start Time</th><th>Finish Time</th></tr></thead><tbody>");

        for (int i = 0; i < cloudletList.size(); i++) {
            Cloudlet cloudlet = cloudletList.get(i);

            //Building cloudlet dropdown list and div of the graph of cloudlet
            clListSB.append("<li class=\"cloudlet-id\" id=\"cloudlet" + cloudlet.getCloudletId() + "\"><a href=\"#\">" + "Cloudlet" + cloudlet.getCloudletId() + "</a></li>\n");
            clGraphListCB.append("<div class=\"cloudlet-graph\" id=\"cloudlet" + cloudlet.getCloudletId() + "Graph\" style=\"min-width: 310px; height: 400px; margin: 0 auto\"></div>\n");

            //Building simuation overview table
            simOverviewSB.append("<tr><td>" + cloudlet.getCloudletId() + "</td>");
            if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
                simOverviewSB.append("<td><font color=\"green\">SUCCESS</font></td>");
            } else {
                simOverviewSB.append("<td><font color=\"red\">FAILED</font></td>");
            }
            DecimalFormat dft = new DecimalFormat("###.##");
            simOverviewSB.append("<td>" + cloudlet.getResourceId() + "</td>" +
                    "<td>" + cloudlet.getVmId() + "</td>" +
                    "<td>" + dft.format(cloudlet.getActualCPUTime()) + "</td>" +
                    "<td>" + dft.format(cloudlet.getExecStartTime()) + "</td>" +
                    "<td>" + dft.format(cloudlet.getFinishTime()) + "</td></tr>");


            //Building cloudlet logs table
            BufferedReader br = new BufferedReader(new FileReader("Cloudlet" + cloudlet.getCloudletId() + ".csv"));
            //clLogsSB.append("<h2 class=\"sub-header\">Cloudlet " + cloudlet.getCloudletId() + "Logs</h2>\n");
            clLogsSB.append("<div class=\"table-responsive cloudlet-log\" id=\"cloudlet" + cloudlet.getCloudletId() + "Log\">"
                    + "<table class=\"table table-striped\"><thead><tr><th>ClockTick</th><th>Cloudlet ID</th><th>Status</th><th>VM ID</th><th>Remaining Cloudlet Length</th><th>Cloudlet Finished So Far</th>"
                    + "</tr></thead><tbody>");
            int k = 0;
            while ((line = br.readLine()) != null) {
                // use comma as separator
                if (k > 0) {
                    String[] cloudletData = line.split(",");
                    clLogsSB.append("<tr><td>" + cloudletData[0] + "</td>" +
                            "<td>" + cloudletData[1] + "</td>" +
                            "<td>" + cloudletData[2] + "</td>" +
                            "<td>" + cloudletData[3] + "</td>" +
                            "<td>" + cloudletData[4] + "</td>" +
                            "<td>" + cloudletData[5] + "</td></tr>");

                }
                k++;
            }
            br.close();
            clLogsSB.append("</tbody></table></div>\n");

        }
        simOverviewSB.append("</tbody></table></div>");

        //Replacing in index.html
        input = input.replace("<!-- OVERVIEW HERE -->", simOverviewSB.toString());
        input = input.replace("<!-- CLOUDLET LIST HERE -->", clListSB.toString());
        input = input.replace("<!-- CLOUDLET GRAPH LIST HERE -->", clGraphListCB.toString());
        input = input.replace("<!-- CLOUDLET LOG TABLE HERE -->", clLogsSB.toString());
        input = input.replace("<!-- CLOUDLET ID HERE -->", clLogsSB.toString());

        URL resource = Thread.currentThread().getContextClassLoader().getResource("Report/index1.html");
        String path = resource.getPath();

        FileOutputStream fileOut = new FileOutputStream(path);
        fileOut.write(input.getBytes());
        fileOut.close();
    }

    /**
     * Make changes in myscript.js file for report generation
     *
     * @throws IOException
     */
    public void generateCloudletReport() throws IOException {

        //Read myscriptFile into buffer

        BufferedReader file = new BufferedReader(new InputStreamReader(
                getClass().getClassLoader().getResourceAsStream("Report/assets/js/myscript.js")
        ));

        //BufferedReader file = new BufferedReader(new FileReader("Report/assets/js/myscript.js"));
        String line;
        String input = "";
        while ((line = file.readLine()) != null) input += line + '\n';
        file.close();


        StringBuilder finalGraphTemplateSB = new StringBuilder();

        for (int i = 0; i < cloudletList.size(); i++) {

            BufferedReader file1 = new BufferedReader(new InputStreamReader(
                    getClass().getClassLoader().getResourceAsStream("Report/assets/js/graphTemplate.js")
            ));

            //BufferedReader file1 = new BufferedReader(new FileReader("Report/assets/js/graphTemplate.js"));
            String line1;
            String input1 = "";
            while ((line1 = file1.readLine()) != null) input1 += line1 + '\n';
            file1.close();

            Cloudlet cloudlet = cloudletList.get(i);
            BufferedReader br = new BufferedReader(new FileReader("Cloudlet" + cloudlet.getCloudletId() + ".csv"));

            StringBuilder clockTicksSB = new StringBuilder();
            StringBuilder remainLengthSB = new StringBuilder();
            int k = 0;
            while ((line = br.readLine()) != null) {
                // use comma as separator
                if (k > 0) {
                    String[] cloudletData = line.split(",");
                    clockTicksSB.append("'" + cloudletData[0] + "',");
                    remainLengthSB.append(cloudletData[4] + ",");
                }
                k++;
            }
            br.close();

            String clockTicks = clockTicksSB.toString();
            String remainLength = remainLengthSB.toString();

            clockTicks = clockTicks.substring(0, clockTicks.length() - 1);
            remainLength = remainLength.substring(0, remainLength.length() - 1);

            input1 = input1.replaceAll("<!-- CLOUDLET ID -->", cloudlet.getCloudletId() + "");
            input1 = input1.replace("<!-- Clock Ticks Here -->", clockTicks);
            input1 = input1.replace("<!-- SERIES NAME -->", "Remaining Cloudlet Length");
            input1 = input1.replace("<!-- SERIES VALUES -->", remainLength);

            finalGraphTemplateSB.append(input1 + "\n\n");

        }
        input = input.replace("<!-- GRAPH TEMPLATE -->", finalGraphTemplateSB.toString());

        URL resource = Thread.currentThread().getContextClassLoader().getResource("Report/assets/js/myscript1.js");
        String path = resource.getPath();

        FileOutputStream fileOut = new FileOutputStream(path);
        fileOut.write(input.getBytes());
        fileOut.close();
    }
}
