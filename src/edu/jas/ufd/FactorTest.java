/*
 * $Id$
 */

package edu.jas.ufd;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.SortedMap;

import edu.jas.kern.ComputerThreads;

import edu.jas.arith.BigInteger;
import edu.jas.arith.ModInteger;
import edu.jas.arith.ModIntegerRing;
import edu.jas.arith.BigRational;
import edu.jas.arith.BigComplex;

import edu.jas.poly.TermOrder;
import edu.jas.poly.GenPolynomial;
import edu.jas.poly.GenPolynomialRing;
import edu.jas.poly.PolyUtil;
import edu.jas.poly.ExpVector;
import edu.jas.poly.AlgebraicNumberRing;
import edu.jas.poly.AlgebraicNumber;


/**
 * Factor tests with JUnit.
 * @author Heinz Kredel.
 */

public class FactorTest extends TestCase {

/**
 * main.
 */
   public static void main (String[] args) {
          junit.textui.TestRunner.run( suite() );
   }

/**
 * Constructs a <CODE>PolyUtilTest</CODE> object.
 * @param name String.
 */
   public FactorTest(String name) {
          super(name);
   }

/**
 */ 
 public static Test suite() {
     TestSuite suite= new TestSuite(FactorTest.class);
     return suite;
   }

   int rl = 3; 
   int kl = 5;
   int ll = 5;
   int el = 3;
   float q = 0.3f;

   protected void setUp() {
   }

