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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Main {

    public static void main(String[] args) {
        int numTests = 50;
        createFile("hadamard.csv",new Hadamard(0),numTests);
        createFile("identity.csv",new Identity(),numTests);
        createFile("pauliX.csv",new X(0),numTests);
        createFile("pauliY.csv",new Y(0),numTests);
        createFile("pauliZ.csv",new Z(0),numTests);
        createFile("fourier.csv",new Fourier(0,0),numTests);
        createFile("probabilitiesGate.csv",new ProbabilitiesGate(0),numTests);
        createFile("toffoli.csv",new Toffoli(0,0,0),numTests);
    }

    private static void createFile(String file, Gate gate, int tests) {
        try {
            File outputFile = new File(file);
            FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
            fileOutputStream.write("testNo,ones,zeros,cycles,average\n".getBytes(StandardCharsets.UTF_8));
            for(int j = 1; j <= tests; j++) {
                int stepsToRun = 1000000;
                long zeroCount = 0;
                long oneCount = 0;
                Program program = new Program(1);
                SimpleQuantumExecutionEnvironment sqee = new SimpleQuantumExecutionEnvironment();
                program.addStep(createGate(gate));
                for (int i = 0; i < stepsToRun; i++) {
                    int temp = simulateCircuit(sqee, program);
                    switch (temp){
                        case 0: zeroCount += 1; break;
                        case 1: oneCount += 1; break;
                        default: break;
                    }
                    //fileOutputStream.write(outputString.getBytes(StandardCharsets.UTF_8));
                }
                double average = (double)oneCount/(oneCount+zeroCount);
                fileOutputStream.write(getCSVLine(",",j,zeroCount,oneCount,zeroCount+oneCount,average).getBytes(StandardCharsets.UTF_8));
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