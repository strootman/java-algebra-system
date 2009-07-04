/*
 * $Id$
 */

package edu.jas.ufd;

import java.util.Map;
import java.util.SortedMap;
import java.util.List;
import java.util.ArrayList;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.log4j.BasicConfigurator;

import edu.jas.kern.ComputerThreads;

import edu.jas.structure.Power;

//import edu.jas.arith.BigInteger;
//import edu.jas.arith.BigRational;
import edu.jas.arith.ModInteger;
import edu.jas.arith.ModIntegerRing;
//import edu.jas.arith.PrimeList;

import edu.jas.poly.ExpVector;
import edu.jas.poly.TermOrder;
import edu.jas.poly.GenPolynomial;
import edu.jas.poly.GenPolynomialRing;
import edu.jas.poly.PolyUtil;


/**
 * Squarefree factorization tests with JUnit.
 * @author Heinz Kredel.
 */

public class SquarefreeModTest extends TestCase {

/**
 * main.
 */
 public static void main (String[] args) {
     //BasicConfigurator.configure();
     junit.textui.TestRunner.run( suite() );
 }


/**
 * Constructs a <CODE>SquarefreeModTest</CODE> object.
 * @param name String.
 */
 public SquarefreeModTest(String name) {
     super(name);
 }


/**
 */ 
 public static Test suite() {
     TestSuite suite= new TestSuite(SquarefreeModTest.class);
     return suite;
 }


    TermOrder to = new TermOrder( TermOrder.INVLEX );

    int rl = 3; 
    int kl = 3;
    int ll = 4;
    int el = 3;
    float q = 0.25f;

    String[] vars;
    String[] cvars;
    String[] c1vars;
    String[] rvars;

    protected void setUp() {
        vars   = ExpVector.STDVARS(rl);
        cvars  = ExpVector.STDVARS(rl-1);
        c1vars = new String[] { cvars[0] };
        rvars  = new String[] { vars[rl-1] };
    }

