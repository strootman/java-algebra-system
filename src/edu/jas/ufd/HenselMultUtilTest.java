/*
 * $Id$
 */

package edu.jas.ufd;


import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.BasicConfigurator;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import edu.jas.arith.PrimeList;
import edu.jas.arith.BigInteger;
import edu.jas.arith.ModInteger;
import edu.jas.arith.ModIntegerRing;
import edu.jas.arith.ModLong;
import edu.jas.arith.ModLongRing;
import edu.jas.arith.ModularRingFactory;
import edu.jas.structure.Power;
import edu.jas.kern.ComputerThreads;
import edu.jas.poly.ExpVector;
import edu.jas.poly.GenPolynomial;
import edu.jas.poly.GenPolynomialRing;
import edu.jas.poly.PolyUtil;
import edu.jas.poly.TermOrder;
import edu.jas.application.Ideal;
import edu.jas.application.Residue;
import edu.jas.application.ResidueRing;


/**
 * HenselMultUtil tests with JUnit.
 * @author Heinz Kredel.
 */

public class HenselMultUtilTest extends TestCase {


    /**
     * main.
     */
    public static void main(String[] args) {
        BasicConfigurator.configure();
        junit.textui.TestRunner.run(suite());
        ComputerThreads.terminate();
    }


    /**
     * Constructs a <CODE>HenselMultUtilTest</CODE> object.
     * @param name String.
     */
    public HenselMultUtilTest(String name) {
        super(name);
    }


    /**
     */
    public static Test suite() {
        TestSuite suite = new TestSuite(HenselMultUtilTest.class);
        return suite;
    }


    TermOrder tord = new TermOrder(TermOrder.INVLEX);


    GenPolynomialRing<BigInteger> dfac;


    GenPolynomialRing<BigInteger> cfac;


    GenPolynomialRing<GenPolynomial<BigInteger>> rfac;


    BigInteger ai;


    BigInteger bi;


    BigInteger ci;


    BigInteger di;


    BigInteger ei;


    GenPolynomial<BigInteger> a;


    GenPolynomial<BigInteger> b;


    GenPolynomial<BigInteger> c;


    GenPolynomial<BigInteger> d;


    GenPolynomial<BigInteger> e;


    int rl = 2;


    int kl = 5;


    int ll = 5;


    int el = 3;


    float q = 0.3f;


    @Override
    protected void setUp() {
        a = b = c = d = e = null;
        ai = bi = ci = di = ei = null;
        dfac = new GenPolynomialRing<BigInteger>(new BigInteger(1), rl, tord);
        cfac = new GenPolynomialRing<BigInteger>(new BigInteger(1), rl - 1, tord);
        rfac = new GenPolynomialRing<GenPolynomial<BigInteger>>(cfac, 1, tord);
    }


    @Override
    protected void tearDown() {
        a = b = c = d = e = null;
        ai = bi = ci = di = ei = null;
        dfac = null;
        cfac = null;
        rfac = null;
        ComputerThreads.terminate();
    }


    protected static java.math.BigInteger getPrime1() {
        return PrimeList.getLongPrime(60,93);
    }


    protected static java.math.BigInteger getPrime2() {
        return PrimeList.getLongPrime(30,35);
    }


