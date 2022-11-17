/*
    Simple not-very-secure password occlusion
    with salt and \0 padding
    
    Easy to crack, but not to read
*/
package xq;

import static java.lang.System.out;

import java.util.Random;
import java.util.Arrays;

public class Occlude_J {
    static Random random=new Random();

    static String hexString(char c) {
        String rs = Integer.toHexString( ((int) c) % 128 );
        return rs.length() == 1 ? '0'+rs : rs;
    }

    public static String occlude(String s) {
        int inc = 7+ random.nextInt(110);
        int off = 0;

        StringBuilder rs = new StringBuilder();
        rs.append(hexString((char)inc));

        if (s.length() < 20) {  // pad 
            for (int i=0; s.length() < 20; ++i) 
                s += '\0';
        }

        for (int i=0; i<s.length(); ++i) {
            off = (off + inc) % 128;
            rs.append(hexString( (char) 
                ( (off + (int)s.charAt(i)) % 128)) );
        }
        return rs.toString();

    }

    public static String reveal(String s) {
        int inc = Integer.parseInt(s.substring(0,2),16);
        int off = 0;
        StringBuilder rs = new StringBuilder();
        for (int i=2; i<s.length(); i += 2) {
            off = (off + inc) % 128;
            char c =(char) ( 
                (128 + Integer.parseInt(s.substring(i,i+2),16) - off) % 128 ); 
            if (c != 0) rs.append(c);
        }
        return rs.toString();
    }

    static void t(String s) {
        String o = occlude(s);
        out.println(o + ' ' +reveal(o));
    }  

    public static void main(String[] args) { 
        String[] testStrings = {"Phoceenne", "12+13\0\0", "$a"};
        Arrays.stream(testStrings).forEach( it -> t(it) ); 
    }

}

