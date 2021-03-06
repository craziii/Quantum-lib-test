package com.quantumtest;

import org.redfx.strange.Gate;
import org.redfx.strange.Program;
import org.redfx.strange.Qubit;
import org.redfx.strange.Result;
import org.redfx.strange.Step;
import org.redfx.strange.gate.Fourier;
import org.redfx.strange.gate.Hadamard;
import org.redfx.strange.gate.Identity;
import org.redfx.strange.gate.ProbabilitiesGate;
import org.redfx.strange.gate.Toffoli;
import org.redfx.strange.gate.X;
import org.redfx.strange.gate.Y;
import org.redfx.strange.gate.Z;
import org.redfx.strange.local.SimpleQuantumExecutionEnvironment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {

    public static void main(String[] args) {
        String firstArg;
        int numTests;
        int stepsToRun;
        try{
            if(args[1] == null){
                numTests = 20;
            }
            else{
                numTests = Integer.parseInt(args[1]);
            }
        }
        catch(Exception e) {
            numTests = 20;
        }
        try{
            if(args[2] == null){
                stepsToRun = 1000000;
            }
            else{
                stepsToRun = Integer.parseInt(args[2]);
            }
        }
        catch(Exception e){
            stepsToRun = 1000000;
        }
        try{
            firstArg = args[0].toLowerCase();
            switch (firstArg){
                case "-h":
                case "--help":
                    getOptions(); break;
                case "-g":
                case "--general":
                    generalTest(numTests,stepsToRun); break;
                case "-r":
                case "--rotation":
                    rotationTest(numTests,stepsToRun); break;
                case "-t":
                case "--testrng":
                    rngTest(numTests,stepsToRun); break;
                case "-a":
                case "--analyse":
                    analyseRngTest(); break;
                default: getOptions(); break;
            }
        }
        catch(Exception e){
            getOptions();
            e.printStackTrace();
        }
        System.console().flush();
    }

    static void getOptions(){
        printIntro();
        printOption("help", "Prints this help option");
        printOption("general","Run a general test on a few gates and print them to a file");
        printOption("rotation", "Run a test on the rotation gate R");
        printOption("testRNG","Run a set of tests for RNG values");
        printOption("analyse", "analyse the RNG test and print the results to a file");
        System.console().flush();
    }

    private static void printIntro() {
        System.console().writer().println("\nQuantum Testing Software for the redfx Strange Library\n");
        System.console().writer().println("Argument format: java -jar quantum-test.jar --option numTests numSteps\n\n");
    }

    private static void printOption(String option, String info) {
        System.console().writer().println("(-"+option.charAt(0)+"),(--"+option+"), "+info+"\n");
    }

    static void generalTest(int numTests,int steps){
        String folder = "general/";
        createFile(folder,"hadamard.csv",new Hadamard(0),numTests,steps);
        createFile(folder,"identity.csv",new Identity(),numTests,steps);
        createFile(folder,"pauliX.csv",new X(0),numTests,steps);
        createFile(folder,"pauliY.csv",new Y(0),numTests,steps);
        createFile(folder,"pauliZ.csv",new Z(0),numTests,steps);
        createFile(folder,"fourier.csv",new Fourier(0,0),numTests,steps);
        createFile(folder,"probabilitiesGate.csv",new ProbabilitiesGate(0),numTests,steps);
        createFile(folder,"toffoli.csv",new Toffoli(0,0,0),numTests,steps);
    }

    private static void rotationTest(int numTests, int steps) {
        String folder = "rotation/";
        String baseFileName = "Rotation";
        String baseFileEnd = ".csv";
        for (int axis = 0; axis < 3; axis++){
            for(double i = 0; i < 6.3; i+=0.1){
                Gate[] gates = new Gate[2];
                gates[0] = new Hadamard(0);
                switch (axis){
                    case 0: gates[1] = new RotationX(i,0);
                    case 1: gates[1] = new RotationY(i,0);
                    case 2: gates[1] = new RotationZ(i,0);
                }
                StringBuilder trimmedName = new StringBuilder();
                for(int j = 0; j < 4; j++){
                    try {
                        trimmedName.append(String.valueOf(i).charAt(j));
                    }
                    catch(Exception e){

                    }
                }
                createFile(folder,baseFileName+trimmedName+"axis"+axis+baseFileEnd,gates,numTests,steps, "angle:"+trimmedName.toString()+" axis:"+axis);
            }
        }
    }

    private static void rngTest(int numTests, int steps){
        String folder = "RNG/";
        String baseFileName = "Angle";
        String baseFileEnd = ".csv";
        for(double i = 0; i < 4*Math.PI; i+=Math.PI/50){
            Gate[] gate = new Gate[1];
            gate[0] = new RotationX(i,0);
            StringBuilder trimmedName = new StringBuilder();
            for(int j = 0; j < 5; j++){
                try {
                    trimmedName.append(String.valueOf(i).charAt(j));
                }
                catch(Exception e){

                }
            }
            createFile(folder,baseFileName+trimmedName+baseFileEnd,gate,numTests,steps, "angle:"+trimmedName.toString());
        }
    }

    private static void analyseRngTest(){
        File outputFile = new File("RNGResults.csv");
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
            String[] header = {"Angle","Zeros","Ones","Sims","Probability"};
            fileOutputStream.write(getCSVLine(",",header).getBytes(StandardCharsets.UTF_8));
            String directory = "RNG/";
            File folder = new File(directory);
            File[] fileList = folder.listFiles();
            List<String> filenames = new ArrayList<>();
            assert fileList != null;
            for (File f:fileList) {
                filenames.add(f.getName());
            }
            for (String filename:filenames) {
                //System.console().writer().println("\n\n\nFilename = "+filename);
                double angle = getAngle(filename);
                int zeros = 0;
                int ones = 0;
                int sims = 0;
                double probability = 0;
                int count = 0;
                BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(directory+filename)));
                //System.console().writer().println("Reading from file: "+filename);
                br.readLine();
                while (br.ready()) {
                    String line = br.readLine();
                    String[] parts = line.split(",");
                    zeros += Integer.parseInt(parts[1]);
                    ones += Integer.parseInt(parts[2]);
                    sims += Integer.parseInt(parts[3]);
                    probability += Double.parseDouble(parts[4]);
                    count++;
                }
                probability = probability/count;
                String[] outputs = {String.valueOf(angle),String.valueOf(zeros),String.valueOf(ones),String.valueOf(sims),String.valueOf(probability)};
                fileOutputStream.write(getCSVLine(",",outputs).getBytes(StandardCharsets.UTF_8));
            }
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    static double getAngle(String filename){
        if(filename.startsWith("Angle") && filename.endsWith(".csv")){
            StringBuilder output = new StringBuilder();
            output.append(filename);
            output.delete(output.length()-4,output.length());
            output.delete(0,"Angle".length());
            return Double.parseDouble(output.toString());
        }
        return 0;
    }

    private static void runSteps(Gate gate, FileOutputStream fileOutputStream, int count, int steps) throws IOException {
        long zeroCount = 0;
        long oneCount = 0;
        Program program = new Program(1);
        SimpleQuantumExecutionEnvironment sqee = new SimpleQuantumExecutionEnvironment();
        program.addStep(createGate(gate));
        simulateCircuit(fileOutputStream, count, steps, zeroCount, oneCount, program, sqee);
    }

    private static void runSteps(Gate[] gates, FileOutputStream fileOutputStream, int count, int steps) throws IOException {
        long zeroCount = 0;
        long oneCount = 0;
        Program program = new Program(1);
        SimpleQuantumExecutionEnvironment sqee = new SimpleQuantumExecutionEnvironment();
        for (Gate gate:gates) {
            program.addStep(createGate(gate));
        }
        simulateCircuit(fileOutputStream, count, steps, zeroCount, oneCount, program, sqee);
    }

    private static void simulateCircuit(FileOutputStream fileOutputStream, int count, int steps, long zeroCount, long oneCount, Program program, SimpleQuantumExecutionEnvironment sqee) throws IOException {
        for (int i = 0; i < steps; i++) {
            int temp = simulateCircuit(sqee, program);
            switch (temp){
                case 0: zeroCount += 1; break;
                case 1: oneCount += 1; break;
                default: break;
            }
            //fileOutputStream.write(outputString.getBytes(StandardCharsets.UTF_8));
        }
        double average = (double)oneCount/(oneCount+zeroCount);
        fileOutputStream.write(getCSVLine(",",count,zeroCount,oneCount,zeroCount+oneCount,average).getBytes(StandardCharsets.UTF_8));
    }

    private static void createFile(String folder, String file, Gate gate, int tests, int steps) {
        try {
            File outputFolder = new File(folder);
            outputFolder.mkdir();
            File outputFile = new File(folder+file);
            FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
            fileOutputStream.write("testNo,zeros,ones,cycles,average\n".getBytes(StandardCharsets.UTF_8));
            for(int j = 1; j <= tests; j++) {
                System.console().writer().println("Running test number: "+j+" on gate: "+gate.getName());
                runSteps(gate, fileOutputStream, j, steps);
            }
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void createFile(String folder, String file, Gate[] gates, int tests, int steps, String extra) {
        try {
            File outputFolder = new File(folder);
            outputFolder.mkdir();
            File outputFile = new File(folder+file);
            FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
            fileOutputStream.write("testNo,ones,zeros,cycles,average\n".getBytes(StandardCharsets.UTF_8));
            for(int j = 1; j <= tests; j++) {
                if(gates.length > 1){
                    System.console().writer().println("Running test number: "+j+" on gate: "+gates[1].getName() + " with extra information: "+extra);
                }
                else{
                    System.console().writer().println("Running test number: "+j+" on gate: "+gates[0].getName() + " with extra information: "+extra);
                }
                runSteps(gates, fileOutputStream, j, steps);
            }
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static Step createSimpleGate(){
        Step step = new Step();
        Gate hadamard = new Hadamard(0);
        step.addGate(hadamard);
        return step;
    }

    static Step createGate(Gate gate){
        Step step = new Step();
        step.addGate(gate);
        return step;
    }

    static int simulateCircuit(SimpleQuantumExecutionEnvironment env, Program p){
        try{
            Result result = env.runProgram(p);
            Qubit qubit = result.getQubits()[0];
            return qubit.measure();
        } catch (Exception e) {
            //e.printStackTrace();
        }
        return 2;
    }

    static String getCSVLine(String delimiter, int testNum, long zeros, long ones, long sims, double probability){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(testNum);
        stringBuilder.append(delimiter);
        stringBuilder.append(zeros);
        stringBuilder.append(delimiter);
        stringBuilder.append(ones);
        stringBuilder.append(delimiter);
        stringBuilder.append(sims);
        stringBuilder.append(delimiter);
        stringBuilder.append(probability);
        stringBuilder.append("\n");
        return stringBuilder.toString();

    }

    static String getCSVLine(String delimiter, String[] strings){
        StringBuilder stringBuilder = new StringBuilder();
        for (String s:strings) {
            stringBuilder.append(s);
            stringBuilder.append(delimiter);
        }
        stringBuilder.deleteCharAt(stringBuilder.length()-1);
        stringBuilder.append("\n");
        return stringBuilder.toString();
    }
}