   protected void tearDown() {
   }


/**
 * Test modular factorization.
 * 
 */
 public void xtestModularFactorization() {

     TermOrder to = new TermOrder(TermOrder.INVLEX);
     ModIntegerRing cfac = new ModIntegerRing(13);
     GenPolynomialRing<ModInteger> pfac = new GenPolynomialRing<ModInteger>(cfac,1,to);
     FactorModular fac = new FactorModular();

     for ( int i = 0; i < 8; i++ ) {
         GenPolynomial<ModInteger> a = pfac.random(kl,ll*(i+1),el*(i+1),q);
         if ( a.isConstant() ) {
             continue;
         }
         System.out.println("\na = " + a);

         SortedMap<GenPolynomial<ModInteger>,Integer> sm = fac.baseFactors( a );
         System.out.println("sm = " + sm);

         boolean t = fac.isFactorization( a, sm );
         //System.out.println("t        = " + t);
         assertTrue("prod(factor(a)) = a",t);
     }
 }


/**
 * Test multivariate modular factorization.
 * 
 */
 public void xtestMultivariateModularFactorization() {

     TermOrder to = new TermOrder(TermOrder.INVLEX);
     ModIntegerRing cfac = new ModIntegerRing(13);
     GenPolynomialRing<ModInteger> pfac = new GenPolynomialRing<ModInteger>(cfac,rl,to);
     FactorModular fac = new FactorModular();

     for ( int i = 0; i < 6; i++ ) {
         GenPolynomial<ModInteger> a = pfac.random(kl,ll*(i+1),el,q);
         if ( a.isConstant() ) {
             continue;
         }
         System.out.println("\na = " + a);

         SortedMap<GenPolynomial<ModInteger>,Integer> sm = fac.factors( a );
         System.out.println("sm = " + sm);

         boolean t = fac.isFactorization( a, sm );
         //System.out.println("t        = " + t);
         assertTrue("prod(factor(a)) = a",t);
     }
     ComputerThreads.terminate();
 }


/**
 * Test integer factorization.
 * 
 */
 public void xtestIntegerFactorization() {

     TermOrder to = new TermOrder(TermOrder.INVLEX);
     BigInteger cfac = new BigInteger(4);
     GenPolynomialRing<BigInteger> pfac = new GenPolynomialRing<BigInteger>(cfac,1,to);
     FactorInteger fac = new FactorInteger();

     for ( int i = 1; i < 6; i++ ) {
         GenPolynomial<BigInteger> a = pfac.random(kl,ll*(i+1),el*(i+1),q);
         if ( ! a.leadingBaseCoefficient().isUnit() ) {
             //continue;
             ExpVector e = a.leadingExpVector();
             //a.doPutToMap(e,cfac.getONE());
             //a.doPutToMap(e,cfac.negate());
             //a = a.multiply( cfac );
         }
         System.out.println("\na = " + a);

         SortedMap<GenPolynomial<BigInteger>,Integer> sm = fac.baseFactors( a );
         System.out.println("sm = " + sm);

         boolean t = fac.isFactorization( a, sm );
         //System.out.println("t        = " + t);
         assertTrue("prod(factor(a)) = a",t);
     }
 }


/**
 * Test multivariate integer factorization.
 * 
 */
 public void xtestMultivariateIntegerFactorization() {

     TermOrder to = new TermOrder(TermOrder.INVLEX);
     BigInteger cfac = new BigInteger(1);
     GenPolynomialRing<BigInteger> pfac = new GenPolynomialRing<BigInteger>(cfac,rl,to);
     FactorInteger fac = new FactorInteger();

     for ( int i = 1; i < 6; i++ ) {
         GenPolynomial<BigInteger> a = pfac.random(kl,ll*(i+1),el,q);
         if ( false && ! a.leadingBaseCoefficient().isUnit() ) {
             //continue;
             //ExpVector e = a.leadingExpVector();
             //a.doPutToMap(e,cfac.getONE());
         }
         System.out.println("\na = " + a);

         SortedMap<GenPolynomial<BigInteger>,Integer> sm = fac.factors( a );
         System.out.println("sm = " + sm);

         boolean t = fac.isFactorization( a, sm );
         //System.out.println("t        = " + t);
         assertTrue("prod(factor(a)) = a",t);
     }
     ComputerThreads.terminate();
 }


/**
 * Test rational factorization.
 * 
 */
 public void xtestRationalFactorization() {

     TermOrder to = new TermOrder(TermOrder.INVLEX);
     BigRational cfac = new BigRational(1);
     GenPolynomialRing<BigRational> pfac = new GenPolynomialRing<BigRational>(cfac,1,to);
     FactorRational fac = new FactorRational();

     for ( int i = 1; i < 3; i++ ) {
         int facs = 0;
         GenPolynomial<BigRational> a;
         GenPolynomial<BigRational> c = pfac.random(kl,ll*(i+1),el+(i+1),q).monic();
         GenPolynomial<BigRational> b = pfac.random(kl,ll,el+1,q).monic();
         //         if ( false && ! a.leadingBaseCoefficient().isONE() ) {
             //continue;
             //ExpVector e = a.leadingExpVector();
             //a.doPutToMap(e,cfac.getONE());
         //}
         if ( c.degree() > 0 ) {
             facs++;
         }
         if ( b.degree() > 0 ) {
             facs++;
         }
         a = c.multiply( b );
         System.out.println("\na = " + a);
         System.out.println("b = " + b);
         System.out.println("c = " + c);

         SortedMap<GenPolynomial<BigRational>,Integer> sm = fac.baseFactors( a );
         System.out.println("sm = " + sm);
         assertTrue("#facs < " + facs , sm.size() >= facs );

         boolean t = fac.isFactorization( a, sm );
         //System.out.println("t        = " + t);
         assertTrue("prod(factor(a)) = a",t);
     }
 }


/**
 * Test algebraic factorization.
 * 
 */
 public void testAlgebraicFactorization() {

     TermOrder to = new TermOrder(TermOrder.INVLEX);
     BigRational cfac = new BigRational(1);
     String[] vars = new String[] { "alpha" };
     GenPolynomialRing<BigRational> pfac = new GenPolynomialRing<BigRational>(cfac,1,to,vars);
     GenPolynomial<BigRational> agen = pfac.univariate(0,2);
     agen = agen.sum( pfac.getONE() ); // x^2 + 1
     AlgebraicNumberRing<BigRational> afac = new AlgebraicNumberRing<BigRational>(agen,true);
     GenPolynomialRing<AlgebraicNumber<BigRational>> apfac 
         = new GenPolynomialRing<AlgebraicNumber<BigRational>>(afac,1,to); // univariate

     System.out.println("agen  = " + agen);
     System.out.println("afac  = " + afac);
     System.out.println("apfac = " + apfac);

     FactorAlgebraic<BigRational> fac = new FactorAlgebraic<BigRational>();

     for ( int i = 1; i < 2; i++ ) {
         int facs = 0;
         GenPolynomial<AlgebraicNumber<BigRational>> a;
         GenPolynomial<AlgebraicNumber<BigRational>> c = apfac.random(kl,ll*(i+1),el+(i+1),q).monic();
         GenPolynomial<AlgebraicNumber<BigRational>> b = apfac.random(kl,ll,el+1,q).monic();
         //         if ( false && ! a.leadingBaseCoefficient().isONE() ) {
             //continue;
             //ExpVector e = a.leadingExpVector();
             //a.doPutToMap(e,cfac.getONE());
         //}
         if ( c.degree() > 0 ) {
             facs++;
         }
         if ( b.degree() > 0 ) {
             facs++;
         }
         a = c; //c.multiply( b );
         System.out.println("\na = " + a);
         System.out.println("b = " + b);
         System.out.println("c = " + c);

         SortedMap<GenPolynomial<AlgebraicNumber<BigRational>>,Integer> sm = fac.baseFactors( a );
         System.out.println("sm = " + sm);
         //assertTrue("#facs < " + facs , sm.size() >= facs );

         //boolean t = fac.isFactorization( a, sm );
         //System.out.println("t        = " + t);
         //assertTrue("prod(factor(a)) = a",t);
         ComputerThreads.terminate();
     }
 }

}
