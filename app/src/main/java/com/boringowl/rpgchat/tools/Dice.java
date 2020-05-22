package com.boringowl.rpgchat.tools;

import java.util.Objects;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Dice {
    private static String outputString;
    private static Pattern rollPattern = Pattern.compile("([0-9]*)?d([0-9]*)?");
    private static Pattern modifierPattern = Pattern.compile("[-]*(\\d+)");

    public static String roll(String sentMessage) {
        String[] rolls;
        sentMessage = sentMessage.split(" ")[0].substring(1);
        rolls = sentMessage.split("(?=-)|[+]");
        outputString = "";
        int result = 0;
        boolean isRoll = false;

        for (String message : rolls) {
            Matcher rollMatcher = rollPattern.matcher(message);
            Matcher modifierMatcher = modifierPattern.matcher(message);

            if (rollMatcher.matches()) {
                isRoll = true;
                int amount = (Objects.requireNonNull(rollMatcher.group(1)).length() != 0) ? Integer.parseInt(Objects.requireNonNull(rollMatcher.group(1))) : 1;
                int side = (Objects.requireNonNull(rollMatcher.group(2)).length() != 0) ? Integer.parseInt(Objects.requireNonNull(rollMatcher.group(2))) : 20;

                int rollResult = calculate(amount, side);
                result += rollResult;


            } else if (modifierMatcher.matches()) {
                isRoll = true;
                result += Integer.parseInt(message);

                if (!outputString.equals("") && !message.contains("-")) outputString += "+";
                outputString += message;
            }
        }

        if (isRoll && !outputString.equals(""))
            return ("\nResult\n(" + outputString + ") = " + result);
        else return "";
    }


    private static int calculate(int amount, int side) {
        int roll = 0;
        Random rand = new Random();

        for (int i = 0; i < amount; i++) {
            int temp = rand.nextInt(side) + 1;
            if (!outputString.equals(""))
                outputString += "+";
            outputString += temp;
            if (temp == side)
                outputString += "(Hit!)";
            if (temp == 1)
                outputString += "(Miss!)";
            roll += temp;
        }

        return roll;
    }
}