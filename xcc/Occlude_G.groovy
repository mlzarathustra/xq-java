/*
    Simple not-very-secure password occlusion
    with salt and \0 padding
    
    Easy to crack, but not to read
*/
package xcc

class Occlude_G {
    static Random random=new Random()

    static def hexString(c) {
        if (c instanceof String) c = c[0]
        def rs = Integer.toHexString( ((int) c) % 128 )
        rs.size() == 1 ? '0'+rs : rs
    }

    static String occlude(String s) {
        if (!s) return ''
        int inc = 7+ random.nextInt(110) // n.b. 128%128 = 0
        int off = 0
        def rs = hexString(inc)
        if (s.size() < 20) s += ('\0' * (20 - s.size()))
        rs + s.collect { 
            off = (off + inc) % 128
            hexString( (off + (int)it.charAt(0)) % 128) 
        }.join()
    }

    static String reveal(String s) {
        if (!s || s.length() < 2) return ''
        int inc = Integer.parseInt(s[0..1],16)
        int off = 0
        def rs = ''<<''
        for (int i=2; i<s.size(); i += 2) {
            off = (off + inc) % 128
            char c = (128 + Integer.parseInt(s[i..i+1],16) - off) % 128
            if (c != 0) rs.append(c)
        }
        rs
    }

    static def t(s) {
        def o = occlude(s)
        println ''+[o,reveal(o)]
    }  

    static void main(args) { 

        ['Phoceenne', '12+13\0\0', '$a'].each { t(it) } 
    }

    //[hexString('a'),hexString('\u0001'), hexString('\u1111')]
    //'abcdefghijklmn'.collect { hexString(it) }

}