    protected void tearDown() {
        ComputerThreads.terminate();
    }


/**
 * Test base squarefree.
 * 
 */
 public void testBaseSquarefree() {
     System.out.println("\nbase:");

     ModIntegerRing fac = new ModIntegerRing(19);

     GreatestCommonDivisorAbstract<ModInteger> ufd; 
     //ufd = new GreatestCommonDivisorSubres<ModInteger>();
     //ufd = GCDFactory.<ModInteger> getImplementation(fac);
     ufd = GCDFactory.<ModInteger> getProxy(fac);

     SquarefreeFiniteFieldCharP<ModInteger> sqf = new SquarefreeFiniteFieldCharP<ModInteger>(fac);

     GenPolynomialRing<ModInteger> dfac;
     dfac = new GenPolynomialRing<ModInteger>(fac,1,to,rvars);

     GenPolynomial<ModInteger> a;
     GenPolynomial<ModInteger> b;
     GenPolynomial<ModInteger> c;
     GenPolynomial<ModInteger> d;
     GenPolynomial<ModInteger> e;

     a = dfac.random(kl,ll,el+2,q);
     b = dfac.random(kl,ll,el+2,q);
     c = dfac.random(kl,ll,el,q);
     System.out.println("a  = " + a);
     System.out.println("b  = " + b);
     System.out.println("c  = " + c);

     if ( a.isZERO() || b.isZERO() || c.isZERO() ) {
        // skip for this turn
        return;
     }
         
     // a a b b b c
     d = a.multiply(a).multiply(b).multiply(b).multiply(b).multiply(c);
     c = a.multiply(b).multiply(c);
     System.out.println("d  = " + d);
     System.out.println("c  = " + c);

     c = sqf.baseSquarefreePart(c);
     d = sqf.baseSquarefreePart(d);
     System.out.println("d  = " + d);
     System.out.println("c  = " + c);
     assertTrue("isSquarefree(c) " + c, sqf.isSquarefree(c) );
     assertTrue("isSquarefree(d) " + d, sqf.isSquarefree(d) );

     e = PolyUtil.<ModInteger>basePseudoRemainder(d,c);
     //System.out.println("e  = " + e);
     assertTrue("squarefree(abc) | squarefree(aabbbc) " + e, e.isZERO() );
 }


/**
 * Test base squarefree factors.
 * 
 */
 public void testBaseSquarefreeFactors() {

     ModIntegerRing fac = new ModIntegerRing(19);

     GreatestCommonDivisorAbstract<ModInteger> ufd; 
     //ufd = new GreatestCommonDivisorSubres<ModInteger>();
     ufd = GCDFactory.<ModInteger> getProxy(fac);

     SquarefreeFiniteFieldCharP<ModInteger> sqf = new SquarefreeFiniteFieldCharP<ModInteger>(fac);

     GenPolynomialRing<ModInteger> dfac;
     dfac = new GenPolynomialRing<ModInteger>(fac,1,to,rvars);

     GenPolynomial<ModInteger> a;
     GenPolynomial<ModInteger> b;
     GenPolynomial<ModInteger> c;
     GenPolynomial<ModInteger> d;
     GenPolynomial<ModInteger> e;

     a = dfac.random(kl,ll,el+3,q);
     b = dfac.random(kl,ll,el+3,q);
     c = dfac.random(kl,ll,el+2,q);
     System.out.println("a  = " + a);
     System.out.println("b  = " + b);
     System.out.println("c  = " + c);

     if ( a.isZERO() || b.isZERO() || c.isZERO() ) {
        // skip for this turn
        return;
     }
     
     // a a b b b c
     d = a.multiply(a).multiply(b).multiply(b).multiply(b).multiply(c);
     System.out.println("d  = " + d);

     SortedMap<GenPolynomial<ModInteger>,Long> sfactors;
     sfactors = sqf.baseSquarefreeFactors(d);
     System.out.println("sfactors = " + sfactors);

     assertTrue("isFactorization(d,sfactors) ", sqf.isFactorization(d,sfactors) );
 }


/**
 * Test recursive squarefree.
 * 
 */
 public void testRecursiveSquarefree() {
     System.out.println("\nrecursive:");

     ModIntegerRing fac = new ModIntegerRing(19);

     GreatestCommonDivisorAbstract<ModInteger> ufd; 
     //ufd = new GreatestCommonDivisorSubres<ModInteger>();
     ufd = GCDFactory.<ModInteger> getProxy(fac);

     SquarefreeFiniteFieldCharP<ModInteger> sqf = new SquarefreeFiniteFieldCharP<ModInteger>(fac);

     GenPolynomialRing<ModInteger> cfac;
     GenPolynomialRing<GenPolynomial<ModInteger>> rfac;
     cfac = new GenPolynomialRing<ModInteger>(fac,2-1,to,c1vars);
     rfac = new GenPolynomialRing<GenPolynomial<ModInteger>>(cfac,1,to,rvars);

     GenPolynomial<GenPolynomial<ModInteger>> ar;
     GenPolynomial<GenPolynomial<ModInteger>> br;
     GenPolynomial<GenPolynomial<ModInteger>> cr;
     GenPolynomial<GenPolynomial<ModInteger>> dr;
     GenPolynomial<GenPolynomial<ModInteger>> er;

     ar = rfac.random(kl,ll,el,q);
     br = rfac.random(kl,ll,el,q);
     cr = rfac.random(kl,ll,el,q);
     System.out.println("ar = " + ar);
     System.out.println("br = " + br);
     //System.out.println("cr = " + cr);

     if ( ar.isZERO() || br.isZERO() || cr.isZERO() ) {
        // skip for this turn
        return;
     }

     dr = ar.multiply(ar).multiply(br).multiply(br);
     cr = ar.multiply(br);
     System.out.println("dr  = " + dr);
     System.out.println("cr  = " + cr);

     cr = sqf.recursiveSquarefreePart(cr);
     dr = sqf.recursiveSquarefreePart(dr);
     System.out.println("dr  = " + dr);
     System.out.println("cr  = " + cr);
     assertTrue("isSquarefree(cr) " + cr, sqf.isRecursiveSquarefree(cr) );
     assertTrue("isSquarefree(dr) " + dr, sqf.isRecursiveSquarefree(dr) );

     er = PolyUtil.<ModInteger>recursivePseudoRemainder(dr,cr);
     //System.out.println("er  = " + er);
     assertTrue("squarefree(abc) | squarefree(aabbc) " + er, er.isZERO() );
 }


/**
 * Test recursive squarefree factors.
 * 
 */
 public void testRecursiveSquarefreeFactors() {

     ModIntegerRing fac = new ModIntegerRing(19);

     GreatestCommonDivisorAbstract<ModInteger> ufd; 
     //ufd = new GreatestCommonDivisorSubres<ModInteger>();
     ufd = GCDFactory.<ModInteger> getProxy(fac);

     SquarefreeFiniteFieldCharP<ModInteger> sqf = new SquarefreeFiniteFieldCharP<ModInteger>(fac);

     GenPolynomialRing<ModInteger> cfac;
     GenPolynomialRing<GenPolynomial<ModInteger>> rfac;
     cfac = new GenPolynomialRing<ModInteger>(fac,2-1,to,c1vars);
     rfac = new GenPolynomialRing<GenPolynomial<ModInteger>>(cfac,1,to,rvars);

     GenPolynomial<GenPolynomial<ModInteger>> ar;
     GenPolynomial<GenPolynomial<ModInteger>> br;
     GenPolynomial<GenPolynomial<ModInteger>> cr;
     GenPolynomial<GenPolynomial<ModInteger>> dr;
     GenPolynomial<GenPolynomial<ModInteger>> er;

     ar = rfac.random(kl,3,2,q);
     br = rfac.random(kl,3,2,q);
     cr = rfac.random(kl,3,2,q);
     System.out.println("ar = " + ar);
     System.out.println("br = " + br);
     System.out.println("cr = " + cr);

     if ( ar.isZERO() || br.isZERO() || cr.isZERO() ) {
        // skip for this turn
        return;
     }
         
     dr = ar.multiply(cr).multiply(br).multiply(br);
     System.out.println("dr  = " + dr);

     SortedMap<GenPolynomial<GenPolynomial<ModInteger>>,Long> sfactors;
     sfactors = sqf.recursiveSquarefreeFactors(dr);
     System.out.println("sfactors = " + sfactors);

     assertTrue("isFactorization(d,sfactors) ", sqf.isRecursiveFactorization(dr,sfactors) );
 }


/**
 * Test squarefree.
 * 
 */
 public void testSquarefree() {
     System.out.println("\nfull:");

     ModIntegerRing fac = new ModIntegerRing(19);

     GreatestCommonDivisorAbstract<ModInteger> ufd; 
     //ufd = new GreatestCommonDivisorSubres<ModInteger>();
     ufd = GCDFactory.<ModInteger> getProxy(fac);

     SquarefreeFiniteFieldCharP<ModInteger> sqf = new SquarefreeFiniteFieldCharP<ModInteger>(fac);

     GenPolynomialRing<ModInteger> dfac;
     dfac = new GenPolynomialRing<ModInteger>(fac,rl,to,vars);

     GenPolynomial<ModInteger> a;
     GenPolynomial<ModInteger> b;
     GenPolynomial<ModInteger> c;
     GenPolynomial<ModInteger> d;
     GenPolynomial<ModInteger> e;

     a = dfac.random(kl,ll,2,q);
     b = dfac.random(kl,ll,2,q);
     c = dfac.random(kl,ll,2,q);
     System.out.println("a  = " + a);
     System.out.println("b  = " + b);
     System.out.println("c  = " + c);

     if ( a.isZERO() || b.isZERO() || c.isZERO() ) {
        // skip for this turn
        return;
     }
         
     d = a.multiply(a).multiply(b).multiply(b).multiply(c);
     c = a.multiply(b).multiply(c);
     System.out.println("d  = " + d);
     System.out.println("c  = " + c);

     c = sqf.squarefreePart(c); 
     d = sqf.squarefreePart(d);
     System.out.println("c  = " + c);
     System.out.println("d  = " + d);
     assertTrue("isSquarefree(d) " + d, sqf.isSquarefree(d) );
     assertTrue("isSquarefree(c) " + c, sqf.isSquarefree(c) );

     e = PolyUtil.<ModInteger>basePseudoRemainder(d,c);
     //System.out.println("e  = " + e);

     assertTrue("squarefree(abc) | squarefree(aabbc) " + e, e.isZERO() );
 }


/**
 * Test squarefree factors.
 * 
 */
 public void testSquarefreeFactors() {

     ModIntegerRing fac = new ModIntegerRing(19);

     GreatestCommonDivisorAbstract<ModInteger> ufd; 
     //ufd = new GreatestCommonDivisorSubres<ModInteger>();
     ufd = GCDFactory.<ModInteger> getProxy(fac);

     SquarefreeFiniteFieldCharP<ModInteger> sqf = new SquarefreeFiniteFieldCharP<ModInteger>(fac);

     GenPolynomialRing<ModInteger> dfac;
     dfac = new GenPolynomialRing<ModInteger>(fac,rl,to,vars);

     GenPolynomial<ModInteger> a;
     GenPolynomial<ModInteger> b;
     GenPolynomial<ModInteger> c;
     GenPolynomial<ModInteger> d;
     GenPolynomial<ModInteger> e;

     a = dfac.random(kl,3,2,q);
     b = dfac.random(kl,3,2,q);
     c = dfac.random(kl,3,2,q);
     System.out.println("a  = " + a);
     System.out.println("b  = " + b);
     System.out.println("c  = " + c);

     if ( a.isZERO() || b.isZERO() || c.isZERO() ) {
        // skip for this turn
        return;
     }
         
     d = a.multiply(a).multiply(b).multiply(b).multiply(b).multiply(c);
     System.out.println("d  = " + d);

     SortedMap<GenPolynomial<ModInteger>,Long> sfactors;
     sfactors = sqf.squarefreeFactors(d);
     System.out.println("sfactors = " + sfactors);

     assertTrue("isFactorization(d,sfactors) ", sqf.isFactorization(d,sfactors) );
 }