    /**
     * Test multivariate diophant lifting.
     * 
     */
    public void testDiophantLifting() {
        java.math.BigInteger p;
        //p = getPrime1();
        p = new java.math.BigInteger("19");
        //p = new java.math.BigInteger("5");
        BigInteger m = new BigInteger(p);
        //.multiply(p).multiply(p).multiply(p);

        ModIntegerRing pm = new ModIntegerRing(p, false);
        //ModLongRing pl = new ModLongRing(p, false);
        GenPolynomialRing<ModInteger> pfac = new GenPolynomialRing<ModInteger>(pm, 2, tord, new String[]{ "x", "y" });

        BigInteger mi = m;
        long k = 5L;
        long d = 3L;
        java.math.BigInteger pk = p.pow((int)k);
        m = new BigInteger(pk);

        ModIntegerRing pkm = new ModIntegerRing(pk, false);
        //ModLongRing pkl = new ModLongRing(pk, false);
        GenPolynomialRing<ModInteger> pkfac = new GenPolynomialRing<ModInteger>(pkm, 2, tord, new String[]{ "x", "y" });
        dfac = new GenPolynomialRing<BigInteger>(mi, 2, tord, new String[]{ "x", "y" });

        //GreatestCommonDivisor<BigInteger> ufd = GCDFactory.getProxy(mi);
        GreatestCommonDivisor<BigInteger> ufd = GCDFactory.getImplementation(mi);

        //ModLong v = pl.fromInteger(3L);
        ModInteger v = pkm.fromInteger(0L);

        GenPolynomial<ModInteger> ap;
        GenPolynomial<ModInteger> bp;
        GenPolynomial<ModInteger> cp;
        GenPolynomial<ModInteger> dp;
        GenPolynomial<ModInteger> sp;
        GenPolynomial<ModInteger> tp;
        GenPolynomial<ModInteger> rp;

        for (int i = 1; i < 2; i++) {
            //a = dfac.random(kl + 70 * i, ll, el + 5, q).abs();
            //b = dfac.random(kl + 70 * i, ll, el + 5, q).abs();
            //a = dfac.parse(" y^2 + 2 x y - 3 y + x^2 - 3 x - 4 ");
            //b = dfac.parse(" y^2 + 2 x y + 5 y + x^2 + 5 x + 4 ");
            //a = dfac.parse(" (x - 4 + y)*( x + y + 1 ) ");
            //b = dfac.parse(" (x + 4 + y)*( x + y + 1 ) ");
            //a = dfac.parse(" (x - 4 + y) ");
            ///a = dfac.parse(" (x - 13 + y) ");
            ///b = dfac.parse(" (x + 4 + y) ");
            //a = dfac.parse(" (x - 1)*(1 + x) ");
            //b = dfac.parse(" (x - 2)*(3 + x) ");
            //a = dfac.parse(" (x - 1)*(y + x) ");
            //b = dfac.parse(" (x - 2)*(y - x) ");
            //a = dfac.parse(" (x - 1)*(y + 1) ");
            //b = dfac.parse(" (x - 2)*(y - 1) ");
            //a = dfac.parse(" (x - 1)*(y^2 + 1) ");
            //b = dfac.parse(" (x - 2)*(y^2 - 1) ");
            a = dfac.parse(" (y - 1)*(1 + y) ");
            b = dfac.parse(" (y - 2)*(3 + y) ");
            ///a = dfac.parse(" (y - 3) "); //2 // tp = 47045880 = -1
            ///b = dfac.parse(" (y - 1) "); // sp = 1
            //a = dfac.parse(" (y - 4) "); // tp = 15681960
            //b = dfac.parse(" (y - 1) "); // sp = 31363921
            //a = dfac.parse(" (x - 3) "); // tp = 15681960,  1238049
            //b = dfac.parse(" (x - 1) "); // sp = 31363921, -1238049

            c = ufd.gcd(a,b);
            System.out.println("\na     = " + a);
            System.out.println("b     = " + b);
            System.out.println("c     = " + c);

            if ( ! c.isUnit() ) {
                continue;
	    }
            //c = c.sum(c);

            ap = PolyUtil.<ModInteger> fromIntegerCoefficients(pfac,a);
            bp = PolyUtil.<ModInteger> fromIntegerCoefficients(pfac,b);
            cp = PolyUtil.<ModInteger> fromIntegerCoefficients(pkfac,c);
            //if (ap.degree(0) < 1 || bp.degree(0) < 1) {
            //    continue;
            //}
            System.out.println("\nap     = " + ap);
            System.out.println("bp     = " + bp);
            System.out.println("cp     = " + cp);

            List<GenPolynomial<ModInteger>> lift;
            try {
                lift = HenselMultUtil.<ModInteger> liftDiophant(ap, bp, cp, v, d, k); // 5 is max
                sp = lift.get(0);
                tp = lift.get(1);
                System.out.println("");
                System.out.println("sp     = " + sp);
                System.out.println("tp     = " + tp);
                System.out.println("isDiophantLift: " +  HenselUtil.<ModInteger> isDiophantLift(bp,ap,sp,tp,cp) );

                GenPolynomialRing<ModInteger> qfac = sp.ring;
                System.out.println("qfac   = " + qfac.toScript());
                System.out.println("sp.ldfc = " + sp.leadingBaseCoefficient());
                GenPolynomialRing<BigInteger> ifac = new GenPolynomialRing<BigInteger>(new BigInteger(),sp.ring);
                dp = PolyUtil.<ModInteger> fromIntegerCoefficients(qfac,c);
                System.out.println("cp == dp: " + cp.equals(dp));
                System.out.println("cp == dp: " + cp.ring.equals(dp.ring));

                //ap = PolyUtil.<ModInteger> fromIntegerCoefficients(qfac,a);
                //bp = PolyUtil.<ModInteger> fromIntegerCoefficients(qfac,b);
                ap = PolyUtil.<ModInteger> fromIntegerCoefficients(qfac,PolyUtil.integerFromModularCoefficients(ifac, ap));
                bp = PolyUtil.<ModInteger> fromIntegerCoefficients(qfac,PolyUtil.integerFromModularCoefficients(ifac, bp));
                sp = PolyUtil.<ModInteger> fromIntegerCoefficients(qfac,PolyUtil.integerFromModularCoefficients(ifac, sp));
                tp = PolyUtil.<ModInteger> fromIntegerCoefficients(qfac,PolyUtil.integerFromModularCoefficients(ifac, tp));

                rp = bp.multiply(sp).sum( ap.multiply(tp) ); // order
                System.out.println("\nap     = " + ap);
                System.out.println("bp     = " + bp);
                System.out.println("cp     = " + cp);
                System.out.println("dp     = " + dp);
                System.out.println("sp     = " + sp);
                System.out.println("tp     = " + tp);
                System.out.println("rp     = " + rp);

                System.out.println("a s + b t = c: " + dp.equals(rp));
                //assertEquals("a s + b t = c ", dp,rp);

                GenPolynomialRing<ModInteger> cfac = qfac.contract(1);
                ModInteger vp = qfac.coFac.fromInteger(v.getSymmetricInteger().getVal());
		GenPolynomial<ModInteger> ya = qfac.univariate(0);
                ya = ya.subtract(vp);
                ya = Power.<GenPolynomial<ModInteger>>power(qfac,ya,d);
                System.out.println("ya     = " + ya);
		List<GenPolynomial<ModInteger>> Y = new ArrayList<GenPolynomial<ModInteger>>();
                Y.add(ya); 
                System.out.println("Y      = " + Y);
		Ideal<ModInteger> Yi = new Ideal<ModInteger>(qfac,Y);
                System.out.println("Yi     = " + Yi);
		ResidueRing<ModInteger> Yr = new ResidueRing<ModInteger>(Yi);
                System.out.println("Yr     = " + Yr);

                Residue<ModInteger> apr = new Residue<ModInteger>(Yr,ap);
                Residue<ModInteger> bpr = new Residue<ModInteger>(Yr,bp);
                Residue<ModInteger> cpr = new Residue<ModInteger>(Yr,cp);
                Residue<ModInteger> spr = new Residue<ModInteger>(Yr,sp);
                Residue<ModInteger> tpr = new Residue<ModInteger>(Yr,tp);
                Residue<ModInteger> rpr = bpr.multiply(spr).sum( apr.multiply(tpr) ); // order
                System.out.println("\napr     = " + apr);
                System.out.println("bpr     = " + bpr);
                System.out.println("cpr     = " + cpr);
                System.out.println("spr     = " + spr);
                System.out.println("tpr     = " + rpr);
                System.out.println("rpr     = " + rpr);
                System.out.println("ar sr + br tr = cr: " + cpr.equals(rpr) + "\n");
                assertEquals("ar sr + br tr = cr ", cpr,rpr);
            } catch (NoLiftingException e) {
                fail("" + e);
            }
        }
    }

}
