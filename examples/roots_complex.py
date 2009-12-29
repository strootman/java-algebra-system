#
# jython examples for jas.
# $Id$
#

import sys;

from java.lang import System
from java.lang import Integer

from jas import Ring
from jas import PolyRing
from jas import Ideal
from jas import terminate
from jas import startLog

from jas import QQ, DD, CR

# polynomial examples: complex roots over Q

#r = Ring( "Rat(x) L" );
#r = Ring( "Q(x) L" );
r = PolyRing( CR(QQ()), "x", PolyRing.lex );

print "Ring: " + str(r);
print;

[one,I,x] = r.gens();


f1 = x**3 - 2;

f2 = ( x - I ) * ( x + I );

f3 = ( x**3 - 2 * I );

#f = f1 * f2 * f3;
#f = f1 * f2;
#f = f1 * f3;
#f = f2 * f3;
f = f3;


print "f = ", f;
print;

#startLog();

t = System.currentTimeMillis();
R = r.complexRoots(f);
t = System.currentTimeMillis() - t;
print "R = ", R;
print "complex roots time =", t, "milliseconds";

#terminate();
#sys.exit();

#eps = QQ(1,10) ** DD().elem.DEFAULT_PRECISION; # too big
eps = QQ(1,10) ** 6;
print "eps = ", eps;

t = System.currentTimeMillis();
R = r.complexRoots(f,eps);
t = System.currentTimeMillis() - t;
print "R = ", R;
print "complex root refinement time =", t, "milliseconds";

#startLog();
terminate();