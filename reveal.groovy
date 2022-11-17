
import xcc.*

println '''
  Enter empty line to quit.
  Spaces will be trimmed.
'''
c=System.console()
og=new Occlude_G()

for (;;) {

    print 'Occluded password: '
    pw=new String(c.readLine()).trim()
    if (!pw) break

    println og.reveal(pw)


}
println '--'

