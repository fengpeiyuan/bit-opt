package com.fengpeiyuan.util;

public class BitUtil {

    /**
     * convert int to byte[]
     * @param amount
     * @return
     */
    public static byte[] getBytesByAmount(Integer amount){
        Integer div = amount / 8;
        Integer remainder = amount % 8;

        byte[] ret = new byte[div+1];
        for(int i=0;i<div;i++){
            ret[i] = (byte)0xff;
        }

        switch (remainder){
            case 1:
                ret[div] = (byte)0x80;
                break;
            case 2:
                ret[div] = (byte)0xc0;
                break;
            case 3:
                ret[div] = (byte)0xe0;
                break;
            case 4:
                ret[div] = (byte)0xf0;
                break;
            case 5:
                ret[div] = (byte)0xf8;
                break;
            case 6:
                ret[div] = (byte)0xfc;
                break;
            case 7:
                ret[div] = (byte)0xfe;
                break;
        }

        return ret;
    }


    /**
     * print or debug only
     * @param in
     * @return
     */
    public static char[] printCharArrayFromByte(byte in) {
        char[] carr = {'0','0','0','0','0','0','0','0'};
        byte b = in;
        for (int i = 7; i >= 0; i--) {
            if((byte)(b & 1) != 0x00)
                carr[i] = '1';
            b = (byte) (b>>1);
        }
        return carr;
    }



}