    /* ------------char-th root ------------------------- */

/**
 * Test base squarefree with char-th root.
 * 
 */
 public void testBaseSquarefreeCharRoot() {
     System.out.println("\nbase CharRoot:");

     long p = 11;
     ModIntegerRing fac = new ModIntegerRing(p);

     GreatestCommonDivisorAbstract<ModInteger> ufd; 
     //ufd = new GreatestCommonDivisorSubres<ModInteger>();
     //ufd = GCDFactory.<ModInteger> getImplementation(fac);
     ufd = GCDFactory.<ModInteger> getProxy(fac);

     SquarefreeFiniteFieldCharP<ModInteger> sqf = new SquarefreeFiniteFieldCharP<ModInteger>(fac);

     GenPolynomialRing<ModInteger> dfac;
     dfac = new GenPolynomialRing<ModInteger>(fac,1,to,rvars);

     GenPolynomial<ModInteger> a;
     GenPolynomial<ModInteger> b;
     GenPolynomial<ModInteger> c;
     GenPolynomial<ModInteger> d;
     GenPolynomial<ModInteger> e;

     a = dfac.random(kl,ll+2,el+2,q);
     b = dfac.random(kl,ll+2,el+2,q);
     c = dfac.random(kl,ll,el,q);
     System.out.println("a  = " + a);
     System.out.println("b  = " + b);
     System.out.println("c  = " + c);

     if ( a.isZERO() || b.isZERO() || c.isZERO() || a.isConstant() || b.isConstant() || c.isConstant() ) {
        // skip for this turn
        return;
     }
         
     // a a b^p c
     d = a.multiply(a).multiply( Power.<GenPolynomial<ModInteger>> positivePower(b, p) ).multiply(c);
     c = a.multiply(b).multiply(c);
     System.out.println("d  = " + d);
     System.out.println("c  = " + c);

     c = sqf.baseSquarefreePart(c);
     d = sqf.baseSquarefreePart(d);
     System.out.println("d  = " + d);
     System.out.println("c  = " + c);
     assertTrue("isSquarefree(c) " + c, sqf.isSquarefree(c) );
     assertTrue("isSquarefree(d) " + d, sqf.isSquarefree(d) );

     e = PolyUtil.<ModInteger>basePseudoRemainder(d,c);
     //System.out.println("e  = " + e);
     assertTrue("squarefree(abc) | squarefree(aab^pc) " + e, e.isZERO() );
 }


/**
 * Test base squarefree factors with char-th root.
 * 
 */
 public void testBaseSquarefreeFactorsCharRoot() {

     long p = 11;
     ModIntegerRing fac = new ModIntegerRing(p);

     GreatestCommonDivisorAbstract<ModInteger> ufd; 
     //ufd = new GreatestCommonDivisorSubres<ModInteger>();
     ufd = GCDFactory.<ModInteger> getProxy(fac);

     SquarefreeFiniteFieldCharP<ModInteger> sqf = new SquarefreeFiniteFieldCharP<ModInteger>(fac);

     GenPolynomialRing<ModInteger> dfac;
     dfac = new GenPolynomialRing<ModInteger>(fac,1,to,rvars);

     GenPolynomial<ModInteger> a;
     GenPolynomial<ModInteger> b;
     GenPolynomial<ModInteger> c;
     GenPolynomial<ModInteger> d;
     GenPolynomial<ModInteger> e;

     a = dfac.random(kl,ll+2,el+3,q);
     b = dfac.random(kl,ll+2,el+3,q);
     c = dfac.random(kl,ll,el+2,q);
     System.out.println("a  = " + a);
     System.out.println("b  = " + b);
     System.out.println("c  = " + c);

     if ( a.isZERO() || b.isZERO() || c.isZERO() || a.isConstant() || b.isConstant() || c.isConstant() ) {
        // skip for this turn
        return;
     }
     
     // a a b^p c
     d = a.multiply(a).multiply( Power.<GenPolynomial<ModInteger>> positivePower(b, p) ).multiply(c);
     System.out.println("d  = " + d);

     SortedMap<GenPolynomial<ModInteger>,Long> sfactors;
     sfactors = sqf.baseSquarefreeFactors(d);
     System.out.println("sfactors = " + sfactors);

     assertTrue("isFactorization(d,sfactors) ", sqf.isFactorization(d,sfactors) );
 }


/**
 * Test recursive squarefree with char-th root.
 * 
 */
 public void testRecursiveSquarefreeCharRoot() {
     System.out.println("\nrecursive CharRoot:");

     long p = 11;
     ModIntegerRing fac = new ModIntegerRing(p);

     GreatestCommonDivisorAbstract<ModInteger> ufd; 
     //ufd = new GreatestCommonDivisorSubres<ModInteger>();
     ufd = GCDFactory.<ModInteger> getProxy(fac);

     SquarefreeFiniteFieldCharP<ModInteger> sqf = new SquarefreeFiniteFieldCharP<ModInteger>(fac);

     GenPolynomialRing<ModInteger> cfac;
     GenPolynomialRing<GenPolynomial<ModInteger>> rfac;
     cfac = new GenPolynomialRing<ModInteger>(fac,2-1,to,c1vars);
     rfac = new GenPolynomialRing<GenPolynomial<ModInteger>>(cfac,1,to,rvars);

     GenPolynomial<GenPolynomial<ModInteger>> ar;
     GenPolynomial<GenPolynomial<ModInteger>> br;
     GenPolynomial<GenPolynomial<ModInteger>> cr;
     GenPolynomial<GenPolynomial<ModInteger>> dr;
     GenPolynomial<GenPolynomial<ModInteger>> er;

     ar = rfac.random(kl,ll,el,q);
     br = rfac.random(kl,ll,el,q);
     cr = rfac.random(kl,ll,el,q);
     System.out.println("ar = " + ar);
     System.out.println("br = " + br);
     //System.out.println("cr = " + cr);

     if ( ar.isZERO() || br.isZERO() || cr.isZERO() || ar.isConstant() || br.isConstant() || cr.isConstant() ) {
        // skip for this turn
        return;
     }

     // a b^p c
     dr = ar.multiply( Power.<GenPolynomial<GenPolynomial<ModInteger>>> positivePower(br, p) ).multiply(cr);
     cr = ar.multiply(br);
     System.out.println("dr  = " + dr);
     System.out.println("cr  = " + cr);

     cr = sqf.recursiveSquarefreePart(cr);
     dr = sqf.recursiveSquarefreePart(dr);
     System.out.println("dr  = " + dr);
     System.out.println("cr  = " + cr);
     assertTrue("isSquarefree(cr) " + cr, sqf.isRecursiveSquarefree(cr) );
     assertTrue("isSquarefree(dr) " + dr, sqf.isRecursiveSquarefree(dr) );

     er = PolyUtil.<ModInteger>recursivePseudoRemainder(dr,cr);
     //System.out.println("er  = " + er);
     assertTrue("squarefree(abc) | squarefree(aabbc) " + er, er.isZERO() );
 }


/**
 * Test recursive squarefree factors with char-th root.
 * 
 */
 public void testRecursiveSquarefreeFactorsCharRoot() {

     long p = 11;
     ModIntegerRing fac = new ModIntegerRing(p);

     GreatestCommonDivisorAbstract<ModInteger> ufd; 
     //ufd = new GreatestCommonDivisorSubres<ModInteger>();
     ufd = GCDFactory.<ModInteger> getProxy(fac);

     SquarefreeFiniteFieldCharP<ModInteger> sqf = new SquarefreeFiniteFieldCharP<ModInteger>(fac);

     GenPolynomialRing<ModInteger> cfac;
     GenPolynomialRing<GenPolynomial<ModInteger>> rfac;
     cfac = new GenPolynomialRing<ModInteger>(fac,2-1,to,c1vars);
     rfac = new GenPolynomialRing<GenPolynomial<ModInteger>>(cfac,1,to,rvars);

     GenPolynomial<GenPolynomial<ModInteger>> ar;
     GenPolynomial<GenPolynomial<ModInteger>> br;
     GenPolynomial<GenPolynomial<ModInteger>> cr;
     GenPolynomial<GenPolynomial<ModInteger>> dr;
     GenPolynomial<GenPolynomial<ModInteger>> er;

     ar = rfac.random(kl,3,2,q);
     br = rfac.random(kl,3,2,q);
     cr = rfac.random(kl,3,2,q);
     System.out.println("ar = " + ar);
     System.out.println("br = " + br);
     System.out.println("cr = " + cr);

     if ( ar.isZERO() || br.isZERO() || cr.isZERO() ) {
        // skip for this turn
        return;
     }

     // a b^p c
     dr = ar.multiply( Power.<GenPolynomial<GenPolynomial<ModInteger>>> positivePower(br, p) ).multiply(cr);
     System.out.println("dr  = " + dr);

     SortedMap<GenPolynomial<GenPolynomial<ModInteger>>,Long> sfactors;
     sfactors = sqf.recursiveSquarefreeFactors(dr);
     System.out.println("sfactors = " + sfactors);

     assertTrue("isFactorization(d,sfactors) ", sqf.isRecursiveFactorization(dr,sfactors) );
 }


/**
 * Test squarefree with char-th root.
 * 
 */
 public void xtestSquarefreeCharRoot() {
     System.out.println("\nfull CharRoot:");

     long p = 11;
     ModIntegerRing fac = new ModIntegerRing(p);

     GreatestCommonDivisorAbstract<ModInteger> ufd; 
     //ufd = new GreatestCommonDivisorSubres<ModInteger>();
     ufd = GCDFactory.<ModInteger> getProxy(fac);

     SquarefreeFiniteFieldCharP<ModInteger> sqf = new SquarefreeFiniteFieldCharP<ModInteger>(fac);

     GenPolynomialRing<ModInteger> dfac;
     dfac = new GenPolynomialRing<ModInteger>(fac,rl,to,vars);

     GenPolynomial<ModInteger> a;
     GenPolynomial<ModInteger> b;
     GenPolynomial<ModInteger> c;
     GenPolynomial<ModInteger> d;
     GenPolynomial<ModInteger> e;

     a = dfac.random(kl,ll,2,q);
     b = dfac.random(kl,ll,2,q);
     c = dfac.random(kl,ll,2,q);
     System.out.println("a  = " + a);
     System.out.println("b  = " + b);
     System.out.println("c  = " + c);

     if ( a.isZERO() || b.isZERO() || c.isZERO() ) {
        // skip for this turn
        return;
     }
         
     // a a b^p c
     d = a.multiply(a).multiply( Power.<GenPolynomial<ModInteger>> positivePower(b, p) ).multiply(c);
     c = a.multiply(b).multiply(c);
     System.out.println("d  = " + d);
     System.out.println("c  = " + c);

     c = sqf.squarefreePart(c); 
     d = sqf.squarefreePart(d);
     System.out.println("c  = " + c);
     System.out.println("d  = " + d);
     assertTrue("isSquarefree(d) " + d, sqf.isSquarefree(d) );
     assertTrue("isSquarefree(c) " + c, sqf.isSquarefree(c) );

     e = PolyUtil.<ModInteger>basePseudoRemainder(d,c);
     //System.out.println("e  = " + e);

     assertTrue("squarefree(abc) | squarefree(aab^pc) " + e, e.isZERO() );
 }


/**
 * Test squarefree factors with char-th root.
 * 
 */
 public void xtestSquarefreeFactorsCharRoot() {

     long p = 11;
     ModIntegerRing fac = new ModIntegerRing(p);

     GreatestCommonDivisorAbstract<ModInteger> ufd; 
     //ufd = new GreatestCommonDivisorSubres<ModInteger>();
     ufd = GCDFactory.<ModInteger> getProxy(fac);

     SquarefreeFiniteFieldCharP<ModInteger> sqf = new SquarefreeFiniteFieldCharP<ModInteger>(fac);

     GenPolynomialRing<ModInteger> dfac;
     dfac = new GenPolynomialRing<ModInteger>(fac,rl,to,vars);

     GenPolynomial<ModInteger> a;
     GenPolynomial<ModInteger> b;
     GenPolynomial<ModInteger> c;
     GenPolynomial<ModInteger> d;
     GenPolynomial<ModInteger> e;

     a = dfac.random(kl,3,2,q);
     b = dfac.random(kl,3,2,q);
     c = dfac.random(kl,3,2,q);
     System.out.println("a  = " + a);
     System.out.println("b  = " + b);
     System.out.println("c  = " + c);

     if ( a.isZERO() || b.isZERO() || c.isZERO() ) {
        // skip for this turn
        return;
     }
         
     // a a b^p c
     d = a.multiply(a).multiply( Power.<GenPolynomial<ModInteger>> positivePower(b, p) ).multiply(c);
     System.out.println("d  = " + d);

     SortedMap<GenPolynomial<ModInteger>,Long> sfactors;
     sfactors = sqf.squarefreeFactors(d);
     System.out.println("sfactors = " + sfactors);

     assertTrue("isFactorization(d,sfactors) ", sqf.isFactorization(d,sfactors) );
 }

}