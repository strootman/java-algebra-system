/*
 * $Id$
 */

package edu.jas.integrate;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;

import edu.jas.arith.BigRational;
import edu.jas.kern.ComputerThreads;
import edu.jas.poly.GenPolynomial;
import edu.jas.poly.GenPolynomialRing;
import edu.jas.poly.PolyUtil;
import edu.jas.poly.TermOrder;
import edu.jas.structure.GcdRingElem;
import edu.jas.structure.Power;
import edu.jas.structure.RingFactory;
import edu.jas.ufd.GCDFactory;
import edu.jas.ufd.GreatestCommonDivisorAbstract;
import edu.jas.ufd.SquarefreeAbstract;
import edu.jas.ufd.SquarefreeFactory;

/**
 * Methods related to elementary integration. For example Hermite reduction.
 * 
 * @author Axel Kramer
 * @param <C>
 *          coefficient type
 */

public class ElementaryIntegration<C extends GcdRingElem<C>> {

  /**
   * Engine for squarefree decomposition.
   */
  public final SquarefreeAbstract<C> sqf;

  /**
   * Engine for greatest common divisors.
   */
  public final GreatestCommonDivisorAbstract<C> ufd;

  /**
   * Constructor.
   */
  public ElementaryIntegration(RingFactory<C> br) {
    ufd = GCDFactory.<C> getProxy(br);
    sqf = SquarefreeFactory.<C> getImplementation(br);
  }

  /**
   * Hermite reduction step.
   * 
   * @param a
   *          numerator
   * @param d
   *          denominator, gcd(a,d) == 1
   * @return [ [ gn_i, gd_i ], [ h0, hn_j, hd_j ] ] such that integrate(a/d) =
   *         sum_i(gn_i/gd_i) + integrate(h0) + sum_j( integrate(hn_j/hd_j) )
   */
  public List<GenPolynomial<C>>[] integrateHermite(GenPolynomial<C> a,
      GenPolynomial<C> d) {
    GenPolynomialRing<C> fac = d.ring;

    SortedMap<GenPolynomial<C>, Long> sfactors;
    sfactors = sqf.squarefreeFactors(d);
    List<GenPolynomial<C>> Di = new ArrayList<GenPolynomial<C>>();
    List<GenPolynomial<C>> DPower = new ArrayList<GenPolynomial<C>>();
    long maxE = 0;
    for (GenPolynomial<C> f : sfactors.keySet()) {
      long e = sfactors.get(f).longValue();
      if (e>maxE){
        maxE = e;
      }
    }
    for (int i = 0; i < maxE; i++) {
      Di.add(fac.getONE());
      DPower.add(fac.getONE());
    }
    for (GenPolynomial<C> f : sfactors.keySet()) {
      long e = sfactors.get(f).longValue();
      Di.set((int) e-1, f);
      DPower.set((int) e-1, Power.<GenPolynomial<C>> positivePower(f, e));
    }

    System.out.println("D_i: " + Di.toString());
    System.out.println(DPower);
    List<GenPolynomial<C>> Ai = ufd.basePartialFraction(a, DPower);
    System.out.println("A_i: " + Ai.toString());
    List<GenPolynomial<C>> G = new ArrayList<GenPolynomial<C>>();
    List<GenPolynomial<C>> H = new ArrayList<GenPolynomial<C>>();
    H.add(Ai.get(0)); // P
    if (!Ai.get(1).isZERO()) {
      H.add(Ai.get(1)); // A_1
      H.add(Di.get(0)); // D_1
    }
    GenPolynomial<C> v;
    GenPolynomial<C> vj;
    GenPolynomial<C> DV_dx;
    GenPolynomial<C> b;
    GenPolynomial<C> c;
    int n = Di.size();
    for (int k = 2; k <= n; k++) {
      if (Di.get(k - 1).degree(0) > 0) {
        v = Di.get(k - 1);
        for (int j = k - 1; j >= 1; j--) {
          System.out.println("Step(" + k + "," + j + ")");
          System.out.println("V:" + v.toString());
          DV_dx = PolyUtil.<C> baseDeriviative(v);
          GenPolynomial<C>[] BC = ufd.baseGcdDiophant(DV_dx, v, Ai.get(k)
              .negate().divide(fac.fromInteger(j)));
          b = BC[0];
          c = BC[1];
          vj = Power.<GenPolynomial<C>> positivePower(v, j);
          G.add(b); // B
          G.add(vj); // v^j
          Ai.set(k, fac.fromInteger(j).negate().multiply(c).subtract(
              PolyUtil.<C> baseDeriviative(b)));
          System.out.println("B:" + b.toString());
          System.out.println("C:" + c.toString());
        }
        if (!Ai.get(k).isZERO()) {
          H.add(Ai.get(k)); // A_k
          H.add(v); // v
        }
      }
    }
    List<GenPolynomial<C>>[] ret = new List[2];
    ret[0] = G;
    ret[1] = H;
    return ret;
  }

  /**
   * Test program.
   * 
   * @param args
   */
  public static void main(String[] args) {

    BigRational br = new BigRational(0);
    String[] vars = new String[] { "x" };
    GenPolynomialRing<BigRational> fac;
    fac = new GenPolynomialRing<BigRational>(br, vars.length, new TermOrder(
        TermOrder.INVLEX), vars);

    ElementaryIntegration<BigRational> eIntegrator = new ElementaryIntegration<BigRational>(
        br);

    GenPolynomial<BigRational> a = fac.parse("x^7 - 24 x^4 - 4 x^2 + 8 x - 8");
    System.out.println("A: " + a.toString());
    GenPolynomial<BigRational> d = fac.parse("x^8 + 6 x^6 + 12 x^4 + 8 x^2");
    System.out.println("D: " + d.toString());
    GenPolynomial<BigRational> gcd = a.gcd(d);
    System.out.println("GCD: " + gcd.toString());
    List<GenPolynomial<BigRational>>[] ret = eIntegrator.integrateHermite(a, d);
    System.out.println("Result: " + ret[0] + " , " + ret[1]);

    System.out.println("-----");

    a = fac.parse("10 x^2 - 63 x + 29");
    System.out.println("A: " + a.toString());
    d = fac.parse("x^3 - 11 x^2 + 40 x -48");
    System.out.println("D: " + d.toString());
    gcd = a.gcd(d);
    System.out.println("GCD: " + gcd.toString());
    ret = eIntegrator.integrateHermite(a, d);
    System.out.println("Result: " + ret[0] + " , " + ret[1]);

    System.out.println("-----");

    a = fac.parse("x+3");
    System.out.println("A: " + a.toString());
    d = fac.parse("x^2 - 3 x - 40");
    System.out.println("D: " + d.toString());
    gcd = a.gcd(d);
    System.out.println("GCD: " + gcd.toString());
    ret = eIntegrator.integrateHermite(a, d);
    System.out.println("Result: " + ret[0] + " , " + ret[1]);

    System.out.println("-----");

    a = fac.parse("10 x^2+12 x + 20");
    System.out.println("A: " + a.toString());
    d = fac.parse("x^3 - 8");
    System.out.println("D: " + d.toString());
    gcd = a.gcd(d);
    System.out.println("GCD: " + gcd.toString());
    ret = eIntegrator.integrateHermite(a, d);
    System.out.println("Result: " + ret[0] + " , " + ret[1]);

    System.out.println("-----");
    ComputerThreads.terminate();
  }
}