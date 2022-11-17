
import xcc.*

println '''
  Enter empty line to quit.
  Spaces will be trimmed.
'''
c=System.console()
og=new Occlude_G()

for (;;) {

    print 'Password: '
    pw=new String(c.readPassword()).trim()
    if (!pw) break

    println og.occlude(pw)


}
println '--'

