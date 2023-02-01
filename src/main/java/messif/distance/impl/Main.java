/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package messif.distance.impl;

/**
 *
 * @author xmic
 */
public class Main {

    public static void main(String[] args) {
        ProteinNativeQScoreDistance.initDistance(args[0]);
        ProteinNativeQScoreDistance dist = new ProteinNativeQScoreDistance();
        float[] statsFloats = dist.getStatsFloats("4BRL:A", "4BRK:B", 0);
        System.out.println("Distance of 4BRL:A and 4BRK:B was successfully evaluated. The result is: " + statsFloats[0]);
    }
}
