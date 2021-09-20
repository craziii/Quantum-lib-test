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
import org.redfx.strange.gate.R;
import org.redfx.strange.gate.Toffoli;
import org.redfx.strange.gate.X;
import org.redfx.strange.gate.Y;
import org.redfx.strange.gate.Z;
import org.redfx.strange.local.SimpleQuantumExecutionEnvironment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

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
                default: getOptions(); break;
            }
        }
        catch(Exception e){
            getOptions();
        }
        System.console().flush();
    }

    static void getOptions(){
        printIntro();
        printOption("help", "Prints this help option");
        printOption("general","Run a general test on a few gates and print them to a file");
        printOption("rotation", "Run a test on the rotation gate R");
        System.console().flush();
    }

    private static void printIntro() {
        System.console().writer().println("\nQuantum Testing Software for the redfx Strange Library\n");
        System.console().writer().println("Argument format: java -jar quantum-test.jar \"--option\" \"numTests\" \"numSteps\"\n\n");
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
        for(double i = 0; i < 6.3; i+=0.1){
            createFile(folder,baseFileName+ i +baseFileEnd,new R(i,0),numTests, steps,i+"");
        }
    }

    private static void runSteps(Gate gate, FileOutputStream fileOutputStream, int count, int steps) throws IOException {
        long zeroCount = 0;
        long oneCount = 0;
        Program program = new Program(1);
        SimpleQuantumExecutionEnvironment sqee = new SimpleQuantumExecutionEnvironment();
        program.addStep(createGate(gate));
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
            fileOutputStream.write("testNo,ones,zeros,cycles,average\n".getBytes(StandardCharsets.UTF_8));
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

    private static void createFile(String folder, String file, Gate gate, int tests, int steps, String extra) {
        try {
            File outputFolder = new File(folder);
            outputFolder.mkdir();
            File outputFile = new File(folder+file);
            FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
            fileOutputStream.write("testNo,ones,zeros,cycles,average\n".getBytes(StandardCharsets.UTF_8));
            for(int j = 1; j <= tests; j++) {
                System.console().writer().println("Running test number: "+j+" on gate: "+gate.getName() + " with extra information: "+extra);
                runSteps(gate, fileOutputStream, j, steps);
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
}