/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.koveloper.apache.mina.wrapper.network;

/**
 *
 * @author koveloper
 */
public class Utils {
    
    public static int[] toIntArray(byte[] byteArray) {
        if (byteArray == null) {
            return null;
        }
        int[] arr = new int[byteArray.length];
        int i = 0;
        for (byte b : byteArray) {
            arr[i++] = Byte.toUnsignedInt(b);
        }
        return arr;
    }
    
    public static String toHexString(byte[] array) {
        int[] arr = toIntArray(array);
        String[] ret = new String[arr.length];
        for(int i = 0; i < arr.length; i++) {
            ret[i] = (arr[i] < 16 ? "0" : "") + Integer.toHexString(arr[i]).toUpperCase();
        }
        return String.join(" ", ret);
    }
}
