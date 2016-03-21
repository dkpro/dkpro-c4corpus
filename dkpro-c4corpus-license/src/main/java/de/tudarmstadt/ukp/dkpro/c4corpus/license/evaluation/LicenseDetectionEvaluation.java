/*
 * Copyright 2016
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.tudarmstadt.ukp.dkpro.c4corpus.license.evaluation;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.apache.commons.io.FileUtils;

/**
 * Binary classification to evaluate license detection on annotated dataset
 *
 * @author Omnia Zayed
 */
public class LicenseDetectionEvaluation {

    public static void evaluate(String inputFile) throws IOException {

        List<String> lines = FileUtils.readLines(new File(inputFile));

        String licenseType = "(by|by-sa|by-nd|by-nc|by-nc-sa|by-nc-nd|publicdomain)";
        String noLicense = "none";

        int tp = 0;
        int tn = 0;
        int fp = 0;
        int fn = 0;

        for (String line : lines) {

            String fileID = line.split("\t")[0];
            String goldLicense = line.split("\t")[1];
            String predictedLicense = line.split("\t")[2];

            if (goldLicense.equalsIgnoreCase(predictedLicense)) {
                if (goldLicense.matches(licenseType)) {
                    tp++;
                } else if (goldLicense.matches(noLicense)) {
                    tn++;
                }
            } else {
                if (goldLicense.matches(noLicense) && predictedLicense.matches(licenseType)) {
                    fp++;
                    System.out.println("False positive detected: " + fileID);
                } else if (goldLicense.matches(licenseType) && predictedLicense.matches(noLicense)) {
                    fn++;
                    System.out.println("False negative detected: " + fileID);
                }
            }
        }

        float precision = tp / (float) (tp + fp);
        float recall = tp / (float) (tp + fn);
        float fScore = (2 * precision * recall) / (precision + recall);

        System.out.println("The page is licensed and predicted as licensed => True Positive: " + tp);
        System.out.println("The page is not licensed and not predicted as licensed => True Negative: " + tn);
        System.out.println("The page has a CC link but not licensed, however predicted as licensed => False Positive: " + fp);
        System.out.println("The page is licensed but not predicted as licensed => False Negative: " + fn);
        System.out.println("=====================================");
        System.out.println("Precision: " + precision);
        System.out.println("Recall: " + recall);
        System.out.println("F-Score: " + fScore);
    }

    public static void main(String args[]) throws IOException {
//        String inputFile = "predicted_gold_licenses_enSplit.txt";
//        String inputFile = "/home/local/UKP/zayed/NetBeansProjects/LicenseDetectionOfAnnotatedPages/predicted_gold_licenses_enSplit.txt";
        String inputFile = args[0];

        evaluate(inputFile);
    }

}
